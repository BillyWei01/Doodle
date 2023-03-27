package io.github.doodle.interfaces;

public interface DLogger {
    boolean isDebug();

    void e(String tag, Throwable t);
}
