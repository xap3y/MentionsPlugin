package eu.xap3y.mentions.service;

import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class VersionCheckerTest {

    @Test
    public void modrinthHasPriorityAndSelectsNewestRelease() {
        List<String> requests = new ArrayList<>();
        VersionChecker checker = new VersionChecker(url -> {
            requests.add(url);
            return "[{\"version_type\":\"release\",\"version_number\":\"1.9.0\"},"
                    + "{\"version_type\":\"beta\",\"version_number\":\"3.0.0-beta\"},"
                    + "{\"version_type\":\"release\",\"version_number\":\"1.10.0\"}]";
        });
        assertEquals("1.10.0", checker.check("1.5.0"));
        assertEquals(Arrays.asList(VersionChecker.MODRINTH), requests);
    }

    @Test
    public void timeoutFallsBackToSpigot() {
        List<String> requests = new ArrayList<>();
        VersionChecker checker = new VersionChecker(url -> {
            requests.add(url);
            if (url.equals(VersionChecker.MODRINTH)) {
                throw new SocketTimeoutException("timeout");
            }
            return "1.6.0\n";
        });
        assertEquals("1.6.0", checker.check("1.5.0"));
        assertEquals(Arrays.asList(VersionChecker.MODRINTH, VersionChecker.SPIGOT), requests);
    }

    @Test
    public void malformedModrinthResponseFallsBack() {
        VersionChecker checker = new VersionChecker(url -> url.equals(VersionChecker.MODRINTH)
                ? "<html>Error</html>" : "1.6.0");
        assertEquals("1.6.0", checker.check("1.5.0"));
    }

    @Test
    public void bothFailuresAndInvalidSpigotResponsesDoNotAnnounceAnUpdate() {
        VersionChecker failed = new VersionChecker(url -> {
            throw new IOException("HTTP 503");
        });
        assertNull(failed.check("1.5.0"));
        VersionChecker invalid = new VersionChecker(url -> {
            if (url.equals(VersionChecker.MODRINTH)) {
                throw new IOException("HTTP 500");
            }
            return "Service unavailable";
        });
        assertNull(invalid.check("1.5.0"));
    }

    @Test
    public void equalOrOlderModrinthVersionDoesNotTriggerFallback() {
        VersionChecker checker = new VersionChecker(url -> {
            assertEquals(VersionChecker.MODRINTH, url);
            return "[{\"version_type\":\"release\",\"version_number\":\"1.4.2\"}]";
        });
        assertNull(checker.check("1.5.0"));
        assertNull(checker.check("1.4.2"));
    }

    @Test
    public void comparesNumericVersionsAndPrereleases() {
        assertTrue(VersionChecker.isNewer("v1.10.0", "1.9.9"));
        assertTrue(VersionChecker.isNewer("1.5.0", "1.5.0-SNAPSHOT"));
        assertTrue(VersionChecker.isNewer("1.5.0-rc.10", "1.5.0-rc.2"));
        assertFalse(VersionChecker.isNewer("1.5.0-beta", "1.5.0"));
        assertFalse(VersionChecker.isNewer("1.5.0+build.2", "1.5.0+build.1"));
        assertFalse(VersionChecker.isNewer("1.5", "1.5.0"));
        assertFalse(VersionChecker.isNewer("error", "1.5.0"));
    }
}
