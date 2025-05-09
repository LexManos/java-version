/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.java_provisioner.api;

import java.io.File;

import net.minecraftforge.java_provisioner.JavaVersion;

public interface IJavaInstall extends Comparable<IJavaInstall> {
    File home();
    boolean isJdk();
    int majorVersion();
    String version();
    String vendor();

    default int compareTo(IJavaInstall o2) {
        if (this.isJdk() != o2.isJdk())
            return this.isJdk() ? -1 : 1;
        if (this.majorVersion() != o2.majorVersion())
            return o2.majorVersion() - this.majorVersion();

        if (this.vendor() != null && o2.vendor() == null)
            return -1;
        else if (this.vendor() == null && o2.vendor() != null)
            return 1;
        else if (this.vendor() != null && !this.vendor().equals(o2.vendor())) {
            int v1 = Util.getVendorOrder(this.vendor());
            int v2 = Util.getVendorOrder(this.vendor());
            if (v1 == v2) {
                if (v1 == -1)
                    return this.vendor().compareTo(o2.vendor());
            } else if (v1 == -1)
                return 1;
            else if (v2 == -1)
                return -1;
            else
                return v1 - v2;
        }

        if (this.version() != null && o2.version() == null)
            return -1;
        else if (this.version() == null && o2.version() != null)
            return 1;
        else if (this.version() != null && !this.version().equals(o2.version())) {
            JavaVersion v1 = JavaVersion.nullableParse(this.version());
            JavaVersion v2 = JavaVersion.nullableParse(o2.version());
            if (v1 == null && v2 != null)
                return 1;
            else if (v1 != null && v2 == null)
                return -1;
            else if (v1 == null)
                return this.version().compareTo(o2.version());
            return v2.compareTo(v1);
        }

        return 0;
    }
}
