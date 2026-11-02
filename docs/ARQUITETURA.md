# Arquitetura

## Premissa

O cliente roda na máquina do adversário. Tudo que ele manda pode ser mentira. Então:

- **o cliente relata fatos, nunca vereditos**;
- **o servidor decide**, cruzando o relato com o que ele mesmo observou;
- **nenhum sinal só do cliente gera punição automática**. Ele só entra no caso para a staff analisar.

O produto base é **evidência para a staff**: caixa-preta dos últimos 60 s, screenshot do jogo e contexto da sessão. Detecção de injeção é uma camada de pesquisa em cima disso.

## Ameaça real no PvP 1.8 brasileiro

| Ameaça | Como entra | Quem pega |
|---|---|---|
| Ghost client injetado (Vape, Drip, Raven, forks) | DLL que usa JNI/JVMTI para definir ou patchear classes em runtime | agente: classe sem origem, redefinição, jar estranho |
| Autoclicker externo | `SendInput` fora da JVM | servidor (estatística) + intervalo de clique preciso do cliente |
| Macro de mouse/driver | hardware | só estatística |
| Client modificado | jar alterado | launcher (SHA-1) + modo restrito |
| Não usar o launcher | vanilla ou outro launcher | modo restrito |

## Divisão cliente × servidor

| Detecção | Onde | Com quê |
|---|---|---|
| Fly, speed, timer, nofall, velocity, reach | servidor | **Grim** (predição). Não reescreva. |
| Multi-aura | servidor | `MultiAuraCheck` (alvos distintos por tick) |
| Ataque sem clique | cruzado | ataques do servidor × cliques do heartbeat |
| Agente silencioso | cruzado | heartbeat ausente → `SILENT_AGENT` e perda da atestação |
| Aura (snap, GCD, aceleração) | servidor | Grim/checagens próprias; a caixa-preta mostra `dx × q` contra o yaw para a staff |
| Injeção na JVM | cliente | `IntegrityMonitor` |
| X-ray | servidor | Orebfuscator |

## Cliente

### Hooks (agente, ASM)

| Hook | Onde | Para quê | Custo |
|---|---|---|---|
| `LaunchClassLoader.<init>` saída | launchwrapper | exclui `com.craftwar.guardian.` do LCL, para que jogo e mod enxerguem a mesma cópia do agente | uma vez |
| `MouseHelper.mouseXYChange` saída | jogo, por frame | lê `deltaX/deltaY` já calculados (**nunca** chama `Mouse.getDX`, que é destrutivo) | 1 escrita em ring, 0 alloc |
| `EntityPlayerSP.onUpdateWalkingPlayer` entrada | jogo, por tick | borda do tick = pacote que o servidor vê; grava yaw/pitch e Σdx/Σdy | 2 leituras de campo, 0 alloc |
| `Mouse.next()` saída | LWJGL | clique com `getEventNanoseconds` (preciso, não quantizado em tick) | só em evento de botão |
| `Display.update(Z)` entrada | LWJGL | executa a captura por PBO quando pedida | 1 leitura volátil por frame |

Os nomes do Forge 1.8.9 em runtime são SRG (`field_74377_a`, `func_74374_c`…). Ficam em `mappings/forge-1.8.9.properties`. O patch só aplica se o método e os campos existirem com o descritor certo; se não, o agente reporta `AGENT_DEGRADED` e segue sem aquele hook.

### Caixa-preta

| Ring | Tamanho | Conteúdo |
|---|---|---|
| frames | 4096 | nanos + dx/dy empacotados em 32 bits |
| ticks | 2048 | nanos, yaw, pitch, Σdx, Σdy, frames, flags |
| cliques | 1024 | nanos do evento, botão, down, em GUI |

- Potências de dois, índice por máscara, um único escritor (a thread do jogo).
- Publicação com `lazySet`: barreira de release, sem StoreLoad.
- ~150 KB fixos, alocados uma vez. Nada é transmitido em regime.
- Exportação sob pedido: 1200 ticks, 60 s de cliques, 10 s de frames, em ~60 KB binário.

### Captura

- **Jogo (padrão):** `glReadPixels` para PBO no frame N, mapeamento no N+2 (sem stall), cópia direta para o `int[]` de um `BufferedImage` (BGRA little-endian = ARGB), flip e JPEG na thread daemon. Exige foco, senão aborta e o abort vai para o caso.
- **Janela:** `Robot` recortado ao `Display`, com banner antes. Só com ticket.
- Limite de 1 captura a cada 10 s no cliente, para proteger o FPS.

### Integridade (camada de pesquisa)

- `-XX:+DisableAttachMechanism` sempre.
- Classe definida **depois** do jogo estar no mundo sem `CodeSource` → `ORPHAN_CLASS`.
- Qualquer redefinição ou retransformação de classe depois do jogo pronto → `REDEFINITION`. O vanilla, o Forge e o OptiFine não fazem isso em runtime.
- Varredura periódica (60 s, na daemon) de classes carregadas de jar fora das raízes confiáveis (JRE, classpath, `mods/`) → `FOREIGN_SOURCE`.
- Args de JVM estranhos (`-agentpath`, `-javaagent` alheio, `-Xbootclasspath`, `-noverify`) → `JVM_ARGUMENT`.
- Filtro de classes geradas pelo runtime (reflection accessors, proxies, lambdas, `ASMEventHandler` do Forge) no cliente, mais prefixos extras enviados pelo servidor na POLICY.

## Servidor

- `Gateway`: plugin message `CW|Guard`; criptografia em thread async e aplicação do resultado na main.
- `Attestation`: concede `craftwar.guardian.attested`, que outros plugins (loja, economia, warps) usam para restringir.
- `RestrictedMode`: sem atestação não há PvP. Desligado por padrão (`enforce-restricted-mode: false`) até passar pelo shadow mode.
- `Watchdog`: timeout de atestação e silêncio de heartbeat.
- `Tracker`: ring de yaw/pitch/ataques do servidor por tick, despejado junto com a caixa-preta para comparação lado a lado.
- `Cases`: pontuação com meia-vida, alertas para `craftwar.guardian.alerts`, caixa-preta automática quando o peso é alto ou o score cruza o limiar.
- `EvidenceStore`: thread única de IO, CSVs prontos para análise, `audit.log`, retenção automática.
- `GrimBridge`: escuta o `FlagEvent` do Grim por reflexão (sem dependência de compilação) e volta para a main thread.

## Não faz, de propósito

Driver de kernel, varredura de memória de outros processos, lista de processos, HWID, anti-VM. Custo jurídico e de suporte desproporcional ao ganho. A linha é a JVM e a janela do jogo.
