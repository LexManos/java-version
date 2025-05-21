/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.java_provisioner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import joptsimple.AbstractOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraftforge.java_provisioner.Disco.Arch;
import net.minecraftforge.java_provisioner.Disco.Distro;
import net.minecraftforge.java_provisioner.api.IJavaInstall;
import net.minecraftforge.java_provisioner.api.IJavaLocator;
import net.minecraftforge.java_provisioner.util.OS;
import net.minecraftforge.util.logging.Log;

public class Main {
    public static void main(String[] args) throws Exception {
        OptionParser parser = new OptionParser();

        if (hasArgument(args, "--disco-main")) {
            DiscoMain.main(args);
            return;
        }

        OptionSpec<Void> helpO = parser.accepts("help", "Displays this help message and exits");
        parser.accepts("disco-main", "Use the DiscoMain entry point");

        OptionSpec<File> cacheO = parser.accepts("cache",
                "Directory to store data needed for this program")
                .withRequiredArg().ofType(File.class).defaultsTo(new File("cache"));

        AbstractOptionSpec<Void> offlineO = parser.accepts("offline",
                "Do not attempt to download any JDKs, only use the cache");

        OptionSpec<Integer> versionO = parser.accepts("version",
                "Major version of java to try and locate")
                .withRequiredArg().ofType(Integer.class);

        OptionSpec<Void> allO = parser.accepts("all",
                "Display information about all detected java installs");

        OptionSpec<Void> testO = parser.accepts("test", "Enable test functionality, provisioning a bunch of jdks.");

        OptionSet options = parser.parse(args);
        if (options.has(helpO)) {
            parser.printHelpOn(Log.INFO);
            return;
        }
        File cache = options.valueOf(cacheO);
        DiscoLocator disco = new DiscoLocator(cache, options.has(offlineO));

        List<IJavaLocator> locators = new ArrayList<>();
        locators.add(new JavaHomeLocator());
        locators.add(new GradleLocator());
        locators.add(new JavaDirectoryLocator());
        locators.add(disco);

        if (options.has(testO)) {
            // populate downloaded for testing
            Disco tmp = new Disco(cache);
            int version = options.has(versionO) ? options.valueOf(versionO) : 22;
            for (Distro dist : new Distro[] { Distro.TEMURIN, Distro.AOJ, Distro.ORACLE, Distro.ZULU, Distro.GRAALVM, Distro.GRAALVM_COMMUNITY}) {
                List<Disco.Package> jdks = tmp.getPackages(version, OS.CURRENT, dist, Arch.CURRENT);
                int seen = 0;
                for (Disco.Package pkg : jdks) {
                    if (seen++ < 3)
                        tmp.extract(pkg);
                }
            }
        }

        if (options.has(allO)) {
            listAllJavaInstalls(locators);
        } else if (options.has(versionO)) {
            int version = options.valueOf(versionO);
            findSpecificVersion(locators, disco, version);
        } else {
            Log.error("You must specify a version to search for using --version or --all to list all java installs.");
            parser.printHelpOn(Log.INFO);
            System.exit(-1);
        }
    }

    private static boolean hasArgument(String[] args, String arg) {
        for (String s : args) {
            if (s.toLowerCase(Locale.ENGLISH).startsWith(arg))
                return true;
        }

        return false;
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
            Log.info(home);
        } else {
            Log.error("Failed to find sutable java for version " + version);
            for (IJavaLocator locator : locators) {
                Log.error("Locator: " + locator.getClass().getSimpleName());
                for (String line : locator.logOutput()) {
                    Log.error("  " + line);
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
            Log.info(install.home().getAbsolutePath());
            Log.info("  Vendor:  " + install.vendor());
            Log.info("  Type:    " + (install.isJdk() ? "JDK" : "JRE"));
            Log.info("  Version: " + install.version());
        }

    }
}
