
/*******************************************************************************

"FreePastry" Peer-to-Peer Application Development Substrate

Copyright 2002-2007, Rice University. Copyright 2006-2007, Max Planck Institute 
for Software Systems.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither the name of Rice  University (RICE), Max Planck Institute for Software 
Systems (MPI-SWS) nor the names of its contributors may be used to endorse or 
promote products derived from this software without specific prior written 
permission.

This software is provided by RICE, MPI-SWS and the contributors on an "as is" 
basis, without any representations or warranties of any kind, express or implied 
including, but not limited to, representations or warranties of 
non-infringement, merchantability or fitness for a particular purpose. In no 
event shall RICE, MPI-SWS or contributors be liable for any direct, indirect, 
incidental, special, exemplary, or consequential damages (including, but not 
limited to, procurement of substitute goods or services; loss of use, data, or 
profits; or business interruption) however caused and on any theory of 
liability, whether in contract, strict liability, or tort (including negligence
or otherwise) arising in any way out of the use of this software, even if 
advised of the possibility of such damage.

*******************************************************************************/ 

// package rice.failurerecovery;

import rice.*;

import rice.environment.Environment;
import rice.environment.logging.*;
import rice.environment.logging.simple.SimpleLogManager;
import rice.environment.params.Parameters;
import rice.environment.params.simple.SimpleParameters;
import rice.environment.processing.Processor;
import rice.environment.processing.sim.SimProcessor;
import rice.environment.random.RandomSource;
import rice.environment.random.simple.SimpleRandomSource;
import rice.environment.time.TimeSource;
import rice.environment.time.simple.SimpleTimeSource;
import rice.environment.time.simulated.DirectTimeSource;
import rice.p2p.commonapi.*;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.scribe.Scribe;
import rice.p2p.scribe.ScribeClient;
import rice.pastry.*;
import rice.pastry.commonapi.*;
import rice.pastry.direct.*;
import rice.pastry.dist.*;
import rice.pastry.leafset.BroadcastLeafSet;
import rice.pastry.leafset.LeafSet;
import rice.pastry.leafset.RequestLeafSet;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.socket.nat.rendezvous.RendezvousSocketPastryNodeFactory;
import rice.pastry.standard.*;
import rice.selector.SelectorManager;

import java.util.*;
import java.net.*;
import java.io.*;

/**
 * Provides regression testing setup for applications written on top of the
 * commonapi.  Currently is written to use Pastry nodes, but this will be abstracted
 * away. 
 *
 * @version $Id: CommonAPITest.java 4654 2009-01-08 16:33:07Z jeffh $
 *
 * @author Alan Mislove
 */
public abstract class CommonAPITest {

  // ----- VARAIBLES -----
  
  // the collection of nodes which have been created
  protected Node[] nodes;

  // ----- PASTRY SPECIFIC VARIABLES -----

  // the factory for creating pastry nodes
  protected PastryNodeFactory factory;

  // the factory for creating random node ids
  protected NodeIdFactory idFactory;

  // the simulator, in case of direct
  protected NetworkSimulator simulator;
  
  // the environment
  protected Environment environment;
  
  protected Parameters params;
  
  // ----- STATIC FIELDS -----

  // the number of nodes to create
  public int NUM_NODES;

  // the number of nodes to kill
  public int NUM_KILL_NODES;

  public Vector apps = new Vector();

  // the factory which creates pastry ids
  public final IdFactory FACTORY; //= new PastryIdFactory();


  // ----- TESTING SPECIFIC FIELDS -----

  // the text to print to the screen
  public static final String SUCCESS = "SUCCESS";
  public static final String FAILURE = "FAILURE";

  // the width to pad the output
  protected static final int PAD_SIZE = 60;

  // the direct protocol
  public static final String PROTOCOL_DIRECT = "direct";
  public static final String PROTOCOL_SOCKET = "socket";
  public static final String PROTOCOL_RENDEZVOUS = "rendezvous";

