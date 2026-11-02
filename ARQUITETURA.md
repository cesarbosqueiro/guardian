# Guardian — Launcher de PvP com Anti-Cheat Client-Side

Especificação de arquitetura. **Perfil: launcher de PvP.** Tudo embutido, UI
própria, e um agente client-side que ajuda o servidor a achar problemas sem
custar FPS.

**Alvo:** cliente Minecraft 1.8.9 (SHA-1 `3870888a6c3d349d3771a3e9d16c9bf5e076b908`),
servidor Spigot/Paper 1.8.8, JRE 8 portátil embutido — mesmo cenário de
[`../launcher-src`](../launcher-src).

---

## 0. O orçamento manda no escopo

Num launcher de PvP, o jogador não compara detecção de cheat — compara FPS e
latência de input. Se o anti-cheat custar 3% de frame time ou um hitch por
minuto, ele troca de launcher e você perde a telemetria inteira. **O orçamento
de performance não é uma meta, é o requisito que define quais sensores
existem.**

### 0.1 Orçamento

| Recurso | Limite | Verificação |
|---|---|---|
| CPU na thread do jogo | < 0,01% | o agente não aparece no top 50 de um profiler |
| Alocação no caminho de input/render | **0 bytes** | ring buffer pré-alocado, tipos primitivos |
| Frame time adicionado | < 0,05 ms no p99 | benchmark com/sem agente, mesma cena |
| Hitch em combate | **nenhum** | zero `retransformClasses`, zero GL query, zero I/O na thread do jogo |
| Memória residente | < 64 KB | `long[2400]` = 19 KB + agregados |
| Banda em regime | < 200 B/s | ~0,7 MB/hora |
| RTT adicionado | 0 | canal de controle é assíncrono, nunca bloqueia tick |

### 0.2 O que foi cortado e por quê

Seis coisas da versão anterior deste documento saíram. Cada corte tem um
motivo de engenharia, não de escopo:

| Cortado | Custo que tinha | Por que dá para cortar |
|---|---|---|
| **Write barrier em `motionX/Y/Z`** | campo quentíssimo, escrito várias vezes por tick por entidade → variância de frame time | anti-KB é detectado server-side pelo deslocamento pós-hit, que é uma das checagens mais confiáveis que existem |
| **Replay de física com input completo por tick** | banda + CPU **do servidor** por jogador em combate | fly/speed são detectáveis server-side sem ajuda; fly em PvP é obvio na simulação |
| **Amostragem de estado GL** (`glGetIntegerv`, `glIsEnabled`) | força sync de pipeline em vários drivers → stutter | num servidor de PvP, X-ray é irrelevante; e fullbright sai de um field read |
| **Análise de opacidade de textura no startup** | decodificar PNGs = CPU + MBs de alocação transitória | hash do pack basta; a análise vira comando sob demanda |
| **Canal TLS de dados separado** | porta extra, alerta de firewall, certificado, sistema de token, uma classe de transporte inteira | captura chunked pelo canal de controle a 1 chunk/tick é invisível |
| **Re-desafio de bytecode mid-game** | `retransformClasses` deotimiza e re-JITa → hitch garantido | re-atestar só em tela de loading (§7.4) dá o mesmo efeito de graça |

Resultado: o custo em regime caiu de ~2 KB/s + 8 MB + queries GL para
**~200 B/s + 19 KB + nenhuma chamada GL**. O que o servidor perde está
declarado em §10.

### 0.3 O que sobrou

Nove sensores. Os três primeiros entregam a maior parte do valor:

| # | Sensor | Custo em regime | Único do cliente? |
|---|---|---|---|
| 1 | Heartbeat | ~0 | sim |
| 2 | Atestação de ambiente (no loading) | 0 em regime | sim |
| 3 | **Registro por frame: mouse + rotação + clique** | ~20 ns/frame, 0 alloc | **sim — é o núcleo** |
| 4 | Multi-aura (alvos/tick) | **0 — é server-side puro** | não |
| 5 | Ataque sem clique | ~0 | sim |
| 6 | Hash de resource packs | 50 ms uma vez | sim |
| 7 | Velocidade recebida (metade barata do KB) | ~0 | sim |
| 8 | Gamma / fullbright | 1 field read/s | sim |
| 9 | Captura de framebuffer sob demanda | 1 hitch, sob demanda | sim |

---

## 1. A premissa

O cliente roda na máquina do adversário. Qualquer verificação no mesmo processo
que o cheat pode ser desligada por ele. Se o cliente manda
`{"veredito":"limpo"}`, um cliente patcheado manda o mesmo.

> **O cliente é um sensor. O servidor é o juiz.**
>
> O cliente coleta observações que o servidor não consegue obter. O servidor
> cruza com o que ele mesmo observou. **Divergência entre as duas fontes** é o
> sinal — nunca o relato do cliente.

Isso reposiciona cada item da lista original:

| Detecção | Onde a checagem fica | O que o cliente agrega |
|---|---|---|
| Fly, speed, step, nofall, reach | **100% servidor** | **nada** (cortado, §0.2) |
| Anti-knockback | **servidor** (deslocamento pós-hit) | "recebi a velocidade no tick T" |
| Multi-aura | **servidor** (alvos/tick) | nada |
| **Kill-aura** | servidor + **cliente** | **proveniência da rotação** ← o valor real |
| Autoclicker | **cliente** | intervalo entre cliques físicos |
| X-ray | **servidor** (Orebfuscator) | hash do pack |
| Mods injetados | **cliente** | classloaders, agentes, bytecode |
| Screenshare | **cliente** | framebuffer do jogo |

### 1.1 O que o client-side realmente compra

1. **Proveniência do input.** O servidor vê a rotação; só o cliente vê o
   `Mouse.getDX()` que a produziu. Num cliente vanilla a relação é
   determinística (§8.3). Inacessível ao servidor, e é o que pega kill-aura.
2. **Atestação do ambiente.** Classes, classloaders, agentes, packs.
3. **Custo.** O cheater passa a precisar fazer o cliente mentir de forma
   internamente consistente. Caro — e a maior parte do público usa client
   pronto, que não vai ter esse trabalho.

### 1.2 O que não compra

