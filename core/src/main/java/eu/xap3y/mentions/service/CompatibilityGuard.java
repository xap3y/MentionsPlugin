package eu.xap3y.mentions.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class CompatibilityGuard {

    private final Logger logger;
    private final Set<String> unavailableFeatures = ConcurrentHashMap.newKeySet();

    public CompatibilityGuard(Logger logger) {
        this.logger = logger;
    }

    public boolean run(String feature, Runnable action) {
        if (unavailableFeatures.contains(feature)) {
            return false;
        }
        try {
            action.run();
            return true;
        } catch (RuntimeException | LinkageError error) {
            warnOnce(feature, error);
            return false;
        }
    }

    public void warnOnce(String feature, Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        String reason = cause.getClass().getSimpleName();
        if (message != null && !message.isEmpty()) {
            reason += ": " + message;
        }
        warnOnce(feature, reason);
    }

    public void warnOnce(String feature, String reason) {
        if (unavailableFeatures.add(feature)) {
            String singleLine = reason.replace('\n', ' ').replace('\r', ' ');
            if (singleLine.length() > 240) {
                singleLine = singleLine.substring(0, 240);
            }
            // Do not attach the exception: Bukkit would print the complete stacktrace.
            logger.warning(feature + " disabled until restart: " + singleLine);
        }
    }
}
