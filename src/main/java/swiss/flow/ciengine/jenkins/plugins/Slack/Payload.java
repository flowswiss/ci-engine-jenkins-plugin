package swiss.flow.ciengine.jenkins.plugins.Slack;

import net.sf.json.JSON;

public class Payload {

    private final String url;

    private final JSON json;

    public Payload(String url, JSON json) {
        this.url = url;
        this.json = json;
    }
    public String getUrl() {
        return url;
    }

    public JSON getJson() {
        return json;
    }

    public String getJsonString() {
        return getJson().toString();
    }
}