Não impede nada (impede-se no servidor), não resolve o cheater dedicado
(resolve o volume), e **não justifica driver de kernel ou varredura de
memória** — isso destruiria a proposta do launcher portátil e trocaria um
problema de cheat por um de suporte e exposição jurídica. §11.

---

## 2. Modelo de ameaça

| Perfil | População | Capacidade | Vence o Guardian? |
|---|---|---|---|
| **T0** client pronto (Wurst, Impact, Sigma free) | ~85% | usa o que baixou | Não. Atestação pega em segundos. |
| **T1** client pronto + vanilla/launcher próprio | ~10% | sabe não usar nosso launcher | Não entra: sem atestação → modo restrito (§10.3) |
| **T2** cheat pago com bypass genérico | ~4% | hooka APIs, fabrica input | Parcialmente — a correlação cruzada (§9) é o que pega |
| **T3** engenharia reversa do Guardian | <1% | reescreve o agente | Não. Aceite e invista em §9 e §10.3. |

**Otimize para T0/T1/T2.** Esforço contra T3 tem retorno quase nulo; esforço em
correlação cruzada pega T2 e endurece contra T3 de graça.

| Evasão | Contramedida |
|---|---|
| Não usar o launcher | Modo restrito (§10.3) |
| Remover o `-javaagent` | Sem agente, sem atestação → modo restrito |
| Matar a thread de transporte | Heartbeat ausente é finding (§7.5) |
| Fabricar deltas de mouse | Coerência com o yaw observado (§8.3.2, checagem 3) |
| Hashes pré-computados | Nonce do servidor saliza cada digest (§7.2) |
| Replay de sessão limpa | HMAC com chave de sessão + nonce |
| Patchear classe e hashear os bytes originais | Digest vem do `Instrumentation`, não do disco |
| Injetar cheat **depois** do handshake | Re-atestação em tela de loading (§7.4) |

---

## 3. Privacidade — requisito, não apêndice

Servidor brasileiro, público com menores, captura de tela. É tratamento de dado
pessoal sob a LGPD, e isso molda o desenho dos sensores.

1. **Divulgação antes da coleta**, com aceite registrado (§5.2).
2. **Minimização.** Cada sensor coleta o mínimo para sua função. O perfil PvP
   ajuda aqui: cortar sensores é cortar coleta.
3. **Captura nunca é do desktop.** Padrão é o framebuffer do jogo (§8.6).
   Captura de janela só sob gatilho de staff, com aviso visível.
4. **Nunca capturar com o jogo fora de foco.** `Display.isActive() == false` →
   aborta e registra o abort.
5. **Retenção:** telemetria 7 dias, capturas 72 h, findings 180 dias. Expurgo
   automático.
6. **Acesso auditado.** Toda visualização de captura grava quem, quando, qual
   ticket.
7. **Proibido coletar**, sempre: digitação fora da janela do jogo, lista de
   processos, janelas de outros apps, clipboard, navegação, HWID (§3.2).
8. **Menores:** o aceite menciona ciência do responsável.

### 3.1 Texto de aceite

```
ANTI-CHEAT DO SERVIDOR

Para jogar na CraftLandia, este launcher ativa um componente de
anti-cheat dentro do jogo. Enquanto você estiver conectado, ele envia
ao servidor:

  - quais mods e resource packs estão carregados no jogo;
  - seu mouse, teclado e rotação DENTRO do jogo;
  - informações do jogo: versão, placa de vídeo, resolução.

Se um membro da equipe abrir uma verificação, ele pode solicitar uma
imagem do que o SEU JOGO está renderizando. Você verá um aviso na tela
quando isso acontecer.

O componente NÃO acessa: outros programas, seus arquivos, o que você
digita fora do jogo, sua navegação, nem a tela inteira do computador.

Os dados ficam no servidor por até 7 dias (90 se houver punição) e são
acessados apenas pela equipe de moderação.

Se você tem menos de 18 anos, confirme com seu responsável.

                                   [ Não aceito ]  [ Aceito e jogar ]
```

"Não aceito" fecha o launcher — é condição de acesso a um serviço privado,
declarada antes.

### 3.2 Por que não coletar HWID

Identificador persistente de dispositivo: maior peso de LGPD da lista, quebra a
cada troca de hardware (falso positivo em jogador legítimo), e é trivialmente
spoofável pelo T2 que você quer pegar. Custo alto, benefício baixo. Para
continuidade de identidade use a conta do servidor, que você já controla.

---

## 4. Arquitetura

```
┌──────────────────────── máquina do jogador ───────────────────────┐
│  Launcher (Swing, JRE 8 embutido)                                 │
│   1. verifica integridade pinada por SHA-1                        │
│   2. obtém aceite do anti-cheat                                   │
│   3. lança com -javaagent                                         │
│                      │                                            │
│                      ▼  ProcessBuilder                            │
│  ┌──────────────────────────────────────────────────┐             │
│  │ JVM do jogo                                      │             │
│  │   Minecraft 1.8.9 vanilla ◄── 3 hooks ──► agente │             │
│  │                                   │              │             │
│  │                        ring buffer long[2400]    │             │
│  │                                   │              │             │
│  │                        thread daemon (agrega)    │             │
│  └───────────────────────────────────┼──────────────┘             │
└──────────────────────────────────────┼────────────────────────────┘
                                       │
              canal único: plugin message "CL|Guard"
              (dentro da conexão do jogo, já autenticada)
                                       ▼
┌────────────────────────────── servidor ───────────────────────────┐
│  guardian-plugin (Paper 1.8.8)                                    │
│   ├─ handshake / nonce / desafio                                  │
│   ├─ checagens vanilla server-side  ← AUTORIDADE                  │
│   └─ Correlacionador: relato do cliente × observação do servidor   │
│         ├─ proveniência de rotação  (§8.3)  ← o núcleo             │
│         ├─ multi-aura / ataque sem clique                         │
│         ├─ coerência de knockback                                 │
│         └─ allowlist de packs/mods                                │
│                          │                                        │
│                          ▼  findings → pontuação → ação           │
└───────────────────────────────────────────────────────────────────┘
```

Três artefatos, **um** canal de transporte:

| Artefato | Onde roda | Responsabilidade |
|---|---|---|
| `CraftLandia.jar` | jogador | UI, integridade, aceite, lançar com o agente |
| `guardian-agent.jar` | JVM do jogo | 3 hooks, ring buffer, transporte |
| `guardian-plugin.jar` | servidor | handshake, correlação, decisão, ação |

