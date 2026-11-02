package com.craftwar.launcher.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.craftwar.launcher.LauncherApp;

final class SettingsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private SettingsDialog(Frame owner, LauncherApp app) {
        super(owner, "Configuracoes", true);
        JTextField javaPath = new JTextField(app.settings().javaPath(), 32);
        JButton folder = new JButton("Abrir pasta do jogo");
        JButton save = new JButton("Salvar");
        folder.addActionListener(e -> openFolder(app));
        save.addActionListener(e -> save(app, javaPath.getText()));
        JPanel fields = new JPanel(new BorderLayout(6, 6));
        fields.setBackground(Theme.PANEL);
        fields.add(new JLabel("Java (vazio = automatico):"), BorderLayout.NORTH);
        fields.add(javaPath, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBackground(Theme.PANEL);
        buttons.add(folder);
        buttons.add(save);
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(Theme.PANEL);
        root.setBorder(new RetroBorder());
        root.add(fields, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
    }

    static void open(Frame owner, LauncherApp app) {
        new SettingsDialog(owner, app).setVisible(true);
    }

    private void save(LauncherApp app, String path) {
        app.settings().javaPath(path);
        try {
            app.settings().save();
        } catch (IOException e) {
            setTitle("Configuracoes - erro ao salvar: " + e.getMessage());
            return;
        }
        dispose();
    }

    private static void openFolder(LauncherApp app) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(app.layout().root());
        } catch (IOException | IllegalArgumentException ignored) {
            app.layout().root().mkdirs();
        }
    }
}
