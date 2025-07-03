package rice.failurerecovery;

import java.util.Vector;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.scribe.ScribeContent;
import rice.p2p.scribe.Topic;


public class UpdateContent implements ScribeContent{

    protected NodeHandle source;

    protected Topic topic;

    protected Vector idVec = new Vector();

    public UpdateContent(NodeHandle source, Topic topic) {
        this.source = source;
        this.topic = topic;
        // this.idVec = idVec;
    }

    public Topic getTopic() {
        return this.topic;
    }

    public void setSource(NodeHandle source) {
        this.source = source;
    }

    public NodeHandle getSource() {
        return this.source;
    }

    public void addId(String fullId) {
        if(!this.idVec.contains(fullId)) this.idVec.add(fullId);
    }

    public Integer getNumIds() {
        return this.idVec.size();
    }

    public Vector getIdVector() {
        return this.idVec;
    }

    public String toString() {
        return idVec.toString();
    }
}
