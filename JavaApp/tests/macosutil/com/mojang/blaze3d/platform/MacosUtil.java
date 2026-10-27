package com.mojang.blaze3d.platform;

/** A game-side class whose initializer must never reach a desktop native. */
public final class MacosUtil {
    static {
        System.loadLibrary("amethyst_test_desktop_jna_must_not_load");
    }
    public static final boolean IS_MACOS = true;
    public static void disableCloseWindowMenuItem() { throw new AssertionError("AppKit"); }
    public static void setCtrlClickEmulatesRightClick(boolean value) { throw new AssertionError("SDL hint"); }
    public static void setFullscreenMenuVisibility(boolean value) { throw new AssertionError("SDL hint"); }
}
