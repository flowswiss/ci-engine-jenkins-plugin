package swiss.flow.ciengine.jenkins.plugins.job;

public class JobLogsResponse {

    private String id;

    private String level;

    private String message;

    private String createdAt;

    private String job;

    JobLogsResponse() {}

    public String getId() {
        return id;
    }

    public String getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getJob() {
        return job;
    }
}
