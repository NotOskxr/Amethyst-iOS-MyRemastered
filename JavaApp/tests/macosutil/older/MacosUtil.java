package com.mojang.blaze3d.platform;

/** Older mapped clients expose fullscreen helpers, not the 26.3 SDL setters. */
public final class MacosUtil {
    public static void exitNativeFullscreen(Object window) { }
    public static void clearResizableBit(Object window) { }
}
