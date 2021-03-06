package nl.esciencecenter.octopus.explorer.files;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import nl.esciencecenter.octopus.Octopus;
import nl.esciencecenter.octopus.credentials.Credential;
import nl.esciencecenter.octopus.exceptions.AttributeNotSupportedException;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.exceptions.OctopusIOException;
import nl.esciencecenter.octopus.files.AbsolutePath;
import nl.esciencecenter.octopus.files.DirectoryStream;
import nl.esciencecenter.octopus.files.FileSystem;
import nl.esciencecenter.octopus.files.PathAttributesPair;
import nl.esciencecenter.octopus.files.RelativePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Returns a RelativePath as result, and produces a RelativePath per File found at the given location+path
 * 
 * @author Niels Drost
 * 
 */
class UpdateFileListWorker extends SwingWorker<RelativePath, PathAttributesPair> {

    private static final Logger logger = LoggerFactory.getLogger(UpdateFileListWorker.class);

    private final DefaultTableModel tableModel;
    private final String location;
    private final RelativePath path;
    private final boolean setPathToFSEntry;
    private final Octopus octopus;
    private final FileListingPanel fileListing;

    UpdateFileListWorker(DefaultTableModel tableModel, String location, RelativePath path, boolean setPathToFSEntry,
            Octopus octopus, FileListingPanel fileListing) {
        this.tableModel = tableModel;
        this.location = location;
        this.path = path;
        this.setPathToFSEntry = setPathToFSEntry;
        this.octopus = octopus;
        this.fileListing = fileListing;
    }

    @Override
    public RelativePath doInBackground() throws OctopusIOException, OctopusException, URISyntaxException {
        FileSystem fileSystem;
        logger.debug("getting file list at {}", location);

        if (location.equals("Local")) {
            fileSystem = octopus.files().getLocalHomeFileSystem();
        } else {
            Credential credential = octopus.credentials().getDefaultCredential("ssh");

            fileSystem = octopus.files().newFileSystem(new URI("ssh://" + location), credential, null);
        }

        logger.debug("got filesystem {}", fileSystem);

        AbsolutePath entryPath = fileSystem.getEntryPath();
        AbsolutePath target;
        if (setPathToFSEntry) {
            target = entryPath;
        } else {
            target = octopus.files().newPath(fileSystem, path);
        }

        DirectoryStream<PathAttributesPair> stream = octopus.files().newAttributesDirectoryStream(target);

        while (stream.iterator().hasNext()) {
            PathAttributesPair pair = stream.iterator().next();
            logger.debug("got file {}", pair.path());
            publish(pair);
        }

        octopus.files().close(fileSystem);

        return entryPath.getRelativePath();
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
                fileListing.setCurrentPath(get());
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Got error updating file listing", e);
            }
        }
    }
}