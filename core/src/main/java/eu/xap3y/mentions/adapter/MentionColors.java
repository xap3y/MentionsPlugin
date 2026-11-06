package eu.xap3y.mentions.adapter;

import java.lang.reflect.Method;

public final class MentionColors {

    private static final Method CONVERTER = findConverter();

    private MentionColors() {
    }

    private static Method findConverter() {
        try {
            return Class.forName("eu.xap3y.mentions.adapter.HexUtil")
                    .getMethod("toLegacy", String.class, boolean.class);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static String color(String text, boolean rgbSupported) {
        if (CONVERTER != null) {
            try {
                return (String) CONVERTER.invoke(null, text, rgbSupported);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // Older servers can still translate legacy and HEX colors without Adventure.
            }
        }
        return LegacyColors.color(text, rgbSupported);
    }
}