---

## 5. O Launcher

Deriva de [`../launcher-src`](../launcher-src) — `Core`, `Ui`, `Sprites`,
`Json`, `Ini`, `Log`. Três mudanças.

### 5.1 Integridade pinada (fatal)

O launcher reconstruído checa só existência. Aqui precisa checar identidade,
porque o agente hooka classes por nome obfuscado e isso só vale para um build
exato.

```java
/**
 * Verifica cada artefato contra o SHA-1 declarado no json de versao.
 *
 * Aqui a falha e FATAL, diferente do launcher original: se o cliente nao e
 * bit-a-bit o esperado, os mapeamentos obfuscados do agente nao valem e o
 * anti-cheat produziria falso positivo em massa.
 *
 * Excecao: allowlist assinada pelo servidor, para OptiFine e mods
 * permitidos (§12).
 */
public void verificarPinado(Object versaoJson) throws Falha { ... }
```

### 5.2 Aceite

Entre `detectar()` e a janela principal, na primeira execução ou quando a
versão do texto muda:

```ini
[guardian]
aceite_versao=3
aceite_em=2026-11-02T14:22:08-03:00
aceite_hash=8f14e45fceea167a5a36dedd4bea2543
```

`aceite_hash` = SHA-256 do texto exibido. Prova *qual* texto foi aceito.

### 5.3 Flags de JVM: o launcher ajuda o FPS

Num launcher de PvP vale explicitar: as flags que o `Core` já monta são parte
do produto. O orçamento do §0.1 existe para **não gastar** o FPS que essas
flags entregam.

```java
cmd.add("-javaagent:" + agentJar + "=" + configAgente);
```

O agente vem antes de `-cp`: o `premain` roda antes do `main`, e os
transformers precisam estar registrados antes de qualquer classe do jogo
carregar.

`configAgente` é string curta, não arquivo (arquivo seria editável sem quebrar
hash):

```
srv=guard.craftlandia.com.br,ver=1.8.9,sha=3870888a6c3d...,pol=12
```

### 5.4 O launcher não é a raiz de confiança

Está na máquina do jogador: dá para descompilar e editar. Então **não** embuta
chave privada nem segredo de HMAC; **não** deixe o launcher decidir se o
jogador passou. A raiz de confiança é a **conexão autenticada do jogo** — o
jogador já fez `/login`. Toda chave de sessão deriva daí (§7.1).

---

## 6. O Agente

### 6.1 Ciclo de vida

```
Premain-Class: br.com.craftlandia.guardian.Agent
Can-Retransform-Classes: true
```

```java
public static void premain(String args, Instrumentation inst) {
    try {
        Config cfg = Config.parse(args);
        Mapa map = Mapa.para(cfg.clientSha1());         // §6.2
        if (map == null) {
            Estado.degradar("build desconhecido: " + cfg.clientSha1());
            return;    // nao instrumenta nada; o servidor decide
        }
        inst.addTransformer(new GuardianTransformer(map), true);
        Transporte.iniciar(cfg);                        // thread daemon
    } catch (Throwable t) {
        Estado.degradar("premain falhou: " + t);
    }
}
```

Duas decisões:

- **Falha nunca derruba o jogo.** Um AC que crasha o jogo vira o maior gerador
  de ticket do servidor. Degradar e reportar.
- **Build desconhecido ⇒ não instrumenta.** Melhor o servidor saber "não pôde
  instrumentar" do que hookar nomes errados e produzir lixo.

### 6.2 Mapeamentos — verificados neste jar

O 1.8.9 é obfuscado. Os nomes abaixo foram **confirmados no bytecode do
`1.8.9.jar` desta instalação**, não adivinhados:

| Semântico | Obfuscado | Confirmação |
|---|---|---|
| `MouseHelper` | `avf` | única classe do jar que referencia `Mouse.getDX` |
| `MouseHelper.deltaX` | `avf.a` (`I`) | recebe `Mouse.getDX()` em `avf.c()` |
| `MouseHelper.deltaY` | `avf.b` (`I`) | recebe `Mouse.getDY()` em `avf.c()` |
| `MouseHelper.mouseXYChange()` | `avf.c()` (`()V`) | corpo = `a = getDX(); b = getDY();` |
| `Minecraft` | `ave` | maior classe do jar (63 KB); dona do campo `avf` |
| `Minecraft.mouseHelper` | `ave.u` (`Lavf;`) | lido antes de `avf.c()` |
| `Minecraft.gameSettings` | `ave.t` (`Lavh;`) | lido na conta da sensibilidade |
| `GameSettings.mouseSensitivity` | `avh.a` (`F`) | multiplicado por `0.6f` |
| `GameSettings.invertMouse` | `avh.b` (`Z`) | ramo de inversão do pitch |
| `GameSettings.smoothCamera` | `avh.aG` (`Z`) | ramo de suavização |
| `EntityRenderer` | `bfk` | único chamador de `avf.c()` |

> **Gere este arquivo em build time** a partir de uma fonte publicada (MCP
> stable 22 para 1.8.9), chaveado pelo SHA-1 do client jar, e **verifique
> estruturalmente em runtime** antes de confiar:

```json
{
  "client_sha1": "3870888a6c3d349d3771a3e9d16c9bf5e076b908",
  "mc_version": "1.8.9",
  "classes": {
    "MouseHelper":    { "obf": "avf", "verify": { "fields": ["a:I","b:B"], "refs": ["org/lwjgl/input/Mouse.getDX"] } },
    "Minecraft":      { "obf": "ave", "verify": { "has_string": "Can't keep up! Did the system time change" } },
    "GameSettings":   { "obf": "avh", "verify": { "fields_min": 40 } },
    "EntityRenderer": { "obf": "bfk", "verify": { "refs": ["avf.c"] } }
  }
}
```

Se a verificação falha, degrada. Isso transforma "mapeamento errado silencioso"
em finding explícito.

### 6.3 Ancore no que nunca é obfuscado

O perfil PvP ficou com apenas **3 hooks**, e dois deles ficam em classes que a
obfuscação não toca:

