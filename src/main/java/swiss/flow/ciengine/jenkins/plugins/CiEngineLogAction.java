package swiss.flow.ciengine.jenkins.plugins;

import hudson.model.Run;
import jenkins.model.RunAction2;


public class CiEngineLogAction implements RunAction2 {

    private String logs;

    public CiEngineLogAction(String logs) {
        this.logs = logs;
    }

    public String getLogs() {
        return logs;
    }
    private transient Run run;

    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }

    public Run getRun() {
        return run;
    }
    @Override
    public String getIconFileName() {
        return "document.png";
    }

    @Override
    public String getDisplayName() {
        return "Ci Engine Logs";
    }

    @Override
    public String getUrlName() {
        return "ci-engine-logs";
    }
}
