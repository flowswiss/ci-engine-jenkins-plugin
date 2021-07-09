package swiss.flow.ciengine.jenkins.plugins;

import hudson.slaves.AbstractCloudComputer;

public class CiEngineNode extends AbstractCloudComputer<Agent> {


    public CiEngineNode(Agent agent){
        super(agent);
    }



}
