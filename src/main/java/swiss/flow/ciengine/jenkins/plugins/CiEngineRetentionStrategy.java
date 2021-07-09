package swiss.flow.ciengine.jenkins.plugins;


import com.google.gson.Gson;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.CloudRetentionStrategy;
import jenkins.model.Jenkins;
import swiss.flow.ciengine.jenkins.plugins.ImageBuild.ImageBuilder;
import swiss.flow.ciengine.jenkins.plugins.ImageBuild.StartImageBuild;
import swiss.flow.ciengine.jenkins.plugins.Messages.SlackMessage;
import swiss.flow.ciengine.jenkins.plugins.Slack.Payload;
import swiss.flow.ciengine.jenkins.plugins.Slack.SlackNotifier;
import swiss.flow.ciengine.jenkins.plugins.job.JobResponse;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CiEngineRetentionStrategy extends CloudRetentionStrategy {

    private static final Logger LOGGER = Logger.getLogger(CiEngineCloud.class.getName());

    public CiEngineRetentionStrategy(int idleMinutes) {
        super(idleMinutes);
    }

    @Override
    public long check(AbstractCloudComputer c) {

        System.out.println("check idle: " + c.isIdle());
        if (!c.isIdle()) {
            //build is still running, check again in a minute
            return 1;
        }
        Agent node1 = (Agent) c.getNode();
        if (node1 != null) {
            System.out.println("check node: " + node1.isReady());

            HttpResponse<String> response = null;
            Gson gson = new Gson();
            JobResponse jobResponse = null;
            CiEngineHttpClient httpclient = new CiEngineHttpClient();

            response = httpclient.get(node1.getBaseUrl() + "/jobs/" + node1.getId(), new HashMap<>() {
                {
                    put("X-AUTH-TOKEN", node1.getUserToken());
                    put("Content-type", "application/json");
                    put("Accept", "application/json");

                }
            });
            jobResponse = gson.fromJson(response.body(), JobResponse.class);

            switch (jobResponse.getStatus()) {
                case "waiting":
                    return 1;
                case "scheduled":
                    return 1;
                case "running":
                    System.out.println("running, check1: " + node1.isReady());
                    System.out.println("running, check2: " + c.isOnline());
                    System.out.println("running, check3: " + (c.isOnline() && !node1.isReady()));

                    if (c.isOnline() && !node1.isReady()) {
                        LOGGER.log(Level.INFO, "Jenkins Agent is idle, waiting 60 sec");
                        node1.setReady(); //if after 1 minute the build hasnt started, then there is something wrong
                        return 1;
                    }



                    // if this is still the case, the build is finished (I hope)
                    LOGGER.log(Level.INFO, "Jenkins Build Job is finished, checking for CI Engine Build Job");

                    if (node1.isBuild()) {
                        LOGGER.log(Level.INFO, "This is a CI Engine Build Job, running build now...");

                        ImageBuilder.startBuild(new StartImageBuild(node1.getUserToken(), node1.getBaseUrl(), node1.getId()));

                        //waiting a bit for proper starting of image build

                        return 5;
                    } else {
                        node1.setReady();
                        //can be finished now
                    }
                    break;
                case "build-box-running":
                    //the build is still running, check-in in 5 minutes again
                    return 5;
                case "build-box-finished":
                    LOGGER.log(Level.INFO, "CI Engine Build finished, cleanup started");

                    cleanup(c, node1);
                break;
                case "stopping":
                case "canceled":
                case "failed":
                case "corrupt":
                case "finished":
                default:
                    node1.setReady();
                    SlackMessage message = new SlackMessage("Finished Build", "Finished Build on CI-Engine", "Job id: "+ node1.getId(), "Status: " + jobResponse.getStatus());

                    Payload payload = new Payload(node1.getSlackHookUrl(), SlackNotifier.createBlockMessages(message));
                    SlackNotifier.sendLogToSlack(payload);


                    cleanup(c, node1);

                    break;
            }

            //if computer is online and idle, there is a possibility that the build is finished OR the build hasn't started yet






            if (!node1.isReady()) {
                //node is not yet set to ready (to delete)
                //still waiting...
                return 1;
            }

            if (node1.isDebug()) {
                //the build is marked as debug, so no cleanup
                return 30;
            }

        } else {
            System.out.println("Node is null, wait a bit...");
            return 1;
        }


        //here in the execution the node should be set to "ready" (to delete)
        if(c.isIdle() || c.isOffline()) {
            System.out.println("Starting Cleanup");

            //computer is idle and node is set to "ready", so it should be safe to cleanup
            cleanup(c, node1);

        }

        return 1;
    }

    private static void cleanup(AbstractCloudComputer c, Agent node1) {
        try {

            CiEngineHttpClient httpclient = new CiEngineHttpClient();
            String baseUrl = node1.getBaseUrl();
            String userToken = node1.getUserToken();
            httpclient.post(baseUrl + "/jobs/"+ node1.getId()+"/finish", new HashMap<>() {
                {
                    put("X-AUTH-TOKEN", userToken);
                    put("Content-type", "application/json");
                }
            }, "");
            AbstractCloudSlave node = c.getNode();
            if (node != null) {
                Jenkins.get().removeNode(node);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
