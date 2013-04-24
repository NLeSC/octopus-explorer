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

import nl.esciencecenter.octopus.explorer.Utils;

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
    //private final ImageIcon certificate;
    private final ImageIcon executable;
    private final ImageIcon audio;
    private final ImageIcon font;
    private final ImageIcon image;
    private final ImageIcon archive;
    private final ImageIcon html;
    private final ImageIcon text;
    //private final ImageIcon empty;
    private final ImageIcon script;
    private final ImageIcon video;
    //private final ImageIcon addressBook;
    private final ImageIcon calendar;
    private final ImageIcon document;
    private final ImageIcon drawing;
    private final ImageIcon presentation;
    private final ImageIcon spreadsheet;

    //Extension->Icon mappings
    private final Map<String, ImageIcon> icons;

    public MimeTypeIcons() throws Exception {
        //certificate = Utils.loadIcon("mimetypes/application-certificate.png");
        executable = Utils.loadIcon("mimetypes/application-x-executable.png");
        audio = Utils.loadIcon("mimetypes/audio-x-generic.png");
        font = Utils.loadIcon("mimetypes/font-x-generic.png");
        image = Utils.loadIcon("mimetypes/image-x-generic.png");
        archive = Utils.loadIcon("mimetypes/package-x-generic.png");
        html = Utils.loadIcon("mimetypes/text-html.png");
        text = Utils.loadIcon("mimetypes/text-x-generic.png");
        //empty = Utils.loadIcon("mimetypes/text-x-generic-template.png");
        script = Utils.loadIcon("mimetypes/text-x-script.png");
        video = Utils.loadIcon("mimetypes/video-x-generic.png");
        //addressBook = Utils.loadIcon("mimetypes/x-office-address-book.png");
        calendar = Utils.loadIcon("mimetypes/x-office-calendar.png");
        document = Utils.loadIcon("mimetypes/x-office-document.png");
        drawing = Utils.loadIcon("mimetypes/x-office-drawing.png");
        presentation = Utils.loadIcon("mimetypes/x-office-presentation.png");
        spreadsheet = Utils.loadIcon("mimetypes/x-office-spreadsheet.png");

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
            return;
            //throw new IOException("cannot get mime type list from classpath");
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
