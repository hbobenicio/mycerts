package br.com.hugobenicio.mycerts.core.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import static java.lang.String.format;

public class ResourceUtils {

    public static File getFileFromResources(String filePath) throws IOException {
        final URL url = ResourceUtils.class.getClassLoader().getResource(filePath);
        if (url == null) {
            var msg = format("Failed to load resource file. path=\"%s\"", filePath);
            throw new IOException(msg);
        }

        final URI uri;
        try {
            uri = url.toURI();
        } catch (Exception e) {
            throw new AssertionError("this URL to URI conversion should never fail", e);
        }

        return new File(uri);
    }

    public static InputStream getInputStreamFromResources(String filePath) {
        return ResourceUtils.class.getClassLoader().getResourceAsStream(filePath);
    }
}
