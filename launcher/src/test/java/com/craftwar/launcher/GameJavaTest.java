package com.craftwar.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GameJavaTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File executable(String relativeBin, String name) throws IOException {
        File bin = new File(tmp.getRoot(), relativeBin);
        if (!bin.isDirectory() && !bin.mkdirs()) throw new IOException("cannot create " + bin);
        File exe = new File(bin, name);
        if (!exe.createNewFile()) throw new IOException("cannot create " + exe);
        return exe;
    }

    @Test
    public void portableJreWinsOverJavaHome() throws Exception {
        File portable = executable("jre/bin", "javaw.exe");
        assertEquals(portable, GameJava.find(tmp.getRoot(), null));
    }

    @Test
    public void craftLandiaLayoutIsAccepted() throws Exception {
        File portable = executable("CraftLandia/bin/java/windows-x64/bin", "javaw.exe");
        assertEquals(portable, GameJava.find(tmp.getRoot(), null));
    }

    @Test
    public void craftWarJreWinsOverCraftLandiaOne() throws Exception {
        File preferred = executable("jre/bin", "javaw.exe");
        executable("CraftLandia/bin/java/windows-x64/bin", "javaw.exe");
        assertEquals(preferred, GameJava.find(tmp.getRoot(), null));
    }

    @Test
    public void javawWinsOverJava() throws Exception {
        File javaw = executable("jre/bin", "javaw.exe");
        executable("jre/bin", "java.exe");
        assertEquals(javaw, GameJava.find(tmp.getRoot(), null));
    }

    @Test
    public void overrideWinsOverEverything() throws Exception {
        executable("jre/bin", "javaw.exe");
        File chosen = executable("outro/bin", "javaw.exe");
        assertEquals(chosen, GameJava.find(tmp.getRoot(), chosen.getAbsolutePath()));
        assertEquals(chosen, GameJava.find(tmp.getRoot(), "outro/bin"));
        assertEquals(chosen, GameJava.find(tmp.getRoot(), "outro\\bin\\javaw.exe"));
    }

    @Test
    public void blankOverrideIsIgnored() throws Exception {
        File portable = executable("jre/bin", "javaw.exe");
        assertEquals(portable, GameJava.find(tmp.getRoot(), "   "));
    }

    @Test
    public void javaHomeIsTheLastResort() {
        List<File> candidates = GameJava.candidates(tmp.getRoot(), null);
        File last = candidates.get(candidates.size() - 1);
        assertTrue(last.getPath().startsWith(new File(System.getProperty("java.home"), "bin").getPath()));
        for (String bin : GameJava.PORTABLE_BINS) {
            File first = new File(tmp.getRoot(), bin + "/javaw.exe");
            assertTrue(bin + " deve vir antes do java.home",
                candidates.indexOf(first) < candidates.indexOf(last));
        }
    }

    @Test
    public void failureNamesEveryPlaceItLooked() {
        // java.home sempre existe no ambiente de teste, entao a busca so falha
        // quando nem ele tem um executavel -- forcado aqui com um java.home falso.
        String home = System.getProperty("java.home");
        System.setProperty("java.home", new File(tmp.getRoot(), "sem-java").getAbsolutePath());
        try {
            GameJava.find(tmp.getRoot(), null);
            fail("deveria ter falhado");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("jre"));
            assertTrue(e.getMessage().contains("CraftLandia"));
        } finally {
            System.setProperty("java.home", home);
        }
    }
}
