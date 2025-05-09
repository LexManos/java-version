/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.java_provisioner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.java_provisioner.api.IJavaInstall;
import net.minecraftforge.java_provisioner.api.IJavaLocator;
import net.minecraftforge.java_provisioner.util.OS;
import net.minecraftforge.java_provisioner.util.ProcessUtils;

/*
 * Attempts to find the java install using the JAVA_HOME environment variable.
 * It also searches some common extras, just in case they are set.
 *
 * Github Actions:
 *   The default images ship Java 8, 11, 17, and 21. In the format JAVA_HOME_{version}_{x64|arm64}
 *     https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2404-Readme.md
 *     https://github.com/actions/runner-images/blob/main/images/windows/Windows2022-Readme.md
 *     https://github.com/actions/runner-images/blob/main/images/macos/macos-14-arm64-Readme.md
 *
 * Lex's Personal Setup:
 *   JAVA_HOME_{version}
 *   I have used this format for years, kinda thought Github would use it, but they append the
 *   architecture for legacy reasons. It just makes life easier.
 */
public class JavaHomeLocator implements IJavaLocator {
    protected List<String> searched = new ArrayList<>();

    @Override
    public File find(int version) {
        IJavaInstall result = fromEnv("JAVA_HOME_" + version + "_X64");
        if (result == null)
            result = fromEnv("JAVA_HOME_" + version + "_arm64");
        if (result == null)
            result = fromEnv("JAVA_HOME_" + version);
        if (result == null) {
            result = fromEnv("JAVA_HOME");
            if (result != null && result.majorVersion() != version) {
                log("  Wrong version: Was " + result.majorVersion() + " wanted " + version);
                result = null;
            }
        }
        return result == null ? null : result.home();
    }

    @Override
    public List<IJavaInstall> findAll() {
        List<IJavaInstall> ret = new ArrayList<>();
        for (String key : System.getenv().keySet()) {
            if (key.startsWith("JAVA_HOME")) {
                IJavaInstall tmp = fromEnv(key);
                if (tmp != null)
                    ret.add(tmp);
            }
        }
        return ret;
    }

    @Override
    public List<String> logOutput() {
        return this.searched;
    }

    protected void log(String line) {
        searched.add(line);
    }

    protected IJavaInstall fromEnv(String name) {
        String env = System.getenv(name);
        if (env == null) {
            log("Environment: \"" + name + "\" Empty");
            return null;
        }

        log("Environment: \"" + name + "\"");
        log("  Value: \"" + env + "\"");
        return fromPath(env);
    }

    protected IJavaInstall fromPath(String path) {
        return fromPath(new File(path));
    }

    protected IJavaInstall fromPath(File path) {
        File exe = new File(path, "bin/java" + OS.CURRENT.exe());

        if (!exe.exists()) {
            log("  Missing Executable");
            return null;
        }

        ProcessUtils.ProbeResult result = ProcessUtils.testJdk(path);
        if (result.exitCode != 0) {
            log("  Exit code: " + result.exitCode);
            for (String line : result.lines)
                searched.add("  " + line);
        }

        return result.meta;
    }
}
