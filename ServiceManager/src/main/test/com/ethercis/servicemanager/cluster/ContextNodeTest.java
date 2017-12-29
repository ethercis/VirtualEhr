//Copyright
package com.ethercis.servicemanager.cluster;

import junit.framework.TestCase;

public class ContextNodeTest extends TestCase {

    public void setUp() throws Exception {
        super.setUp();

    }

    /**
     * Method for testing only.
     * <p />
     *
     * Invoke: java -Djava.compiler= org.ehrserver.common.context.ContextNode
     */
    public void testArgument() {
        try {
            ContextNode heron = new ContextNode(ContextNode.CLUSTER_MARKER_TAG,
                    "heron", null);
            System.out.println("AbsoluteName=" + heron.getAbsoluteName()
                    + " RelativeName=" + heron.getRelativeName());
            ContextNode jack = new ContextNode(ContextNode.SUBJECT_MARKER_TAG,
                    "jack", heron);
            System.out.println("AbsoluteName=" + jack.getAbsoluteName()
                    + " RelativeName=" + jack.getRelativeName());

            ContextNode ses2 = new ContextNode(ContextNode.SESSION_MARKER_TAG,
                    "2", jack);
            System.out.println("AbsoluteName=" + ses2.getAbsoluteName()
                    + " RelativeName=" + ses2.getRelativeName());
            System.out.println("AbsoluteName=" + ses2.getAbsoluteName("xpath")
                    + " RelativeName=" + ses2.getRelativeName("xpath"));
            System.out.println("AbsoluteName=" + ses2.getAbsoluteName("jmx")
                    + " RelativeName=" + ses2.getRelativeName("jmx"));

            {
                System.out.println("\nTopic:");
                ContextNode hello = new ContextNode(
                        ContextNode.TOPIC_MARKER_TAG, "hello", heron);
                System.out.println("AbsoluteName=" + hello.getAbsoluteName()
                        + " RelativeName=" + hello.getRelativeName());
                System.out.println("AbsoluteName="
                        + hello.getAbsoluteName("xpath") + " RelativeName="
                        + hello.getRelativeName("xpath"));
                System.out.println("AbsoluteName="
                        + hello.getAbsoluteName("jmx") + " RelativeName="
                        + hello.getRelativeName("jmx"));
            }
            {
                System.out.println("\nWith NULL:");
                ContextNode hello = new ContextNode(
                        ContextNode.TOPIC_MARKER_TAG, null, heron);
                System.out.println("AbsoluteName=" + hello.getAbsoluteName()
                        + " RelativeName=" + hello.getRelativeName());
                System.out.println("AbsoluteName="
                        + hello.getAbsoluteName("xpath") + " RelativeName="
                        + hello.getRelativeName("xpath"));
                System.out.println("AbsoluteName="
                        + hello.getAbsoluteName("jmx") + " RelativeName="
                        + hello.getRelativeName("jmx"));
            }
            {
                System.out.println("\nMERGE:");
                ContextNode root = ContextNode
                        .valueOf("/node/heron/client/joe");
                ContextNode other = ContextNode
                        .valueOf("/node/xyz/client/joe/session/1");
                ContextNode leaf = root.mergeChildTree(other);
                // -> /ehrserver/node/heron/client/joe/session/1
                System.out.println("Orig=" + root.getAbsoluteName() + " merge="
                        + other.getAbsoluteName() + " result="
                        + leaf.getAbsoluteName());
            }
            {
                System.out.println("\nMERGE:");
                ContextNode root = ContextNode
                        .valueOf("/node/heron/client/joe/session/1");
                ContextNode other = ContextNode
                        .valueOf("/node/xyz/service/Pop3Driver");
                ContextNode leaf = root.mergeChildTree(other);
                // ->
                // /ehrserver/node/heron/client/joe/session/1/service/Pop3Driver
                System.out.println("Orig=" + root.getAbsoluteName() + " merge="
                        + other.getAbsoluteName() + " result="
                        + ((leaf == null) ? "null" : leaf.getAbsoluteName()));
            }
            {
                System.out.println("\nMERGE:");
                ContextNode root = ContextNode
                        .valueOf("/node/heron/client/joe/session/1");
                ContextNode other = ContextNode
                        .valueOf("/node/clientjoe1/\"connection:client/joe/1\"");
                ContextNode leaf = root.mergeChildTree(other);
                // ->
                // /ehrserver/node/heron/client/joe/session/1/service/Pop3Driver
                System.out.println("Orig=" + root.getAbsoluteName() + " merge="
                        + other.getAbsoluteName() + " result="
                        + ((leaf == null) ? "null" : leaf.getAbsoluteName()));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR: " + e.toString());
        }
    }
}