| Classe | Obfuscada? | Para quê |
|---|---|---|
| `org.lwjgl.input.Mouse` | **não** | botões (clique físico) |
| `org.lwjgl.opengl.Display` | **não** | foco, resolução |
| `java.lang.ClassLoader` | **não** | árvore de classloaders |
| `avf` (`MouseHelper`) | sim, mas é classe de 4 métodos | delta bruto |

Isso reduz drasticamente o custo de portar para outra versão: só `avf` e
`avh.aG` precisam ser remapeados.

### 6.4 Os 3 hooks

```java
// 1. SAIDA de avf.c()  — por FRAME, quando o jogo tem foco
//    Le avf.a e avf.b (ja calculados) e acumula no bucket do tick atual.
//
//    CRITICO: NUNCA chame Mouse.getDX() voce mesmo. A chamada e DESTRUTIVA
//    -- ela consome o delta. Se o agente chamar, ele ROUBA o movimento do
//    jogador e a mira quebra. Leia sempre a copia que o jogo guardou em
//    avf.a / avf.b.

// 2. ENTRADA de Minecraft.clickMouse()  — por clique, nao e hot path
//    Incrementa contador de clique fisico.

// 3. ENTRADA de NetHandlerPlayClient.handleEntityVelocity()  — ~1 por hit
//    Registra "recebi velocidade V no tick T".
```

Nada mais. Sem write barrier, sem query GL, sem hook no tick de física.

### 6.5 Registro por frame e agregação por tick

**Detalhe que morde quem implementa:** no 1.8.9 o mouse look roda **por frame**,
não por tick — é por isso que a mira é suave a 300 FPS e não travada em 20 Hz.
Então `avf.c()` dispara na taxa de FPS, e o servidor vê rotação na taxa de tick
(20 Hz, nos pacotes de movimento).

A agregação correta:

```java
/**
 * Ring buffer SPSC sem lock e sem alocacao.
 *
 * Escritor: thread do jogo (por frame). Leitor: thread daemon (por segundo).
 * Dois longs por slot, 2400 slots = 19 KB, alocados uma vez no premain e
 * nunca mais. Zero pressao de GC -- um GC no meio de uma luta custa frames,
 * e frames perdidos em PvP sao perdidos de verdade.
 */
final class Registro {
    private final long[] buf = new long[2400 * 2];
    private volatile int escrita;          // so o escritor incrementa

    // chamado na saida de avf.c(), por frame
    void frame(int dx, int dy, float yaw, float pitch, int flags) {
        int i = (escrita & 2399) << 1;
        buf[i]     = ((long) dx << 48) | ((long) (dy & 0xFFFF) << 32)
                   | (Float.floatToRawIntBits(yaw) & 0xFFFFFFFFL);
        buf[i + 1] = ((long) Float.floatToRawIntBits(pitch) << 32) | flags;
        escrita = escrita + 1;             // publica
    }
}
```

No fechamento de cada tick, o agregador soma os frames daquele tick:

```
ΣdeltaX_tick  = soma dos avf.a dos frames entre duas bordas de tick
ΔYaw_tick     = yaw no fim do tick − yaw no fim do tick anterior
```

**O invariante sobrevive à agregação**, e é isso que faz a checagem funcionar:
a soma de inteiros multiplicada por `q` continua sendo múltiplo de `q` (§8.3).

Custo medido: ~20 ns por frame. A 500 FPS são 10 µs/s, ou 0,001% de um core.
Zero alocação, zero lock, zero I/O.

---

## 7. Protocolo

### 7.1 Chave de sessão

Nenhum segredo sobrevive no cliente. A chave é **emitida pelo servidor** dentro
da conexão do jogo, que já está autenticada pelo `/login`:

```
jogador conecta → /login → servidor sabe quem é
                         → gera K_sessao (32 B), guarda em memória
                         → envia por plugin message
```

**Garante:** não há replay entre sessões, não há forja por terceiro na rede, a
atestação está ligada àquele jogador naquela sessão.
**Não garante:** que o jogador não extraia `K_sessao` do próprio processo. Ele
pode — e está tudo bem, a chave é ligação de sessão, não defesa.

### 7.2 Handshake (na tela de loading)

```
S→C  HELLO      { nonce[32], desafio[4..6], politicaVer }
C→S  ATESTACAO  { agenteVer, corpo, hmac }
S→C  POLITICA   { sensores[], janela, intervaloHeartbeat }
```

`hmac = HMAC-SHA256(K_sessao, nonce || serialize(corpo))`

`desafio[]` = subconjunto **aleatório** de 4–6 nomes de classe, sorteado de um
pool de ~200. Isso inviabiliza tabela pré-computada: o cheater teria que manter
o bytecode original de todas as 200 e interceptar o caminho de hash.

Reduzido de 8–16 para 4–6 por causa do custo de `retransformClasses` (§7.3).

```json
{
  "jvm":      { "args": ["-Xmx4096M","-javaagent:..."], "versao": "1.8.0_504" },
  "agentes":  ["guardian-agent.jar"],
  "loaders":  [ { "classe": "sun.misc.Launcher$AppClassLoader", "nClasses": 4812 } ],
  "digests":  { "ave": "9f86d081...", "avf": "2c26b46b..." },
  "packs":    [ { "nome": "Textura-CL-1.8-V3.2.zip", "sha1": "a1b2c3...", "ordem": 0 } ],
  "render":   { "lwjgl": "2.9.4", "gpu": "NVIDIA RTX 3060", "res": "1920x1080" },
  "opcoes":   { "sens": 0.5, "smoothCamera": false, "gamma": 1.0 },
  "estado":   "OK"
}
```

`opcoes.sens` e `smoothCamera` são obrigatórios: sem eles o servidor não
calcula `q` nem sabe se pode aplicar a checagem de GCD (§14.4).

### 7.3 Digest de bytecode: só no loading, nunca em combate

```java
/**
 * Hash do bytecode como a JVM o tem, salgado com o nonce do servidor.
 *
 * Le via retransformClasses, nao do jar em disco: cheat injeta em memoria e
 * o jar fica intacto.
 *
 * CUSTO: retransformClasses deotimiza e re-JITa a classe. Isso e um hitch
 * de dezenas de ms. Em PvP isso e inaceitavel no meio de uma luta, e
 * aceitavel numa tela de loading -- onde o jogador ja espera travar.
 *
 * Por isso: SO no handshake e nas re-atestacoes de §7.4. Nunca por timer.
 */
byte[] digest(Class<?> alvo, byte[] nonce) { ... }
```

