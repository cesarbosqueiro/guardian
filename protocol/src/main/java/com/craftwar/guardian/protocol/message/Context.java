package com.craftwar.guardian.protocol.message;

import java.util.List;

import com.craftwar.guardian.protocol.Reader;
import com.craftwar.guardian.protocol.Writer;

public final class Context {

    private static final int MAX_ITEMS = 512;

    public final String agentVersion;
    public final String mapping;
    public final int agentState;
    public final String javaVersion;
    public final String os;
    public final String gpu;
    public final int width;
    public final int height;
    public final float sensitivity;
    public final boolean smoothCamera;
    public final float gamma;
    public final List<String> jvmArguments;
    public final List<String> mods;
    public final List<String> resourcePacks;

    public Context(String agentVersion, String mapping, int agentState, String javaVersion, String os, String gpu,
                   int width, int height, float sensitivity, boolean smoothCamera, float gamma,
                   List<String> jvmArguments, List<String> mods, List<String> resourcePacks) {
        this.agentVersion = agentVersion;
        this.mapping = mapping;
        this.agentState = agentState;
        this.javaVersion = javaVersion;
        this.os = os;
        this.gpu = gpu;
        this.width = width;
        this.height = height;
        this.sensitivity = sensitivity;
        this.smoothCamera = smoothCamera;
        this.gamma = gamma;
        this.jvmArguments = jvmArguments;
        this.mods = mods;
        this.resourcePacks = resourcePacks;
    }

    public byte[] encode() {
        return new Writer(1024).str(agentVersion).str(mapping).u8(agentState).str(javaVersion).str(os).str(gpu)
            .u16(width).u16(height).f32(sensitivity).bool(smoothCamera).f32(gamma)
            .strings(jvmArguments, MAX_ITEMS).strings(mods, MAX_ITEMS).strings(resourcePacks, MAX_ITEMS).toArray();
    }

    public static Context decode(byte[] payload) {
        Reader r = new Reader(payload);
        Context c = new Context(r.str(), r.str(), r.u8(), r.str(), r.str(), r.str(), r.u16(), r.u16(),
            r.f32(), r.bool(), r.f32(), r.strings(), r.strings(), r.strings());
        r.requireEnd();
        return c;
    }
}