  // the possible network simulation models
  public static final String SIMULATOR_SPHERE = "sphere";
  public static final String SIMULATOR_EUCLIDEAN = "euclidean";
  public static final String SIMULATOR_GT_ITM = "gt-itm";


  // ----- PASTRY SPECIFIC FIELDS -----

  // the port to begin creating nodes on
  public int PORT;

  // the host to boot the first node off of
  public InetSocketAddress BOOTSTRAP;

  // the port on the bootstrap to contact
  public static int BOOTSTRAP_PORT = 5009;

  // the procotol to use when creating nodes
  public String PROTOCOL;// = PROTOCOL_DIRECT; //DistPastryNodeFactory.PROTOCOL_DEFAULT;

  // the simulator to use in the case of direct
  public String SIMULATOR; // = SIMULATOR_SPHERE;

  // the instance name to use
  public static String INSTANCE_NAME = "DistCommonAPITest";
  
  protected Logger logger;

  boolean start;
  Random randomIndex; 
  
  // ----- EXTERNALLY AVAILABLE METHODS -----
  
  /**
   * Constructor, which takes no arguments and sets up the
   * factories in preparation for node creation.
   */
  public CommonAPITest(Environment env) throws IOException {
    this.environment = env;
    this.start = true;
    this.randomIndex = new Random();
    
    setupParams(env);
        
      FACTORY = new PastryIdFactory(env);
      //idFactory = new IPNodeIdFactory(PORT); 
      idFactory = new RandomNodeIdFactory(environment);

    if (PROTOCOL.equalsIgnoreCase(PROTOCOL_DIRECT)) {
      if (SIMULATOR.equalsIgnoreCase(SIMULATOR_SPHERE)) {
        simulator = new SphereNetwork<NodeHandle, Message>(env);
      } else if (SIMULATOR.equalsIgnoreCase(SIMULATOR_GT_ITM)){
        simulator = new GenericNetwork<NodeHandle, Message>(env);        
      } else {
        simulator = new EuclideanNetwork<NodeHandle, Message>(env);
      }
//      simulator.addSimulatorListener(new SimulatorListener() {      
//        public void messageSent(Message m, NodeHandle from, NodeHandle to, int delay) {
//          if (!(m instanceof BroadcastLeafSet) && !(m instanceof RequestLeafSet))
//            System.out.println("messageSent("+m+","+from+","+to+","+delay+")");
//        }      
//        public void messageReceived(Message m, NodeHandle from, NodeHandle to) {
//          if (!(m instanceof BroadcastLeafSet) && !(m instanceof RequestLeafSet))
//            System.out.println("messageReceived("+m+","+from+","+to+")");
//        }      
//      });
      
      factory = new DirectPastryNodeFactory(idFactory, simulator, env);
    } else if (PROTOCOL.equalsIgnoreCase(PROTOCOL_SOCKET)) {
      InetSocketAddress address = params.getInetSocketAddress("commonapi_testing_bootstrap");
      factory = new SocketPastryNodeFactory(idFactory, address.getAddress(), PORT, env);
    } else if (PROTOCOL.equalsIgnoreCase(PROTOCOL_RENDEZVOUS)) {
      InetSocketAddress address = params.getInetSocketAddress("commonapi_testing_bootstrap");
      factory = new RendezvousSocketPastryNodeFactory(idFactory, address.getAddress(), PORT, env, false);
    }

    if (factory == null) {
      factory = DistPastryNodeFactory.getFactory(idFactory,
          DistPastryNodeFactory.PROTOCOL_SOCKET,PORT,env);
    }
      
    nodes = new Node[NUM_NODES+1];
  }

  public static final int LEASE_PERIOD = 10000; // 10 seconds
  public static final int TIME_TO_FIND_FAULTY = 15000; // 15 seconds
  
  /**
   * Nodes will be non-ready for a while after killing.  Need to account for this in the test.  
   * 
   * wait for LEASE+TimeToFindFaulty+periodic protocol
   */
  public void waitToRecoverFromKilling(int additionalTime) {
    waitOnClock(LEASE_PERIOD+TIME_TO_FIND_FAULTY+additionalTime);
  }

