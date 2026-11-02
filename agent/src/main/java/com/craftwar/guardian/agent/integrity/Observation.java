package com.craftwar.guardian.agent.integrity;

import java.net.URL;

final class Observation {

    final boolean redefinition;
    final String className;
    final String loader;
    final URL location;

    Observation(boolean redefinition, String className, String loader, URL location) {
        this.redefinition = redefinition;
        this.className = className;
        this.loader = loader;
        this.location = location;
    }
}
