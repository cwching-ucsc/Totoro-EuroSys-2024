import rice.p2p.commonapi.NodeHandle;
import rice.p2p.scribe.ScribeContent;

public class MyScribeContent implements ScribeContent {
    /**
     * The source of this content.
     */
    NodeHandle from;

    /**
     * The sequence number of the content.
     */
    int seq;
    String line;

    /**
     * Simple constructor.  Typically, you would also like some
     * interesting payload for your application.
     *
     * @param from Who sent the message.
     * @param seq the sequence number of this content.
     */
    public MyScribeContent(NodeHandle from, int seq, String line) {
        this.from = from;
        this.seq = seq;
        this.line = line;
//    System.out.println(this+".ctor");
    }

    /**
     * Ye ol' toString()
     */
    public String toString() {
        return "MyScribeContent #"+seq+" from "+from;
    }
}
