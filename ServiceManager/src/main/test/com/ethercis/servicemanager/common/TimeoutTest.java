//Copyright
package com.ethercis.servicemanager.common;

import junit.framework.TestCase;

public class TimeoutTest extends TestCase {

    /**
     * Method for testing only.
     * <p />
     * Invoke: java -Djava.compiler= ..Timeout
     */
    public void testTimeout() throws Exception {
        Timeout t = new Timeout();
        System.out.println("Timeout constructor done, sleeping 10 sec "
                + t.toString());
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
    }

}