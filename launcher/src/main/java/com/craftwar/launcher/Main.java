package com.craftwar.launcher;

import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.craftwar.launcher.ui.LauncherWindow;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception lookAndFeelUnavailable) {
                UIManager.getLookAndFeelDefaults();
            }
            try {
                GameLayout layout = new GameLayout(new File(Platform.launcherDir(), "game"));
                new LauncherWindow(new LauncherApp(LauncherConfig.load(), layout)).setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Falha ao iniciar o launcher: " + e.getMessage());
                System.exit(1);
            }
        });
    }
}
