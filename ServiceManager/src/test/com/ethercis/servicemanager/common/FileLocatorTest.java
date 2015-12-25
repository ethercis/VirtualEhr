//Copyright
package com.ethercis.servicemanager.common;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import junit.framework.TestCase;

import java.net.URL;

public class FileLocatorTest extends TestCase {

    public void testLocator(){
        RunTimeSingleton glob = RunTimeSingleton.instance();
        glob.init(new String[]{"-pluginsFile","http://.../empty.html"});

        FileLocator locator = new FileLocator(glob);

        try {
            URL url = locator.findFileInSearchPath("pluginsFile", "services.xml");
            if (url != null && url.getFile() != null) {
                System.out.println("The file 'services.xml' has been found");
                System.out.println("Its complete path is: '" + url.toString() + "' and the file is '" + url.getFile() + "'");
                System.out.println("DUMP:");
                System.out.println(locator.read(url));
            }
            else {
                System.out.println("The file 'services.xml' has not been found");
            }
        }
        catch (Exception ex) {
            System.err.println("Error occured: " + ex.toString());
        }

    }

}