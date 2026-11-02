package com.craftwar.launcher.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

final class ConsentDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private boolean accepted;

    private ConsentDialog(Frame owner, String text) {
        super(owner, "Anti-cheat do servidor", true);
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        Theme.field(area);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(520, 380));
        JButton decline = new JButton("Nao aceito");
        JButton accept = Theme.primary("Aceito e jogar");
        decline.addActionListener(e -> close(false));
        accept.addActionListener(e -> close(true));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setBackground(Theme.BACKGROUND);
        buttons.add(decline);
        buttons.add(accept);
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(Theme.padding(18, 18));
        root.add(scroll, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
    }

    private void close(boolean result) {
        accepted = result;
        dispose();
    }

    static boolean ask(Frame owner, String text) {
        ConsentDialog d = new ConsentDialog(owner, text);
        d.setVisible(true);
        return d.accepted;
    }
}
