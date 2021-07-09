package swiss.flow.ciengine.jenkins.plugins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.*;
import hudson.slaves.*;

import java.io.IOException;

public class Agent extends AbstractCloudSlave {


    private String id;

    private String subscription;

    private String userToken;

    private Boolean debug;

    private String debugPort;

    private Boolean build;

    private String box;
    private String baseUrl;

    private String slackHookUrl;
    private Boolean isReady = false;

    private Boolean isUsed = false;

    public Agent(@NonNull String name, String remoteFS, ComputerLauncher launcher) throws Descriptor.FormException, IOException {
        super(name, remoteFS, launcher);
    }



    @Override
    public CiEngineNode createComputer() {
        return new CiEngineNode(this);
    }

    @Override
    protected void _terminate(TaskListener taskListener) throws IOException, InterruptedException {
        System.out.println("agent should be terminated");
    }

    @Override
    public void terminate() throws InterruptedException, IOException {

        System.out.println("agent should be terminated 2");
        //Queue.getInstance().clear();
        //Optional<Queue.BuildableItem> hallo = Queue.getInstance().getPendingItems().stream().findFirst();
        //hallo.ifPresent(task -> Queue.getInstance().cancel(task));
        //Queue.BuildableItem hallo = Queue.getInstance().getPendingItems().get(0);
        try {
            Queue.Item item = Queue.getInstance().getItems()[0];
            Queue.getInstance().cancel(item);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Queue is empty!");
        }
        super.terminate();
    }

    @Override
    public void setRetentionStrategy(RetentionStrategy availabilityStrategy) {
        super.setRetentionStrategy(availabilityStrategy);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setSubscription(String subscription) {
        this.subscription = subscription;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public String getSubscription() {
        return subscription;
    }

    public String getUserToken() {
        return userToken;
    }

    public Boolean isDebug() {
        return debug;
    }

    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    public Boolean isBuild () {
        return build;
    }

    public void setBuild(Boolean build) {
        this.build = build;
    }

    public String getDebugPort() {
        return debugPort;
    }

    public void setDebugPort(String debugPort) {
        this.debugPort = debugPort;
    }

    public String getBox() {
        return box;
    }

    public void setBox(String box) {
        this.box = box;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSlackHookUrl() {
        return slackHookUrl;
    }

    public void setSlackHookUrl(String slackHookUrl) {
        this.slackHookUrl = slackHookUrl;
    }

    public void setReady() {
        this.isReady = true;
    }
    
    public Boolean isReady() {
        return this.isReady;
    }

    public void setUsed() { this.isUsed = true;}

    public Boolean isUsed() { return this.isUsed; }
}
