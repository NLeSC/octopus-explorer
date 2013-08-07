package nl.esciencecenter.octopus.explorer.jobs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.esciencecenter.octopus.Octopus;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.exceptions.OctopusIOException;
import nl.esciencecenter.octopus.jobs.Job;
import nl.esciencecenter.octopus.jobs.JobDescription;
import nl.esciencecenter.octopus.jobs.JobStatus;
import nl.esciencecenter.octopus.jobs.Scheduler;

/**
 * Updates a Job list by fetching all files from the given scheduler location
 * @author Niels Drost
 *
 */
class SubmitJobWorker extends SwingWorker<Void, Void> {
    
    private static final Logger logger = LoggerFactory.getLogger(SubmitJobWorker.class);
    
    private final JobDescription jobDescription;
    private final String location;
    private final Octopus octopus;

    SubmitJobWorker(JobDescription jobDescription, String location, Octopus octopus) {
        this.jobDescription = jobDescription;
        this.location = location;
        this.octopus = octopus;
    }

    @Override
    public Void doInBackground() throws OctopusIOException, OctopusException, URISyntaxException {
        String queue;

        logger.debug("submitting job");
        
        Scheduler scheduler;
        if (location.equals("Local")) {
            scheduler = octopus.jobs().getLocalScheduler();
            queue = "multiq";
        } else {
            scheduler = octopus.jobs().newScheduler(new URI("ge://" + location), null, null);
            queue = "all.q";
        }
        jobDescription.setQueueName(queue);
        
        logger.debug("submitting job: " + jobDescription);

        Job result = octopus.jobs().submitJob(scheduler, jobDescription);
        
        logger.debug("submitted job: " + result);

        return null;
    }
}