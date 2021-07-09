package swiss.flow.ciengine.jenkins.plugins;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class CiEngineHttpClient {

    public CiEngineHttpClient() {
    }

    public HttpResponse<String> post(String url, Map<String, String> headers, String body) {

        HttpResponse<String> response = null;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .POST(HttpRequest.BodyPublishers.ofString(body));

            for (Map.Entry<String,String> entry:
                 headers.entrySet()) {
                builder.header(entry.getKey(),entry.getValue());
            }
            
            HttpRequest request = builder.build();

            response = HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return response;
    }

    public HttpResponse<String> get(String url, Map<String, String> headers) {

        HttpResponse<String> response = null;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .GET();

            for (Map.Entry<String,String> entry:
                    headers.entrySet()) {
                builder.header(entry.getKey(),entry.getValue());
            }

            HttpRequest request = builder.build();

            response = HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return response;
    }

    public Map<String, String> getJsonHeader() {
        return new HashMap<>() {
            {
                put("Content-type", "application/json");
            }
        };
    }
}
