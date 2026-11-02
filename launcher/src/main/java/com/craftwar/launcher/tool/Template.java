package com.craftwar.launcher.tool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.craftwar.launcher.manifest.Manifest;
import com.craftwar.launcher.manifest.ServerEntry;

final class Template {

    String name;
    String cdnBaseUrl;
    String agentJar;
    Manifest.Consent consent = new Manifest.Consent();
    Manifest.Java java = new Manifest.Java();
    List<ServerEntry> servers = new ArrayList<ServerEntry>();
    List<ProfileTemplate> profiles = new ArrayList<ProfileTemplate>();

    static final class ProfileTemplate {
        String id;
        String label;
        String instance;
        String versionId;
        String agentMapping;
        String cdnBaseUrl;
    }

    static File relativeTo(File templateFile, String path) {
        File f = new File(path);
        return f.isAbsolute() ? f : new File(templateFile.getAbsoluteFile().getParentFile(), path);
    }
}