  public void waitOnClock(int time) {
    if (environment.getSelectorManager().isSelectorThread()) return;
    
    try {
      environment.getTimeSource().sleep(time);
    } catch (InterruptedException e) {}    
  }
  
  public void setupParams(Environment env) {
    params = env.getParameters();
    
    // reduce the check liveness policy to make test run faster
    params.setInt("pastry_socket_scm_num_ping_tries",2);

    // disable the UPnP setting (in case you are testing this on a NATted LAN)
    params.setString("nat_search_policy","never");
    
    // reduce the LEASE/Lease-Refresh
    params.setInt("pastry_protocol_periodicLeafSet_ping_neighbor_period",8000); // 8 seconds
    params.setInt("pastry_protocol_periodicLeafSet_lease_period",LEASE_PERIOD);  // 10 seconds
    params.setInt("pastry_protocol_periodicLeafSet_request_lease_throttle",2000);// 2 seconds
    
    
    this.logger = env.getLogManager().getLogger(getClass(),null);
    NUM_NODES = params.getInt("commonapi_testing_num_nodes");
    NUM_KILL_NODES = params.getInt("commonapi_testing_num_kill_nodes");
    PORT = params.getInt("commonapi_testing_startPort");
    PROTOCOL = params.getString("commonapi_testing_protocol");
    SIMULATOR = params.getString("direct_simulator_topology"); 
  }
  
  /**
   * Method which creates the nodes
   */
  public void createNodes(int bindport, InetSocketAddress bootaddress, Environment env, int numKill, int numChildren) throws Exception {
    if (simulator != null) simulator.setMaxSpeed(10.0f);
    long last = environment.getTimeSource().currentTimeMillis();
    
    NodeIdFactory nidFactory = new RandomNodeIdFactory(env);
    PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);
    PastryNode node = factory.newNode();
		node.boot(bootaddress);	


    env.getTimeSource().sleep(10000);
			// the node may require sending several messages to fully boot into the ring
    synchronized(node){
      while(!node.isReady() && !node.joinFailed()){
        System.out.println("here1");
        node.wait(1500);
        if (node.joinFailed()){
          throw new IOException("Could not join the FreePastry ring. Reason:"+node.joinFailedReason());
        }
      }
    }
    System.out.println("Finished creating new node: "+node);
    env.getTimeSource().sleep(15000);

    MiniProjectClient app = new MiniProjectClient(node);

    env.getTimeSource().sleep(10000);
    app.subscribe();
    env.getTimeSource().sleep(10000);

    long first_kill_time = 0;

