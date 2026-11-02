# Privacidade (LGPD)

## Inventário

| Dado | Quando | Onde fica | Retenção |
|---|---|---|---|
| Contexto (mods, packs, GPU, resolução, args da JVM, sensibilidade) | todo login | memória do plugin | sessão |
| Heartbeat (FPS, contagem de cliques, estado) | a cada 5 s | memória do plugin | sessão |
| Achados | quando ocorrem | `evidence/<uuid>/findings.log` | 180 dias (rotacionar fora do plugin) |
| Caixa-preta (60 s de mouse/rotação/cliques no jogo) | flag ou pedido da staff | `evidence/<uuid>/*-box/` | 7 dias; 90 com `/guardian hold` |
| Captura do jogo | pedido da staff com ticket | `evidence/<uuid>/*-capture.jpg` | 72 h; 90 dias com hold |
| Auditoria (quem pediu, quando, ticket) | todo pedido/recebimento/abort | `evidence/audit.log` | permanente |

## O que nunca é coletado

Teclado (o texto antigo de aceite citava teclado e saiu), processos, outras janelas, clipboard, arquivos, navegação, tela fora do jogo, HWID, IP no log de achados.

## Minimização

- Em regime só trafegam heartbeat e contexto.
- Input bruto sai do cliente **apenas** sob gatilho: flag com peso ≥ `auto-black-box.min-weight`, score cruzando o limiar de alerta, ou pedido da staff.
- O ring da caixa-preta vive na RAM do jogador e morre com o processo.

## Transparência

- Aceite versionado no manifesto. O launcher grava versão, hash SHA-256 do texto e data em `game/launcher/settings.properties`. Mudou o texto, pede de novo.
- Captura do jogo: aviso na tela depois da captura.
- Captura da janela: banner **antes** e durante.
- Captura fora de foco: aborta, e o abort é registrado.

## Pendências jurídicas

- **Base legal:** legítimo interesse (segurança do serviço) é mais defensável que consentimento, que é revogável. O aceite continua como transparência e condição de uso.
- **Menor de 12 anos:** o art. 14 exige consentimento específico do responsável. "Confirme com seu responsável" não basta. Valide com jurídico.
- **Acesso às capturas:** hoje é pelo sistema de arquivos do servidor, então só a solicitação é auditada, não a visualização. Um painel web com log de visualização resolve.
