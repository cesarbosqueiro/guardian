package com.craftwar.guardian.agent.session;

import com.craftwar.guardian.agent.GameState;
import com.craftwar.guardian.agent.Notice;
import com.craftwar.guardian.agent.capture.CaptureAbortedException;
import com.craftwar.guardian.agent.capture.CaptureService;
import com.craftwar.guardian.agent.capture.GameCapture;
import com.craftwar.guardian.agent.record.BlackBoxExporter;
import com.craftwar.guardian.agent.record.Recorder;
import com.craftwar.guardian.protocol.Chunker;
import com.craftwar.guardian.protocol.MessageType;
import com.craftwar.guardian.protocol.RequestKind;
import com.craftwar.guardian.protocol.blackbox.BlackBoxCodec;
import com.craftwar.guardian.protocol.message.Abort;
import com.craftwar.guardian.protocol.message.Request;

final class RequestHandler {

    private static final long MIN_CAPTURE_INTERVAL_NANOS = 10_000_000_000L;
    private static final long WINDOW_WARNING_MILLIS = 2000;
    private static final String GAME_NOTICE = "A staff registrou uma imagem do seu jogo.";
    private static final String WINDOW_NOTICE = "Verificacao da staff em andamento: captura da janela do jogo.";

    private final CaptureService captures = new CaptureService(GameCapture.INSTANCE);
    private long lastCapture;
    private boolean captured;

    void handle(Request request, Sender out) {
        try {
            switch (request.kind) {
                case RequestKind.BLACK_BOX:
                    transfer(request, blackBox(), out);
                    break;
                case RequestKind.GAME_CAPTURE:
                    throttle();
                    transfer(request, captures.captureGame(), out);
                    Notice.show(noticeOr(request, GAME_NOTICE), 6000);
                    break;
                case RequestKind.WINDOW_CAPTURE:
                    throttle();
                    Notice.show(noticeOr(request, WINDOW_NOTICE), WINDOW_WARNING_MILLIS + 6000);
                    Thread.sleep(WINDOW_WARNING_MILLIS);
                    transfer(request, captures.captureWindow(), out);
                    break;
                default:
                    out.send(MessageType.ABORT, new Abort(request.id, "unknown request kind").encode());
            }
        } catch (CaptureAbortedException e) {
            out.send(MessageType.ABORT, new Abort(request.id, e.getMessage()).encode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static byte[] blackBox() {
        return BlackBoxCodec.encode(BlackBoxExporter.export(Recorder.INSTANCE, GameState.sensitivity(), GameState.smoothCamera()));
    }

    private void throttle() throws CaptureAbortedException {
        long now = System.nanoTime();
        if (captured && now - lastCapture < MIN_CAPTURE_INTERVAL_NANOS) throw new CaptureAbortedException("rate limited");
        captured = true;
        lastCapture = now;
    }

    private static void transfer(Request request, byte[] data, Sender out) {
        for (byte[] chunk : Chunker.split(request.id, request.kind, data)) out.send(MessageType.CHUNK, chunk);
    }

    private static String noticeOr(Request request, String fallback) {
        return request.notice == null || request.notice.isEmpty() ? fallback : request.notice;
    }
}
