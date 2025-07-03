package rice.failurerecovery;

import java.io.IOException;

import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.scribe.ScribeContent;
import rice.p2p.scribe.Topic;
import rice.p2p.scribe.messaging.ScribeMessage;
import rice.p2p.scribe.rawserialization.JavaSerializedScribeContent;
import rice.p2p.scribe.rawserialization.RawScribeContent;
import rice.p2p.scribe.rawserialization.ScribeContentDeserializer;

public class UpdateMessage extends ScribeMessage{

    protected NodeHandle source;
    protected Topic topic;
    protected RawScribeContent content;

	public static final short TYPE = 21;


    public UpdateMessage(NodeHandle source, Topic topic, ScribeContent content) {
        this(source, topic, content instanceof RawScribeContent ? (RawScribeContent)content : new JavaSerializedScribeContent(content));
        // this.topic = topic;
        // this.content = content;
    }

    public UpdateMessage(NodeHandle source, Topic topic, RawScribeContent content) {
	    super(source, topic);

	    this.content = content;
	}

    public int getPriority() {
        return MAX_PRIORITY;
    }
  
    // public Topic getTopic() {
    //     return this.topic;
    // }
    
    public ScribeContent getContent() {
        //  if (content == null) 
            if (content.getType() == 0) return ((JavaSerializedScribeContent)content).getContent();
            return content;
    }

    public void setContent(ScribeContent content) {
        if (content instanceof RawScribeContent) {
            setContent(content); 
        } else {
            setContent(new JavaSerializedScribeContent(content));
        }
    }

    /***************** Raw Serialization ***************************************/
    public short getType() {
        return TYPE;
    }

    public void serialize(OutputBuffer buf) throws IOException {
        buf.writeByte((byte)0); // version
        super.serialize(buf); 
        
        buf.writeShort(content.getType());
        content.serialize(buf);      
    }
	   
    public static UpdateMessage build(InputBuffer buf, Endpoint endpoint, ScribeContentDeserializer scd) throws IOException {
	    byte version = buf.readByte();
	    switch(version) {
	      case 0:
	        return new UpdateMessage(buf, endpoint, scd);
	      default:
	        throw new IOException("Unknown Version: "+version);
	    }
    }
	  
    /**
     * Private because it should only be called from build(), if you need to extend this,
     * make sure to build a serializeHelper() like in AnycastMessage/SubscribeMessage, and properly handle the 
     * version number.
     */
    private UpdateMessage(InputBuffer buf, Endpoint endpoint, ScribeContentDeserializer cd) throws IOException {
        super(buf, endpoint);
    
    // this can be done lazilly to be more efficient, must cache remaining bits, endpoint, cd, and implement own InputBuffer
        short contentType = buf.readShort();
        if (contentType == 0) {
            content = new JavaSerializedScribeContent(cd.deserializeScribeContent(buf, endpoint, contentType));
        } else {
            content = (RawScribeContent)cd.deserializeScribeContent(buf, endpoint, contentType); 
        }
    
    }
}
