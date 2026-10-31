package eu.xap3y.mentions.service;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class CompatibilityGuardTest {

    @Test
    public void missingBaseComponentLogsOneWarningWithoutStacktraceAndSkipsFurtherAttempts() {
        List<LogRecord> records = new ArrayList<>();
        CompatibilityGuard guard = new CompatibilityGuard(capturingLogger(records));
        AtomicInteger attempts = new AtomicInteger();
        Runnable action = () -> {
            attempts.incrementAndGet();
            NoClassDefFoundError error = new NoClassDefFoundError("net/md_5/bungee/api/chat/BaseComponent");
            error.initCause(new ClassNotFoundException("net.md_5.bungee.api.chat.BaseComponent"));
            throw error;
        };

        assertFalse(guard.run("Mention action bar", action));
        assertFalse(guard.run("Mention action bar", action));
        assertEquals(1, attempts.get());
        assertEquals(1, records.size());
        assertEquals(Level.WARNING, records.get(0).getLevel());
        assertTrue(records.get(0).getMessage().contains("ClassNotFoundException: net.md_5.bungee.api.chat.BaseComponent"));
        assertNull(records.get(0).getThrown());
    }

    @Test
    public void unavailableHoverDoesNotDisableOtherNotifications() {
        List<LogRecord> records = new ArrayList<>();
        CompatibilityGuard guard = new CompatibilityGuard(capturingLogger(records));
        guard.warnOnce("Mention hover", "component API unavailable");
        guard.warnOnce("Mention hover", "component API unavailable");
        AtomicInteger notifications = new AtomicInteger();

        assertTrue(guard.run("Mention titles", notifications::incrementAndGet));
        assertEquals(1, notifications.get());
        assertEquals(1, records.size());
    }

    @Test
    public void runtimeFailureProducesASingleLineWarning() {
        List<LogRecord> records = new ArrayList<>();
        CompatibilityGuard guard = new CompatibilityGuard(capturingLogger(records));

        assertFalse(guard.run("Mention titles", () -> {
            throw new IllegalStateException("Unsupported API\nDetails\rMore details");
        }));

        assertEquals(1, records.size());
        assertFalse(records.get(0).getMessage().contains("\n"));
        assertFalse(records.get(0).getMessage().contains("\r"));
        assertNull(records.get(0).getThrown());
    }

    private static Logger capturingLogger(List<LogRecord> records) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return logger;
    }
}
