package io.github.doodle;

import io.github.doodle.interfaces.DLogger;

final class LogProxy {
    private static DLogger logger;

    static void register(DLogger realLogger) {
        logger = realLogger;
    }

    public static boolean isDebug() {
        return logger != null && logger.isDebug();
    }

    public static void e(String tag, Throwable t) {
        if (logger != null) {
            logger.e(tag, t);
        }
    }
}
