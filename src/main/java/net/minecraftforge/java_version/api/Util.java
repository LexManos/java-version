/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.java_version.api;

import java.util.regex.Pattern;

class Util {
    private static final Pattern[] SORTED_VENDORS = new Pattern[] {
        Pattern.compile("temurin|adoptium|eclipse foundation", Pattern.CASE_INSENSITIVE),
        Pattern.compile("adoptopenjdk", Pattern.CASE_INSENSITIVE),
        Pattern.compile("azul systems", Pattern.CASE_INSENSITIVE)
    };

    static int getVendorOrder(String vendor) {
        for (int x = 0; x < SORTED_VENDORS.length; x++) {
            if (SORTED_VENDORS[x].matcher(vendor).find())
                return x;
        }
        return -1;
    }
}
