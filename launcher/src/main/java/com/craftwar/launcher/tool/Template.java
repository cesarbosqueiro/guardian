package com.craftwar.launcher.tool;

import com.craftwar.launcher.manifest.Manifest;

final class Template {

    int version;
    String name;
    String versionId;
    String cdnBaseUrl;
    String agentJar;
    String agentMapping = "forge-1.8.9";
    Manifest.Server server = new Manifest.Server();
    Manifest.Consent consent = new Manifest.Consent();
    Manifest.Java java = new Manifest.Java();

    String cdn(String path) {
        String base = cdnBaseUrl.endsWith("/") ? cdnBaseUrl : cdnBaseUrl + "/";
        return base + path;
    }
}
