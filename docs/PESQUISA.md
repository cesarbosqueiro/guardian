# Pesquisa: detecção de injeção

O que já existe (`IntegrityMonitor`) pega o grosso de injeção via JNI `DefineClass`, `Unsafe.defineClass`, retransform/redefine e jars carregados de fora. Próximos passos, por retorno:

| # | Técnica | Pega | Custo / risco |
|---|---|---|---|
| 1 | Medir falso positivo do `ORPHAN_CLASS` e do `FOREIGN_SOURCE` em shadow mode, com OptiFine + shaders + resource packs populares | calibração | nenhum; é pré-requisito do resto |
| 2 | Bancada com os ghost clients atuais (Vape, Drip, Raven, Entropy): o que cada um dispara | cobertura real | precisa dos binários; não versione |
| 3 | Lista de módulos nativos **do próprio processo** (`EnumProcessModules` do PID do jogo, via JNI mínimo) | DLL do injetor | nativo por SO; o limite de privacidade (processo do jogo) continua defensável |
| 4 | Threads da JVM com nome ou stack fora do esperado | loop do cheat | ruidoso; só dado para a staff |
| 5 | Comparar o yaw do tick do cliente com o yaw recebido pelo servidor | "silent rotation" (aura que troca o yaw só no pacote) | barato: os dois lados já estão no despejo da caixa-preta |
| 6 | `LLMHF_INJECTED` em hook de mouse de baixo nível | autoclicker por `SendInput` | hook global pesa na LGPD e gera alerta de antivírus; avalie antes |
| 7 | Rotação de protocolo e obfuscação do agente por build | bypass público | é corrida contínua; defina cadência |

Regra que não muda: o que sai daqui é evidência no caso, não ban automático.