Tamper-**evident**, não tamper-proof: um T3 pode hookar `retransformClasses`.
Mas agora precisa hookar *isso*, em vez de só editar uma classe.

### 7.4 Re-atestação de graça: tela de loading

O corte do re-desafio periódico abre um buraco real: **cheat ativado depois do
handshake** não é pego pelo desafio único. A mitigação não custa nada:

> Re-atestar exatamente nos momentos em que já existe um hitch de loading:
> **respawn, troca de mundo/dimensão, troca de servidor (BungeeCord), e sob
> pedido de staff.**

Num servidor de PvP isso é frequente por construção — o jogador morre e
respawna o tempo todo. Você consegue re-atestação a cada poucos minutos, com
custo percebido **zero**, porque o hitch está escondido na tela que já ia
travar.

### 7.5 Heartbeat — o melhor retorno por linha de código

A cada 5 s. Faltando 3 consecutivos (15 s) com o jogador conectado → finding
`AGENTE_SILENCIOSO`.

Por que importa mais do que parece: matar o agente só é vantajoso se o servidor
não notar o silêncio. Notando, "desligar o AC" fica tão detectável quanto "ser
pego por ele". Custo: ~20 linhas.

### 7.6 Canal único

| | |
|---|---|
| Transporte | plugin message, canal `CL|Guard` (8 chars, cabe no limite de 20 do 1.8.9) |
| Já autenticado | sim — vai dentro da conexão do jogo |
| Porta extra / firewall | **nenhuma** |
| Limite de payload | 32767 B por mensagem no 1.8.9 |
| Captura de tela | chunked, **1 chunk de 32 KB por tick** |

Uma captura de 200 KB = 7 chunks = 350 ms, imperceptível, e não compete com o
tráfego do jogo porque é rate-limited por tick. Foi isso que permitiu cortar o
canal TLS inteiro (§0.2).

### 7.7 Telemetria em regime

Janela de 20 ticks (1 s), só estatística — banda e privacidade ao mesmo tempo:

```json
{
  "t": [48120, 48140],
  "mouse":  { "sdx": 143, "adx": 187, "sdy": -22, "ady": 61 },
  "rot":    { "syaw": 6.435, "ayaw": 8.415, "spitch": -0.9 },
  "cliq":   3,
  "atq":    { "n": 3, "semClique": 0 },
  "kb":     { "recebidas": 1 },
  "flags":  "focado,noChao"
}
```

~120 bytes serializados. **~200 B/s incluindo overhead e heartbeat.**

---

## 8. Os sensores

### 8.1 Atestação de ambiente

**Coleta (uma vez, no loading):** `getAllLoadedClasses()` filtrado, árvore de
classloaders, args da JVM, agentes, digests do desafio, hashes de pack.

| Indicador | Significado |
|---|---|
| Classe em pacote não-vanilla no AppClassLoader | mod injetado |
| ClassLoader customizado não previsto | client injetado em runtime |
| Digest divergente no desafio | bytecode patcheado |
| `-javaagent` além do nosso | agente de terceiro |
| `premain` degradado | tentativa de neutralização |

**Não dispare em "classe desconhecida".** OptiFine, shaders e mods de FPS
carregam centenas de classes fora do jar vanilla. Isso exige **allowlist
assinada** (§12), não denylist.

### 8.2 Kill-aura: proveniência de rotação — o núcleo

#### 8.2.1 O invariante, verificado no bytecode deste jar

Em `bfk` (`EntityRenderer`), logo após `avf.c()`:

```
f  = avh.a * 0.6f + 0.2f        // avh.a = mouseSensitivity
f1 = f * f * f * 8.0f
f2 = avf.a * f1                 // avf.a = deltaX (int)
f3 = avf.b * f1                 // avf.b = deltaY (int)
→ setAngles(f2, f3 * inverter)
```

E `setAngles` aplica:

```
rotationYaw   += f2 * 0.15
rotationPitch -= f3 * 0.15
```

Portanto, **em cliente não modificado**:

```
ΔrotationYaw = deltaX × f1 × 0.15        onde  f1 = (sens·0,6 + 0,2)³ × 8
```

`deltaX` é **inteiro**. Logo toda variação de yaw é múltiplo inteiro de

```
q = f1 × 0,15        ("GCD" da sensibilidade)
```

Para `sens = 0,5`: `f = 0,5`, `f1 = 1,0`, **`q = 0,15`**. Todo ΔYaw é múltiplo
de 0,15. E, como visto em §6.5, a soma dos frames de um tick preserva isso.

#### 8.2.2 As três checagens (rodam no servidor)

```java
/**
 * Cruza o input relatado pelo cliente com a rotacao observada pelo servidor.
 *
 * Funciona porque as duas metades vem de fontes diferentes: deltaX e
 * sensibilidade vem do cliente; o yaw vem dos pacotes de movimento, que o
 * servidor ja tinha. Um cheat precisa mentir nas duas, de forma coerente.
 */
List<Finding> checar(JanelaCliente c, JanelaServidor s) {

    if (c.smoothCamera()) return semGcd(c, s);   // §14.4
    double q = Math.pow(c.sens() * 0.6 + 0.2, 3) * 8 * 0.15;

    // (1) ROTACAO SEM MOUSE  — aura escreveu o campo direto
    if (s.absYaw() > 2.0 && c.absDeltaX() == 0) {
        add(ROTACAO_SEM_INPUT, ALTA);
    }

    // (2) QUEBRA DE QUANTIZACAO  — rotacao que nao passou pelo pipeline
    double resto = s.yawDeltas().stream()
                    .mapToDouble(d -> Math.abs(Math.IEEEremainder(d, q)))
                    .average().orElse(0);
    if (resto > q * 0.05) {
        add(GCD_INCONSISTENTE, MEDIA);
    }

    // (3) MOUSE INCOERENTE  — pega quem descobriu a (1) e fabricou deltaX
    if (Math.abs(c.somaDeltaX() * q - s.somaYaw()) > q * 2) {
        add(INPUT_FABRICADO, ALTA);
    }
    return findings;
}
```

A checagem (3) é o que fecha o cerco no T2. Para passar nas três, o cheat
precisa mover a rotação **só** em múltiplos de `q` e relatar um stream de
`deltaX` inteiro coerente. É possível — e aí entra 8.2.3.

