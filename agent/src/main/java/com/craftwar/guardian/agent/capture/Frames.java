package com.craftwar.guardian.agent.capture;

final class Frames {

    private Frames() {
    }

    static void flipVertically(int[] pixels, int width, int height) {
        int[] row = new int[width];
        for (int top = 0, bottom = (height - 1) * width; top < bottom; top += width, bottom -= width) {
            System.arraycopy(pixels, top, row, 0, width);
            System.arraycopy(pixels, bottom, pixels, top, width);
            System.arraycopy(row, 0, pixels, bottom, width);
        }
    }
}
