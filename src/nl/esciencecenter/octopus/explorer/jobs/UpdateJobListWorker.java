package nl.esciencecenter.octopus.explorer.jobs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import nl.esciencecenter.octopus.Octopus;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.exceptions.OctopusIOException;
import nl.esciencecenter.octopus.jobs.Job;
import nl.esciencecenter.octopus.jobs.JobStatus;
import nl.esciencecenter.octopus.jobs.Scheduler;

/**
 * Updates a Job list by fetching all files from the given scheduler location
 * @author Niels Drost
 *
 */
class UpdateJobListWorker extends SwingWorker<List<JobStatus>, JobStatus> {
    private final DefaultTableModel tableModel;
    private final String location;
    private final Octopus octopus;

    UpdateJobListWorker(DefaultTableModel tableModel, String location, Octopus octopus) {
        this.tableModel = tableModel;
        this.location = location;
        this.octopus = octopus;
    }

    @Override
    public List<JobStatus> doInBackground() throws OctopusIOException, OctopusException, URISyntaxException {
        String queue;
        List<JobStatus> result = new LinkedList<JobStatus>();

        Scheduler scheduler;
        if (location.equals("Local")) {
            scheduler = octopus.jobs().getLocalScheduler();
            queue = "multiq";
        } else {
            scheduler = octopus.jobs().newScheduler(new URI("ge://" + location), null, null);
            queue = "all.q";
        }

        Job[] jobs = octopus.jobs().getJobs(scheduler, queue);

        for (int i = 0; i < jobs.length; i++) {
            JobStatus status = octopus.jobs().getJobStatus(jobs[i]);

            result.add(status);
            publish(status);

            setProgress((100 * i) / jobs.length);
        }

        return null;
    }

    @Override
    protected void process(List<JobStatus> chunks) {
        for (JobStatus status : chunks) {
            tableModel.addRow(new String[] { status.getJob().getIdentifier(), status.getState(),
                    status.getSchedulerSpecficInformation().get("JB_owner"),
                    status.getSchedulerSpecficInformation().get("slots") });

        }
    }
}