#### 8.2.3 Assinatura de input fabricado

| Métrica | Humano | Gerado |
|---|---|---|
| Distribuição de `|dx|` | cauda longa, pico em 0–2 | uniforme ou bimodal |
| Jerk (3ª derivada) | suave, limitado | descontínuo |
| Correlação dx↔dy | alta (diagonal natural) | baixa ou fixa |
| Ticks em `dx = 0` | rajadas longas (mão parada) | raro |
| **Snap até o alvo** | **3–8 ticks, com overshoot** | **1 tick, sem overshoot** |

O **snap** é o mais discriminante e o mais simples de implementar: quantos ticks
a rotação levou para chegar a < 2° do ângulo ideal, e se houve overshoot.
Humano erra e corrige. Aura acerta de primeira.

### 8.3 Multi-aura e autoclicker

```
Servidor:  ataques/tick, alvos distintos/tick
Cliente:   cliques físicos/janela, intervalo entre cliques

alvosDistintos > 1 no mesmo tick   → MULTIAURA        ALTA   (server-side puro)
ataques > 0 e cliques == 0         → ATAQUE_SEM_CLIQUE ALTA   (cruzado)
desvio padrão do intervalo < 3 ms  → AUTOCLICKER       MEDIA
```

`alvosDistintos > 1` é detecção 100% server-side, custo zero no cliente,
praticamente sem falso positivo. **Implemente isso primeiro** — pega multi-aura
sozinho, antes de qualquer sensor existir.

### 8.4 Knockback: só a metade barata

O write barrier em `motionX/Y/Z` foi cortado (§0.2). O que sobra:

```
Servidor:  enviei S12PacketEntityVelocity(V) no tick T
Cliente:   "recebi V no tick T"            ← hook 3, ~1 chamada por hit
Servidor:  deslocamento observado em T..T+10
```

| Cliente relata recebimento | Posição acompanha | Veredito |
|---|---|---|
| sim | sim | limpo |
| sim | **não** | `KB_NAO_APLICADO` — anti-KB |
| **não** | não | relato patcheado (e silêncio já é finding) |

A checagem autoritativa continua server-side: deslocamento pós-hit vs.
knockback esperado. O cliente só diz se recebeu — o que já separa "anti-KB" de
"lag". Custo: um hook numa função chamada uma vez por pancada.

### 8.5 X-ray: hash + Orebfuscator

Perfil PvP → prioridade baixa, custo quase zero:

1. **Orebfuscator no servidor.** Não envie o bloco que o jogador não pode ver.
   Isso não detecta X-ray, **elimina a classe**. Se for fazer uma coisa só
   contra X-ray, faça esta.
2. **Hash do pack** no handshake. O servidor compara com a allowlist. Pack
   desconhecido → restrição ou pedido de análise sob demanda.
3. **Gamma** (`gameSettings.gammaSetting > 1,0`) → fullbright. Um field read por
   segundo.
4. Análise de opacidade de textura: **só sob comando**, nunca no startup.

Cortados: toda query GL (§0.2).

### 8.6 Captura de tela

**Padrão — framebuffer do jogo:**

```java
/**
 * Captura o que o JOGO esta renderizando, lendo o framebuffer do GL.
 *
 * Escopo exatamente certo: nao ve o desktop, nao ve outra janela. Pega a GUI
 * de qualquer client injetado que desenhe pelo pipeline do Minecraft -- a
 * maioria.
 *
 * NAO pega overlay externo desenhado por outro processo. Limitacao conhecida
 * e aceita; o modo elevado cobre, sob gatilho de staff.
 *
 * glReadPixels e um stall de pipeline: um hitch de um frame. Aceitavel
 * porque e sob demanda e raro -- nunca por timer.
 */
BufferedImage capturarFramebuffer() {
    if (!Display.isActive()) throw new CapturaAbortada("fora de foco");
    int w = Display.getWidth(), h = Display.getHeight();
    ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
    GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
    return paraImagem(buf, w, h);
}
```

**Elevado — janela do jogo** (`Robot`, recortado ao `Display`): pega overlay
externo. Requisitos não-negociáveis: gatilho de staff com ticket, **banner
visível** na tela do jogador, consentimento na primeira vez, recorte estrito à
janela (nunca `GraphicsDevice` inteiro), abortar se perder foco (e registrar o
abort — "minimizou durante a verificação" é informação), retenção 72 h, acesso
logado.

**Transporte:** chunked pelo canal de controle, 1 chunk/tick (§7.6).

---

## 9. Correlação — a escada de custo

| Finding | Fonte cliente | Fonte servidor | Evasão exige |
|---|---|---|---|
| `MULTIAURA` | — | alvos/tick | **nada — não há evasão** |
| `AGENTE_SILENCIOSO` | ausência | jogador conectado | **nada — não há evasão** |
| `ROTACAO_SEM_INPUT` | deltaX = 0 | ΔYaw > 0 | fabricar deltaX |
| `INPUT_FABRICADO` | deltaX relatado | ΔYaw observado | fabricar deltaX **coerente** |
| `GCD_INCONSISTENTE` | sens + smoothCamera | ΔYaw observado | rotacionar só em múltiplos de `q` |
| `SNAP_NAO_HUMANO` | série de deltaX | ângulo até o alvo | modelar movimento humano |
| `ATAQUE_SEM_CLIQUE` | cliques | ataques | fabricar cliques |
| `AUTOCLICKER` | intervalo de clique | — | jitter realista |
| `KB_NAO_APLICADO` | velocidade recebida | deslocamento | fabricar recebimento + mexer na física |
| `PACK_DESCONHECIDO` | hash do pack | allowlist | patchar o hasher |
| `BYTECODE_DIVERGENTE` | digest salgado | nonce | manter originais + hookar o hash |

Leia a coluna da direita de cima para baixo: é a escada que o cheater tem que
subir. As duas primeiras linhas **não têm evasão** e são as mais baratas de
implementar. É por onde se começa.

---

## 10. O que o servidor perde com os cortes

Honestidade sobre o custo das decisões do §0.2:

