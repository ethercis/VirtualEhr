//Copyright
package com.ethercis.servicemanager.common;

import junit.framework.TestCase;

public class ThreadListerTest extends TestCase {

    /**
     * java org.xmlBlaster.common.ThreadLister
     */
    public void testListener() {
        // ThreadLister.listAllThreads(System.out);
        for (int i = 0; i < 5; i++) {
            new Thread("TestThread-"+i) {
                public void run() {
                    synchronized (this) {
                        System.out.println("Thread started");
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Thread.dumpStack();
        System.out.println("----------------------------");
        System.out.println("Thread overview:\n" + ThreadLister.listAllThreads());
        System.out.println("----------------------------");
        System.out.println("There are " + ThreadLister.countThreads() + " threads in use");
        System.out.println("----------------------------");
        System.out.println("getAllStackTraces(): " + ThreadLister.getAllStackTraces());
    }

}