# Protocolo `CW|Guard`

Plugin message dentro da conexão do jogo. Big-endian. Fonte da verdade: `protocol/`.

## Envelope

```
u8 versao (1) | u8 tipo | u32 seq | payload | tag[16]
```

- `HELLO` e `REPLY` vão sem tag e com `seq = 0`.
- Os demais são selados: `tag = HMAC-SHA256(chave_da_direcao, envelope_sem_tag)[0..16]`.
- `seq` estritamente crescente por direção: replay e reordenação são rejeitados.
- Máximo de 32000 B por mensagem (o C17 do 1.8 aceita 32767).

## Handshake

O servidor tem uma identidade ECDSA P-256 de longo prazo (`plugins/Guardian/identity.key`). A pública vai no manifesto e, pelo argumento `server=` do `-javaagent`, chega ao agente.

```
S→C HELLO  : nonce[32] | ephS[65] | u16 len | assinatura ECDSA(identidade, "CWG1 hello"|nonce|ephS)
C→S REPLY  : ephC[65] | confirm[16]

shared = ECDH(eph)
okm    = HKDF-SHA256(sal = nonce, ikm = shared, info = "CWG1 keys"|ephS|ephC, 96)
         → kConfirm | kC2S | kS2C
confirm = HMAC(kConfirm, "CWG1 confirm"|nonce|ephS|ephC)[0..16]
```

- Servidor autenticado: um MITM não consegue se passar pelo servidor.
- Chaves por conexão; nada de segredo embutido no cliente.
- Não protege contra o próprio jogador extrair as chaves do processo. Ninguém protege; é por isso que o servidor é o juiz.

## Mensagens

| Tipo | Direção | Payload |
|---|---|---|
| `0x10 CONTEXT` | C→S | versão do agente, mapping, estado, java, os, gpu, largura, altura, sens, smoothCamera, gamma, args da JVM[], mods[], packs[] |
| `0x11 HEARTBEAT` | C→S | u16 fps, u16 cliques esq., u16 cliques dir. (desde o último, fora de GUI), u8 flags, u8 estado, i32 classes carregadas |
| `0x12 FINDING` | C→S | u8 tipo, str assunto, str detalhe |
| `0x13 CHUNK` | C→S | i32 pedido, u8 tipo, u16 índice, u16 total, i32 tamanho total, dados |
| `0x14 ABORT` | C→S | i32 pedido, str motivo |
| `0x20 POLICY` | S→C | u16 heartbeat s, u16 inventário s, prefixos ignorados[] |
| `0x21 REQUEST` | S→C | i32 id, u8 tipo (1 caixa-preta, 2 captura do jogo, 3 captura da janela), str aviso |

Strings: u16 de tamanho + UTF-8. Listas: u16 de contagem.

## Caixa-preta (`CWBX` v1)

```
i32 magic 0x43574258 | u8 1 | f32 sens | bool smooth | i64 fim_ms
u16 n × (i32 µs_antes_do_fim, f32 yaw, f32 pitch, i32 Σdx, i32 Σdy, u16 frames, u8 flags)
u16 n × (i32 µs_antes_do_fim, u8 botao<<2|gui<<1|down)
u16 n × (i32 µs_antes_do_fim, i32 dx<<16|dy)
```

O plugin converte para `client-ticks.csv`, `client-clicks.csv`, `client-frames.csv` e `server-ticks.csv`. Em `client-ticks.csv`, `expected_delta = Σdx × q`, com `q = (sens·0,6 + 0,2)³ × 8 × 0,15`. `residual` diferente de zero, sem `smoothCamera`, teleporte ou montaria, indica rotação que não veio do mouse.
