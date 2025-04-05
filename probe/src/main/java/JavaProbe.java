/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
/**
 * Probes the currently-running Java environment and prints out its properties for usage in Java Version.
 * <p>In order to prevent needless memory usage for simply probing the JVM, each property is manually written out in the
 * {@linkplain #main(String[]) main method}. The string concatenation is done by the compiler and only exists for ease
 * of reading.</p>
 */
public class JavaProbe {
    public static void main(String[] args) {
        // java.home
        System.out.print("JAVA_PROBE: " + "java.home" + " ");
        System.out.println(System.getProperty("java.home", "unset"));

        // java.version
        System.out.print("JAVA_PROBE: " + "java.version" + " ");
        System.out.println(System.getProperty("java.version", "unset"));

        // java.vendor
        System.out.print("JAVA_PROBE: " + "java.vendor" + " ");
        System.out.println(System.getProperty("java.vendor", "unset"));

        // java.runtime.name
        System.out.print("JAVA_PROBE: " + "java.runtime.name" + " ");
        System.out.println(System.getProperty("java.runtime.name", "unset"));

        // java.runtime.version
        System.out.print("JAVA_PROBE: " + "java.runtime.version" + " ");
        System.out.println(System.getProperty("java.runtime.version", "unset"));

        // java.vm.name
        System.out.print("JAVA_PROBE: " + "java.vm.name" + " ");
        System.out.println(System.getProperty("java.vm.name", "unset"));

        // java.vm.version
        System.out.print("JAVA_PROBE: " + "java.vm.version" + " ");
        System.out.println(System.getProperty("java.vm.version", "unset"));

        // java.vm.vendor
        System.out.print("JAVA_PROBE: " + "java.vm.vendor" + " ");
        System.out.println(System.getProperty("java.vm.vendor", "unset"));

        // os.arch
        System.out.print("JAVA_PROBE: " + "os.arch" + " ");
        System.out.println(System.getProperty("os.arch", "unset"));
    }
}
