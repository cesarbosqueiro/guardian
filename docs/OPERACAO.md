# Operação

## 1. Servidor

1. Instale `guardian-plugin.jar` (e o Grim, opcional mas recomendado).
2. Suba uma vez. O plugin cria `plugins/Guardian/identity.key` (privada, **não versione**) e loga a chave pública.
3. `/guardian key` mostra a pública de novo. Ela vai no template do manifesto.
4. Deixe `enforce-restricted-mode: false` durante 2 a 4 semanas de shadow mode. Ligue depois de medir os falsos positivos.
5. Plugins de economia, loja e warps: exijam `craftwar.guardian.attested`.

## 2. Instâncias de referência (uma por versão)

Cada versão do launcher é um **perfil** com instância própria. O jogador escolhe a versão na tela; o launcher baixa só os arquivos daquele perfil para `game/<perfil>/`. Os assets da Mojang ficam compartilhados em `game/assets/`.

```
instancias/
  1.8.9/                     .minecraft com o Forge 1.8.9-11.15.1.2318 instalado
    versions/1.8.9/  versions/1.8.9-forge.../  libraries/
    mods/                    craftwar-client, OptiFine, bspkrsCore, ArmorStatusHUD, StatusEffectHUD, macro
    config-defaults/         options.txt, optionsof.txt, config/*.cfg (escritos só se o jogador não tiver)
  1.5.2/                     .minecraft com o 1.5.2 vanilla rodado uma vez
    versions/1.5.2/
    jarmods/                 forge-1.5.2-7.8.1.738-universal.zip (aplicado no jar pelo launcher, na ordem do nome)
    mods/
```

- O client jar e os assets **não** são hospedados por você: o manifesto aponta para a Mojang.
- **Jar-mod (1.5.2):** o launcher baixa o `1.5.2.jar` da Mojang e grava `1.5.2-patched.jar` localmente com o conteúdo de `jarmods/` por cima, sem `META-INF/`. Ninguém redistribui o cliente.
- **OptiFine:** hospedar exige permissão do sp614x.

## 3. Manifesto

```bash
java -cp launcher/target/CraftWar.jar com.craftwar.launcher.tool.ManifestTool keygen chaves/
```

Cole o `manifestKey=` impresso em `launcher/src/main/resources/launcher.properties`, ajuste `manifestUrl` e rebuilde o launcher.

```bash
cp tools/manifest-template.example.json tools/template.json   # caminhos relativos ao template
java -cp launcher/target/CraftWar.jar com.craftwar.launcher.tool.ManifestTool \
    build tools/template.json chaves/manifest.key saida/
```

- `servers[]`: a lista que aparece no jogo, cada um com a chave de `/guardian key` **daquele** servidor. O agente confia em todas.
- `profiles[]`: uma entrada por versão, com `instance`, `versionId` e `agentMapping`.
- Um manifesto só, assinado uma vez, com todos os perfis.

### Onde os arquivos ficam hospedados

`saida/files/<perfil>/` tem o que precisa ir para a web. Cada perfil pode ter **o próprio repositório**:

- sem `cdnBaseUrl` no perfil: URL = `cdnBaseUrl` global + `<perfil>/` + caminho. Publique `saida/files/` inteira.
- com `cdnBaseUrl` no perfil: URL = essa base + caminho. Publique o **conteúdo** de `saida/files/<perfil>/` na raiz desse repositório.

GitHub serve: um repositório por versão, com a URL raw (`https://raw.githubusercontent.com/<dono>/<repo>/main/`). Duas regras:

1. **Sem Git LFS.** O raw de um arquivo em LFS devolve o ponteiro, não o jar, e o SHA-1 não bate. Arquivos até 100 MB vão direto (OptiFine e Forge universal ficam bem abaixo).
2. **Releases não servem** para a árvore `libraries/...`: asset de release não tem pasta.

`manifest.json` e `manifest.json.sig` vão em `manifestUrl`, que pode ser outro repositório. Mudou mod ou flag: rebuild + republicação, sem mexer no launcher do jogador.

