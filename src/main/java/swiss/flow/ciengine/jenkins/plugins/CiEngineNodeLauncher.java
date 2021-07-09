package swiss.flow.ciengine.jenkins.plugins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import hudson.model.*;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DelegatingComputerLauncher;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
import jenkins.slaves.JnlpAgentReceiver;
import net.sf.json.JSONObject;
import swiss.flow.ciengine.jenkins.plugins.ImageBuild.ImageBuilder;
import swiss.flow.ciengine.jenkins.plugins.ImageBuild.StartImageBuild;
import swiss.flow.ciengine.jenkins.plugins.Messages.SlackMessage;
import swiss.flow.ciengine.jenkins.plugins.Slack.Payload;
import swiss.flow.ciengine.jenkins.plugins.Slack.SlackNotifier;
import swiss.flow.ciengine.jenkins.plugins.job.JobCreateResponse;
import swiss.flow.ciengine.jenkins.plugins.job.JobResponse;


import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CiEngineNodeLauncher extends DelegatingComputerLauncher {

    private static final Logger LOGGER = Logger.getLogger(CiEngineCloud.class.getName());

    protected CiEngineNodeLauncher(ComputerLauncher launcher) {
        super(launcher);
    }

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {

        Agent node = (Agent) computer.getNode();
        String nodeName = null;
        String subscription = null;
        String userToken = null;
        Boolean debug = null;
        String debugPort = null;
        String box = null;
        String baseUrl = null;
        String slackHookUrl = null;
        String jobId = null;
        if (node != null) {
            nodeName = node.getNodeName();
            subscription = node.getSubscription();
            userToken = node.getUserToken();
            debug = node.isDebug();
            debugPort = node.getDebugPort();
            jobId = node.getId();
            box = node.getBox();
            baseUrl = node.getBaseUrl();
            slackHookUrl = node.getSlackHookUrl();
            if (node.isUsed()) {
                LOGGER.log(Level.INFO, "Job Id " + jobId + " is already used, exiting the launcher");
                return;
            }

        } else {
            nodeName = "fail";
        }

        if (jobId != null) {
            LOGGER.log(Level.INFO, "Job Id " + jobId + " is already started, exiting the launcher");
            return;
        }


        String slaveSecret = JnlpAgentReceiver.SLAVE_SECRET.mac(nodeName);
        //String slaveSecret = "e50bf80b2974a98c380d398fc6e0046ef5a9af3f20086f25b407d7b4084ddd15"; // JnlpAgentReceiver.SLAVE_SECRET.mac(nodeName);
        String jenkinsUrl = Jenkins.get().getRootUrl();


        if (debugPort == null || debugPort.isBlank()) {
            debugPort = "0";
        }


        JSONObject json = new JSONObject();

        json.put("subscription", "subscriptions/" + subscription);
        json.put("jenkinsSecret", slaveSecret);
        json.put("jenkinsAgentName", nodeName);
        json.put("jenkinsBaseUrl", jenkinsUrl);
        json.put("box", "boxes/" + box);
        json.put("debug", debug);
        json.put("debugPort", Integer.valueOf(debugPort)); //has to be int

        CiEngineHttpClient httpclient = new CiEngineHttpClient();

        try {
            LOGGER.log(Level.INFO, "Job-Request to the Flow CI Engine API");
            LOGGER.log(Level.INFO, "Payload: " + json);
            String finalUserToken = userToken;
            HttpResponse<String> response = httpclient.post(baseUrl + "/jenkins_jobs", new HashMap<>() {
                {
                    put("X-AUTH-TOKEN", finalUserToken);
                    put("Content-type", "application/json");
                }
            }, json.toString());

            if (response.statusCode() == 201) {
                Gson gson = new Gson();
                JobCreateResponse jobCreateResponse = gson.fromJson(response.body(), JobCreateResponse.class);
                if (node != null) {
                    node.setId(jobCreateResponse.getId());
                    node.setUsed();
                }
                LOGGER.log(Level.INFO, "Response successful, job id: " + jobCreateResponse.getId() + " is in status: " + jobCreateResponse.getStatus());
            } else {
                //something went wrong
                LOGGER.log(Level.SEVERE, "Request to Flow CI Engine API failed, please look at response and contact support: " + response.headers().toString());
                LOGGER.log(Level.SEVERE, response.body());

                throw new RuntimeException("Flow CI Engine API request Failed");
            }


        } catch (RuntimeException e) {
            e.printStackTrace();
            if (node != null) {

                Jenkins.get().removeNode(node);
            }
            return;
        }

        if (node != null) {
            //hier GET job/:id, und dann nach status filtern und entscheiden
            LOGGER.log(Level.INFO, "Waiting for agent to boot...(30 seconds)");
            Thread.sleep(30000);



            String logs = "";
            CiEngineLogAction logAction = null;

            boolean jobIsRunning = true;


            String finalUserToken1 = userToken;

            Gson gson = new Gson();
            HttpResponse<String> response = null;
            JobResponse jobResponse = null;
            int isOnlineCount = 0;
            int isIdleCount = 0;
            while (jobIsRunning) {

                response = httpclient.get(baseUrl + "/jobs/" + node.getId(), new HashMap<>() {
                    {
                        put("X-AUTH-TOKEN", finalUserToken1);
                        put("Content-type", "application/json");
                        put("Accept", "application/json");

                    }
                });
                jobResponse = gson.fromJson(response.body(), JobResponse.class);

                LOGGER.log(Level.INFO, "Response in Loop GET job successful, job id: " + jobResponse.getId() + " is in status: " + jobResponse.getStatus());

                //logs
                if (logAction != null) {
                    computer.removeAction(logAction);
                }
                logs = this.gatherCiEngineLogs(jobResponse.getId(), finalUserToken1, baseUrl);

                logAction = new CiEngineLogAction(logs);

                computer.addAction(logAction);

                //endlogs


                switch (jobResponse.getStatus()) {
                    case "waiting":
                        //joblimit reached, waiting for completion of another job
                        LOGGER.log(Level.INFO, "Waiting for queuing");
                        Thread.sleep(1000 * 60 * 2); // 2 minute wait

                        break;
                    case "scheduled":
                        //job is scheduled in the queue, waiting for a node to take it and execute
                        LOGGER.log(Level.INFO, "Job queued, scheduled for execution");
                        Thread.sleep(1000 * 30); // 30 seconds wait

                        break;
                    case "running":
                        //vm is running, and job is presumably running

                            //1 check if computer is online. if not, wait a bit. this should take about 15-20 seconds
                        if (computer.isOffline()) {
                            if (isOnlineCount > 10) {
                                //5 minutes are past, that agent is not coming online...
                                LOGGER.log(Level.INFO, "Jenkins Agent is not coming online, cleaning up");
                                jobIsRunning = false;
                                node.setReady();
                                break;
                            }
                            LOGGER.log(Level.INFO, "Jenkins Agent is not yet online, waiting 30 sec");
                            Thread.sleep(1000 * 30); // 30 seconds wait
                            isOnlineCount++;
                            break;
                        }
                            //2 check if computer is idle. This computer should take the build and start, not be in idle too long
                        if (!computer.isIdle()) {
                            //the agent is working, revisit this later
                            LOGGER.log(Level.INFO, "Jenkins Agent is working on the build, waiting 30 sec");
                            Thread.sleep(1000 * 30); // 30 seconds wait
                            break;
                        }
                            //3 if both true, wait a bit...
                        if (computer.isOnline() && computer.isIdle()) {
                            if (isIdleCount > 4) {
                                //2 minutes are past, it's safe to assume the build is finished
                                LOGGER.log(Level.INFO, "Jenkins Agent is idle for a longer time, it's safe to assume the build is finished");
                                //now check if it's a build job...
                            }
                            LOGGER.log(Level.INFO, "Jenkins Agent is idle, waiting 30 sec");
                            Thread.sleep(1000 * 30); // 30 seconds wait
                            isIdleCount++;
                            break;
                        }
                            // if this is still the case, the build is finished (I hope)
                        LOGGER.log(Level.INFO, "Jenkins Build Job is finished, checking for CI Engine Build Job");

                        if (node.isBuild()) {
                            LOGGER.log(Level.INFO, "This is a CI Engine Build Job, running build now...");

                            ImageBuilder.startBuild(new StartImageBuild(finalUserToken1, baseUrl, node.getId()));

                            //waiting a bit for proper starting of image build
                            Thread.sleep(1000 * 30); // 30 seconds wait
                            //now the program should not come here again...
                        } else {
                            node.setReady();
                        }

                        break;
                    case "build-box-running":
                            LOGGER.log(Level.INFO, "CI Engine Build running...");
                            Thread.sleep(1000 * 60 * 2); // 2 minute waiting
                        break;
                    case "build-box-finished":
                        //is finished? then make the /finish call...
                        LOGGER.log(Level.INFO, "CI Engine Build finished, cleanup started");

                        httpclient.post(baseUrl + "/jobs/"+node.getId()+"/finish", new HashMap<>() {
                            {
                                put("X-AUTH-TOKEN", finalUserToken1);
                                put("Content-type", "application/json");
                            }
                        }, "");
                        break;
                    case "stopping":
                    case "canceled":
                    case "failed":
                    case "corrupt":
                    case "finished":
                        //setting this for the retention  strategy.
                        // there is a real possibility that jenkins tries to execute the retention strategy while the build (and this while loop) is still running
                        node.setReady();
                        SlackMessage message = new SlackMessage("Finished Build", "Finished Build on CI-Engine", "Job id: "+ node.getId(), "Status: " + jobResponse.getStatus());

                        Payload payload = new Payload(slackHookUrl, SlackNotifier.createBlockMessages(message));
                        SlackNotifier.sendLogToSlack(payload);
                        jobIsRunning = false;
                        break;
                    default:
                        LOGGER.log(Level.SEVERE, "Unknown job status, trying to clean up");
                        jobIsRunning = false;
                        node.setReady();
                }
            }

            //while loop is finished
            /*
            There are a few scenarios:
            1. the build is finished, all is well. we return from here and let the retention strategy deal with cleanup
            2. the build failed / is corrupt, the backend has tagged the status correctly and the retention strategy can cleanup
            3. the build is in running, but the agent had a problem. ->cleanup
            4. the build has a unknown job status, why? investigate and adjust the code
             */
            return;

        }

    }

    private String gatherCiEngineLogs(String jobId, String usertoken, String baseUrl) {

        CiEngineHttpClient httpclient = new CiEngineHttpClient();

        HttpResponse<String> response = httpclient.get(baseUrl + "/jobs/"+jobId+"/job_logs?itemsPerPage=100", new HashMap<>() {
            {
                put("X-AUTH-TOKEN", usertoken);
                put("Content-type", "application/json");
                put("Accept", "application/json");
            }
        });
        LOGGER.log(Level.INFO, "Got ci logs, printing body:");
        LOGGER.log(Level.INFO, response.body());



        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(response.body());

        return gson.toJson(je);


        //return response.body();
//        Gson gson = new Gson();
//
//        JobLogsResponse[] jobResponse = gson.fromJson(response.body(), JobLogsResponse[].class);
//        LOGGER.log(Level.INFO, "JobLogsResponse: " + jobResponse[0].getJob());
//        LOGGER.log(Level.INFO, "JobLogsResponse: " + jobResponse[0].getMessage());

    }
}
