# Operação

## 1. Servidor

1. Instale `guardian-plugin.jar` (e o Grim, opcional mas recomendado).
2. Suba uma vez. O plugin cria `plugins/Guardian/identity.key` (privada, **não versione**) e loga a chave pública.
3. `/guardian key` mostra a pública de novo. Ela vai no template do manifesto.
4. Deixe `enforce-restricted-mode: false` durante 2 a 4 semanas de shadow mode. Ligue depois de medir os falsos positivos.
5. Plugins de economia, loja e warps: exijam `craftwar.guardian.attested`.

## 2. Instância de referência

Numa máquina limpa, com o launcher oficial da Mojang ou instalação manual:

1. Instale o Forge `1.8.9-11.15.1.2318` numa pasta (`instancia/`).
2. Coloque os mods em `instancia/mods/`: `craftwar-client-0.1.0.jar` + OptiFine.
3. Opcional: `instancia/config-defaults/` com `options.txt`, `optionsof.txt` e `config/*.cfg`. Esses arquivos são escritos só se o jogador ainda não tiver o arquivo.

**OptiFine:** a licença proíbe redistribuir sem permissão do autor, mesmo em launcher privado: você está entregando o arquivo para os seus jogadores. Peça permissão ao sp614x ou não o hospede (nesse caso, o jogador instala e a trava de `mods/` precisa permitir). Sem permissão, hospedar no seu CDN é violação.

O client jar da Mojang e os assets **não** são hospedados por você: o manifesto aponta para os servidores da Mojang.

## 3. Manifesto

```bash
java -cp launcher/target/CraftWar.jar com.craftwar.launcher.tool.ManifestTool keygen chaves/
```

Cole o `manifestKey=` impresso em `launcher/src/main/resources/launcher.properties`, ajuste `manifestUrl` e rebuilde o launcher.

```bash
cp tools/manifest-template.example.json template.json
java -cp launcher/target/CraftWar.jar com.craftwar.launcher.tool.ManifestTool \
    build instancia/ template.json chaves/manifest.key saida/
```

Publique `saida/` inteira na URL de `cdnBaseUrl`, mantendo `manifest.json` e `manifest.json.sig` em `manifestUrl`. Qualquer mudança de mod ou flag é rebuild + republicação; o launcher do jogador não precisa mudar.

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

## 6. Primeiro teste em servidor real

1. Conecte com o launcher e confira `/guardian info`: atestado `sim`, agente `OK`.
2. Se o agente aparecer `DEGRADADO`, um nome SRG do mapping está errado. O `findings.log` diz qual patch falhou.
3. Se ficar `nao` atestado: confira se o FML entrega o `S3F` ao canal `CW|Guard`. É o ponto não verificado do mod.
4. Rode `/guardian box` e `/guardian ss` em você mesmo e abra os CSVs.
