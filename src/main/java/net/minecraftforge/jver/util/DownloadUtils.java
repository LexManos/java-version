/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jver.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.net.ssl.SSLHandshakeException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class DownloadUtils {
    // A GSO parser that prints good looking output, and treats empty strings as nulls
    public static final Gson GSON = new GsonBuilder()
        .setLenient()
        .setPrettyPrinting()
        .registerTypeAdapter(String.class, new TypeAdapter<String>() {
            @Override
            public void write(final JsonWriter out, final String value) throws IOException {
                if (value == null || value.isEmpty())
                    out.nullValue();
                else
                    TypeAdapters.STRING.write(out, value);
            }

            @Override
            public String read(final JsonReader in) throws IOException {
                String value = TypeAdapters.STRING.read(in);
                return value != null && value.isEmpty() ? null : value; // Read empty strings as null
            }
        })
        .create();

    private static URLConnection getConnection(String address) {
        URL url = null;
        try {
            url = new URL(address);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        try {
            int timeout = 5 * 1000;
            int max_redirects = 3;

            URLConnection con = null;
            for (int x = 0; x < max_redirects; x++) {
                con = url.openConnection();
                con.setConnectTimeout(timeout);
                con.setReadTimeout(timeout);

                if (con instanceof HttpURLConnection) {
                    HttpURLConnection hcon = (HttpURLConnection)con;
                    hcon.setRequestProperty("User-Agent", "MinecraftMaven");
                    hcon.setRequestProperty("accept", "application/json");
                    hcon.setInstanceFollowRedirects(false);

                    int res = hcon.getResponseCode();
                    if (res == HttpURLConnection.HTTP_MOVED_PERM || res == HttpURLConnection.HTTP_MOVED_TEMP) {
                        String location = hcon.getHeaderField("Location");
                        hcon.disconnect();
                        if (x == max_redirects - 1) {
                            System.out.println("Invalid number of redirects: " + location);
                            return null;
                        } else {
                            //System.out.println("Following redirect: " + location);
                            url = new URL(url, location);
                        }
                    } else if (res == 404) {
                        // File not found
                        return null;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            return con;
        } catch (SSLHandshakeException e ) {
            System.out.println("Failed to establish connection to " + address);
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.out.println("Failed to establish connection to " + address);
            e.printStackTrace();
            return null;
        }
    }

    public static String downloadString(String url) {
        try {
            URLConnection connection = getConnection(url);
            if (connection != null) {
                try (InputStream stream = connection.getInputStream();
                     ByteArrayOutputStream out = new ByteArrayOutputStream();
                ) {
                    byte[] buf = new byte[1024];
                    int n;
                    while ((n = stream.read(buf)) > 0) {
                        out.write(buf, 0, n);
                    }

                    return new String(out.toByteArray(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean downloadFile(File target, String url) {
        try {
            File parent = target.getAbsoluteFile().getParentFile();
            if (parent != null && !parent.exists())
                parent.mkdirs();

            URLConnection connection = getConnection(url);
            if (connection != null) {
                Files.copy(connection.getInputStream(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
