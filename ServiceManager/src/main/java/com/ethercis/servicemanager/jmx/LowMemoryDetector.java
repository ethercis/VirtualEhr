/*
 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
This code is a refactoring and adaptation of the original
work provided by the XmlBlaster project (see http://xmlblaster.org)
for more details.
This code is therefore supplied under LGPL 2.1
 */

/**
 * Project: EtherCIS system application
 * 
 * @author <a href="mailto:christian@adoc.co.th">Christian Chevalley</a>
 * @author <a href="mailto:michele@laghi.eu">Michele Laghi</a>
 * @author <a href="mailto:xmlblast@marcelruff.info">Marcel Ruff</a>
 */


package com.ethercis.servicemanager.jmx;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.*;
import java.lang.management.*;
import java.util.Iterator;
import java.util.List;


/**
 * Get notification when heap memory usage exceeds 90%. 
 * Configuration:
 * <pre>
-ehrserver/jmx/observeLowMemory       Write a log error when 90% of the JVM memory is used (JDK >= 1.5) [true]
-ehrserver/jmx/memoryThresholdFactor  Configure the log error memory threshhold (defaults to 90%) (JDK >= 1.5) [0.9]
-ehrserver/jmx/exitOnMemoryThreshold  If true xmlBlaster stops if the memoryThresholdFactor is reached (JDK >= 1.5) [false]
 * </pre>
 */
public class LowMemoryDetector {
    private final static Logger log = LogManager.getLogger(LowMemoryDetector.class);
    private MBeanServer mbeanServer;
    private MemoryPoolMXBean pool;
    private double thresholdFactor;

    /**
     * Access the max available RAM for this JVM.
     * You can increase it with 'java -Xmx256M ...'
     *
     * @return bytes
     */
    public static long maxJvmMemory() {
        MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
        MemoryUsage usage = mbean.getHeapMemoryUsage();
        return usage.getMax();
    }

    public MBeanServer getMBeanServer() {
        return this.mbeanServer;
    }

    /**
     * Default ctor for 90% threshold and registered DefaultLowMemoryListener.
     */
    public LowMemoryDetector() {
        this((float) 0.9);
        register(new DefaultLowMemoryListener(this));
    }

    /**
     * @param thresholdFactor Use typically 0.9, if 90% of heap is used up the
     *                        listener is triggered
     */
    public LowMemoryDetector(float thresholdFactor) {
        this.thresholdFactor = thresholdFactor;
        // http://java.sun.com/j2se/1.5.0/docs/api/java/lang/management/MemoryPoolMXBean.html
        this.mbeanServer = MBeanServerFactory.createMBeanServer();

        List list = ManagementFactory.getMemoryPoolMXBeans();
        Iterator it = list.iterator();
        while (it.hasNext()) {
            MemoryPoolMXBean tmpPool = (MemoryPoolMXBean) it.next();
            if (tmpPool.isUsageThresholdSupported() && tmpPool.getType().equals(MemoryType.HEAP)) {
                this.pool = tmpPool;
                // "Tenured Gen" = pool.getUserId()
                long myThreshold = (long) (this.thresholdFactor * (double) this.pool.getUsage().getMax()); //getCommitted());
                this.pool.setUsageThreshold(myThreshold);
                //System.out.println("Adding maxJvmMemory=" + maxJvmMemory() +
                //      ", committed for heap=" + this.pool.getUsage().getCommitted() +
                //      ", max for heap=" + this.pool.getUsage().getMax() +
                //      ", used threshold=" + this.pool.getUsageThreshold());
                break;
            }
        }

        register(new DefaultLowMemoryListener(this));
    }

    /**
     * Register your low memory listener.
     */
    public void register(NotificationListener listener) {
        if (this.pool != null) {
            // register for notification ...
            MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
            NotificationEmitter emitter = (NotificationEmitter) mbean;
            emitter.addNotificationListener(listener, null, this.pool);
        }
    }


    /**
     * The default handler just logs the situation or exits if configured.
     */
    public class DefaultLowMemoryListener implements NotificationListener {
        private final Logger log = LogManager.getLogger(DefaultLowMemoryListener.class);
        boolean exitOnThreshold;
        MBeanServer mbeanServer;