## 4. Distribuição do launcher

Pasta portátil:

```
CraftWar/
  jre/                (JRE 8 x64 com SunEC: sunec.jar + sunec.dll)
  CraftWar.jar
  CraftWar.exe        (launch4j ou similar apontando para jre/)
```

O jogo é instalado em `CraftWar/game/`. Nada vai para o `%APPDATA%`.

**Atenção:** JRE 8 "minimizado" às vezes remove o SunEC. Sem ele, P-256 não existe e o agente desliga.

### Qual Java roda o jogo

`GameJava` procura nesta ordem e para no primeiro que existir:

1. `javaPath` em `game/launcher/settings.properties`, se preenchido (absoluto ou relativo à pasta do launcher; aceita o executável ou a pasta que o contém);
2. `jre/bin` — o layout desta seção;
3. `CraftLandia/bin/java/windows-x64/bin` — o layout do pacote antigo da CraftLandia;
4. `j8/bin`;
5. o `java.home` do próprio launcher, por último.

Em cada pasta tenta `javaw.exe`, depois `java.exe`, depois `java`.

O JRE do pacote vem **antes** do `java.home` de propósito: se o jogador abrir o `CraftWar.jar` com duplo clique, o launcher sobe no Java registrado no sistema, e não é ele que pode decidir a JVM do jogo. O 1.8.9 vanilla sobe mancando numa JVM moderna; o Forge não sobe (launchwrapper e ASM 5 não leem class files novos).

As primeiras linhas de `game/logs/launcher-game.log` são o comando inteiro, um argumento por linha. É o primeiro lugar a olhar quando o jogo não abre ou o agente reporta degradado.

## 5. Staff

| Comando | O que faz |
|---|---|
| `/guardian info <jogador>` | atestação, contexto, último heartbeat, achados recentes, score |
| `/guardian case <jogador>` | últimos 15 achados |
| `/guardian box <jogador> [ticket]` | caixa-preta dos últimos 60 s |
| `/guardian ss <jogador> <ticket>` | captura do jogo (ticket obrigatório) |
| `/guardian window <jogador> <ticket>` | captura da janela com banner (ticket obrigatório) |
| `/guardian hold <jogador> [ticket]` / `release` | retém evidências por 90 dias / volta à retenção normal |
| `/guardian key` | chave pública do servidor |

As evidências ficam em `plugins/Guardian/evidence/<uuid>/`.

### Tag no chat (LegendChat)

Com o PlaceholderAPI instalado, o plugin registra a expansão `guardian`:

| Placeholder | Valor |
|---|---|
| `%guardian_attested%` | `sim` / `nao` |
| `%guardian_tag%` | `&a✔` / `&c✖` |
| `%guardian_agent%` | `OK` / `DEGRADADO` / `DESLIGADO` / `SEM AGENTE` |
| `%guardian_score%` | score atual (inteiro) |

Os textos ficam em `placeholders:` no `config.yml`. No LegendChat, ponha `%guardian_tag%` no formato do canal. Os valores vêm de uma cópia atualizada a cada segundo na main thread, então podem ser lidos do chat assíncrono sem risco.

Teste: `/papi parse me %guardian_attested%` com e sem o launcher.

## 6. Primeiro teste em servidor real

1. Conecte com o launcher e confira `/guardian info`: atestado `sim`, agente `OK`.
2. Se o agente aparecer `DEGRADADO`, um nome SRG do mapping está errado. O `findings.log` diz qual patch falhou.
3. Se ficar `nao` atestado: confira se o FML entrega o `S3F` ao canal `CW|Guard`. É o ponto não verificado do mod.
4. Com dois servidores na lista: os dois aparecem no multiplayer, "Adicionar" e "Conexão direta" não existem, e o agente atesta nos dois.
5. Rode `/guardian box` e `/guardian ss` em você mesmo e abra os CSVs.
