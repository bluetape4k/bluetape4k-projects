package io.bluetape4k.concurrent.virtualthread.jdk25;

import io.bluetape4k.concurrent.virtualthread.VirtualThreadRuntime;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Java 25 기준 Virtual Thread 구현체입니다.
 * <p>
 * Java 25 전용 최적화가 필요할 경우 이 클래스에 반영합니다.
 */
public final class Jdk25VirtualThreadRuntime implements VirtualThreadRuntime {

    @Override
    public @NotNull String getRuntimeName() {
        return "jdk25";
    }

    @Override
    public int getPriority() {
        return 25;
    }

    @Override
    public boolean isSupported() {
        return Runtime.version().feature() >= 25;
    }

    @Override
    public @NotNull ThreadFactory threadFactory(@NotNull String prefix) {
        return Thread.ofVirtual().name(prefix, 0).factory();
    }

    @Override
    public @NotNull ExecutorService executorService() {
        return Executors.newThreadPerTaskExecutor(threadFactory("vt25-"));
    }
}
