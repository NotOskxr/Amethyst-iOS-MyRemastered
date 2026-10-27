# iOS MacosUtil / JNA startup fix

Minecraft 26.3's MacosUtil identifies macOS through Util.getPlatform(), which
follows the launcher's macOS identity. disableCloseWindowMenuItem then calls
java-objc-bridge (NSApplication/windowsMenu), causing JNA's desktop native load.
The existing source stub was stripped from launcher.jar by the Forge split-package
build fix, so it could not intercept that call.

Move the existing no-op class into patchjna_agent.jar as a `.class.patch` resource
and replace the game class at definition time. No second Mojang classpath package
is exported. The agent is already installed by the iOS native launcher. It keeps
the existing JNA Platform replacement and does not change OS identity, JNA versions,
code signing, native AppKit stubs, or library loading policy.

Only the exact MacosUtil class name with both 26.3 SDL setter names is replaced.
Older mapped clients (including Forge) have different fullscreen/icon helpers;
those classes must remain untouched. Older obfuscated/intermediary names also
remain untouched. This replacement mirrors the public API of the 26.3 release;
future Minecraft versions adding methods will require another compatibility review.

## Validation

Run with JDK 8 (BOOTJDK is the directory containing javac/java/jar):

```sh
BOOTJDK=/path/to/jdk8/bin bash JavaApp/tests/test-macosutil-agent.sh
```

Optionally test an additional runtime and a locally downloaded official 26.3 client:

```sh
BOOTJDK=/path/to/jdk8/bin \
JAVA_RUNTIME=/path/to/jdk25/bin/java \
MC_CLIENT_JAR=/path/to/26.3-client.jar \
bash JavaApp/tests/test-macosutil-agent.sh
```

The test uses the production agent packaging recipe, verifies no exported Mojang
class, checks unrelated and older mapped classes are left alone, and loads both
MacosUtil and JNA Platform through a parentless game loader. Its control run must
fail without the agent at a simulated native load. The instrumented run must pass
all 26.3 public methods/field checks with full JVM verification. This exercises
class loading, not a complete Fabric/Forge launch or gameplay.

Locally passed on Temurin 8, OpenJDK 21 and Temurin 25, with the official Minecraft
26.3 client on Java 25. The actual 26.3 public API was checked with javap; the
1.21.11 mappings confirm its different older API. The full Java make target was
attempted on Linux and stopped at PojavLauncher's existing com.apple.eawt dependency
(an Apple JDK API). Native iOS compilation requires the existing macOS Actions job;
the new regression step runs before that build. The regular IPA artifact can then
be signed by SideStore. No on-device launch has been verified.

## Device acceptance

Install the regular iOS IPA from the PR branch's Development build using SideStore.
Enable JIT and launch 26.3 with Java 25. Look for
`PatchJNAAgent: Replacing com/mojang/blaze3d/platform/MacosUtil` and check the main
menu loads without the reported extracted libjnidispatch signature error. Then
check 1.21.11, Fabric with Sodium, and Forge as applicable. If any launch fails,
collect latestlog.txt; other JNA consumers may expose separate unsupported native
paths, which this targeted fix does not address.
