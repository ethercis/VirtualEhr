//Copyright
package com.ethercis.servicemanager.service;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class ServiceHolderSaxFactoryTest extends TestCase {

    static Logger log = Logger.getLogger(ServiceHolderSaxFactory.class);

    static String[] args = new String[]{"-servicesFile", "resources/services.xml"};

    public void testHolder() {
        Logger.getRootLogger().setLevel(Level.DEBUG);
        Logger.getRootLogger().addAppender(new ConsoleAppender());
        RunTimeSingleton glob = RunTimeSingleton.instance();
        glob.init(args);

        try {
            ServiceHolderSaxFactory factory = new ServiceHolderSaxFactory(glob);
            ServiceHolder holder = factory.readConfigFile();
            System.out.println(holder.toXml());
        } catch (ServiceManagerException ex) {
            System.out.println("Error:"+ex.getMessage());
        }
    }


}