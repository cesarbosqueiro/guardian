package com.craftwar.guardian.agent.record;

import com.craftwar.guardian.protocol.blackbox.BlackBox;

public final class ClickCounter {

    private final Recorder recorder;
    private long consumed;
    private int left;
    private int right;

    public ClickCounter(Recorder recorder) {
        this.recorder = recorder;
        this.consumed = recorder.clicks.get();
    }

    public void advance() {
        long end = recorder.clicks.get();
        long from = Math.max(consumed, end - Recorder.CLICKS + 64);
        left = 0;
        right = 0;
        for (long seq = from; seq < end; seq++) {
            int code = recorder.clickCode[(int) seq & Recorder.CLICK_MASK];
            if (!BlackBox.clickDown(code) || BlackBox.clickInGui(code)) continue;
            int button = BlackBox.clickButton(code);
            if (button == 0) left++;
            else if (button == 1) right++;
        }
        consumed = end;
    }

    public int left() {
        return left;
    }

    public int right() {
        return right;
    }
}
