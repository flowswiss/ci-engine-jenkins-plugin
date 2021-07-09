package swiss.flow.ciengine.jenkins.plugins;

import com.google.common.base.Strings;
import hudson.Extension;
import hudson.model.*;
import hudson.slaves.*;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import swiss.flow.ciengine.jenkins.plugins.Messages.SlackMessage;
import swiss.flow.ciengine.jenkins.plugins.Slack.Payload;
import swiss.flow.ciengine.jenkins.plugins.Slack.SlackNotifier;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CiEngineCloud extends Cloud {

    private static final Logger LOGGER = Logger.getLogger(CiEngineCloud.class.getName());

    private static final Object provisionSynchronizor = new Object();

    private String userToken;

    private String subscriptionId;

    private String label;

    private String box;

    private String baseUrl;

    private String slackHookUrl;

    private Boolean debug;

    private String debugPort;

    private Boolean build;

    @DataBoundConstructor
    public CiEngineCloud(String name,
                         String userToken,
                         String subscriptionId,
                         String label,
                         String box,
                         String baseUrl,
                         String slackHookUrl,
                         Boolean debug,
                         String debugPort,
                         Boolean build
                      ) {
        super(name);
        this.userToken = userToken;
        this.subscriptionId = subscriptionId;
        this.label = label;

        this.box = box;
        this.baseUrl = baseUrl;
        this.slackHookUrl = slackHookUrl;
        this.debug = debug;
        this.debugPort = debugPort;
        this.build = build;
    }


    protected CiEngineCloud(String name) {
        super(name);
    }

    public String getUserToken() {
        return userToken;
    }

    @DataBoundSetter
    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    @DataBoundSetter
    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getLabel() {
        return label;
    }

    @DataBoundSetter
    public void setLabel(String label) {
        this.label = label;
    }

    public String getBox() { return box; }

    @DataBoundSetter
    public void setBox(String box) { this.box = box; }


    public String getBaseUrl() { return baseUrl; }

    @DataBoundSetter
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }


    public String getSlackHookUrl() { return slackHookUrl; }

    @DataBoundSetter
    public void setSlackHookUrl(String slackHookUrl) { this.slackHookUrl = slackHookUrl; }


    public Boolean getDebug() {
        return debug;
    }

    @DataBoundSetter
    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    public String getDebugPort() {
        return debugPort;
    }

    @DataBoundSetter
    public void setDebugPort(String debugPort) {
        this.debugPort = debugPort;
    }

    public Boolean getBuild() {
        return build;
    }

    @DataBoundSetter
    public void setBuild(Boolean build) {
        this.build = build;
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(CloudState state, int excessWorkload) {

        Label stateLabel = state.getLabel();
        if (stateLabel != null) {
            LOGGER.log(Level.SEVERE, "Label: " + stateLabel.toString());
            LOGGER.log(Level.SEVERE, "Clouds: " + stateLabel.getClouds());
            LOGGER.log(Level.SEVERE, "label Clouds: " + label);

            if(stateLabel.getName().equals(label)) {
                //all is well, continue
                String isBuild = build ? "Yes": "No";

                SlackMessage message = new SlackMessage("Starting Build", "Starting Build on Ci-Engine", "Api Url: " + baseUrl, "Box: " + box, "Image Build: " + isBuild);

                Payload payload = new Payload(slackHookUrl, SlackNotifier.createBlockMessages(message));
                SlackNotifier.sendLogToSlack(payload);

                synchronized (provisionSynchronizor) {

                    List<NodeProvisioner.PlannedNode> provisioningNodes = new ArrayList<>();

                    final CompletableFuture<Node> plannedNode = new CompletableFuture<>();
                    provisioningNodes.add(new NodeProvisioner.PlannedNode("PlannedNode", plannedNode, 1));

                    Computer.threadPoolForRemoting.submit(() -> {
                        Agent node = null;


                        synchronized (provisionSynchronizor) {

                            Jenkins instanceOrNull = Jenkins.getInstanceOrNull();

                            if (instanceOrNull == null) {
                                LOGGER.log(Level.SEVERE, "Jenkins Instance not found");
                                throw new RuntimeException("Jenkins.getInstanceOrNull() returned null");
                            }

                            UUID uuid = UUID.randomUUID();
                            //UUID uuid = UUID.fromString("7e089bde-83ae-44d3-84b3-343b5c376b12");
                            try {
                                node = new Agent(uuid.toString(), "/tmp", new CiEngineNodeLauncher(new JNLPLauncher(true)));
                                node.setLabelString(label);
                            } catch (Descriptor.FormException | IOException e) {
                                throw new RuntimeException(e);
                            }
                            node.setRetentionStrategy(new CiEngineRetentionStrategy(build ? 5 : 1));
                            node.setSubscription(subscriptionId);
                            node.setUserToken(userToken);
                            node.setBox(box);
                            node.setDebug(debug);
                            node.setDebugPort(debugPort);
                            node.setBaseUrl(baseUrl);
                            node.setSlackHookUrl(slackHookUrl);
                            node.setBuild(build);
                        }
                        plannedNode.complete(node);
                    });

                    return provisioningNodes;
                }

            } else {
                //ignoring this cloud
                LOGGER.log(Level.SEVERE, "Cloud " + stateLabel.getClouds() + " ignored");
                return new ArrayList<>();

            }

        }


        return null;
    }


    @Override
    public boolean canProvision(CloudState state) {

        //check joblimit

        //subsciption/{id}/jobs

        //return true if joblimit is not reached

        return true;
    }

    @Override
    public boolean canProvision(Label label) {
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Cloud> {
        public DescriptorImpl() {
            load();
        }

        public String getDisplayName() {
            return "CI Engine Configuration";
        }


        public FormValidation doCheckName(@QueryParameter String name) {
            if (Strings.isNullOrEmpty(name)) {
                return FormValidation.error("Must be set");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckUserToken(@QueryParameter String userToken) {
            if (Strings.isNullOrEmpty(userToken)) {
                return FormValidation.error("Must be set");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckSubscriptionId(@QueryParameter String subscriptionId) {
            if (Strings.isNullOrEmpty(subscriptionId)) {
                return FormValidation.error("Must be set");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckBaseUrl(@QueryParameter String baseUrl) {
            if (Strings.isNullOrEmpty(baseUrl)) {
                return FormValidation.error("Must be set");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckBox(@QueryParameter String box) {
            if (Strings.isNullOrEmpty(box)) {
                return FormValidation.error("Must be set");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckSlackHookUrl(@QueryParameter String slackHookUrl) {
            return FormValidation.ok();
        }

        public FormValidation doCheckDebug(@QueryParameter Boolean debug) {
            return FormValidation.ok();
        }

        public FormValidation doCheckDebugPort(@QueryParameter String debugPort) {
            return FormValidation.ok();
        }

        public FormValidation doCheckBuild(@QueryParameter Boolean build) {
            return FormValidation.ok();
        }
    }

}
