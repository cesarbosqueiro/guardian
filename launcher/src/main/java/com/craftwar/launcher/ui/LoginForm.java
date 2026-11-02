package com.craftwar.launcher.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.craftwar.launcher.manifest.Profile;
import com.craftwar.launcher.manifest.ServerEntry;

final class LoginForm extends JPanel {

    private static final long serialVersionUID = 1L;
    static final Integer[] MEMORY_OPTIONS = {1024, 1536, 2048, 3072, 4096};

    final JTextField nick = new JTextField(20);
    final JComboBox<Profile> profile = new JComboBox<Profile>();
    final JComboBox<ServerEntry> server = new JComboBox<ServerEntry>();
    final JComboBox<Integer> memory = new JComboBox<Integer>(MEMORY_OPTIONS);
    final JCheckBox forceUpdate = new JCheckBox("Forcar atualizacao");
    final JButton settings = new JButton("Configuracoes");
    final JButton play = new JButton("Entrar");
    final JLabel error = new JLabel(" ", SwingConstants.CENTER);
    final JProgressBar bar = new JProgressBar();

    LoginForm() {
        super(new BorderLayout(0, 8));
        setBackground(Theme.PANEL);
        setBorder(new RetroBorder());
        error.setFont(Theme.ERROR_FONT);
        error.setForeground(Theme.ERROR);
        forceUpdate.setOpaque(false);
        bar.setForeground(Theme.PROGRESS);
        bar.setStringPainted(true);
        bar.setString("");
        add(error, BorderLayout.NORTH);
        add(fields(), BorderLayout.CENTER);
        add(bottom(), BorderLayout.SOUTH);
    }

    private JPanel fields() {
        JPanel labels = column(label("Login:"), label("Versao:"), label("Servidor:"), label("Memoria (MB):"));
        JPanel inputs = column(nick, profile, server, memory);
        JPanel grid = new JPanel(new BorderLayout(6, 0));
        grid.setOpaque(false);
        grid.add(labels, BorderLayout.WEST);
        grid.add(inputs, BorderLayout.CENTER);
        grid.add(forceUpdate, BorderLayout.SOUTH);
        return grid;
    }

    private JPanel bottom() {
        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setOpaque(false);
        buttons.add(settings, BorderLayout.WEST);
        buttons.add(play, BorderLayout.EAST);
        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setOpaque(false);
        south.add(buttons, BorderLayout.NORTH);
        south.add(bar, BorderLayout.SOUTH);
        return south;
    }

    private static JPanel column(Component... items) {
        JPanel p = new JPanel(new GridLayout(items.length, 1, 0, 4));
        p.setOpaque(false);
        for (Component c : items) p.add(c);
        return p;
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text, SwingConstants.RIGHT);
        l.setFont(Theme.BODY);
        l.setForeground(Theme.TEXT);
        return l;
    }

    void busy(boolean busy) {
        play.setEnabled(!busy);
        settings.setEnabled(!busy);
        profile.setEnabled(!busy);
        server.setEnabled(!busy);
    }
}
