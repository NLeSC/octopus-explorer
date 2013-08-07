package nl.esciencecenter.octopus.explorer.jobs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.factories.FormFactory;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.Action;

import nl.esciencecenter.octopus.Octopus;
import nl.esciencecenter.octopus.jobs.JobDescription;

public class SubmitJobDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private final JPanel contentPanel = new JPanel();
    private JTextField executable;
    private JTextField arguments;
    private final Action okAction;
    private final Action cancelAction;
    private String location;
    private final Octopus octopus;

    /**
     * Create the dialog.
     */
    public SubmitJobDialog(JFrame parent, String location, Octopus octopus) {
        super(parent, true);
        this.location = location;
        this.octopus = octopus;
        okAction = new OKAction(this);
        cancelAction = new CancelAction(this);
        setBounds(100, 100, 450, 300);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new FormLayout(new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC, FormFactory.DEFAULT_COLSPEC,
                FormFactory.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), }, new RowSpec[] {
                FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC, }));
        {
            JLabel lblExecutable = new JLabel("Executable");
            contentPanel.add(lblExecutable, "2, 2, right, default");
        }
        {
            executable = new JTextField();
            contentPanel.add(executable, "4, 2, fill, default");
            executable.setColumns(10);
        }
        {
            JLabel lblArguments = new JLabel("Arguments");
            contentPanel.add(lblArguments, "2, 4, right, default");
        }
        {
            arguments = new JTextField();
            contentPanel.add(arguments, "4, 4, fill, default");
            arguments.setColumns(10);
        }
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton okButton = new JButton("OK");
                okButton.setAction(okAction);
                okButton.setActionCommand("OK");
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
            {
                JButton cancelButton = new JButton("Cancel");
                cancelButton.setAction(cancelAction);
                cancelButton.setActionCommand("Cancel");
                buttonPane.add(cancelButton);
            }
        }
    }

    private void clear() {
        executable.setText(null);
        arguments.setText(null);
    }

    void setLocation(String location) {
        this.location = location;
    }

    private class OKAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        SubmitJobDialog dialog;

        public OKAction(SubmitJobDialog dialog) {
            this.dialog = dialog;
            putValue(NAME, "OK");
            putValue(SHORT_DESCRIPTION, "Submit this job");
        }

        public void actionPerformed(ActionEvent e) {
            JobDescription description = new JobDescription();
            description.setExecutable(executable.getText());
            description.setArguments(arguments.getText().split(" "));

            SubmitJobWorker worker = new SubmitJobWorker(description, location, octopus);
            worker.execute();

            dialog.clear();
            dialog.setVisible(false);
        }
    }

    private class CancelAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        SubmitJobDialog dialog;

        public CancelAction(SubmitJobDialog dialog) {
            this.dialog = dialog;
            putValue(NAME, "Cancel");
            putValue(SHORT_DESCRIPTION, "Cancel");
        }

        public void actionPerformed(ActionEvent e) {
            dialog.clear();
            dialog.setVisible(false);
        }
    }
}