    // for(int i=0; i<1000; i++){
    while (true){
      // System.out.println(app.firstTime + " " + app.secondTime);
      env.getTimeSource().sleep(1);

      if (app.isRoot()){ // root node
        System.out.println("Head node");
        System.out.println(app.getChildren());
        // System.out.println("Sending multicast");
        // app.sendMulticast("Hello how are you?");
        // System.out.println("Done multicast");

        int curActiveChilds = app.activeChilds;
        System.out.println("Active childs: " + curActiveChilds);
       

        if(app.firstTime && curActiveChilds==numChildren){
          System.out.println("All child activated, sending kill message");
          NodeHandle[] children = app.getChildren();
          Set<Integer>childToDestroy = new LinkedHashSet<Integer>();
          while (childToDestroy.size() < numKill) {
            childToDestroy.add(this.randomIndex.nextInt(children.length));
          }
          first_kill_time = System.currentTimeMillis();
          for (int childIndex : childToDestroy ){
            // o is the reference to the currently selected object
            NodeHandle child = children[childIndex];
            app.routeMyMsgDirect(child, "Kill Node");
          }
          app.firstTime = false;
          app.secondTime = true;
          System.out.println("Setting active childs to 0");
          app.activeChilds = 0;
        }
        else if(app.secondTime && curActiveChilds==0){
          System.out.println("Start Second Round");
          NodeHandle[] children = app.getChildren();
          for (NodeHandle child : children ){
            app.routeMyMsgDirect(child, "Start Second Round");
          }
          app.secondTime = false;
        }
        else{
          System.out.println("Active Childs: " + curActiveChilds);
          if(!app.firstTime && !app.secondTime && curActiveChilds==numChildren-numKill){
            System.out.println("Second completed, Recording time");
            long finished_time = System.currentTimeMillis();
            int total_nodes = numChildren+1;
            System.out.println("It takes " + total_nodes + " nodes " + (finished_time-first_kill_time)
                            + " ms to finish failure recovery of " + numKill + " node failures."
                            + " and fanout is "+environment.getParameters().getInt("pastry_rtBaseBitLength"));
            NodeHandle[] children = app.getChildren();
            for (NodeHandle child : children ){
              app.routeMyMsgDirect(child, "End Experiment");
            }
            // node.destroy();
            break;
            // System.exit(0);
          }
        }
      }
      else{
        if(app.firstTime){
          System.out.println("Sending first alive message");
          System.out.println("Child node");
          System.out.println(app.getParent());
          NodeHandle parent_node = app.getParent();
          System.out.println("First Round: Sending Route Message");
          app.routeMyMsgDirect(parent_node, "Alive");
          System.out.println("Done Sending Route Message");
          app.firstTime = false;
        }
        else if(app.secondTime){
          System.out.println("Sending second alive message");
          System.out.println("Child node");
          System.out.println(app.getParent());
          NodeHandle parent_node = app.getParent();
          System.out.println("Second Round: Sending Route Message");
          app.routeMyMsgDirect(parent_node, "Alive");
          System.out.println("Done Sending Route Message");
          app.secondTime = false;
          break;
        }
        
      }
    }

    

    // simulate();
    
    // processNode(node);
    // simulate();
  
    // long now = environment.getTimeSource().currentTimeMillis();
    // System.out.println("Created node " + " with id " + node+ " at "+now+" "+(now-last));
    // last = now;
    //   if (logger.level <= Logger.FINE) logger.log(((PastryNode)node).getLeafSet().toString());


