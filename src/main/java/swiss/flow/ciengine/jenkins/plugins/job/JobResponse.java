package swiss.flow.ciengine.jenkins.plugins.job;

public class JobResponse {
    private String id = "";
    private String status = "created";

    private String jenkinsAgentName = "";

    private String createdAt = "";

    JobResponse(){}

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