| Perda | Gravidade | Compensação |
|---|---|---|
| Cross-check de input para fly/speed | **baixa** | fly/speed são bem detectados server-side; fly em PvP é óbvio na simulação |
| Metade "apliquei" do knockback | **baixa** | o deslocamento server-side já é autoritativo; sobra o "recebi" |
| Detecção de wireframe/no-depth | **baixa** no perfil PvP | Orebfuscator resolve X-ray; gamma pega fullbright |
| Detecção de pack de X-ray por análise de pixel | **média** | hash + allowlist cobre o caso comum; análise sob demanda cobre o resto |
| Cheat ativado após o handshake | **média** → baixa | re-atestação em tela de loading (§7.4), frequente em PvP por construção |

Nenhuma dessas perdas atinge o sensor central (§8.2), que é o que justifica ter
um componente client-side.

---

## 11. Decisão e ação

### 11.1 Nunca punir por sinal exclusivamente client-side

| Origem do finding | Ação máxima automática |
|---|---|
| Só client-side | **flag para revisão** — nunca ban |
| Só server-side autoritativo | kick / restrição |
| Cruzado (cliente × servidor) | restrição + alerta de staff |
| Cruzado + confirmação server-side independente | ban automático permitido |

Não é cautela genérica: todo sinal client-side depende de um relato que pode ser
falso **em qualquer direção**. Corolário: **nunca aceite relato do cliente A
sobre o jogador B** — senão um cheat fabrica evidência para incriminar
inocentes.

### 11.2 Pontuação com decaimento

```
pontos += peso × confianca
pontos *= 0,5 por hora
```

| Faixa | Ação |
|---|---|
| 0–20 | nada |
| 20–50 | alerta silencioso para staff |
| 50–80 | **restrição**: sem PvP, sem economia |
| 80+ | kick + fila de revisão prioritária |
| 80+ com confirmação server-side | ban |

A restrição em 50–80 é melhor que kick: mantém o jogador gerando evidência, em
vez de avisá-lo de que foi detectado.

### 11.3 Modo restrito — o que resolve o T1

Jogador sem atestação válida não deve ser banido (pode ser legítimo com
problema), mas também não deve ter acesso igual:

| | Atestado | Não atestado |
|---|---|---|
| Entrar, chat, lobby | sim | sim |
| **PvP, survival** | sim | **não** |
| **Economia, loja** | sim | **não** |
| Mensagem | — | "baixe o launcher oficial em craftlandia.com.br" |

Resolve o vetor "não usar o launcher" sem punir ninguém injustamente, e vira o
principal motor de adoção do launcher.

---

## 12. Anti-tamper: o que vale

**Vale:** nonce por sessão nos digests (mata pré-computação); desafio de
subconjunto aleatório (mata tabela estática); digest via `Instrumentation`
(mata patch em memória); **heartbeat com finding no silêncio** (o maior retorno
de todos); correlação cruzada do §9 (a defesa real); obfuscação do agente e
nomes aleatorizados por build (atrasa T2).

**Não vale:**

| Medida | Por que não |
|---|---|
| Driver de kernel / ring-0 | destrói "portátil, não instala nada"; exposição jurídica e de suporte desproporcional |
| Varredura de memória de outros processos | privacidade indefensável; falso positivo em Discord/OBS; contornável |
| Enumerar processos | a lista não prova nada — ter um `.exe` aberto não é cheat |
| Anti-debugger / anti-VM | quebra jogador em VM/Proton; não pega cheat em JVM |
| Veredito no cliente | inútil por construção (§1) |
| Cripto caseira | HMAC-SHA256 resolve; cripto própria só adiciona bug |

A tentação de escalar é a forma mais comum deste projeto dar errado. O ganho
marginal de cada degrau de invasividade cai rápido; o custo sobe rápido. **A
linha certa é a janela do jogo e a JVM do jogo.**

---

## 13. Allowlist assinada

Denylist não funciona (cheat novo toda semana). Allowlist assinada:

```json
{
  "versao": 12,
  "assinatura": "<Ed25519 sobre o corpo; chave PUBLICA embutida no agente>",
  "mods": [
    { "nome": "OptiFine_1.8.9_HD_U_M5", "sha1": "e4f2...", "pacotes": ["optifine/**","net/optifine/**"] },
    { "nome": "5zig_modern_4.0.4",      "sha1": "9ab1...", "pacotes": ["eu/the5zig/**"] }
  ],
  "packs": [ { "nome": "Textura-CL-1.8-V3.2.zip", "sha1": "a1b2c3..." } ]
}
```

A assinatura é essencial: sem ela o jogador edita a allowlist local e libera o
próprio cheat. Chave pública no agente — pública pode, privada não (§5.4).

Cada mod permitido declara os prefixos de pacote que legitimamente carrega.
Classe fora do vanilla **e** fora dos prefixos = finding. É isso que faz a
atestação do §8.1 ser utilizável em vez de ruidosa.

> **OptiFine é obrigatório na allowlist desde o dia 1.** Num launcher de PvP,
> uma fração enorme da base usa OptiFine ou um client de FPS. Bloquear é perder
> os jogadores, não os cheaters.

---

## 14. Fases

Cada fase vai a **shadow mode** (coleta e registra, não age) por 2–4 semanas
antes de habilitar ação. Sem exceção.

| Fase | Entrega | Pega | Esforço |
|---|---|---|---|
| **0** | Launcher + integridade pinada + aceite | — | 1–2 sem |
| **1** | Canal, handshake, atestação, **heartbeat** | **T0 e T1 — o maior salto** | 2–3 sem |
| **2** | `MULTIAURA` + `ATAQUE_SEM_CLIQUE` | multi-aura, aura ingênua | **1 sem** |
| **3** | Registro por frame + proveniência completa (§8.2.2) | aura, incluindo T2 | 2–3 sem |
| **4** | Snap + assinatura estatística (§8.2.3) | aura com input fabricado | 2 sem |
| **5** | Knockback (metade barata) + autoclicker | anti-KB, autoclicker | 1 sem |
| **6** | Orebfuscator + allowlist de packs | X-ray | 1 sem |
| **7** | Captura de framebuffer + painel de staff | review manual | 1–2 sem |
| **8** | Captura de janela + fluxo de `/ss` | overlay externo | 2 sem |

**A ordem é deliberada.** Fase 1 sozinha, com modo restrito (§11.3), resolve
~95% do volume — porque 95% usa client pronto e não vai conseguir conectar com
acesso completo. A fase 2 custa uma semana e pega multi-aura inteiro. Tudo
depois é retorno decrescente contra população cada vez menor.