    // if (logger.level <= Logger.INFO) logger.log(((PastryNode)nodes[0]).getLeafSet().toString());
    // System.out.println(NUM_NODES+" nodes created and start subscribing.");
    // if (simulator != null) simulator.setFullSpeed();
  }

    // public void getSubscribe() {
    //     for(int i = 0; i< apps.size(); i++) {
    //         ((MiniProjectClient)apps.get(i)).subscribe();
    //         simulate();
    //     }
    //     // simulate(apps.size()/2);
    // }

    public boolean checkTreeConstruction() {
      boolean tree_con = true;
      for (int i = 0; i < apps.size(); i++) {
        if (((MiniProjectClient)apps.get(i)).getParent() == null 
            && !((MiniProjectClient)apps.get(i)).isRoot()) {
          tree_con = false;
          this.logger.log(((MiniProjectClient)apps.get(i)).getScribeImpl().getId() + " has no parent!");
        }
      }
      return tree_con;
    }
  
  /**
   * Method which starts the creation of nodes
   */
  public void start(int bindport, InetSocketAddress bootaddress, Environment env, int numKill, int numChildren) throws Exception {
//    simulator.start();
    createNodes(bindport, bootaddress, env, numKill, numChildren);
    // getSubscribe();
    // String tree = "";
    // while (!checkTreeConstruction()) {
    //   try {
    //     environment.getTimeSource().sleep(300);
    //   } catch (InterruptedException e) {
    //     // TODO Auto-generated catch block
    //     e.printStackTrace();
    //   }
    // }
    // System.out.println("Finished subscription");
    // printTree(apps);
    // this.logger.log(tree);

    // System.out.println("\nTest Beginning\n");
    
    // Uncomment to  run test
    // runTest();
  }


  // ----- INTERNAL METHODS -----

  /**
   * In case we're using the direct simulator, this method
   * simulates the message passing.
   */
  protected void simulate() {
    simulate(1);
  }

  /**
   * 
   * @param numOps the approximate number of operations that will occur during this time, an operation is a network hop for example
   */
  protected void simulate(int numOps) {
    if (environment.getSelectorManager().isSelectorThread()) return;
    synchronized(this) {try { wait(250*numOps); } catch (InterruptedException e) {}}
    
//    if (PROTOCOL.equalsIgnoreCase(PROTOCOL_DIRECT)) {
//      while (simulator.simulate()) {}
//    } else {
//      pause(500);
//    }
  }


  
  
  /**
   * Method which creates a single node, given it's node
   * number
   *
   * @param num The number of creation order
   * @return The created node
   */
  protected Node createNode(int num) {
    PastryNode ret;
    if (num == 0) {
      ret = factory.newNode((rice.pastry.NodeHandle) null);
    } else {
      ret = factory.newNode(getBootstrap());
    }
//    ret.addNetworkListener(new NetworkListener() {
//    
//      public void dataSent(int msgAddress, short msgType,
//          InetSocketAddress socketAddress, int size, int wireType) {
//        System.out.println("dataSent("+msgAddress+","+msgType+","+socketAddress+","+size+","+wireType+")");
//      }
//    
//      public void dataReceived(int msgAddress, short msgType,
//          InetSocketAddress socketAddress, int size, int wireType) {
//        System.out.println("dataReceived("+msgAddress+","+msgType+","+socketAddress+","+size+","+wireType+")");
//      }
//    
//      public void channelOpened(InetSocketAddress addr, int reason) {
//        System.out.println("channelOpened("+addr+","+reason+")");
//      }
//    
//      public void channelClosed(InetSocketAddress addr) {
//        System.out.println("channelClosed("+addr+")");
//      }    
//    });
    synchronized(ret) {
      while(!ret.isReady()) {
        try {
          ret.wait(1000);
        } catch (InterruptedException ie) {
          ie.printStackTrace();
          return null;
        }
        if (!ret.isReady()) {
          if (logger.level <= Logger.INFO) logger.log("Node "+ret+" is not yet ready.");
        }
      }
    }
    
    return ret;
  }

  /**
   * Gets a handle to a bootstrap node.
   *
   * @return handle to bootstrap node, or null.
   */
  protected rice.pastry.NodeHandle getBootstrap() {
    if (PROTOCOL.equalsIgnoreCase(PROTOCOL_DIRECT)) {
      return ((PastryNode)nodes[0]).getLocalHandle();
    } else {
      try {
        InetSocketAddress address = params.getInetSocketAddress("commonapi_testing_bootstrap");
        return ((SocketPastryNodeFactory) factory).getNodeHandle(address);
      } catch (UnknownHostException uhe) {
        throw new RuntimeException(uhe); 
      }
    }
  }

  /**
   * Method which pauses for the provided number of milliseconds
   *
   * @param ms The number of milliseconds to pause
   */
  protected synchronized void pause(int ms) {
    try { 
      environment.getTimeSource().sleep(ms); 
    } catch (InterruptedException e) {}
    //    if (!PROTOCOL.equalsIgnoreCase(PROTOCOL_DIRECT))
//      try { wait(ms); } catch (InterruptedException e) {}
  }

  /**
   * Method which kills the specified node
   *
   * @param n The node to kill
   */
  protected void kill(int n) {
    //if (PROTOCOL.equalsIgnoreCase(PROTOCOL_DIRECT))
      ((PastryNode)nodes[n]).destroy();
    if (!PROTOCOL.equalsIgnoreCase(PROTOCOL_DIRECT)) {
      // give node time to show up dead
      pause(2000);
    }
      
//      simulator.setAlive((rice.pastry.NodeId) nodes[n].getId(), false);
    
  }


  // ----- METHODS TO BE PROVIDED BY IMPLEMENTATIONS -----

  /**
   * Method which should process the given newly-created node
   *
   * @param num The number o the node
   * @param node The newly created node
   */
  // protected abstract void processNode(int num, Node node);
  protected abstract void processNode(Node node);

  /**
   * Method which should run the test - this is called once all of the
   * nodes have been created and are ready.
   */
  protected abstract void runTest();
  

  // ----- TESTING UTILITY METHODS -----

  /**
   * Method which prints the beginning of a test section.
   *
   * @param name The name of section
   */
  protected final void sectionStart(String name) {
    System.out.println(name);
  }

  /**
   * Method which prints the end of a test section.
   */
  protected final void sectionDone() {
    System.out.println();
  }

  /**
   * Method which prints the beginning of a test section step.
   *
   * @param name The name of step
   */
  protected final void stepStart(String name) {
    System.out.print(/*environment.getTimeSource().currentTimeMillis()+*/pad("  " + name));
  }

  /**
   * Method which prints the end of a test section step, with an
   * assumed success.
   */
  protected final void stepDone() {
    stepDone(SUCCESS);
  }

  /**
   * Method which prints the end of a test section step.
   *
   * @param status The status of step
   */
  protected final void stepDone(String status) {
    stepDone(status, "");
  }

  /**
   * Method which prints the end of a test section step, as
   * well as a message.
   *
   * @param status The status of section
   * @param message The message
   */
  protected final void stepDone(String status, String message) {
    System.out.println("[" + status + "]");

    if ((message != null) && (! message.equals(""))) {
      System.out.println("     " + message);
    }

    if(status.equals(FAILURE)) {
      if (environment.getParameters().getBoolean("commonapi_testing_exit_on_failure")) {
        System.exit(1);
      } else {
        try { Thread.sleep(100000000); }catch(InterruptedException ie) {System.exit(1);}
      }
    }
  }

  /**
   * Method which prints an exception which occured during testing.
   *
   * @param e The exception which was thrown
   */
  protected final void stepException(Exception e) {
    logger.logException("\nException occurred during testing.",e);

    System.exit(2);
  }

  /**
   * Method which pads a given string with "." characters.
   *
   * @param start The string
   * @return The result.
   */
  private final String pad(String start) {
    if (start.length() >= PAD_SIZE) {
      return start.substring(0, PAD_SIZE);
    } else {
      int spaceLength = PAD_SIZE - start.length();
      char[] spaces = new char[spaceLength];
      Arrays.fill(spaces, '.');

      return start.concat(new String(spaces));
    }
  }

  /**
   * Throws an exception if the test condition is not met.
   */
  protected final void assertTrue(String intention, boolean test) {
    if (!test) {
      stepDone(FAILURE, "Assertion '" + intention + "' failed.");
    }
  }

  /**
   * Thows an exception if expected is not equal to actual.
   */
  protected final void assertEquals(String description,
                                    Object expected,
                                    Object actual) {
    if (!expected.equals(actual)) {
      stepDone(FAILURE, "Assertion '" + description +
               "' failed, expected: '" + expected +
               "' got: " + actual + "'");
    }
  }
  

  // ----- COMMAND LINE PARSING METHODS -----
  
  /**
   * process command line args
   */
  
   /*
  public static Environment parseArgs(String args[]) throws IOException {
    // process command line arguments

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-help")) {
        System.out.println("Usage: DistCommonAPITest [-params paramsfile] [-port p] [-protocol (direct|socket)] [-bootstrap host[:port]] [-help]");
        System.exit(1);
      }
    }

    Parameters params = null;
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-params") && i+1 < args.length) {
        params = new SimpleParameters(Environment.defaultParamFileArray,args[i+1]);
        break;
      }
    }
    if (params == null) {
      params = new SimpleParameters(Environment.defaultParamFileArray,null); 
    }
    
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-protocol") && i+1 < args.length) {
        params.setString("commonapi_testing_protocol",args[i+1]);
        break;
      }
    }

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-simulator") && i+1 < args.length) {
        params.setString("direct_simulator_topology",args[i+1]);
        break;
      }
    }

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-killnodes") && i+1 < args.length) {
        params.setString("commonapi_testing_num_kill_nodes",args[i+1]);
        break;
      }
    }
    
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-nodes") && i+1 < args.length) {
        int p = Integer.parseInt(args[i+1]);
        if (p > 0) params.setInt("commonapi_testing_num_nodes",p);
        break;
      }
    }

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-port") && i+1 < args.length) {
        int p = Integer.parseInt(args[i+1]);
        if (p > 0) params.setInt("commonapi_testing_startPort",p);
        break;
      }
    }
    
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-bootstrap") && i+1 < args.length) {
        String str = args[i+1];
        int index = str.indexOf(':');
        if (index == -1) {
          // no port specified
          params.setInetSocketAddress("commonapi_testing_bootstrap", 
              new InetSocketAddress(InetAddress.getByName(str),
                  params.getInt("commonapi_testing_startPort")));
          
        } else {
          params.setString("commonapi_testing_bootstrap",str);
        }
        break;
      }
    }
    
    // ----- ATTEMPT TO LOAD LOCAL HOSTNAME -----
    if (!params.contains("commonapi_testing_bootstrap")) {
      try {
        InetAddress localHost = InetAddress.getLocalHost();      
        params.setInetSocketAddress("commonapi_testing_bootstrap", 
            new InetSocketAddress(localHost,
                params.getInt("commonapi_testing_startPort")));
      } catch (UnknownHostException e) {
        System.err.println("Error determining local host: " + e);
      }
    }
    
    TimeSource timeSource;
    SelectorManager selector = null;
    Processor proc = null;
    LogManager logManager = null;
    RandomSource rand = null; 
    if (params.getString("commonapi_testing_protocol").equals("direct")) {
      timeSource = new DirectTimeSource(params);
      logManager = Environment.generateDefaultLogManager(timeSource, params);
      rand = Environment.generateDefaultRandomSource(params, logManager);
      ((DirectTimeSource)timeSource).setLogManager(logManager);
      selector = Environment.generateDefaultSelectorManager(timeSource, logManager, rand);
      ((DirectTimeSource)timeSource).setSelectorManager(selector);
      
      proc = new SimProcessor(selector);
    } else {
      timeSource = new SimpleTimeSource(); 
    }

    return new Environment(selector,proc,null,timeSource,logManager,params,null);
  }

  */

  public static void printTree(Vector apps) {
        // build a hashtable of the apps, keyed by nodehandle
        Hashtable<NodeHandle, MiniProjectClient> appTable = new Hashtable<NodeHandle, MiniProjectClient>();
        Iterator<MiniProjectClient> i = apps.iterator();
        while (i.hasNext()) {
          MiniProjectClient app = (MiniProjectClient) i.next();
        //   appTable.put(app.endpoint.getLocalNodeHandle(), app);
          appTable.put(app.getScribeImpl().getEndpoint().getLocalNodeHandle(), app);
        }
        NodeHandle seed = ((MiniProjectClient) apps.get(0)).getScribeImpl().getEndpoint().getLocalNodeHandle();
    
        // get the root
        NodeHandle root = getRoot(seed, appTable);
    
        // print the tree from the root down
        recursivelyPrintChildren(root, 0, appTable);
    }
    
      /**
       * Recursively crawl up the tree to find the root.
       */
    public static NodeHandle getRoot(NodeHandle seed, Hashtable<NodeHandle, MiniProjectClient> appTable) {
        MiniProjectClient app = (MiniProjectClient) appTable.get(seed);
        if (app.isRoot())
            return seed;
        NodeHandle nextSeed = app.getParent();
        return getRoot(nextSeed, appTable);
    }
    
    /**
     * Print's self, then children.
     */
    public static void recursivelyPrintChildren(NodeHandle curNode,
        int recursionDepth, Hashtable<NodeHandle, MiniProjectClient> appTable) {
        // print self at appropriate tab level
        String s = "";
        for (int numTabs = 0; numTabs < recursionDepth; numTabs++) {
            s += "-|";
        }
        s += curNode.getId().toString();
        System.out.println(s);

        // recursively print all children
        MiniProjectClient app = (MiniProjectClient) appTable.get(curNode);
        NodeHandle[] children = app.getChildren();
        for (int curChild = 0; curChild < children.length; curChild++) {
            recursivelyPrintChildren(children[curChild], recursionDepth + 1, appTable);
        }
    }

   
}
