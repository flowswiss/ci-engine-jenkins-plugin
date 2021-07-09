package swiss.flow.ciengine.jenkins.plugins.Slack;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import swiss.flow.ciengine.jenkins.plugins.CiEngineHttpClient;
import swiss.flow.ciengine.jenkins.plugins.Messages.SlackMessage;

import java.net.http.HttpResponse;

public class SlackNotifier {

    /*
                LOGGER.log(Level.INFO, "Waiting for agent to boot...(10 seconds)");
            LOGGER.log(Level.INFO, "Waiting for agent to boot...(10 seconds)");
{
	"blocks": [
		{
			"type": "header",
			"text": {
				"type": "plain_text",
				"text": "STAGE: New Build started",
				"emoji": true
			}
		},
		{
			"type": "section",
			"text": {
				"type": "mrkdwn",
				"text": "*Agent Name:*\n6bdea240-c823-4a68-9530-0c2394fe9a98"
			}
		},
		{
			"type": "section",
			"text": {
				"type": "mrkdwn",
				"text": "*User:*\nTest1234"
			}
		},
		{
			"type": "section",
			"text": {
				"type": "mrkdwn",
				"text": "*Job-Status:*\nWaiting"
			}
		}
	]
}
     */


    private static JSONObject createContent(String type, String text) {
        JSONObject json = new JSONObject();
        json.put("type", type);
        json.put("text", text);
        return json;
    }

    private static JSONObject createContent(String type, JSONObject object) {
        JSONObject json = new JSONObject();
        json.put("type", type);
        json.put("text", object);
        return json;
    }

    public static JSONObject createBlockMessages(SlackMessage message) {
        JSONObject json = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(createContent("header", createContent("plain_text", message.getHeader())));

        for (String section :
                message.getSections()) {
            jsonArray.add(createContent("section", createContent("mrkdwn", section)));
        }
        json.put("blocks", jsonArray);
        if (message.getNotification() != null) {
            json.put("text", message.getNotification());
        }
        return json;
    }


    public static void sendLogToSlack(Payload payload) {

        CiEngineHttpClient httpClient = new CiEngineHttpClient();

        HttpResponse<String> response = httpClient.post(payload.getUrl(), httpClient.getJsonHeader(), payload.getJsonString());
        System.out.println("Slack: Here the content:");
        System.out.println(response.toString());
        System.out.println(response.body());
        System.out.println("Slack: Content finished");


    }
}
