package com.mojang.blaze3d.platform;

/**
 * iOS replacement for Minecraft's MacosUtil, installed by PatchJNAAgent.
 *
 * The launcher reports os.name=Mac OS X, so Minecraft takes its macOS code
 * paths. MacosUtil drives AppKit through ca.weblite java-objc-bridge
 * (NSApplication / windowsMenu), which does not exist on iOS and crashes the
 * game during initialization.
 *
 * The compiled class is stored as a .class.patch resource in patchjna_agent.jar.
 * The agent replaces the game class before its static initializer can load JNA.
 * It is not an exported classpath package, avoiding Forge split-package errors.
 * Older obfuscated versions do not match the agent's exact class name filter.
 *
 * IMPORTANT: this class must mirror every member Minecraft actually calls.
 * A missing member surfaces as NoSuchMethodError / NoSuchFieldError at
 * runtime, not as a compile error. Known call sites (26.3-rc1):
 *   - Window.<init>                 -> disableCloseWindowMenuItem()
 *   - Options.<init>                -> setCtrlClickEmulatesRightClick(boolean)
 *                                      setFullscreenMenuVisibility(boolean)
 *   - Minecraft.<init>              -> both setters above
 *   - VideoSettingsScreen           -> IS_MACOS
 * The two setters only forward SDL hints in vanilla and were added in
 * 26.3 snapshot 9 (macOS "Right Click Emulation"); they are no-ops here.
 */
public final class MacosUtil {
    private MacosUtil() {
    }

    public static final boolean IS_MACOS = false;

    public static void disableCloseWindowMenuItem() {
        // No-op: there is no menu bar to tweak outside of macOS.
    }

    /**
     * Vanilla forwards this to SDL_HINT_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK.
     * No-op on iOS: touch input never produces ctrl+left-click.
     */
    public static void setCtrlClickEmulatesRightClick(boolean value) {
        // No-op: not applicable on iOS.
    }

    /**
     * Vanilla forwards this to SDL_HINT_VIDEO_MAC_FULLSCREEN_MENU_VISIBILITY.
     * No-op on iOS: there is no fullscreen menu bar.
     */
    public static void setFullscreenMenuVisibility(boolean value) {
        // No-op: not applicable on iOS.
    }
}
