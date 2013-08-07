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
package nl.esciencecenter.octopus.explorer;

import nl.esciencecenter.octopus.Octopus;
import nl.esciencecenter.octopus.OctopusFactory;
import nl.esciencecenter.octopus.explorer.files.FileListingPanel;
import nl.esciencecenter.octopus.explorer.jobs.JobListingPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OctopusExplorer extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(OctopusExplorer.class);

    private static final long serialVersionUID = 1L;

    final JList<String> locationList;

    /**
     * @author Niels Drost
     * 
     */
    private final class ListPopupMouseListener extends MouseAdapter {
        private final JPopupMenu popup;

        /**
         * @param popup
         */
        private ListPopupMouseListener(JPopupMenu popup) {
            this.popup = popup;
        }

        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showMenu(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showMenu(e);
            }
        }

        private void showMenu(MouseEvent e) {
            int index = locationList.locationToIndex(e.getPoint());
            locationList.setSelectedIndex(index);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /**
     * @author Niels Drost
     * 
     */
    private final class ListMouseAdapter extends MouseAdapter {
        private final JobListingPanel jobListing;
        private final FileListingPanel fileListing;

        /**
         * @param jobListing
         * @param fileListing
         */
        private ListMouseAdapter(JobListingPanel jobListing, FileListingPanel fileListing) {
            this.jobListing = jobListing;
            this.fileListing = fileListing;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int index = locationList.locationToIndex(e.getPoint());

                logger.debug("Double clicked on Location item " + index + " which contains element "
                        + locationList.getModel().getElementAt(index));

                fileListing.setCurrentLocation("" + locationList.getModel().getElementAt(index));
                fileListing.triggerRefresh(true);
                jobListing.setCurrentLocation("" + locationList.getModel().getElementAt(index));
                jobListing.triggerRefresh(true);
            }
        }
    }

    /**
     * @author Niels Drost
     * 
     */
    private static final class LocationListModel extends AbstractListModel<String> {
        private static final long serialVersionUID = 1L;
        String[] values = new String[] { "Local", "fs0.das4.cs.vu.nl", "fs1.das4.liacs.nl" };

        public int getSize() {
            return values.length;
        }

        public String getElementAt(int index) {
            return values[index];
        }
    }

    static class IconLabelCellRenderer extends JLabel implements ListCellRenderer<String> {
        private static final long serialVersionUID = 1L;
        private final ImageIcon homeIcon;
        private final ImageIcon remoteIcon;

        IconLabelCellRenderer() throws Exception {
            homeIcon = Utils.loadIcon("places/user-home.png");
            remoteIcon = Utils.loadIcon("places/network-server.png");
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected,
                boolean cellHasFocus) {
            setText(value);
            if (value.equals("Local")) {
                setIcon(homeIcon);
            } else {
                setIcon(remoteIcon);
            }
            //setIcon((s.length() > 10) ? homeIcon : shortIcon);
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            return this;
        }
    }

    /**
     * Create the frame.
     * 
     * @throws Exception
     *             if the octopus could not be made
     */
    public OctopusExplorer() throws Exception {
        logger.debug("Initializing Octopus Explorer");
        setTitle("Octopus Explorer [Technology Preview]");
        Octopus octopus = OctopusFactory.newOctopus(null);
        final JobListingPanel jobListing = new JobListingPanel(octopus, this);
        final FileListingPanel fileListing = new FileListingPanel(octopus);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 965, 735);
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        JSplitPane splitPane = new JSplitPane();
        contentPane.add(splitPane, BorderLayout.CENTER);

        final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        splitPane.setRightComponent(tabbedPane);

        tabbedPane.addTab("Files", Utils.loadIcon("apps/system-file-manager.png"), fileListing, null);

        tabbedPane.addTab("Jobs", Utils.loadIcon("apps/accessories-calculator.png"), jobListing, null);

        JPanel panel = new JPanel();
        splitPane.setLeftComponent(panel);
        panel.setLayout(new BorderLayout(0, 0));

        JScrollPane scrollPane = new JScrollPane();
        panel.add(scrollPane);

        locationList = new JList<String>();

        locationList.addMouseListener(new ListMouseAdapter(jobListing, fileListing));

        //hide the renderer from windowsbuilder.
        //$hide$
        locationList.setCellRenderer(new IconLabelCellRenderer());

        locationList.setModel(new LocationListModel());
        scrollPane.setViewportView(locationList);

        JPopupMenu popupMenu = new JPopupMenu();
        locationList.addMouseListener(new ListPopupMouseListener(popupMenu));

        JMenuItem mntmAddALocation = new JMenuItem("Add a Location");
        ImageIcon remoteIcon = Utils.loadIcon("places/network-server.png");
        mntmAddALocation.setIcon(remoteIcon);
        popupMenu.add(mntmAddALocation);

        JPanel panel_1 = new JPanel();
        panel.add(panel_1, BorderLayout.SOUTH);

        JLabel lblNlescLogo = new JLabel(Utils.loadIcon("nlesc-logo.png"));
        panel_1.add(lblNlescLogo);

    }

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    OctopusExplorer frame = new OctopusExplorer();
                    frame.setVisible(true);
                    logger.info("Octopus Explorer Initialized");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
