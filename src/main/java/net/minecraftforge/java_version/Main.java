/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.java_version;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraftforge.java_version.Disco.Arch;
import net.minecraftforge.java_version.Disco.Distro;
import net.minecraftforge.java_version.api.IJavaInstall;
import net.minecraftforge.java_version.api.IJavaLocator;
import net.minecraftforge.java_version.util.OS;

public class Main {
    public static void main(String[] args) throws Exception {
        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        OptionSpec<Void> helpO = parser.accepts("help", "Displays this help message and exits");

        OptionSpec<File> cacheO = parser.accepts("cache",
                "Directory to store data needed for this program")
                .withRequiredArg().ofType(File.class).defaultsTo(new File("cache"));

        OptionSpec<Integer> versionO = parser.accepts("version",
                "Major version of java to try and locate")
                .withOptionalArg().ofType(Integer.class);

        OptionSpec<Void> allO = parser.accepts("all",
                "Display information about all detected java installs");

        OptionSet options = parser.parse(args);
        if (options.has(helpO)) {
            parser.printHelpOn(System.out);
            return;
        }
        File cache = options.valueOf(cacheO);
        DiscoLocator disco = new DiscoLocator(cache);

        List<IJavaLocator> locators = new ArrayList<>();
        locators.add(new JavaHomeLocator());
        locators.add(new GradleLocator());
        locators.add(disco);

        // populate downloaded for testing
        Disco tmp = new Disco(cache);
        for (Distro dist : new Distro[] { Distro.TEMURIN, Distro.AOJ, Distro.ORACLE, Distro.ZULU, Distro.GRAALVM, Distro.GRAALVM_COMMUNITY}) {
            List<Disco.Package> jdks = tmp.getPackages(22, OS.CURRENT, dist, Arch.CURRENT);
            int seen = 0;
            for (Disco.Package pkg : jdks) {
                if (seen++ < 3)
                    tmp.extract(pkg);
            }
        }

        if (options.has(allO)) {
            listAllJavaInstalls(locators);
        } else {
            int version = options.valueOf(versionO);
            findSpecificVersion(locators, disco, version);
        }
    }

    private static void findSpecificVersion(List<IJavaLocator> locators, DiscoLocator disco, int version) {
        File result = null;
        for (IJavaLocator locator : locators) {
            result = locator.find(version);
            if (result != null)
                break;
        }

        // Could not find it with a locator, lets try downloading it.
        if (result == null) {
            IJavaInstall probe = disco.provision(version);
            if (probe != null)
                result = probe.home();
        }

        if (result != null && result.exists()) {
            String home = result.getAbsolutePath();
            if (!home.endsWith(File.separator))
                home += File.separatorChar;
            System.out.println(home);
        } else {
            System.out.println("Failed to find sutable java for version " + version);
            for (IJavaLocator locator : locators) {
                System.out.println("Locator: " + locator.getClass().getSimpleName());
                for (String line : locator.logOutput()) {
                    System.out.println("  " + line);
                }
            }
            System.exit(1);
        }
    }

    private static void listAllJavaInstalls(List<IJavaLocator> locators) {
        List<IJavaInstall> installs = new ArrayList<>();

        for (IJavaLocator locator : locators) {
            List<IJavaInstall> found = locator.findAll();
            installs.addAll(found);
        }

        // Remove duplicates
        Set<File> seen = new HashSet<File>();
        for (Iterator<IJavaInstall> itr = installs.iterator(); itr.hasNext(); ) {
            IJavaInstall install = itr.next();
            if (!seen.add(install.home()))
                itr.remove();
        }

        Collections.sort(installs);

        for (IJavaInstall install : installs) {
            System.out.println(install.home().getAbsolutePath());
            System.out.println("  Vendor:  " + install.vendor());
            System.out.println("  Type:    " + (install.isJdk() ? "JDK" : "JRE"));
            System.out.println("  Version: " + install.version());
        }

    }
}
