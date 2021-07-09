package swiss.flow.ciengine.jenkins.plugins.ImageBuild;

import swiss.flow.ciengine.jenkins.plugins.CiEngineHttpClient;

import java.net.http.HttpResponse;
import java.util.HashMap;

public class ImageBuilder {


    public static void startBuild(StartImageBuild startImageBuild) {

        CiEngineHttpClient httpclient = new CiEngineHttpClient();
        String authToken = startImageBuild.getAuthToken();
        httpclient.post(startImageBuild.getBaseUrl() + "/jobs/" + startImageBuild.getId() + "/build-box", new HashMap<>() {
            {
                put("X-AUTH-TOKEN", authToken);
                put("Content-type", "application/json");
            }
        }, "");

    }
}
