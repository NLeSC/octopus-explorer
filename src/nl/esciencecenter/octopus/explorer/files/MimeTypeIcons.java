/*
 * Copyright 2013 Netherlands eScience Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.esciencecenter.octopus.explorer.files;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to extensions to mime types, with the purpose of choosing the right icon when listing files. Most are taken from a
 * mime.types file that comes standard with most linux distributions, and is included in octopus-explorer
 * 
 * @author Niels Drost
 * 
 */
public class MimeTypeIcons {

    private static final Logger logger = LoggerFactory.getLogger(MimeTypeIcons.class);

    //all available icons
    ImageIcon certificate = new ImageIcon("resources/icons/mimetypes/application-certificate.png");
    ImageIcon executable = new ImageIcon("resources/icons/mimetypes/application-x-executable.png");
    ImageIcon audio = new ImageIcon("resources/icons/mimetypes/audio-x-generic.png");
    ImageIcon font = new ImageIcon("resources/icons/mimetypes/font-x-generic.png");
    ImageIcon image = new ImageIcon("resources/icons/mimetypes/image-x-generic.png");
    ImageIcon archive = new ImageIcon("resources/icons/mimetypes/package-x-generic.png");
    ImageIcon html = new ImageIcon("resources/icons/mimetypes/text-html.png");
    ImageIcon text = new ImageIcon("resources/icons/mimetypes/text-x-generic.png");
    ImageIcon empty = new ImageIcon("resources/icons/mimetypes/text-x-generic-template.png");
    ImageIcon script = new ImageIcon("resources/icons/mimetypes/text-x-script.png");
    ImageIcon video = new ImageIcon("resources/icons/mimetypes/video-x-generic.png");
    ImageIcon addressBook = new ImageIcon("resources/icons/mimetypes/x-office-address-book.png");
    ImageIcon calendar = new ImageIcon("resources/icons/mimetypes/x-office-calendar.png");
    ImageIcon document = new ImageIcon("resources/icons/mimetypes/x-office-document.png");
    ImageIcon drawing = new ImageIcon("resources/icons/mimetypes/x-office-drawing.png");
    ImageIcon presentation = new ImageIcon("resources/icons/mimetypes/x-office-presentation.png");
    ImageIcon spreadsheet = new ImageIcon("resources/icons/mimetypes/x-office-spreadsheet.png");

    //Extension->Icon mappings
    private final Map<String, ImageIcon> icons;

    public MimeTypeIcons() throws IOException {
        icons = new HashMap<String, ImageIcon>();

        long start = System.currentTimeMillis();
        readMimeTypes();

        //add a few extensions explicitly, as they are not listed in the mime.types file
        icons.put("gz", archive);
        icons.put("wiki", text);
        icons.put("f90", script);

        logger.debug("icon table contains " + icons.size() + " extensions, took " + (System.currentTimeMillis() - start)
                + " milliseconds to build");
    }

    /**
     * Function that maps MimeTypes to an icon. Called for each found mimetype while reading the mime.types file.
     */
    private ImageIcon determineIcon(String mimeType) {
        //catch-all for whole categories of the mimetypes
        String category = mimeType.substring(0, mimeType.indexOf("/"));
        switch (category) {
        case "audio":
            return audio;
        case "image":
            return image;
        case "video":
            return video;
        case "text":
            if (mimeType.equals("text/html")) {
                return html;
            } else if (mimeType.equals("text/calendar")) {
                return calendar;
            } else if (mimeType.startsWith("text/x")) {
                return script;
            } else {
                return text;
            }
        case "inode":
        case "chemical":
        case "message":
        case "model":
        case "multipart":
            return null;
        }

        //handle the "application" category
        if (mimeType.contains("java") || mimeType.contains("octet-stream") || mimeType.contains("x-msdos-program")
                | mimeType.contains("python")) {
            return executable;
        }

        if (mimeType.contains("archive") || mimeType.contains("compressed") || mimeType.contains("zip")
                || mimeType.contains("gz") || mimeType.contains("tar") || mimeType.contains("deb")
                || mimeType.contains("stuffit")) {
            return archive;
        }

        if (mimeType.contains("presentation") || mimeType.contains("powerpoint")) {
            return presentation;
        }

        if (mimeType.contains("spreadsheet") || mimeType.contains("excel") || mimeType.contains("calc")) {
            return spreadsheet;
        }

        if (mimeType.contains("font")) {
            return font;
        }

        if (mimeType.contains("x-shockwave-flash")) {
            return video;
        }

        if (mimeType.contains("tex") || mimeType.contains("xml")) {
            return text;
        }

        if (mimeType.contains("xcf") | mimeType.contains("fig")) {
            return image;
        }

        //keep these towards the end, as they are a bit generic

        if (mimeType.contains("graphics") || mimeType.contains("draw")) {
            return drawing;
        }

        if (mimeType.contains("word") || mimeType.contains("document") || mimeType.contains("postscript")
                || mimeType.contains("pdf")) {
            return document;
        }

        //we don't recognize this type
        //logger.debug("failed to get icon for " + type + " extension: " + extension);
        return null;
    }

    private void readMimeTypes() throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("resources/mime.types");

        if (in == null) {
            throw new IOException("cannot get mime type list from classpath");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            while (true) {
                String line = reader.readLine();

                if (line == null) {
                    in.close();
                    return;
                } else if (line.startsWith("#") || line.isEmpty()) {
                    //SKIP LINE
                } else {
                    StringTokenizer stringTokenizer = new StringTokenizer(line);

                    String mimeType = stringTokenizer.nextToken();
                    ImageIcon icon = determineIcon(mimeType);

                    while (stringTokenizer.hasMoreTokens()) {
                        String extension = stringTokenizer.nextToken();

                        if (icon != null) {
                            icons.put(extension, icon);
                        }
                    }
                }
            }
        }

    }

    Icon getIconFor(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");

        if (dotIndex == -1) {
            //the generic file icon
            return text;
        }

        String extension = fileName.substring(dotIndex + 1);

        ImageIcon result = icons.get(extension.toLowerCase());

        if (result == null) {
            logger.debug("unknown file extension, defaulting to text: " + extension);
            return text;
        }

        return result;
    }

}
