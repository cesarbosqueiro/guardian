package com.craftwar.launcher.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.craftwar.launcher.LauncherApp;
import com.craftwar.launcher.Platform;

public final class LauncherWindow extends JFrame implements LauncherApp.View {

    private static final long serialVersionUID = 1L;
    private static final Integer[] MEMORY_OPTIONS = {1024, 1536, 2048, 3072, 4096};

    private final transient LauncherApp app;
    private final JTextField nick = new JTextField(16);
    private final JComboBox<Integer> memory = new JComboBox<Integer>(MEMORY_OPTIONS);
    private final JButton play = Theme.primary("JOGAR");
    private final JProgressBar bar = new JProgressBar();
    private final JLabel status = new JLabel(" ");

    public LauncherWindow(LauncherApp app) {
        super(app.title());
        this.app = app;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(layout(app));
        pack();
        setLocationRelativeTo(null);
        play.addActionListener(e -> start());
        getRootPane().setDefaultButton(play);
    }

    private JPanel layout(LauncherApp app) {
        JLabel title = new JLabel(app.title());
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.TEXT);
        nick.setText(app.settings().nick());
        Theme.field(nick);
        Theme.field(memory);
        memory.setSelectedItem(defaultMemory(app.settings().memoryMbOr(2048)));
        status.setForeground(Theme.MUTED);
        status.setFont(Theme.BODY);
        bar.setBorderPainted(false);
        bar.setBackground(Theme.SURFACE);
        bar.setForeground(Theme.ACCENT);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 0, 4, 12);
        c.anchor = GridBagConstraints.WEST;
        form.add(label("Nick"), c);
        c.gridx = 1;
        form.add(nick, c);
        c.gridx = 0;
        c.gridy = 1;
        form.add(label("Memoria (MB)"), c);
        c.gridx = 1;
        form.add(memory, c);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.add(play, BorderLayout.NORTH);
        south.add(bar, BorderLayout.CENTER);
        south.add(status, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(Theme.padding(24, 28));
        root.add(title, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        return root;
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Theme.MUTED);
        l.setFont(Theme.BODY);
        return l;
    }

    private static Integer defaultMemory(int saved) {
        long physical = Platform.physicalMemoryMb();
        Integer best = MEMORY_OPTIONS[0];
        for (Integer m : MEMORY_OPTIONS) {
            if (m <= saved && m <= physical / 2) best = m;
        }
        return best;
    }

    private void start() {
        String name = nick.getText().trim();
        if (!LauncherApp.validNick(name)) {
            failed("Nick invalido: use 3 a 16 caracteres (letras, numeros, _).");
            return;
        }
        int mb = (Integer) memory.getSelectedItem();
        play.setEnabled(false);
        Thread t = new Thread(() -> app.play(name, mb, this), "launcher-play");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void stage(String description) {
        SwingUtilities.invokeLater(() -> {
            status.setText(description);
            bar.setIndeterminate(true);
        });
    }

    @Override
    public void advance(int done, int total) {
        SwingUtilities.invokeLater(() -> {
            bar.setIndeterminate(false);
            bar.setMaximum(Math.max(1, total));
            bar.setValue(done);
        });
    }

    @Override
    public boolean askConsent(String text) {
        AtomicBoolean result = new AtomicBoolean();
        try {
            SwingUtilities.invokeAndWait(() -> result.set(ConsentDialog.ask(this, text)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            return false;
        }
        return result.get();
    }

    @Override
    public void notice(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message, app.title(), JOptionPane.INFORMATION_MESSAGE));
    }

    @Override
    public void failed(String message) {
        SwingUtilities.invokeLater(() -> {
            bar.setIndeterminate(false);
            bar.setValue(0);
            status.setText(" ");
            play.setEnabled(true);
            JOptionPane.showMessageDialog(this, message, app.title(), JOptionPane.ERROR_MESSAGE);
        });
    }

    @Override
    public void launched() {
        SwingUtilities.invokeLater(() -> {
            dispose();
            System.exit(0);
        });
    }
}