Não inverta por achar aura "mais urgente": **sem a fase 1, o cheater não usa seu
launcher e nenhum sensor roda.**

---

## 15. Testes

### 15.1 Falso positivo é o risco dominante

Um AC que pune 1% dos legítimos causa mais dano que o cheat que previne.

| Métrica | Alvo | Como medir |
|---|---|---|
| FP rate por finding | < 0,1% das sessões | shadow mode × base conhecida-limpa |
| Cobertura (T0) | > 90% | bancada com clients conhecidos |
| **Frame time p99** | **+0 ms mensurável** | mesma cena, mesma seed, com e sem agente |
| **Alocação/s do agente** | **0 B na thread do jogo** | async-profiler em modo alloc |
| Latência de detecção | < 30 s para T0 | connect → finding |

As duas métricas em negrito são as que decidem se isso é ou não um launcher de
PvP. Meça-as em **todo** PR do agente, em CI.

### 15.2 Bancada

Wurst, Impact, LiquidBounce, Aristois + dois pagos. Cada client × cada sensor,
resultado documentado. É o que transforma "achamos que detecta" em "detecta".

### 15.3 Grupo de controle

2% dos jogadores com atestação coletada e **nenhuma ação** aplicada,
permanentemente. É a única forma honesta de medir FP rate em produção e de
pegar regressão depois de cada mudança de limiar.

### 15.4 Fontes conhecidas de falso positivo

| Causa | Afeta | Mitigação |
|---|---|---|
| **`smoothCamera` ligado** | GCD, snap | ler `avh.aG`; **desligar a checagem de GCD** — a suavização quebra o invariante |
| Troca de sensibilidade em sessão | GCD | reenviar `sens` a cada mudança |
| Mouse de alto DPI / raw input mod | GCD | recalcular `q` por sessão a partir dos dados |
| Controle / acessibilidade | deltaX ausente | allowlist por conta |
| FPS muito baixo ou travado | agregação frame→tick | normalizar por tick real; descartar janelas com < 20 frames |
| Alt+Tab no meio da janela | tudo | descartar janela sem foco |
| OptiFine / shaders | atestação | allowlist assinada (§13) |
| Jogador em VM ou Proton | atestação | não detectar VM (§12) |
| Resource pack do servidor | pack | pré-aprovar o próprio pack |
| Lag spike | KB, atestação | exigir N janelas consecutivas |

---

## 16. Estrutura de repositório

```
guardian/
├── ARQUITETURA.md              este documento
├── PROTOCOLO.md                wire format do canal CL|Guard
├── PRIVACIDADE.md              inventário de dados, retenção, base legal
│
├── launcher/                   deriva de ../launcher-src
│   └── src/br/com/craftlandia/launcher/
│       ├── Core.java           + verificarPinado()
│       ├── Integridade.java    novo
│       ├── Aceite.java         novo
│       └── Ui.java             + tela de aceite
│
├── agent/
│   ├── mappings/
│   │   └── 3870888a....json    chaveado por SHA-1 do client.jar
│   └── src/br/com/craftlandia/guardian/
│       ├── Agent.java              premain
│       ├── Mapa.java               carga + verificação estrutural
│       ├── GuardianTransformer.java  3 hooks, nada mais
│       ├── Registro.java           ring buffer SPSC, 19 KB, 0 alloc
│       ├── Agregador.java          frame → tick → janela de 1 s
│       ├── Atestacao.java          handshake e re-atestação
│       ├── Captura.java            framebuffer, sob demanda
│       └── Canal.java              plugin message, chunking
│
├── plugin/                     Paper/Spigot 1.8.8
│   └── src/br/com/craftlandia/guardian/servidor/
│       ├── GuardianPlugin.java
│       ├── Sessao.java             nonce, K_sessao, desafio
│       ├── correlacao/
│       │   ├── Pacotes.java        multiaura  ← fase 2, faça primeiro
│       │   ├── Rotacao.java        §8.2.2     ← fase 3, o núcleo
│       │   ├── Snap.java           §8.2.3
│       │   └── Knockback.java      §8.4
│       ├── Pontuacao.java
│       ├── ModoRestrito.java       §11.3
│       └── staff/ComandoSS.java
│
└── bancada/                    §15.2 — NÃO versionar binários de cheat
```

---

## 17. Resumo

1. **O orçamento de performance define o escopo**, não o contrário. 19 KB,
   200 B/s, zero alocação na thread do jogo, zero hitch em combate.
2. **Seis sensores foram cortados** (§0.2): write barrier em motion, replay de
   física, queries GL, análise de textura no startup, canal TLS, re-desafio
   periódico. Cada corte tem motivo de performance, e §10 declara o que se
   perde.
3. **O cliente é sensor, o servidor é juiz.** Nenhuma decisão no cliente.
4. **Fly, speed, reach e anti-KB ficam 100% no servidor.** O cliente não
   reimplementa checagem.
5. **O sensor que justifica o projeto é `ΔYaw = deltaX × q`**, com
   `q = (sens·0,6+0,2)³ × 8 × 0,15` — verificado no bytecode deste jar.
   Inacessível ao servidor.
6. **Nunca chame `Mouse.getDX()` você mesmo** — é destrutivo e quebra a mira do
   jogador. Leia `avf.a` / `avf.b`.
7. **O mouse look é por frame, a rotação do servidor é por tick.** Agregue
   `Σ deltaX` por tick; o invariante sobrevive.
8. **`MULTIAURA` e `AGENTE_SILENCIOSO` não têm evasão possível** e são os mais
   baratos. Comece por eles.
9. **Fase 1 + modo restrito resolve ~95% do volume.** O resto é retorno
   decrescente.
10. **Re-atestação só em tela de loading** — grátis em PvP, onde o jogador
    respawna o tempo todo.
11. **Nunca banir por sinal exclusivamente client-side.** Nunca aceitar relato
    do cliente A sobre o jogador B.
12. **OptiFine na allowlist no dia 1.** Bloquear perde jogadores, não cheaters.
13. **Fique dentro da JVM e da janela do jogo.** §12.
14. **Frame time p99 e alocação/s em CI, em todo PR do agente.** São as métricas
    que dizem se isso continua sendo um launcher de PvP.
