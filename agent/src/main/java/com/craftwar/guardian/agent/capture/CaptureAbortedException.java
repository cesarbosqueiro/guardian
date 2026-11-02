package com.craftwar.guardian.agent.capture;

public final class CaptureAbortedException extends Exception {

    private static final long serialVersionUID = 1L;

    public CaptureAbortedException(String reason) {
        super(reason);
    }
}
