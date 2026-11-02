# Guardian

Launcher de PvP da CraftWar com anti-cheat client-side para Minecraft 1.8.9 (Forge + OptiFine) e servidor Spigot/Paper 1.8.8.

O cliente é **sensor e caixa-preta forense**. O servidor é o juiz. Nenhuma decisão é tomada no cliente.

## Módulos

| Módulo | Roda em | O que faz | Build |
|---|---|---|---|
| `protocol/` | agente + plugin + launcher | wire format, handshake ECDH P-256 assinado, selagem HMAC, fragmentação, formato da caixa-preta | Maven, testado |
| `agent/` | JVM do jogo (`-javaagent`) | 5 hooks ASM, ring buffers sem alocação, captura de tela por PBO, monitor de integridade, sessão com o servidor | Maven, testado |
| `launcher/` | máquina do jogador | manifesto assinado, sync por SHA-1, trava da pasta `mods`, aceite LGPD, launch com flags | Maven, testado |
| `launcher/.../tool/ManifestTool` | máquina do operador | gera e assina o manifesto a partir de uma instância Forge pronta | Maven, testado |
| `plugin/` | servidor 1.8.8 | handshake, atestação, modo restrito, multi-aura, ataque sem clique, casos, evidências, comandos de staff, ponte com Grim | Maven (precisa do repo Spigot) |
| `mod/` | jogo (Forge) | HUD de PvP (FPS, ping, CPS, keystrokes, toggle sprint) + ponte de rede jogo ↔ agente + aviso de captura | ForgeGradle 2.1 |

## Build

```bash
mvn install                      # protocol, agent, launcher, plugin
cd mod && gradle build           # depois do mvn install: o mod usa o jar do agente (provided)
```

Artefatos:

- `agent/target/guardian-agent.jar`
- `launcher/target/CraftWar.jar`
- `plugin/target/guardian-plugin.jar`
- `mod/build/libs/craftwar-client-0.1.0.jar`

O mod usa ForgeGradle 2.1 (Gradle 2.14 + JDK 8). Esse toolchain é legado e frágil. Se não subir, use um template moderno de Forge 1.8.9 com loom (por exemplo, o `gg.essential.loom`), mantendo os mesmos fontes.

## O que foi verificado e o que não foi

Verificado no ambiente de desenvolvimento:

- 15 testes unitários: handshake, adulteração, replay, reflexão de quadro, fragmentação, caixa-preta, patches ASM em classes falsas com os nomes SRG, exportação de ring buffer, contador de cliques, geração e assinatura de manifesto, comando de launch.
- Agente real em `-javaagent` numa JVM: handshake completo contra `ServerHandshake`, POLICY, CONTEXT, heartbeat, detecção de `-javaagent` alheio e transferência da caixa-preta.
- Plugin compilado contra stubs da API Bukkit: valida tipos internos, **não** a API real.

Não verificado (sem acesso ao maven do Spigot e do Forge daqui):

- plugin contra o `spigot-api` 1.8.8 real;
- mod contra o Forge 1.8.9;
- **mapeamentos SRG** em `agent/src/main/resources/mappings/forge-1.8.9.properties`. Se um nome estiver errado, o patch não aplica e o agente reporta `AGENT_DEGRADED` (não quebra o jogo), mas confira no jar deobfuscado antes de ir pra produção;
- se o FML repassa `S3F` de um servidor Bukkit para o canal `CW|Guard` registrado no mod. Esse é o primeiro teste a fazer num servidor de verdade.

## Deploy

Ver [docs/OPERACAO.md](docs/OPERACAO.md).

## Documentos

- [docs/ARQUITETURA.md](docs/ARQUITETURA.md): decisões e divisão cliente/servidor
- [docs/PROTOCOLO.md](docs/PROTOCOLO.md): wire format do canal `CW|Guard`
- [docs/PRIVACIDADE.md](docs/PRIVACIDADE.md): inventário de dados e retenção
- [docs/OPERACAO.md](docs/OPERACAO.md): chaves, manifesto, CDN, OptiFine, servidor
- [docs/PESQUISA.md](docs/PESQUISA.md): detecção de injeção, o que vem depois
