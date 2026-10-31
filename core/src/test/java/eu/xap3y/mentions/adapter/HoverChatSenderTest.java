package eu.xap3y.mentions.adapter;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class HoverChatSenderTest {

    @Test
    public void createsSenderWhenComponentApiIsAvailable() {
        assertNotNull(HoverChatSender.create());
    }

    @Test
    public void listenerLoadsAndHoverIsSkippedWithoutTextComponent() throws Exception {
        assertMissingComponentIsSafe("net.md_5.bungee.api.chat.TextComponent");
    }

    @Test
    public void listenerLoadsAndHoverIsSkippedWithoutAnyBungeeClasses() throws Exception {
        assertMissingComponentIsSafe("net.md_5.bungee.");
    }

    private static void assertMissingComponentIsSafe(String missingPrefix) throws Exception {
        ClassLoader loader = new ClassLoader(HoverChatSenderTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                synchronized (getClassLoadingLock(name)) {
                    if (name.startsWith(missingPrefix)) {
                        throw new ClassNotFoundException(name);
                    }
                    if (!name.startsWith("eu.xap3y.mentions.")) {
                        return super.loadClass(name, resolve);
                    }

                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        String resource = name.replace('.', '/') + ".class";
                        try (InputStream input = getParent().getResourceAsStream(resource)) {
                            if (input == null) {
                                throw new ClassNotFoundException(name);
                            }
                            ByteArrayOutputStream output = new ByteArrayOutputStream();
                            byte[] buffer = new byte[4096];
                            int count;
                            while ((count = input.read(buffer)) != -1) {
                                output.write(buffer, 0, count);
                            }
                            byte[] bytes = output.toByteArray();
                            loaded = defineClass(name, bytes, 0, bytes.length);
                        } catch (IOException exception) {
                            throw new ClassNotFoundException(name, exception);
                        }
                    }
                    if (resolve) {
                        resolveClass(loaded);
                    }
                    return loaded;
                }
            }
        };

        Class<?> sender = Class.forName("eu.xap3y.mentions.adapter.HoverChatSender", true, loader);
        assertNull(sender.getMethod("create").invoke(null));
        Class<?> listener = Class.forName("eu.xap3y.mentions.listeners.ChatListener", true, loader);
        assertTrue(listener.getDeclaredMethods().length > 0);
        assertEquals(1, listener.getDeclaredConstructors().length);
    }
}
