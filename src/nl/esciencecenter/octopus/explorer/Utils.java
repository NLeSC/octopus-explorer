package nl.esciencecenter.octopus.explorer;

import java.net.URL;

import javax.swing.ImageIcon;

public class Utils {
    public static ImageIcon loadIcon(String path) throws Exception {
        URL imgURL = Utils.class.getClassLoader().getResource("icons/" + path);

        if (imgURL == null) {
            throw new Exception("could not find icon: " + path);
        }

        return new ImageIcon(imgURL);
    }
}
