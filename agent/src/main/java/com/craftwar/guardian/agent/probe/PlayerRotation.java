package com.craftwar.guardian.agent.probe;

import java.lang.reflect.Field;

import com.craftwar.guardian.agent.Health;

final class PlayerRotation {

    float yaw = Float.NaN;
    float pitch = Float.NaN;
    private String yawName;
    private String pitchName;
    private Field yawField;
    private Field pitchField;
    private boolean broken;

    void configure(String yawField, String pitchField) {
        yawName = yawField;
        pitchName = pitchField;
    }

    void read(Object player) throws IllegalAccessException {
        if (yawField == null && !resolve(player.getClass())) return;
        yaw = yawField.getFloat(player);
        pitch = pitchField.getFloat(player);
    }

    private boolean resolve(Class<?> type) {
        if (broken || yawName == null) return false;
        yawField = find(type, yawName);
        pitchField = find(type, pitchName);
        if (yawField != null && pitchField != null) return true;
        yawField = null;
        pitchField = null;
        broken = true;
        Health.degrade("rotation fields not found: " + yawName + ", " + pitchName);
        return false;
    }

    private static Field find(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                if (f.getType() != float.class) return null;
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
