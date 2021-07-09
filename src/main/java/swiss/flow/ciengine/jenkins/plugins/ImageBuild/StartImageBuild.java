package swiss.flow.ciengine.jenkins.plugins.ImageBuild;

public class StartImageBuild {
    private final String authToken;
    private final String id;
    private final String baseUrl;


    public StartImageBuild(String authToken, String baseUrl, String id) {
        this.authToken = authToken;
        this.id = id;
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getId() {
        return id;
    }

    public String getAuthToken() {
        return authToken;
    }
}
