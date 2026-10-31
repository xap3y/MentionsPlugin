package eu.xap3y.mentions.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionChecker {

    static final String MODRINTH = "https://api.modrinth.com/v2/project/mEkLhCOx/version?include_changelog=false";
    static final String SPIGOT = "https://api.spigotmc.org/legacy/update.php?resource=137565";
    private static final Pattern VERSION = Pattern.compile(
            "^[vV]?(\\d+(?:\\.\\d+)*)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$");
    private final HttpReader reader;

    interface HttpReader {
        String read(String url) throws IOException;
    }

    public VersionChecker(String userAgent) {
        this.reader = url -> readHttp(url, userAgent);
    }

    VersionChecker(HttpReader reader) {
        this.reader = reader;
    }

    public String check(String installed) {
        String latest;
        try {
            latest = parseModrinth(reader.read(MODRINTH));
        } catch (IOException | RuntimeException error) {
            try {
                latest = reader.read(SPIGOT).trim();
                if (!VERSION.matcher(latest).matches()) {
                    return null;
                }
            } catch (IOException | RuntimeException ignored) {
                return null;
            }
        }
        return latest != null && isNewer(latest, installed) ? latest : null;
    }

    static String parseModrinth(String json) throws IOException {
        String latest = null;
        for (JsonElement element : new JsonParser().parse(json).getAsJsonArray()) {
            if (!"release".equals(element.getAsJsonObject().get("version_type").getAsString())) {
                continue;
            }
            String version = element.getAsJsonObject().get("version_number").getAsString();
            if (!VERSION.matcher(version).matches()) {
                throw new IOException("Invalid version");
            }
            if (latest == null || isNewer(version, latest)) {
                latest = version;
            }
        }
        return latest;
    }

    public static boolean isNewer(String candidate, String installed) {
        Matcher next = VERSION.matcher(candidate.trim());
        Matcher current = VERSION.matcher(installed.trim());
        if (!next.matches() || !current.matches()) {
            return false;
        }
        String[] a = next.group(1).split("\\.");
        String[] b = current.group(1).split("\\.");
        for (int index = 0; index < Math.max(a.length, b.length); index++) {
            BigInteger left = index < a.length ? new BigInteger(a[index]) : BigInteger.ZERO;
            BigInteger right = index < b.length ? new BigInteger(b[index]) : BigInteger.ZERO;
            int comparison = left.compareTo(right);
            if (comparison != 0) {
                return comparison > 0;
            }
        }
        String preA = next.group(2);
        String preB = current.group(2);
        if (preA == null || preB == null) {
            return preA == null && preB != null;
        }
        String[] partsA = preA.split("\\.");
        String[] partsB = preB.split("\\.");
        for (int index = 0; index < Math.min(partsA.length, partsB.length); index++) {
            String left = partsA[index];
            String right = partsB[index];
            boolean numericA = left.matches("[0-9]+");
            boolean numericB = right.matches("[0-9]+");
            int comparison = numericA && numericB
                    ? new BigInteger(left).compareTo(new BigInteger(right))
                    : numericA != numericB ? (numericA ? -1 : 1) : left.compareTo(right);
            if (comparison != 0) {
                return comparison > 0;
            }
        }
        return partsA.length > partsB.length;
    }

    private static String readHttp(String url, String userAgent) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept", "application/json, text/plain");
        try {
            if (connection.getResponseCode() != 200) {
                throw new IOException("HTTP request failed");
            }
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (output.size() + count > 262144) {
                        throw new IOException("Response too large");
                    }
                    output.write(buffer, 0, count);
                }
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }
}
