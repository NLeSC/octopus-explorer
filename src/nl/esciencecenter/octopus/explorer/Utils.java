package nl.esciencecenter.octopus.explorer;

import java.net.URL;

import javax.swing.ImageIcon;

public class Utils {
    public static ImageIcon loadIcon(String path) {
        URL imgURL = null;

        if (path != null) {
            imgURL = ClassLoader.getSystemClassLoader().getResource("resources/icons/" + path);
        }

        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            return null;
        }
    }
}
