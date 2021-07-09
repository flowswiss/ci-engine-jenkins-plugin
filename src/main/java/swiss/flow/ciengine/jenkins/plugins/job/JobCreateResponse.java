package swiss.flow.ciengine.jenkins.plugins.job;

public class JobCreateResponse {

    private String id = "";
    private String status = "created";

    private String jenkinsAgentName = "";

    private String createdAt = "";
    JobCreateResponse(){}

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getJenkinsAgentName() {
        return jenkinsAgentName;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
