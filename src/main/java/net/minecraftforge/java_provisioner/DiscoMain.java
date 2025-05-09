/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.java_provisioner;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.EnumConverter;
import net.minecraftforge.java_provisioner.util.OS;
import net.minecraftforge.java_provisioner.util.ProcessUtils;
import net.minecraftforge.util.logging.Log;

public class DiscoMain {
    public static void main(String[] args) throws Exception {
        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        OptionSpec<Void> helpO = parser.accepts("help", "Displays this help message and exits");

        parser.accepts("disco-main", "Instructs the executable jar to use the DiscoMain entry point");

        OptionSpec<File> cacheO = parser.accepts("cache",
                "Directory to store data needed for this program")
                .withRequiredArg().ofType(File.class).defaultsTo(new File("cache"));

        OptionSpec<Void> downloadJdkO = parser.accepts("download-jdk", "Download an extracts a JDK");
        OptionSpec<Integer> javeVersionO = parser.accepts("java-version",
                "Major version of java to download, will attempt the highest version avalible if unspecified")
                .withOptionalArg().ofType(Integer.class);
        OptionSpec<Disco.Arch> archO = parser.acceptsAll(l("arch", "architecture"),
                "Architecture for use in Disco api")
                .withRequiredArg().withValuesConvertedBy(converter(Disco.Arch.class)).defaultsTo(Disco.Arch.CURRENT);
        OptionSpec<OS> osO = parser.acceptsAll(l("os", "operating-system"),
                "Operating System for use in Disco api")
                .withRequiredArg().withValuesConvertedBy(converter(OS.class)).defaultsTo(OS.CURRENT);
        OptionSpec<Disco.Distro> distroO = parser.acceptsAll(l("distro", "distribution"),
                "Distribution for use in Disco api")
                .withRequiredArg().withValuesConvertedBy(converter(Disco.Distro.class)).defaultsTo(Disco.Distro.TEMURIN);
        OptionSpec<Void> autoO = parser.accepts("auto",
                "Auto select a JDK to download without prompting if there are multiple options");

        OptionSet options = parser.parse(args);

        // File gradle = new File(System.getProperty("user.home", "."), ".gradle/jdks/");
        File cache = cacheO.value(options);

        boolean success = true;
        if (options.has(helpO)) {
            parser.printHelpOn(Log.INFO);
        } else if (options.has(downloadJdkO)) {
            success = downloadJdk(
                options.hasArgument(javeVersionO) ? javeVersionO.value(options) : -1,
                archO.value(options),
                osO.value(options),
                distroO.value(options),
                options.has(autoO),
                cache
            );
        } else {
            parser.printHelpOn(Log.INFO);
        }

        if (!success)
            System.exit(1);
    }

    private static List<String> l(String...strings) {
        return Arrays.asList(strings);
    }

    private static boolean downloadJdk(
        int javaVersion, Disco.Arch arch, OS os, Disco.Distro distro,
        boolean auto, File cache
    ) {

        if (arch == Disco.Arch.UNKNOWN) {
            arch = Disco.Arch.X64;
            Log.warn("Unknown Architecture (" + System.getProperty("os.arch") + ")" +
                " Defaulting to " + arch.name() + "." +
                " Use --arch to specify an alternative.");
        }

        if (os == OS.UNKNOWN) {
            os = OS.LINUX;
            Log.warn("Unknown Operating System (" + System.getProperty("os.name") + ")" +
                " Defaulting to " + os.name() + "." +
                " Use --os to specify an alternative.");
        }

        Log.info("Downloading JDK:");
        Log.info("    Version: " + (javaVersion == -1 ? "latest" : javaVersion));
        Log.info("    Arch:    " + (arch   == null ? "null" : arch  .name()));
        Log.info("    OS:      " + (os     == null ? "null" : os    .name()));
        Log.info("    Distro:  " + (distro == null ? "null" : distro.name()));
        Log.info("    Cache:   " + cache.getAbsolutePath());
        Disco disco = new Disco(new File(cache, "jdks"));

        List<Disco.Package> jdks = disco.getPackages(javaVersion, os, distro, arch);
        Disco.Package pkg = null;
        if (jdks == null || jdks.isEmpty()) {
            Log.error("Failed to find any download, try specifying a different java version or distro");
            return false;
        } else if (jdks.size() == 1 || auto) {
            pkg = jdks.get(0);
        } else if (jdks.size() > 1) {
            for (int x = 0; x < jdks.size(); x++) {
                Disco.Package jdk = jdks.get(x);
                Log.info(String.format("[%2d] %s: %s", x+1, jdk.distribution, jdk.filename));
                //log(Disco.GSON.toJson(jdk));
            }

            @SuppressWarnings("resource")
            Scanner scan = new Scanner(System.in);
            System.out.print("Select Download: ");
            String line = scan.nextLine();

            int selected = -1;
            try {
                selected = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                Log.error("Invalid selection \"" + line + "\" is not a number.");
            }

            if (selected <= 0 || selected > jdks.size()) {
                Log.error("Invalid selection, must be between 1 and " + jdks.size());
                return false;
            } else
                pkg = jdks.get(selected - 1);
        }

        Log.info();

        File java_home = disco.extract(pkg);

        if (java_home == null)
            System.exit(1);

        ProcessUtils.Result result = ProcessUtils.testJdk(java_home);
        if (result.exitCode != 0) {
            Log.error("Failed to run extracted java:");
            for (String line : result.lines)
                Log.error(line);

            return false;
        }

        return true;
    }

    private static <T extends Enum<T>> EnumConverter<T> converter(Class<T> clazz) {
        return new EnumConverter<T>(clazz) {
            @Override
            public T convert(String value) {
                if ("null".equals(value))
                    return null;
                return super.convert(value);
            }
        };
    }}
