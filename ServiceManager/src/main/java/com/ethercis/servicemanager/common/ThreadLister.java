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


package com.ethercis.servicemanager.common;

import java.io.*;
import java.util.Iterator;
import java.util.Map;

/**
 * List all threads in this virtual machine.
 * <p />
 * 
 */
public class ThreadLister {
   /**
    * Display info about a thread.
    */
   private static void print_thread_info(PrintStream out, Thread t,
         String indent) {
      if (t == null)
         return;
      out.println(indent + "Thread: " + t.getName() + "  Priority: "
            + t.getPriority() + (t.isDaemon() ? " Daemon" : "")
            + (t.isAlive() ? "" : " Not Alive"));
   }

   /**
    * Display info about a thread group and its threads and groups
    */
   private static void list_group(PrintStream out, ThreadGroup g, String indent) {
      if (g == null)
         return;
      int numThreads = g.activeCount();
      int num_groups = g.activeGroupCount();
      Thread[] threads = new Thread[numThreads];
      ThreadGroup[] groups = new ThreadGroup[num_groups];
      g.enumerate(threads, false);
      g.enumerate(groups, false);
      out.println(indent + "Thread Group: " + g.getName() + "  Max Priority: "
            + g.getMaxPriority() + (g.isDaemon() ? " Daemon" : ""));
      for (int i = 0; i < numThreads; i++)
         print_thread_info(out, threads[i], indent + "    ");
      for (int i = 0; i < num_groups; i++)
         list_group(out, groups[i], indent + "    ");
   }

   /**
    * List all threads below the root thread group recursively.
    */
   public static void listAllThreads(PrintStream out) {
      // And list it, recursively
      list_group(out, getRootThreadGroup(), "");
   }

   /**
    * List all threads below the root thread group recursively.
    */
   public static String listAllThreads() {
      ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
      PrintStream ps = new PrintStream(os, true);
      listAllThreads(ps);
      return os.toString();
   }

   /**
    * Count all active threads in this virtual machine.
    * 
    * @return The number of threads.
    */
   public static int countThreads() {
      return getRootThreadGroup().activeCount();
      // return countThreads(0, getRootThreadGroup());
   }

   /**
    * Find the root thread group
    * 
    * @return The top level thread group
    */
   public static ThreadGroup getRootThreadGroup() {
      ThreadGroup current_thread_group;
      ThreadGroup root_thread_group;
      ThreadGroup parent;

      // Get the current thread group
      current_thread_group = Thread.currentThread().getThreadGroup();

      // Now go find the root thread group
      root_thread_group = current_thread_group;
      parent = root_thread_group.getParent();
      while (parent != null) {
         root_thread_group = parent;
         parent = parent.getParent();
      }
      return root_thread_group;
   }

   public static String getAllStackTraces() {
	  String NL = System.getProperty("line.separator");
	  if (NL == null) NL = "\n";
      try {
         Class[] paramCls = new Class[0];
         Object[] params = new Object[0];
         java.lang.reflect.Method method = Thread.class.getMethod("getAllStackTraces", paramCls);
         Map stacks = (Map)method.invoke(Thread.class, params);
         // Since JDK 1.5
         //Map stacks = Thread.getAllStackTraces();
         Iterator iter = stacks.entrySet().iterator();
         StringBuffer buf = new StringBuffer(2048);
         while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry)iter.next();
            Thread key = (Thread)entry.getKey();
            StackTraceElement[] elements = (StackTraceElement[])entry.getValue();
            buf.append(key.getName()).append(NL); // append(" state=").append(key.getState()).append(NL);
            for (int i=0; i < elements.length; i++) {
               buf.append("  ").append(elements[i].getClassName()).append(".").append(elements[i].getMethodName());
               if (elements[i].getLineNumber()> 0)
                  buf.append(": ").append(elements[i].getFileName()).append("#").append(elements[i].getLineNumber());
               buf.append(NL);
            }
            buf.append(NL);
         }
         return buf.toString();
      }
      catch (Throwable e) {
         return ThreadLister.listAllThreads(); // JDK 1.4
      }
   }

}
