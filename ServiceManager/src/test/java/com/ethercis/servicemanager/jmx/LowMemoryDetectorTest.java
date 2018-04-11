//Copyright
package com.ethercis.servicemanager.jmx;

import junit.framework.TestCase;

import java.util.ArrayList;

public class LowMemoryDetectorTest extends TestCase {

    /**
     * Tester: java -Xms2M -Xmx3M -Dcom.sun.management.jmxremote -Dehrserver/jmx/exitOnMemoryThreshold=true com.ethercis.servicemanager.jmx.LowMemoryDetector
     */
    public void donttestLowMemoryDetector() throws java.io.IOException {

        LowMemoryDetector mem = new LowMemoryDetector((float)0.9);
        mem.register(mem.new DefaultLowMemoryListener(mem));

        ArrayList list = new ArrayList();
        System.out.println("Hit a key to start");
        System.in.read();
        int chunkSize = 100000;
        try {
            for (int i=0; i<1000; i++) {
                System.out.println("Hit a key to allocate next " + chunkSize + " bytes");
                System.in.read();
                System.out.println("Adding another junk " + chunkSize);
                byte[] buffer = new byte[chunkSize];
                list.add(buffer);
            }
        }
        catch(java.lang.OutOfMemoryError e) {
            System.out.println("OOOOO: " + e.toString());
            System.in.read();
        }
        System.out.println("DONE, hit a key to finish");
        System.in.read();
    }

    public void testDummy(){
        assertTrue(true);
    }
}
