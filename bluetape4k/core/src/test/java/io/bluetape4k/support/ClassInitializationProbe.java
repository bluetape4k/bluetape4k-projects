package io.bluetape4k.support;

public final class ClassInitializationProbe {

    private static final String PROPERTY_NAME = "bluetape4k.class-support.probe.initialized";

    static {
        System.setProperty(PROPERTY_NAME, "true");
    }

    private ClassInitializationProbe() {
    }
}
