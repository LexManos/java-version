/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.java_version.api;

import java.io.File;
import java.util.List;

import net.minecraftforge.java_version.DiscoLocator;
import net.minecraftforge.java_version.GradleLocator;
import net.minecraftforge.java_version.JavaHomeLocator;

public interface IJavaLocator {
    /**
     * Locates the first available java install for the specified major java version.
     * @return null is no install found, or a File pointing at the JAVA_HOME directory
     */
    File find(int version);

    /**
     * Locates all possible java installations that this provider knows about.
     * This can be used as a bulk version of {@link #find(int)}
     * @return A list containing all java installs, possibly empty but not null.
     */
    List<IJavaInstall> findAll();

    /**
     * Returns all loged messages this provider has output, honestly this is just a hack
     * until I decide what type of logging API I want to expose.
     */
    List<String> logOutput();

    /**
     * Instructs the provider to do anything it needs to provide a compatible java installer.
     * This is currently only implemented by the Disco provider to download and extract a JDK from
     * the foojay api.
     *
     * @return Null if this failed to provision a JDK
     */
    default IJavaInstall provision(int version) {
        return null;
    }

    /**
     * Returns a locator that attempts to find any toolchains installed by Gradle's toolchain plugin.
     * Uses GRADLE_HOME as the root directory.
     */
    static IJavaLocator gradle() {
        return new GradleLocator();
    }

    /**
     * Returns a locator that search the JAVA_HOME environment variables, such as the ones provided by
     * Github Actions.
     */
    static IJavaLocator home() {
        return new JavaHomeLocator();
    }

    /**
     * Returns a new locator that implements the Disco API, using the specified directory as its download cache.
     */
    static IJavaLocator disco(File cache) {
        return new DiscoLocator(cache);
    }

    static IJavaLocator disco(File cache, boolean offline) {
        return new DiscoLocator(cache, offline);
    }
}
