package com.craftwar.launcher;

import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.craftwar.launcher.ui.LauncherWindow;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                LauncherApp app = new LauncherApp(LauncherConfig.load(), new File(Platform.launcherDir(), "game"));
                new LauncherWindow(app).setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Falha ao iniciar o launcher: " + e.getMessage());
                System.exit(1);
            }
        });
    }
}
