package com.craftwar.launcher.ui;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.craftwar.launcher.LauncherApp;
import com.craftwar.launcher.Platform;
import com.craftwar.launcher.Selection;
import com.craftwar.launcher.manifest.Manifest;
import com.craftwar.launcher.manifest.Profile;
import com.craftwar.launcher.manifest.ServerEntry;

public final class LauncherWindow extends JFrame implements LauncherApp.View {

    private static final long serialVersionUID = 1L;
    private static final int TITLE_CLEARANCE = 90;

    private final transient LauncherApp app;
    private final LoginForm form = new LoginForm();

    public LauncherWindow(LauncherApp app) {
        super(app.title());
        this.app = app;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        DirtPanel background = new DirtPanel(DirtTile.load(Platform.launcherDir()), app.title());
        GridBagConstraints below = new GridBagConstraints();
        below.insets = new Insets(TITLE_CLEARANCE, 0, 0, 0);
        background.add(form, below);
        setContentPane(background);
        pack();
        setLocationRelativeTo(null);
        form.nick.setText(app.settings().nick());
        form.memory.setSelectedItem(MemoryChoice.initial(app.settings().memoryMbOr(2048)));
        form.play.addActionListener(e -> start());
        form.settings.addActionListener(e -> SettingsDialog.open(this, app));
        getRootPane().setDefaultButton(form.play);
        loadLists();
    }

    private void loadLists() {
        form.busy(true);
        stage("Carregando servidores");
        Thread t = new Thread(() -> {
            try {
                Manifest m = app.refresh();
                SwingUtilities.invokeLater(() -> populate(m));
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    form.busy(false);
                    showError("Sem conexao com a atualizacao: " + e.getMessage());
                });
            }
        }, "launcher-manifest");
        t.setDaemon(true);
        t.start();
    }

    private void populate(Manifest m) {
        form.profile.removeAllItems();
        form.server.removeAllItems();
        for (Profile p : m.profiles) form.profile.addItem(p);
        for (ServerEntry s : m.servers) form.server.addItem(s);
        form.profile.setSelectedItem(m.profile(app.settings().profile()));
        form.server.setSelectedItem(m.server(app.settings().server()));
        form.busy(false);
        idle();
    }

    private void start() {
        String name = form.nick.getText().trim();
        if (!LauncherApp.validNick(name)) {
            showError("Login invalido: 3 a 16 letras, numeros ou _");
            return;
        }
        showError(" ");
        Profile p = (Profile) form.profile.getSelectedItem();
        ServerEntry s = (ServerEntry) form.server.getSelectedItem();
        Selection sel = new Selection(name, (Integer) form.memory.getSelectedItem(),
            p == null ? app.settings().profile() : p.id, s == null ? app.settings().server() : s.id, form.forceUpdate.isSelected());
        form.busy(true);
        Thread t = new Thread(() -> app.play(sel, this), "launcher-play");
        t.setDaemon(true);
        t.start();
    }

    private void showError(String message) {
        form.error.setText(message);
    }

    private void idle() {
        form.bar.setIndeterminate(false);
        form.bar.setValue(0);
        form.bar.setString("");
    }

    @Override
    public void stage(String description) {
        SwingUtilities.invokeLater(() -> {
            form.bar.setString(description);
            form.bar.setIndeterminate(true);
        });
    }

    @Override
    public void advance(int done, int total) {
        SwingUtilities.invokeLater(() -> {
            form.bar.setIndeterminate(false);
            form.bar.setMaximum(Math.max(1, total));
            form.bar.setValue(done);
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
            idle();
            form.busy(false);
            showError(message);
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
