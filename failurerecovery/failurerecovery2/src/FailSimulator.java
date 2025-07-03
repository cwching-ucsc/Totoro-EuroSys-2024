// package rice.failurerecovery2;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;


import rice.environment.Environment;
import rice.environment.logging.Logger;
import rice.p2p.commonapi.Node;
// import rice.p2p.commonapi.testing.CommonAPITest;
import rice.p2p.commonapi.NodeHandle;
// import rice.environment.logging.Logger;
import rice.pastry.PastryNode;

public class FailSimulator extends CommonAPITest {

    public static String INSTANCE = "SimAggTest";
    // public Logger logger;

    public FailSimulator(Environment env) throws IOException{
        super(env);
    }

    public void setupParams(Environment env) {
        super.setupParams(env);
        // we want to see if messages are dropped because not ready
    //    if (!env.getParameters().contains("rice.p2p.scribe.ScribeImpl@ScribeRegrTest_loglevel"))
    //      env.getParameters().setInt("rice.p2p.scribe.ScribeImpl@ScribeRegrTest_loglevel",Logger.INFO);
        
        // want to retry fast because of problems with isReady()
        env.getParameters().setInt("p2p_scribe_message_timeout",3000); 
      }

    @Override
    protected void processNode(int num, Node node) {
        // TODO Auto-generated method stub
        
    }

    

    @Override
    protected void runTest() {

        Object[] obj = killNode();
        int root_idx = (Integer) obj[0];
        long first_kill_time = (Long) obj[1];
        boolean failure_fix = false;
        int global_round = 0;
        while(!failure_fix){
            MiniProjectClient _temp = (MiniProjectClient) apps.get(root_idx);
            _temp.startPublishTask();
            simulate(2);
            if(_temp.ids.size() == NUM_NODES - NUM_KILL_NODES) {
                failure_fix = true;
            }
            // else {
            //     logger.log("Root received ")
            // }
            global_round++;
            if (global_round == 100) failure_fix = true;
        }
        long finished_time = System.currentTimeMillis();
        System.out.println("It takes " + NUM_NODES + " nodes " + (finished_time-first_kill_time)
                            + " ms to finish failure recovery of " + NUM_KILL_NODES + " node failures."
                            + " and fanout is "+environment.getParameters().getInt("pastry_rtBaseBitLength"));
    }

    protected Object[] killNode() {
        int root_Index = 0;
        int killed_num_nodes = 0;
        boolean root_find = false;
        long first_kill_time = 0;
        Vector killed_node_seq = new Vector();
        for (;killed_num_nodes < NUM_KILL_NODES;){
            int rand = environment.getRandomSource().nextInt(NUM_NODES);
            if (apps.get(rand) != null) {
                MiniProjectClient _temp = (MiniProjectClient) apps.get(rand);
                if (_temp.isRoot()) {
                    root_Index = rand;
                    root_find = true;
                    logger.log("Root is "+_temp.getScribeImpl().getId()+" and port is "+(8000+rand));
                }
                else {
                    if (killed_node_seq.contains(rand)) continue;
                    if (killed_num_nodes == 0) {
                        first_kill_time = System.currentTimeMillis();
                    }
                    logger.log("Kill node: "+_temp.getScribeImpl().getId()+" with port "+(8000+rand)+" at "+System.currentTimeMillis());
                    ((MiniProjectClient)apps.get(rand)).getScribeImpl().destroy();
                    nodes[rand].getEnvironment().getLogManager().getLogger(PastryNode.class, null).log("Killed at "+System.currentTimeMillis());
                    ((PastryNode)nodes[rand]).destroy();
                    // apps.remove(_temp);
                    killed_num_nodes++;
                    killed_node_seq.add(rand);
                }
            }
        }
        if (!root_find) {
            for (int i = 0; i < apps.size(); i ++) {
                MiniProjectClient _temp = (MiniProjectClient) apps.get(i);
                if(_temp.isRoot()) {
                    root_Index = i;
                    logger.log("Root is "+_temp.getScribeImpl().getId()+" and port is "+(8000+i));
                    break;
                }
                // clients[i].endpoint.scheduleMessage(new MaintenanceMessage(), 3000, 3000);
                // endpoint.scheduleMessage(new MaintenanceMessage(), environment.getRandomSource().nextInt(60000), 5000);
            }
        }
        return new Object[] {root_Index, first_kill_time};
    }

    public static void main(String args[]) throws IOException {
		//args==NULL, take the args from default freepastry in "\dedrs\bin"
		Environment env = parseArgs(args);

        System.out.println("Using Fanout (pastry_rtBaseBitLength):" + env.getParameters().getInt("pastry_rtBaseBitLength"));
        System.out.println("Using pastry_socket_writer_max_queue_length:" + env.getParameters().getInt("pastry_socket_writer_max_queue_length"));

        // System.exit(0);

		FailSimulator FailSimulator = new FailSimulator(env);
		/*
	    env.getSelectorManager().getTimer().schedule(new TimerTask() {
	    	public void run() {
	    		simulator.stop();
	    	}
	    }, 50000);*/
		// start is the function defined in commonapi class and it calls runtest
		FailSimulator.start();
		env.destroy();
        System.exit(0);
	}
}