        public DefaultLowMemoryListener(LowMemoryDetector lowMemoryDetector) {
            this.exitOnThreshold = RunTimeSingleton.instance().getProperty().get("ehrserver/jmx/exitOnMemoryThreshold", this.exitOnThreshold);
            this.mbeanServer = lowMemoryDetector.getMBeanServer();
        }

        /**
         * Called when memory threshold is reached.
         */
        public void handleNotification(Notification notification, Object handback) {
            try {
                String notifType = notification.getType();
                if (!notifType.equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED))
                    return;

                MemoryPoolMXBean pool = (MemoryPoolMXBean) handback;

                int numTries = 5;
                for (int i = 0; i < numTries; i++) {
                    // Memory is low! maxJvmMemory=8.323 MBytes, max for heap=7.340 MBytes, otherMem=983040, threshold reached=6.606 MBytes,
                    // Runtime.totalMemory=8323072, Runtime.freeMemory=1461904, usedMem=6.861 MBytes

                    log.debug(
                            "Memory is low! maxJvmMemory=" + RunTimeSingleton.byteString(LowMemoryDetector.maxJvmMemory()) +
                                    ", max for heap=" + RunTimeSingleton.byteString(pool.getUsage().getMax()) +
                                    ", otherMem=" + RunTimeSingleton.byteString(LowMemoryDetector.maxJvmMemory() - pool.getUsage().getMax()) +  // 8.323-7.340=0.983
                                    ", threshold reached=" + RunTimeSingleton.byteString(pool.getUsageThreshold()) +
                                    ", Runtime.totalMemory=" + RunTimeSingleton.byteString(Runtime.getRuntime().totalMemory()) +
                                    ", Runtime.freeMemory=" + RunTimeSingleton.byteString(Runtime.getRuntime().freeMemory()) +
                                    ", usedMem=" + RunTimeSingleton.byteString(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

                    System.gc();
                    try {
                        Thread.sleep(1);
                    } catch (Exception e) {
                    }

                    long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                    if (usedMem < pool.getUsageThreshold()) {

                        log.debug("Low memory: Nothing to do, the garbage collector has handled it usedMem=" + RunTimeSingleton.byteString(usedMem) + " threshold=" + RunTimeSingleton.byteString(pool.getUsageThreshold()));
                        return;  // Nothing to do, the garbage collector has handled it
                    }
                }

                long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());

                //Memory is low! maxJvmMemory=8.323 MBytes, committed for heap=7.340 MBytes, max for heap=7.340 MBytes,
                //threshold reached=6.606 MBytes, currently used=7.595 MBytes, count=2.
                //Physical RAM size is 1.060 GBytes, this JVM may use max 8.323 MBytes and max 1024 file descriptors
                log.error("Memory is low! maxJvmMemory=" + RunTimeSingleton.byteString(LowMemoryDetector.maxJvmMemory()) +
                        //", committed for heap=" + Global.byteString(pool.getUsage().getCommitted()) +
                        ", max for heap=" + RunTimeSingleton.byteString(pool.getUsage().getMax()) +
                        ", threshold reached=" + RunTimeSingleton.byteString(pool.getUsageThreshold()) +
                        ", currently used=" + RunTimeSingleton.byteString(usedMem) +
                        ", count=" + pool.getUsageThresholdCount() +
                        ". Physical RAM size is " + RunTimeSingleton.byteString(RunTimeSingleton.totalPhysicalMemorySize) + "," +
                        " this JVM may use max " + RunTimeSingleton.byteString(RunTimeSingleton.heapMemoryUsage) +
                        " and max " + RunTimeSingleton.maxFileDescriptorCount + " file descriptors");
                if (this.exitOnThreshold) {
                    System.gc();
                    try {
                        Thread.sleep(1);
                    } catch (Exception e) {
                    }
                    System.gc();
                    try {
                        Thread.sleep(1);
                    } catch (Exception e) {
                    }
                    usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                    if (usedMem > pool.getUsageThreshold()) {
                        log.error("Exiting now because of low memory (see '-ehrserver/jmx/exitOnMemoryThreshold true'");
                        System.exit(-9);
                    }
                    log.info("Garbage collected to usedMem=" + RunTimeSingleton.byteString(usedMem) + ", we continue");
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    } // class DefaultLowMemoryListener
}


