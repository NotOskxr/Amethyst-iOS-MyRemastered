package com.sun.jna;

/** Verify that adding the Minecraft fix preserves the existing JNA patch. */
public final class Platform {
    public static int getOSType() { return -999; }
}
