import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import net.kdt.patchjna.PatchJNAAgent;

public final class AgentRegression {
    public static void main(String[] args) throws Exception {
        try (JarFile agent = new JarFile(args[0])) {
            if (agent.getEntry("com/mojang/blaze3d/platform/MacosUtil.class.patch") == null
                    || agent.getEntry("com/sun/jna/Platform.class.patch") == null) {
                throw new AssertionError("Missing replacement resources");
            }
            if (agent.stream().anyMatch(e -> e.getName().startsWith("com/mojang/")
                    && e.getName().endsWith(".class"))) {
                throw new AssertionError("Agent exports a Mojang package (Forge conflict)");
            }
        }
        PatchJNAAgent transformer = new PatchJNAAgent();
        byte[] unrelated = { 1, 2, 3 };
        if (transformer.transform(null, "example/Unrelated", null, null, unrelated) != null
                || transformer.transform(null, null, null, null, unrelated) != null) {
            throw new AssertionError("Unrelated classes must remain untouched");
        }
        byte[] oldMac = Files.readAllBytes(Paths.get(args[2]));
        if (transformer.transform(null, "com/mojang/blaze3d/platform/MacosUtil",
                null, null, oldMac) != null) {
            throw new AssertionError("Older mapped MacosUtil must remain untouched");
        }
        // A parentless loader models game isolation: the replacement must be
        // defined in the game's loader, not supplied by launcher.jar shadowing.
        try (URLClassLoader game = new URLClassLoader(
                new URL[] { new File(args[1]).toURI().toURL(),
                    new File(args[3]).toURI().toURL() }, null)) {
            Class<?> mac = Class.forName("com.mojang.blaze3d.platform.MacosUtil", true, game);
            if (mac.getClassLoader() != game || mac.getField("IS_MACOS").getBoolean(null)) {
                throw new AssertionError("Wrong loader or macOS flag");
            }
            mac.getMethod("disableCloseWindowMenuItem").invoke(null);
            for (boolean value : new boolean[] { false, true }) {
                mac.getMethod("setCtrlClickEmulatesRightClick", boolean.class).invoke(null, value);
                mac.getMethod("setFullscreenMenuVisibility", boolean.class).invoke(null, value);
            }
            Class<?> platform = Class.forName("com.sun.jna.Platform", true, game);
            if (!Integer.valueOf(0).equals(platform.getMethod("getOSType").invoke(null))) {
                throw new AssertionError("Existing JNA Platform replacement stopped working");
            }
        }
        System.out.println("PASS: isolated game loader bypasses desktop native initialization; JNA patch preserved");
    }
}
