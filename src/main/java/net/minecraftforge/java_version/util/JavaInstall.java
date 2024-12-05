/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.java_version.util;

import java.io.File;

import net.minecraftforge.java_version.JavaVersion;
import net.minecraftforge.java_version.api.IJavaInstall;

class JavaInstall implements IJavaInstall {
    private final File home;
    private final String version;
    private final String vendor;
    private final int majorVersion;
    private final File java;
    private final File javac;

    JavaInstall(File home, String version, String vendor) {
        this.home = home;
        this.version = version;
        this.vendor = vendor;
        this.majorVersion = version == null ? -1 : JavaVersion.parse(version).major();
        File tmp = new File(home, "bin/java" + OS.CURRENT.exe());
        this.java = tmp.exists() ? tmp : null;
        tmp = new File(home, "bin/javac" + OS.CURRENT.exe());
        this.javac = tmp.exists() ? tmp : null;
    }

    @Override
    public File home() {
        return this.home;
    }

    @Override
    public boolean isJdk() {
        return this.java != null && this.javac != null;
    }

    @Override
    public int majorVersion() {
        return this.majorVersion;
    }

    @Override
    public String version() {
        return this.version;
    }

    @Override
    public String vendor() {
        return this.vendor;
    }

    @Override
    public String toString() {
        return this.vendor
            + (isJdk() ? " JDK" : "JRE")
            + " v" + this.version
            + " @ " + this.home.getAbsolutePath();
    }
}
