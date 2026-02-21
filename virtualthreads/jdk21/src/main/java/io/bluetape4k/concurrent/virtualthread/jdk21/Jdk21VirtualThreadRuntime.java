package io.bluetape4k.concurrent.virtualthread.jdk21;

import io.bluetape4k.concurrent.virtualthread.VirtualThreadRuntime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Java 21 기준 Virtual Thread 구현체입니다.
 */
public final class Jdk21VirtualThreadRuntime implements VirtualThreadRuntime {

    @Override
    public String getRuntimeName() {
        return "jdk21";
    }

    @Override
    public int getPriority() {
        return 21;
    }

    @Override
    public boolean isSupported() {
        return Runtime.version().feature() >= 21;
    }

    @Override
    public ThreadFactory threadFactory(String prefix) {
        return Thread.ofVirtual().name(prefix, 0).factory();
    }

    @Override
    public ExecutorService executorService() {
        return Executors.newThreadPerTaskExecutor(threadFactory("vt21-"));
    }
}

