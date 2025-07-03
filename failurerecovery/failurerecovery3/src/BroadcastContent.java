// package rice.failurerecovery;

import java.util.Vector;

import rice.p2p.commonapi.Id;
import rice.p2p.scribe.ScribeContent;

public class BroadcastContent implements ScribeContent {

    String content;

    public BroadcastContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String toString() {
        return content;
    }
    
}
