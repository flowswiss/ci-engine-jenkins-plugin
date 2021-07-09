package swiss.flow.ciengine.jenkins.plugins.Messages;

import java.util.ArrayList;
import java.util.List;

public class SlackMessage {

    private String header;

    private final String notification;

    private ArrayList<String> sections;

    public SlackMessage(String header, String notification, String ...section) {
        this.header = header;
        this.notification = notification;
        this.sections = new ArrayList<>(List.of(section));
    }

    public String getHeader() {
        return header;
    }

    public ArrayList<String> getSections() {
        return sections;
    }

    public String getNotification() {
        return notification;
    }
}
