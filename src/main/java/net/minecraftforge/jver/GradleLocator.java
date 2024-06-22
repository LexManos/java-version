/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import net.minecraftforge.jver.api.IJavaInstall;
import net.minecraftforge.jver.util.OS;

/*
 * Attempts to find the java install using various tools that Gradle uses
 *
 * https://docs.gradle.org/current/userguide/toolchains.html#sec:custom_loc
 *   System Properties:
 *     org.gradle.java.installations.fromEnv=ENV1,ENV2
 *     org.gradle.java.installations.paths=PATH1,PATH2
 *
 * https://docs.gradle.com/enterprise/test-distribution-agent/#capabilities
 *   Environment Variables: JDK\d\d*
 *
 * GRADLE_HOME/jdks folder
 */
public class GradleLocator extends JavaHomeLocator {
    private static final String GRADLE_FROMENV = "org.gradle.java.installations.fromEnv";
    private static final String GRADLE_PATHS = "org.gradle.java.installations.paths";
    private static final String MARKER_FILE = ".ready";
    private static final String LEGACY_MARKER_FILE = "provisioned.ok";
    private static final String MAC_JAVA_HOME_FOLDER = "Contents/Home";
    private static final Pattern GRADLE_ENV = Pattern.compile("JDK\\d\\d*");

    @Override
    public File find(int version) {
        List<IJavaInstall> results = new ArrayList<>();
        fromGradleEnv(results, version);
        if (!results.isEmpty())
            return results.get(0).home();

        fromPaths(results, version);
        if (!results.isEmpty())
            return results.get(0).home();

        IJavaInstall result = fromEnv("JDK" + version);
        if (result != null) {
            if (result.majorVersion() != version)
                log("  Wrong version: Was " + result.majorVersion() + " wanted " + version);
            else
                return result.home();
        }

        fromGradleHome(results, version);
        if (!results.isEmpty())
            return results.get(0).home();

        return null;
    }

    @Override
    public List<IJavaInstall> findAll() {
        List<IJavaInstall> ret = new ArrayList<>();
        fromGradleEnv(ret, -1);
        fromPaths(ret, -1);

        for (String key : System.getenv().keySet()) {
            if (GRADLE_ENV.matcher(key).matches()) {
                IJavaInstall tmp = fromEnv(key);
                if (tmp != null)
                    ret.add(tmp);
            }
        }

        fromGradleHome(ret, -1);

        return ret;
    }

    @Override
    public List<String> logOutput() {
        return this.searched;
    }

    private void fromGradleEnv(Collection<IJavaInstall> list, int version) {
        String prop = System.getProperty(GRADLE_FROMENV);
        log("Property: " + GRADLE_FROMENV + " = " + prop);
        if (prop == null)
            return;

        String[] envs = prop.split(",");
        for (String env : envs) {
            IJavaInstall ret = fromEnv(env);
            if (ret == null)
                continue;

            if (version == -1) {
                list.add(ret);
            } else if (ret.majorVersion() != version) {
                log("  Wrong version: Was " + ret.majorVersion() + " wanted " + version);
            } else {
                list.add(ret);
                return;
            }
        }
    }

    private void fromPaths(Collection<IJavaInstall> list, int version) {
        String prop = System.getProperty(GRADLE_PATHS);
        log("Property: " + GRADLE_PATHS + " = " + prop);
        if (prop == null)
            return;

        String[] envs = prop.split(",");
        for (String path : envs) {
            IJavaInstall ret = fromPath(path);
            if (ret == null)
                continue;

            if (version == -1) {
                list.add(ret);
            } else if (ret.majorVersion() != version) {
                log("  Wrong version: Was " + ret.majorVersion() + " wanted " + version);
            } else {
                list.add(ret);
                return;
            }
        }
    }

    private File getGradleHome() {
        String home = System.getProperty("gradle.user.home");
        if (home == null) {
            home = System.getenv("GRADLE_USER_HOME");
            if (home == null)
                home = System.getProperty("user.home") + "/.gradle";
        }

        File ret = new File(home);
        try {
            ret = ret.getCanonicalFile();
        } catch (IOException e) {
            return ret;
        }

        return ret;
    }

    private void fromGradleHome(Collection<IJavaInstall> list, int version) {
        File gradleHome = getGradleHome();
        if (!gradleHome.exists() || !gradleHome.isDirectory()) {
            log("Gradle home: \"" + gradleHome.getAbsolutePath() + "\" Does not exist");
            return;
        }
        File jdks = new File(gradleHome, "jdks");
        if (!jdks.exists() || !jdks.isDirectory()) {
            log("Gradle Home JDKs: \"" + jdks.getAbsolutePath() + "\" Does not exist");
            return;
        }

        for (File dir : jdks.listFiles()) {
            if (!dir.isDirectory())
                continue;

            List<File> markers = findMarkers(dir);
            for (File marked : markers) {
                if (OS.CURRENT == OS.OSX)
                    marked = findMacHome(dir);

                log("Gradle Home JDK: \"" + marked.getAbsolutePath() + "\"");

                IJavaInstall ret = fromPath(marked);
                if (ret != null) {
                    if (version == -1) {
                        list.add(ret);
                    } else if (ret.majorVersion() != version) {
                        log("  Wrong version: Was " + ret.majorVersion() + " wanted " + version);
                    } else {
                        list.add(ret);
                        return;
                    }
                }
            }
        }
    }

    private List<File> findMarkers(File root) {
        // Prior to Gradle 8.8 jdks did not have their root directory trimmed
        // It also could cause multiple archives to be extracted to the same folder.
        // So lets just find anything that it marked as 'properly installed' and hope for the best.
        List<File> ret = new ArrayList<>();
        if (new File(root, MARKER_FILE).exists() ||
            new File(root, LEGACY_MARKER_FILE).exists())
            ret.add(root);

        for (File child : root.listFiles()) {
            if (!child.isDirectory())
                continue;
            if (new File(child, MARKER_FILE).exists() ||
                new File(child, LEGACY_MARKER_FILE).exists())
                ret.add(child);
        }

        return ret;
    }

    // Macs are weird and can have their files packaged into a content folder
    private File findMacHome(File root) {
        File tmp = new File(root, MAC_JAVA_HOME_FOLDER);
        if (tmp.exists())
            return tmp;

        for (File child : root.listFiles()) {
            if (!child.isDirectory())
                continue;

            tmp = new File(child, MAC_JAVA_HOME_FOLDER);
            if (tmp.exists())
                return tmp;
        }

        return root;
    }
}
