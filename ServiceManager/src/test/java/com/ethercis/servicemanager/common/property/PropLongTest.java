//Copyright
package com.ethercis.servicemanager.common.property;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import junit.framework.TestCase;

public class PropLongTest extends TestCase {


    /** java org.xmlBlaster.common.property.PropLong */
    public void testPropLong() {
        PropLong maxEntries = new PropLong(123456L);
        System.out.println(maxEntries.toXml());

        RunTimeSingleton glob = RunTimeSingleton.instance().getClone(new String[] {});
        String nodeId = null;
        String prefix = null;
        String className = null;
        String instanceName = null;
        String propName = "maxEntries";

        try {
            glob.getProperty().set("maxEntries", "444444");
            glob.getProperty().set("persistence/msgUnitStore/maxEntries", "666666");
            glob.getProperty().set("topic/hello/persistence/msgUnitStore/maxEntries", "777777"); // this should be ignored in current version
            glob.getProperty().set("/node/heron/topic/hello/persistence/msgUnitStore/maxEntries", "999999");
            //System.out.println(glob.getProperty().toXml());


            System.out.println("PropName=" + propName + ", used env name=" +
                    maxEntries.setFromEnv(glob, nodeId, prefix, className, instanceName, propName) +
                    ": " + maxEntries.toXml(""));

            nodeId = "heron";
            prefix = "topic/hello";
            className = "persistence";
            instanceName = "msgUnitStore";
            System.out.println("PropName=" + propName + ", used env name=" +
                    maxEntries.setFromEnv(glob, nodeId, prefix, className, instanceName, propName) +
                    ": " + maxEntries.toXml(""));
        }
        catch (Exception e) {
            System.out.println("ERROR: " + e.toString());
        }
    }

}