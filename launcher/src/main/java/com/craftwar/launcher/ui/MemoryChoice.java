package com.craftwar.launcher.ui;

import com.craftwar.launcher.Platform;

final class MemoryChoice {

    private MemoryChoice() {
    }

    static Integer initial(int saved) {
        long physical = Platform.physicalMemoryMb();
        Integer best = LoginForm.MEMORY_OPTIONS[0];
        for (Integer m : LoginForm.MEMORY_OPTIONS) {
            if (m <= saved && m <= physical / 2) best = m;
        }
        return best;
    }
}
