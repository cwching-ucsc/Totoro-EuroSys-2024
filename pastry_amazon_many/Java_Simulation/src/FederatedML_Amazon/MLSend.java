// package rice.tutorial.FederatedML_Amazon;

package FederatedML_Amazon;

import java.io.IOException;
import java.net.*;
import java.util.*;

import rice.environment.Environment;
import rice.p2p.commonapi.NodeHandle;
import rice.pastry.*;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;
import rice.pastry.transport.TransportPastryNodeFactory;
import java.io.*;
import java.util.zip.*;


public class MLSend {
	// apps stand for the vector space that contains all the scribe client applications
	// Vector<MyScribeClient> apps = new Vector<MyScribeClient>();
	// we have a parent client
	// Vector<MyScribeClient> apps = new Vector<MyScribeClient>();

	Vector<MyScribeClient> apps = new Vector<MyScribeClient>();
	Vector<PastryNode> nodes = new Vector<PastryNode>();
	public MLSend(int bindport, InetSocketAddress bootaddress,
		int numNodes, Environment env, int num_trees) throws Exception {
		// generate a random node id
		NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

		// construct the PastryNodeFactory; how we use socket
		PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);
		

		// loop to construct # nodes
		// All the nodes are joining the ring
		// construct a new node
		PastryNode node = factory.newNode();
		node.boot(bootaddress);		
		env.getTimeSource().sleep(10000);
		// the node may require sending several messages to fully boot into the ring
		synchronized(node){
			while(!node.isReady() && !node.joinFailed()){
				node.wait(1500);
				if (node.joinFailed()){
					throw new IOException("Could not join the FreePastry ring. Reason:"+node.joinFailedReason());
				}
			}
		}
		System.out.println("Finished creating new node: "+node);
		// env.getTimeSource().sleep(15000);
		env.getTimeSource().sleep(300);
		System.out.println("Here1");
		//construct a new scribe application
		// All the nodes are joining applications
		MyScribeClient app = new MyScribeClient(node, num_trees);
		env.getTimeSource().sleep(100);
		app.subscribe();
		// env.getTimeSource().sleep(10000);
		env.getTimeSource().sleep(300);
		System.out.println("Here2");
		long startTime = System.nanoTime();
		for (int iteration = 0; iteration < 5; iteration++){
			System.out.println("Iteration: "+Integer.toString(iteration)+" begins...");
			// System.out.println("Here3");
			// if root node
			if (app.isRoot()){ // root node
				// System.out.println("Here4");
				System.out.println("Head node");
				if (iteration == 0){
					app.buildModel(); // build initial model
				}
				else{
					app.combineModels(iteration);
				}
				app.convertByte("head", iteration); // convert the zip file to byte
				app.sendMulticast(); 
			}

			int count = 0;
			int wait_counter = 0;
			boolean override_if = false;
			// you only launch one node
			while (count != 1){
				// System.out.println("Iteration " + iteration + ", inside while, Wait Counter: " + wait_counter);
				// root node
				if (app.isRoot()){
					wait_counter = 0;
					System.out.println("Here5");
					// check all the traing results are gathered
					if (app.model_list.size() == app.getChildren().length){
						System.out.println("Head Node");
						// head node combines all the results and generates a new h5 file
						app.convertFromByte("head", iteration);
						long endTime = System.nanoTime();
						long duration = (endTime - startTime);
						System.out.println("Training is done at: "+duration);
						
						// head node is visited
						count += 1;
						}
						
					}
				else if (app.getChildren().length == 0){
					System.out.println("Child Node");
					while (app.bytes.length == 0){
						env.getTimeSource().sleep(50);
					}
					long receiveTime = System.nanoTime();
					System.out.println("Multicast received at: "+receiveTime);
					app.convertFromByte("child", iteration); // convert the byte file to h5 file and save it 
					NodeHandle parent_node = app.getParent();
					app.trainModel(iteration); // train the model for the child node			
					app.convertByte("child", iteration); // convert a file into a byte array
					app.routeMyMsg(parent_node); // send a byte array to the parent node

					// check this index is visited
					count += 1;

					}
					
					// node in between (serve as a children and head node)
				else {
					// System.out.println("Here6");
					System.out.println(app.model_list.size() + ", " + app.getChildren().length);
					if ((app.model_list.size() == app.getChildren().length) || (override_if)){
					// if (true){
						// wait_counter = 0;
						// System.out.println("Here7");
						System.out.println("Parent Node");
						app.convertFromByte("head", iteration);
						// System.out.println("Here8");
						app.combineModels(iteration);
						// System.out.println("Here9");
						app.convertByte("head", iteration);
						// System.out.println("Here10");
						NodeHandle parent_node = app.getParent();
						app.routeMyMsg(parent_node); // send a byte array to the parent node
						// System.out.println("Here11");
						// parent node is visited
						count += 1;
					}

				}
				env.getTimeSource().sleep(500);
				wait_counter += 1;
				
				// if(wait_counter==100){
				// 	override_if = true;
				// }

				if(wait_counter==240){
					NodeHandle parent_node = app.getParent();
					app.routeMyMsg(parent_node); // send a byte array to the parent node
					break;
				}
			}

			if(wait_counter==240){
				NodeHandle parent_node = app.getParent();
				app.routeMyMsg(parent_node); // send a byte array to the parent node
				break;
			}
			
		}

	}

	public static void isSame(ZipEntry child, ZipEntry original) {
		boolean isHashcodeEquals = child.hashCode() == original.hashCode(); 
		if (isHashcodeEquals){
				System.out.println("Received File is the same as original!");
		}
		else{
			System.out.println("They are not the same");
			}
	}


	// /**
 //   * Note that this function only works because we have global knowledge. Doing
 //   * this in an actual distributed environment will take some more work.
 //   * 
 //   * @param apps Vector of the applicatoins.
 //   */
	// public static void printTree(Vector<MyScribeClient> apps) {
	//     // build a hashtable of the apps, keyed by nodehandle
	//     Hashtable<NodeHandle, MyScribeClient> appTable = new Hashtable<NodeHandle, MyScribeClient>();
	//     Iterator<MyScribeClient> i = apps.iterator();
	//     while (i.hasNext()) {
	//       MyScribeClient app = (MyScribeClient) i.next();
	//       appTable.put(app.endpoint.getLocalNodeHandle(), app);
	//     }
	//     NodeHandle seed = ((MyScribeClient) apps.get(0)).endpoint
	//         .getLocalNodeHandle();

	//     // get the root
	//     NodeHandle root = getRoot(seed, appTable);

	//     // print the tree from the root down
	//     recursivelyPrintChildren(root, 0, appTable);
 //  }

 //  /**
 //   * Recursively crawl up the tree to find the root.
 //   */
	// public static NodeHandle getRoot(NodeHandle seed, Hashtable<NodeHandle, MyScribeClient> appTable) {
	//     MyScribeClient app = (MyScribeClient) appTable.get(seed);
	//     if (app.isRoot())
	//       return seed;
	//     NodeHandle nextSeed = app.getParent();
	//     return getRoot(nextSeed, appTable);
 //  }

 //  /**
 //   * Print's self, then children.
 //   */
	// public static void recursivelyPrintChildren(NodeHandle curNode,
 //      int recursionDepth, Hashtable<NodeHandle, MyScribeClient> appTable) {
	// 	// print self at appropriate tab level
	// 	String s = "";
	// 	for (int numTabs = 0; numTabs < recursionDepth; numTabs++) {
	// 	  s += "  ";
	// 	}
	// 	s += curNode.getId().toString();
	// 	System.out.println(s);

	// 	// recursively print all children
	// 	MyScribeClient app = (MyScribeClient) appTable.get(curNode);
	// 	NodeHandle[] children = app.getChildren();
	// 	for (int curChild = 0; curChild < children.length; curChild++) {
	// 	  recursivelyPrintChildren(children[curChild], recursionDepth + 1, appTable);
	// 	}
 //  }


	public static void main(String[] args) throws Exception {
    // Loads pastry configurations

	System.out.println(args.length);

    Environment env = new Environment("user");
	
	System.out.println("Using Fanout (pastry_rtBaseBitLength):" + env.getParameters().getInt("pastry_rtBaseBitLength"));

    // disable the UPnP setting (in case you are testing this on a NATted LAN)
    env.getParameters().setString("nat_search_policy","never");
    
    try {
      // the port to use locally
      int bindport = Integer.parseInt(args[0]);

      // build the bootaddress from the command line args
      InetAddress bootaddr = InetAddress.getByName(args[1]);
      int bootport = Integer.parseInt(args[2]);
      InetSocketAddress bootaddress = new InetSocketAddress(bootaddr, bootport);

      // the port to use locally
      int numNodes = Integer.parseInt(args[3]);
      int num_trees = Integer.parseInt(args[4]);

      // launch our node!
      MLSend dt = new MLSend(bindport, bootaddress, numNodes,
          env, num_trees);

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
