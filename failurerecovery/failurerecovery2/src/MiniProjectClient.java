// package rice.failurerecovery;

import java.util.Vector;

import rice.environment.Environment;
import rice.environment.logging.Logger;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.CancellableTask;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.scribe.ScribeClient;
import rice.p2p.scribe.ScribeContent;
// import rice.p2p.scribe.ScribeImpl;
import rice.p2p.scribe.Topic;
import rice.p2p.scribe.rawserialization.JavaSerializedScribeContent;
import rice.p2p.scribe.rawserialization.RawScribeContent;
import rice.pastry.commonapi.PastryIdFactory;

public class MiniProjectClient implements ScribeClient {

    CancellableTask publishTask;
    protected ScribeImpl scribe;
    protected Topic topic;
    protected Logger logger;
    Vector ids = new Vector();

    public MiniProjectClient(Node node) {
        this.scribe = new ScribeImpl(node, "instance");
        this.topic = new Topic(new PastryIdFactory(node.getEnvironment()), "SimpleAggr");
        this.scribe.setClient(this);
        this.logger = node.getEnvironment().getLogManager().getLogger(MiniProjectClient.class, null);
    }

    /**
     * Subscribes to myTopic.
     */
    public void subscribe() {
        this.scribe.subscribe(this.topic, this); 
    }

    /**
     * Starts the publish task.
     */
    public void startPublishTask() {
        BroadcastContent content = new BroadcastContent("Publish BroadcastMessage to child nodes");
        publishTask = this.scribe.getEndpoint().scheduleMessage(new BroadcastMessage(this.scribe.getEndpoint().getLocalNodeHandle(),this.topic, (ScribeContent)content), 5000, 5000);    
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


    @Override
    public boolean anycast(Topic topic, ScribeContent content) {
        // TODO Auto-generated method stub
        return false;
    }


    // it is better to change this function to send since it is for sending up to next node
    @Override
    public void deliver(Topic topic, ScribeContent content) {
        if(content instanceof BroadcastContent) {
            if(!this.getScribeImpl().isRoot(topic)) {
                this.logger.log("Node: " + this.scribe.getId().toStringFull()
                                + " received BroadcastContent \"" + content
                                + "\" from the root");
                UpdateContent upcontent = new UpdateContent(getScribeImpl().getEndpoint().getLocalNodeHandle(), topic);
                upcontent.addId(getScribeImpl().getId().toStringFull());
                ids = upcontent.getIdVector();
                sendUpdate(topic, upcontent);
            }
        }
        if(content instanceof UpdateContent) {
            UpdateContent _content = (UpdateContent)content;
            updateIds(_content.getTopic(), _content);
        }
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

    @Override
    public void childAdded(Topic topic, NodeHandle child) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void childRemoved(Topic topic, NodeHandle child) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void subscribeFailed(Topic topic) {
        // TODO Auto-generated method stub
        
    }


    public boolean isRoot() {
        return this.scribe.isRoot(this.topic);
    }
      
    public NodeHandle getParent() {
    // NOTE: Was just added to the Scribe interface.  May need to cast myScribe to a
    // ScribeImpl if using 1.4.1_01 or older.
        return ((ScribeImpl)this.scribe).getParent(this.topic); 
    //return myScribe.getParent(myTopic); 
    }
      
    public NodeHandle[] getChildren() {
        return scribe.getChildren(this.topic); 
    }

    public Topic getTopic() {
        return this.topic;
    }

    public ScribeImpl getScribeImpl() {
        return this.scribe;
    }

}

