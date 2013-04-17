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
package nl.esciencecenter.octopus.explorer.jobs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import nl.esciencecenter.octopus.Octopus;
import nl.esciencecenter.octopus.exceptions.OctopusException;
import nl.esciencecenter.octopus.exceptions.OctopusIOException;
import nl.esciencecenter.octopus.jobs.Job;
import nl.esciencecenter.octopus.jobs.JobStatus;
import nl.esciencecenter.octopus.jobs.Scheduler;

public class JobListing extends JPanel {
    private static final long serialVersionUID = 1L;
    private JTable table;
    private final Action refreshAction = new RefreshAction();
    private final DefaultTableModel theModel;
    private UpdateListWorker currentTask = null;

    private String currentLocation = "Local";
    private final Octopus octopus;

    public void setCurrentLocation(String location) {
        this.currentLocation = location;
    }
    
    public void triggerRefresh(boolean setPathToFSEntry) {
        if (currentTask != null) {
            currentTask.cancel(false);
        }
        theModel.setRowCount(0);
        currentTask = new UpdateListWorker(theModel, currentLocation);
        currentTask.execute();
    }

    /**
     * Create the panel.
     */
    public JobListing(Octopus octopus) {
        this.octopus = octopus;
        setLayout(new BorderLayout(0, 0));

        JPanel panel = new JPanel();
        add(panel, BorderLayout.NORTH);
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JButton btnRefresh = new JButton(refreshAction);
        panel.add(btnRefresh);

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

        theModel = new DefaultTableModel(new String[] { "Job ID", "State", "Owner", "Slots" }, 0);

        table.setModel(theModel);

        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
    }

    class UpdateListWorker extends SwingWorker<List<JobStatus>, JobStatus> {
        private final DefaultTableModel tableModel;
        private final String location;

        UpdateListWorker(DefaultTableModel tableModel, String location) {
            this.tableModel = tableModel;
            this.location = location;
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

    private class RefreshAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public RefreshAction() {
            putValue(NAME, "Refresh");
            putValue(SHORT_DESCRIPTION, "Some short description");

            Icon refreshIcon = new ImageIcon("resources/icons/actions/view-refresh.png");
            putValue(SMALL_ICON, refreshIcon);
        }

        public void actionPerformed(ActionEvent e) {
            if (currentTask != null) {
                currentTask.cancel(true);
            }
            theModel.setRowCount(0);
            currentTask = new UpdateListWorker(theModel, currentLocation);
            currentTask.execute();
        }
    }
}
