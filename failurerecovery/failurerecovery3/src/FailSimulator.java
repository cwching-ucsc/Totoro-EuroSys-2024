// package rice.failurerecovery2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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
    protected void processNode(Node node) {
        // TODO Auto-generated method stub
        
    }
    // protected void processNode(int num, Node node) {
    //     // TODO Auto-generated method stub
        
    // }

    @Override
    protected void runTest() {
    }

    

    public static void main(String args[]) throws IOException, Exception {
		//args==NULL, take the args from default freepastry in "\dedrs\bin"
		// Environment env = parseArgs(args);
        Environment env = new Environment("user");

        System.out.println("Using Fanout (pastry_rtBaseBitLength):" + env.getParameters().getInt("pastry_rtBaseBitLength"));
        System.out.println("Using pastry_socket_writer_max_queue_length:" + env.getParameters().getInt("pastry_socket_writer_max_queue_length"));

        // System.exit(0);
        try{

            FailSimulator FailSimulator = new FailSimulator(env);
            /*
            env.getSelectorManager().getTimer().schedule(new TimerTask() {
                public void run() {
                    simulator.stop();
                }
            }, 50000);*/
            // start is the function defined in commonapi class and it calls runtest


            int bindport = Integer.parseInt(args[0]);

            // build the bootaddress from the command line args
            InetAddress bootaddr = InetAddress.getByName(args[1]);
            int bootport = Integer.parseInt(args[2]);
            InetSocketAddress bootaddress = new InetSocketAddress(bootaddr, bootport);

            int numKill = Integer.parseInt(args[3]);
            int numChildren = Integer.parseInt(args[4]);

            FailSimulator.start(bindport, bootaddress, env, numKill, numChildren);
            env.destroy();
            System.exit(0);
        } catch (Exception e) {
            // remind user how to use
            System.out.println("Usage:");
            System.out
                .println("java [-cp FreePastry-<version>.jar] rice.tutorial.scribe.ScribeTutorial localbindport bootIP bootPort numNodes numTrees");
            System.out
                .println("example java rice.tutorial.scribe.ScribeTutorial 9001 pokey.cs.almamater.edu 9001 1 10");
            throw e;
        }

	}
}
