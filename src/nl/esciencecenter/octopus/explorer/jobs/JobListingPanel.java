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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import nl.esciencecenter.octopus.Octopus;
import nl.esciencecenter.octopus.explorer.Utils;

public class JobListingPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private JTable table;
    private final DefaultTableModel theModel;
    private UpdateJobListWorker currentTask = null;

    private String currentLocation = "Local";
    private final Octopus octopus;
    private final Action refreshAction;
    private final Action submitJobAction;

    private final SubmitJobDialog dialog;

    private final JFrame frame;

    public void setCurrentLocation(String location) {
        this.currentLocation = location;
    }

    public void triggerRefresh(boolean setPathToFSEntry) {
        if (currentTask != null) {
            currentTask.cancel(false);
        }
        theModel.setRowCount(0);
        currentTask = new UpdateJobListWorker(theModel, currentLocation, octopus);
        currentTask.execute();
    }

    /**
     * Create the panel.
     */
    public JobListingPanel(Octopus octopus, JFrame frame) throws Exception {
        this.octopus = octopus;
        this.frame = frame;

        refreshAction = new RefreshAction();
        submitJobAction = new SubmitJobAction();
        dialog = new SubmitJobDialog(frame, currentLocation, octopus);

        setLayout(new BorderLayout(0, 0));

        JPanel panel = new JPanel();
        add(panel, BorderLayout.NORTH);
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JButton btnRefresh = new JButton(refreshAction);
        panel.add(btnRefresh);

        JButton btnSubmitJob = new JButton(submitJobAction);
        panel.add(btnSubmitJob);

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

    private class RefreshAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public RefreshAction() throws Exception {
            putValue(NAME, "Refresh");
            putValue(SHORT_DESCRIPTION, "Refresh the jobs list");

            putValue(SMALL_ICON, Utils.loadIcon("actions/view-refresh.png"));
        }

        public void actionPerformed(ActionEvent e) {
            if (currentTask != null) {
                currentTask.cancel(true);
            }
            theModel.setRowCount(0);
            currentTask = new UpdateJobListWorker(theModel, currentLocation, octopus);
            currentTask.execute();
        }
    }

    private class SubmitJobAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public SubmitJobAction() throws Exception {
            putValue(NAME, "Submit Job");
            putValue(SHORT_DESCRIPTION, "Submit a new Job");

            putValue(SMALL_ICON, Utils.loadIcon("actions/appointment-new.png"));
        }

        public void actionPerformed(ActionEvent e) {
            dialog.setLocation(currentLocation);
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
            triggerRefresh(false);
        }
    }
}
