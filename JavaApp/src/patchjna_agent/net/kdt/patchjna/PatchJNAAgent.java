package net.kdt.patchjna;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.nio.charset.StandardCharsets;

public class PatchJNAAgent implements ClassFileTransformer {
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        // This agent is installed by the iOS launcher. Keep the macOS identity
        // used by JNA/mods, but bypass Minecraft's unsupported AppKit menu code.
        // Transform in the game's own loader (including Fabric/Forge), rather
        // than exporting a second com.mojang.blaze3d package from launcher.jar.
        if (!"com/sun/jna/Platform".equals(className)
                && !"com/mojang/blaze3d/platform/MacosUtil".equals(className)) {
            return null;
        }
        // Older Forge versions can also use the mapped MacosUtil name, but
        // expose a different API (fullscreen/icon helpers). Do not replace it.
        if ("com/mojang/blaze3d/platform/MacosUtil".equals(className)
                && (!hasUtf8Constant(classfileBuffer, "setCtrlClickEmulatesRightClick")
                    || !hasUtf8Constant(classfileBuffer, "setFullscreenMenuVisibility"))) {
            return null;
        }
        String resource = className + ".class.patch";
        try (InputStream input = PatchJNAAgent.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Missing agent resource: " + resource);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            System.out.println("PatchJNAAgent: Replacing " + className);
            return output.toByteArray();
        } catch (IOException e) {
            // Retain the original class if the agent bundle is damaged.
            e.printStackTrace();
            return null;
        }
    }

    private static boolean hasUtf8Constant(byte[] bytes, String name) {
        // Match a complete CONSTANT_Utf8 entry: tag, u2 length, then ASCII name.
        byte[] utf8 = name.getBytes(StandardCharsets.UTF_8);
        for (int i = 10; i <= bytes.length - utf8.length - 3; i++) {
            if (bytes[i] != 1 || (bytes[i + 1] & 0xff) != (utf8.length >>> 8)
                    || (bytes[i + 2] & 0xff) != (utf8.length & 0xff)) {
                continue;
            }
            int j = 0;
            while (j < utf8.length && bytes[i + 3 + j] == utf8[j]) j++;
            if (j == utf8.length) return true;
        }
        return false;
    }

    public static void premain(String args, Instrumentation instrumentation) {
        System.out.println("PatchJNAAgent: premain called");
        instrumentation.addTransformer(new PatchJNAAgent());
    }
}
