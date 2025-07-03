// package rice.failurerecovery;

import java.util.Vector;

// import org.mpisws.p2p.testing.transportlayer.replay.MyScribeContent;

import rice.environment.Environment;
import rice.environment.logging.Logger;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.CancellableTask;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.scribe.Scribe;
import rice.p2p.scribe.ScribeClient;
import rice.p2p.scribe.ScribeContent;
// import rice.p2p.scribe.ScribeImpl;
import rice.p2p.scribe.Topic;
import rice.p2p.scribe.rawserialization.JavaSerializedScribeContent;
import rice.p2p.scribe.rawserialization.RawScribeContent;
import rice.pastry.PastryNode;
import rice.pastry.commonapi.PastryIdFactory;

public class MiniProjectClient implements ScribeClient, Application {

    CancellableTask publishTask;
    protected ScribeImpl scribe;
    protected Topic topic;
    protected Logger logger;
    Vector ids = new Vector();

    protected Endpoint endpoint;
    int seqNum = 0;
    Scribe myScribe;
    Topic myTopic;

    int activeChilds = 0;
    PastryNode node;
    boolean firstTime = true;
    boolean secondTime = false;

    public MiniProjectClient(Node node) {
        this.node = (PastryNode) node;
        this.endpoint = node.buildEndpoint(this, "myinstance");
        myScribe = new ScribeImpl(node, "myScribeInstance");
        myTopic = new Topic(new PastryIdFactory(node.getEnvironment()), "example topic0");
        System.out.println("myTopic = "+myTopic);
        // this.scribe = new ScribeImpl(node, "instance");
        // this.topic = new Topic(new PastryIdFactory(node.getEnvironment()), "SimpleAggr");
        // this.scribe.setClient(this);
        this.logger = node.getEnvironment().getLogManager().getLogger(MiniProjectClient.class, null);
        endpoint.register();
    }



    public void sendMulticast(Message message, boolean clear_ids) {
        // System.out.println("Node "+this.scribe.getEndpoint().getLocalNodeHandle()+" broadcasting ");
        if (clear_ids) this.ids.clear();
        // this.ids.clear();
        this.logger.log("Publish BroadcastMessage to child nodes for topic " + this.topic+" at " + System.currentTimeMillis());
        // System.out.println("=================================================================");
        this.scribe.publish(((BroadcastMessage)message).getTopic(), ((BroadcastMessage)message).getContent()); 
        // seqNum++;
    }

    

    public void sendMulticast(String line) {
        System.out.println("Node "+endpoint.getLocalNodeHandle()+" broadcasting "+seqNum);
        MyScribeContent myMessage = new MyScribeContent(endpoint.getLocalNodeHandle(), seqNum, line);
        myScribe.publish(myTopic, myMessage);
        seqNum++;
    }

    public void sendAnycast(String line) {
        System.out.println("Node "+endpoint.getLocalNodeHandle()+" anycasting "+seqNum);
        MyScribeContent myMessage = new MyScribeContent(endpoint.getLocalNodeHandle(), seqNum, line);
        myScribe.anycast(myTopic, myMessage); 
        seqNum++;
    }


  
    //it is better find check leaf set and children => done
    public void updateIds(Topic topic, UpdateContent content) {
        if(this.scribe.isRoot(topic)) {
            content.addId(this.scribe.getId().toStringFull());
            Vector _temp = content.getIdVector();
            for (int i = 0; i < _temp.size(); i++) {
                if(!ids.contains(_temp.get(i))) ids.add(_temp.get(i));
            }
            // System.out.println("Root received " + ids.size() + " ids");
            this.logger.log("Root received " + ids.size() 
                            // + " ids as follows: " + ids + 
                            + " at " + System.currentTimeMillis());
        }
        else {
            content.addId(scribe.getId().toStringFull());
            content.setSource(getScribeImpl().getEndpoint().getLocalNodeHandle());
            // this.ids.clear();
            sendUpdate(topic, content);
            // ids = content.getIdVector();
        }
    }

    public void sendUpdate(Topic topic, ScribeContent content) {
        sendUpdate(topic, content instanceof RawScribeContent ? (RawScribeContent)content : new JavaSerializedScribeContent(content));
    }

    public void sendUpdate(Topic topic, RawScribeContent content) {
        this.logger.log("Node: " + getScribeImpl().getId()
                        + " sends UpdateContent \"" + content 
                        + "\" to parent node: " + getScribeImpl().getParent(topic));
        getScribeImpl().getEndpoint().route(null, 
                                            new UpdateMessage(getScribeImpl().getEndpoint().getLocalNodeHandle(), topic, content), 
                                            getScribeImpl().getParent(topic));
        // try {
        //     getScribeImpl().getEnvironment().getTimeSource().sleep(100);
        // } catch (InterruptedException e) {
        //     // TODO Auto-generated catch block
        //     e.printStackTrace();
        // }
    }



      
   

    public Topic getTopic() {
        return this.topic;
    }

    public ScribeImpl getScribeImpl() {
        return this.scribe;
    }

