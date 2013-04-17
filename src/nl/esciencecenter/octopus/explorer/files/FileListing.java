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

import nl.esciencecenter.octopus.Octopus;
import nl.esciencecenter.octopus.credentials.Credential;
import nl.esciencecenter.octopus.credentials.Credentials;
import nl.esciencecenter.octopus.exceptions.AttributeNotSupportedException;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.exceptions.OctopusIOException;
import nl.esciencecenter.octopus.explorer.OctopusExplorer;
import nl.esciencecenter.octopus.files.AbsolutePath;
import nl.esciencecenter.octopus.files.DirectoryStream;
import nl.esciencecenter.octopus.files.FileSystem;
import nl.esciencecenter.octopus.files.PathAttributesPair;
import nl.esciencecenter.octopus.files.RelativePath;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileListing extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(FileListing.class);

    private static final long serialVersionUID = 1L;
    private JTable table;
    private final Action refreshAction = new RefreshAction();
    private final DefaultTableModel theModel;
    private UpdateListWorker currentTask = null;

    private String currentLocation = "Local";

    private RelativePath currentPath = new RelativePath();

    private final Octopus octopus;

    private final Action action = new UpAction();

    /**
     * @author Niels Drost
     * 
     */
    private final class FileTableMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                try {
                    int row = table.rowAtPoint(e.getPoint());

                    Object value = theModel.getValueAt(row, 0);

                    PathAttributesPair pa = (PathAttributesPair) value;

                    logger.debug("double-click on file: " + pa.path().getFileName());

                    if (pa.attributes().isDirectory()) {
                        toChildDirectory(pa.path().getFileName());
                        triggerRefresh(false);
                    } else if (pa.path().isLocal()) {
                        Desktop dt = Desktop.getDesktop();
                        dt.open(new java.io.File(pa.path().getPath()));
                    } else {
                        logger.warn("Cannot open non-local files");
                    }

                } catch (ClassCastException | IOException exception) {
                    logger.error("Error on viewing file / going into directory", exception);
                }
            }
        }
    }

    /**
     * @author Niels Drost
     * 
     */
    private final static class FileListingTableModel extends DefaultTableModel {
        private static final long serialVersionUID = 1L;

        private static final Class[] COLUMN_TYPES = new Class[] { PathAttributesPair.class, String.class, String.class,
                String.class };

        private static final String[] COLUMN_NAMES = new String[] { "Name", "Size", "Type", "Date Modified" };

        /**
         * @param columnNames
         * @param rowCount
         */
        private FileListingTableModel() {
            super(COLUMN_NAMES, 0);
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return COLUMN_TYPES[columnIndex];
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    /**
     * Displays Strings as JLabels
     */
    private class LabelRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 2l;

        private final ImageIcon folderIcon;

        private final MimeTypeIcons mimeTypeIcons;

        LabelRenderer() throws IOException {
            folderIcon = OctopusExplorer.loadIcon("places/folder.png");
            mimeTypeIcons = new MimeTypeIcons();
        }

        /* 
         * (non-Javadoc) 
         *  
         * @see 
         * javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent 
         * (javax.swing.JTable, java.lang.Object, boolean, boolean, int, int) 
         */
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            PathAttributesPair pa = (PathAttributesPair) value;

            String fileName = pa.path().getFileName();
            boolean isFolder;
            try {
                isFolder = pa.attributes().isDirectory();
            } catch (AttributeNotSupportedException e) {
                isFolder = false;
            }

            Icon icon;
            if (isFolder) {
                icon = folderIcon;
            } else {
                icon = mimeTypeIcons.getIconFor(fileName);
            }

            JLabel label = new JLabel(fileName, icon, SwingConstants.LEFT);
            label.setBorder(new EmptyBorder(1, 1, 1, 1));

            //make the label draw its entire content (including background)
            label.setOpaque(true);

            if (isSelected) {
                label.setBackground(table.getSelectionBackground());
                label.setForeground(table.getSelectionForeground());
            } else {
                label.setBackground(table.getBackground());
                label.setForeground(table.getForeground());
            }

            if (hasFocus) {
                label.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            }

            setEnabled(table.isEnabled());
            setFont(table.getFont());

            return label;
        }

    }

    /**
     * Create the panel.
     * @throws IOException 
     */
    public FileListing(Octopus octopus) throws IOException {
        this.octopus = octopus;
        setLayout(new BorderLayout(0, 0));

        JPanel panel = new JPanel();
        add(panel, BorderLayout.NORTH);
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JButton btnRefresh = new JButton(refreshAction);
        panel.add(btnRefresh);

        JButton btnUp = new JButton("Up");
        btnUp.setAction(action);
        panel.add(btnUp);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane);

        table = new JTable();
        table.setGridColor(Color.LIGHT_GRAY);
        table.setFont(new Font("Dialog", Font.PLAIN, 12));
        JTableHeader header = table.getTableHeader();
        header.setFont(header.getFont().deriveFont(Font.BOLD).deriveFont(14f));
        table.setFillsViewportHeight(true);
        scrollPane.setViewportView(table);

        table.addMouseListener(new FileTableMouseListener());

        theModel = new FileListingTableModel();

        table.setDefaultRenderer(PathAttributesPair.class, new LabelRenderer());

        table.setModel(theModel);

        table.setRowHeight(30);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);

        //trigger refresh, set path
        triggerRefresh(true);
    }

    public void triggerRefresh(boolean setPathToFSEntry) {
        if (currentTask != null) {
            currentTask.cancel(false);
        }
        theModel.setRowCount(0);
        currentTask = new UpdateListWorker(theModel, currentLocation, currentPath, setPathToFSEntry);
        currentTask.execute();
    }

    public void setCurrentLocation(String location) {
        this.currentLocation = location;
        logger.debug("current location (updated): " + currentLocation + " current path: " + currentPath);
    }

    /**
     * Update relative path to a new location.
     * 
     * @param path
     */
    private void setCurrentPath(RelativePath path) {
        this.currentPath = path;
        logger.debug("current location: " + currentLocation + " current path(updated): " + currentPath);
    }

    void toParentDirectory() {
        if (currentPath.isEmpty()) {
            logger.debug("current location: " + currentLocation + " current path(up): " + currentPath);
            return;
        }

        currentPath = currentPath.getParent();

        if (currentPath == null) {
            currentPath = new RelativePath();
        }
        logger.debug("current location: " + currentLocation + " current path(up): " + currentPath);
    }

    void toChildDirectory(String fileName) {
        if (currentPath == null) {
            currentPath = new RelativePath();
        }

        currentPath = currentPath.resolve(new RelativePath(fileName));
        logger.debug("current location: " + currentLocation + " current path(down): " + currentPath);
    }

    /**
     * Returns a RelativePath as result, and produces a RelativePath per File found at the given location+path
     * 
     * @author Niels Drost
     * 
     */
    class UpdateListWorker extends SwingWorker<RelativePath, PathAttributesPair> {
        private final DefaultTableModel tableModel;
        private final String location;
        private final RelativePath path;
        private final boolean setPathToFSEntry;

        UpdateListWorker(DefaultTableModel tableModel, String location, RelativePath path, boolean setPathToFSEntry) {
            this.tableModel = tableModel;
            this.location = location;
            this.path = path;
            this.setPathToFSEntry = setPathToFSEntry;
        }

        @Override
        public RelativePath doInBackground() throws OctopusIOException, OctopusException, URISyntaxException {
            FileSystem fileSystem;
            if (location.equals("Local")) {
                fileSystem = octopus.files().getLocalHomeFileSystem(null);
            } else {
                Credentials c = octopus.credentials();

                //FIXME: only works for rsa keys, a bit explicit.
                String username = System.getProperty("user.name");
                Credential credential =
                        c.newCertificateCredential("ssh", null, "/home/" + username + "/.ssh/id_rsa", "/home/" + username
                                + "/.ssh/id_rsa.pub", username, "");

                fileSystem = octopus.files().newFileSystem(new URI("ssh://" + location), credential, null);
            }

            RelativePath entryPath = fileSystem.getEntryPath();
            AbsolutePath target;
            if (setPathToFSEntry) {
                target = octopus.files().newPath(fileSystem, entryPath);
            } else {
                target = octopus.files().newPath(fileSystem, path);
            }

            DirectoryStream<PathAttributesPair> stream = octopus.files().newAttributesDirectoryStream(target);

            while (stream.iterator().hasNext()) {
                PathAttributesPair pair = stream.iterator().next();
                publish(pair);
            }

            octopus.files().close(fileSystem);

            return entryPath;
        }

        @Override
        protected void process(List<PathAttributesPair> chunks) {
            for (PathAttributesPair pair : chunks) {

                try {
                    String size;
                    String type;

                    if (pair.attributes().isDirectory()) {
                        size = "";
                        type = "folder";
                    } else {
                        size = String.format("%.3f Mb", (pair.attributes().size() / 1000000f));
                        type = "file";
                    }

                    String modified = new Date(pair.attributes().lastModifiedTime()).toString();

                    if (!pair.attributes().isHidden()) {
                        tableModel.addRow(new Object[] { pair, size, type, modified });
                    }
                } catch (AttributeNotSupportedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }

        @Override
        protected void done() {
            //update path in main class / swing thread. 
            if (!isCancelled() && setPathToFSEntry) {
                try {
                    setCurrentPath(get());
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Got error updating file listing", e);
                }
            }
        }
    }

    private class RefreshAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public RefreshAction() {
            putValue(NAME, "Refresh");
            putValue(SHORT_DESCRIPTION, "Some short description");

            Icon refreshIcon = OctopusExplorer.loadIcon("actions/view-refresh.png");
            putValue(SMALL_ICON, refreshIcon);
        }

        public void actionPerformed(ActionEvent e) {
            triggerRefresh(false);
        }
    }

    private class UpAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public UpAction() {
            putValue(NAME, "Up");
            putValue(SHORT_DESCRIPTION, "Some short description");

            Icon upIcon = OctopusExplorer.loadIcon("actions/go-up.png");
            putValue(SMALL_ICON, upIcon);
        }

        public void actionPerformed(ActionEvent e) {
            logger.debug("Going up");
            toParentDirectory();
            triggerRefresh(false);
        }
    }
}