    /////////////////////////
    public void subscribe(){
		myScribe.subscribe(myTopic, this);
		// for (int index = 0; index < this.topic_list.size(); index++){
  //     		myScribe.subscribe(this.topic_list.get(index), this);
  //   	}
	}


	// this gets called first
	public void startPublishTask(){
		publishTask = endpoint.scheduleMessage(new PublishContent(), 5000, 5000);  
	}
	// this gets called second
	// headnode received a message
	// sendmulticast

    
	public void deliver(Id id, Message message){
        System.out.println(this+" received "+message);

        if (message instanceof MyMsg){
			String line = ((MyMsg)message).line;
            if(isRoot()) {
            // if(this.scribe.isRoot(topic)) {
                System.out.println("Received at root");
                System.out.println(line);
                this.activeChilds+=1;
            }
            else{
                System.out.println("Received at child");
                System.out.println(line);
                if(line.equals("Kill Node")){
                    System.out.println("Killing myself :(");
                    this.node.destroy();
                    // System.exit(0);
                    // Runtime.getRuntime().halt(0);
                }
                else if(line.equals("Start Second Round")){
                    this.secondTime = true;
                }
                else if(line.equals("End Experiment")){
                    System.out.println("Finished Experiment");
                    // this.node.destroy();
                    // System.exit(0);
                    // Runtime.getRuntime().halt(0);
                }
            }
		}

		// System.out.println(this.endpoint.getId()+" received "+message); //this: headnode
		// if (message instanceof PublishContent){
		// 	sendMulticast();
		// }
		// if (message instanceof MyMsg){
		// 	System.out.println("Bytes training results transferred to Head Node");
		// 	// System.out.println();
        //     // byte[] bytes = ((MyMsg)message).bytes;
		// 	// this.model_list.add(bytes);
		// }
	}

	public void deliver(Topic topic, ScribeContent content) {
        System.out.println("MyScribeClient.deliver("+topic+","+content+")");

        String line = ((MyScribeContent)content).line;
        
        if(isRoot()) {
        // if(this.scribe.isRoot(topic)) {
            System.out.println("Received at root");
            System.out.println(line);
            this.activeChilds+=1;
        }
        else{
            System.out.println("Received at child");
            System.out.println(line);
            if(line=="Kill Node"){
                System.out.println("Killing myself :(");
                this.node.destroy();
            }
        }

        

        if (((MyScribeContent)content).from == null) {
            new Exception("Stack Trace").printStackTrace();
        }
    }
    

	// PublishContent is used to publish a model from headnode to children nodes
// 	public void sendMulticast(){
// 		System.out.println("Node "+endpoint.getLocalNodeHandle()+" broadcasting "+seqNum); // headnode start broadcasting
// 		MyScribeContent myMessage = new MyScribeContent(endpoint.getLocalNodeHandle(), seqNum, this.bytes);
// 		myScribe.publish(myTopic, myMessage);
// 		// for (int index = 0; index < this.topic_list.size(); index++){
//   //     		myScribe.publish(this.topic_list.get(index), myMessage);
//   //   	}
// 		seqNum++;
// 	}

    

	public void childAdded(Topic topic, NodeHandle child) {

	}

	public void subscribeFailed(Topic topic) {
		System.out.println("MyScribeClient.childFailed("+topic+")");
  	}
  	public void childRemoved(Topic topic, NodeHandle child) {
		System.out.println("MyScribeClient.childRemoved("+topic+","+child+")");
  	}

	public boolean forward(RouteMessage message) {
		return true;
	}

	public void update(NodeHandle handle, boolean joined) {

	}
	public boolean anycast(Topic topic, ScribeContent content) {
    boolean returnValue = myScribe.getEnvironment().getRandomSource().nextInt(3) == 0;
    System.out.println("MyScribeClient.anycast("+topic+","+content+"):"+returnValue);
    return returnValue;
  	}

	// class PublishContent implements Message {

	// 	byte[] bytes;
	// 	public PublishContent(byte[] bytes){
	// 		this.bytes = bytes;
	// 	}
	// 	public int getPriority(){
	// 		return MAX_PRIORITY;
	// 	}
	// 	public byte[] getBytes(){
	// 		return this.bytes;
	// 	}

	// }

	public boolean isRoot() {
		return myScribe.isRoot(myTopic);
	}
	public NodeHandle getParent(){
		return ((ScribeImpl)myScribe).getParent(myTopic);
	}

	public NodeHandle[] getChildren(){
		return myScribe.getChildren(myTopic);
	}

	class PublishContent implements Message {
	    public int getPriority() {
	      return MAX_PRIORITY;
	    }
  }
	// route message
	public void routeMyMsg(Id id, String line){
		System.out.println(this+" sending to "+id);    
        Message msg = new MyMsg(endpoint.getId(), id, line);
        endpoint.route(id, msg, null);
	}

    public void routeMyMsgDirect(NodeHandle nh, String line) {
        System.out.println(this+" sending direct to "+nh);
        Message msg = new MyMsg(this.endpoint.getId(), nh.getId(), line);
        this.endpoint.route(null, msg, nh);
      }

}

