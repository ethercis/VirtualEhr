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

/**
 * High performing TimeStamp class, time elapsed since 1970, the nanos are simulated
 * logonservice a unique counter.
 * <br />
 * The counter is rewound on any millisecond step.
 * <br />
 * TimeStamp objects are immutable - a TimeStamp can not be changed once it is created.
 * <br />
 * Guarantees that any created TimeStamp instance is unique in the current
 * Java Virtual Machine (and Classloader).
 * <br /><br />
 * Fails only if
 * <ul>
 *   <li>a CPU can create more than 999999 TimeStamp instances per millisecond</li>
 *   <li>In ~ 288 years when Long.MAX_VALUE = 9223372036854775807 overflows (current value is 1013338358124000008)</li>
 * </ul>
 * A typical response is:<br />
 * <ul>
 *   <li>toString()=2002-02-10 11:57:51.804000001</li>
 *   <li>getTimeStamp()=1013338671804000001</li>
 *   <li>getMillis()=1013338671804</li>
 *   <li>getMillisOnly()=804</li>
 *   <li>getNanosOnly()=804000001</li>
 * </ul>
 * Performance hints (600 MHz Intel PC, Linux 2.4.10, JDK 1.3.1):
 * <br />
 * <ul>
 *   <li>new TimeStamp()  1.2 micro seconds</li>
 *   <li>toString()       55 micro seconds the first time, further access 0.1 micro seconds</li>
 *   <li>valueOf()        19 micro seconds</li>
 *   <li>toXml("", false) 16 micro seconds</li>
 *   <li>toXml("", true)  17 micro seconds</li>
 * </ul>
 * XML representation:
 * <pre>
 *  &lt;TimeStamp nanos='1013346248150000001'>
 *     2002-02-10 14:04:08.150000001
 *  &lt;/TimeStamp>
 * </pre>
 * or
 * <pre>
 *  &lt;TimeStamp nanos='1013346248150000001'/>
 * </pre>
 */
public class TimeStamp implements Comparable, java.io.Serializable
{
   private static final long serialVersionUID = 1L;
   public static final int MILLION = 1000000;
   public static final int BILLION = 1000000000;
   //private static Object SYNCER = new Object();
   private static int nanoCounter = 0;
   private static long lastMillis = 0L;
   
   /** The TimeStamp in nanoseconds */
   private final long timestamp;
   private transient Long timestampLong; // cached for Long retrieval

   /** Cache for string representation */
   private transient String strFormat = null;

   /** You may overwrite the tag name for XML dumps in derived classes, defaults to &lt;TimeStamp ... */
   protected String tagName = "TimeStamp";

   /**
    * Constructs a current TimeStamp which is guaranteed to be unique in time for this JVM
    * @exception RuntimeException on overflow (never happens :-=)
    */
   public TimeStamp() {
      synchronized (TimeStamp.class) {
         long timeMillis = System.currentTimeMillis();
         if (lastMillis < timeMillis) {
            nanoCounter = 0; // rewind counter
            lastMillis = timeMillis;
            this.timestamp = timeMillis*MILLION;
            return;
         }
         else if (lastMillis == timeMillis) {
            nanoCounter++;
            if (nanoCounter >= MILLION)
               throw new RuntimeException("TimeStamp nanoCounter overflow - internal error");
            this.timestamp = timeMillis*MILLION + nanoCounter;
            return;
         }
         else { // Time goes backwards - this should not happen
            // NOTE from ruff 2002/12: This happens on my DELL notebook once and again (jumps to past time with 2-3 msec).
            // CAUTION: If a sysadmin changes time of the server hardware for say 1 hour backwards
            // The server should run without day time saving with e.g. GMT
            nanoCounter++;
            if (nanoCounter >= MILLION) {
               throw new RuntimeException("TimeStamp nanoCounter overflow - the system time seems to go back into past, giving up after " + MILLION + " times: System.currentTimeMillis() is not ascending old=" + lastMillis + " new=" + timeMillis);
            }
            this.timestamp = lastMillis*MILLION + nanoCounter;
            System.err.println("WARNING: TimeStamp System.currentTimeMillis() is not ascending old=" +
                   lastMillis + " new=" + timeMillis + " created TimeStamp=" + this.timestamp);
            return;
         }
      }
   }

   /**
    * Create a TimeStamp with given nanoseconds since 1970
    * @see java.util.Date
    */
   public TimeStamp(long nanos) {
      this.timestamp = nanos;
   }

   /**
    * @return The exact TimeStamp in nanoseconds
    */
   public final long getTimeStamp() {
      return timestamp;
   }

   /**
    * We cache a Long object for reuse (helpful when used logonservice a key in a map).
    * @return The exact TimeStamp in nanoseconds
    */
   public final Long getTimeStampLong() {
      if (this.timestampLong == null) {
         this.timestampLong = new Long(this.timestamp);
      }
      return timestampLong;
   }

   /**
    * The nano part only
    * @return The nano part only
    */
   public final int getNanosOnly() {
      return (int)(timestamp % BILLION);
   }

   /**
    * The milli part only
    * @return The milli part only
    */
   public final int getMillisOnly() {
      return getNanosOnly() / MILLION;
   }

   /**
    * You can use this value for java.common.Date(millis)
    * @return Rounded to millis
    * @see #getTime
    * @see java.util.Date
    */
   public final long getMillis() {
      return timestamp / MILLION;
   }

   /**
    * You can use this value for java.common.Date(millis)
    * @return Rounded to millis
    * @see #getMillis
    * @see java.util.Date
    */
   public final long getTime() {
      return getMillis();
   }

   /**
    * TimeStamp in JDBC TimeStamp escape format (human readable). 
    * @return The TimeStamp in JDBC TimeStamp escape format: "2002-02-10 10:52:40.879456789"
    */
   public String toString() {
      if (strFormat == null) {
         java.sql.Timestamp ts = new java.sql.Timestamp(getMillis());
         //if (ts.getTime() != getMillis()) {
         //   System.out.println("PANIC:java.sql.TimeStamp failes: sqlMillis=" + ts.getTime() + " givenMillis=" + getMillis()); 
         //}
         ts.setNanos(getNanosOnly());
         //System.out.println("ts.getTime=" + ts.getTime() + " givenMillis=" + getMillis() + " nanos=" + getNanosOnly()); 
         strFormat = ts.toString();
      }
      return strFormat;
   }

   /**
    * Converts a <code>String</code> object in JDBC TimeStamp escape format to a
    * <code>TimeStamp</code> value.
    *
    * @param s TimeStamp in format <code>yyyy-mm-dd hh:mm:ss.fffffffff</code>
    * @return corresponding <code>TimeStamp</code> value
    * @exception java.lang.IllegalArgumentException if the given argument
    * does not have the format <code>yyyy-mm-dd hh:mm:ss.fffffffff</code>
    */
   public static TimeStamp valueOf(String s) {
      java.sql.Timestamp tsSql = java.sql.Timestamp.valueOf(s);
      return new TimeStamp(((tsSql.getTime()/1000L)*1000L) * MILLION + tsSql.getNanos());
   }

   /**
    * Compares two TimeStamps for ordering.
    *
    * @param   ts The <code>TimeStamp</code> to be compared.
    * @return  the value <code>0</code> if the argument TimeStamp is equal to
    *          this TimeStamp; a value less than <code>0</code> if this 
    *          TimeStamp is before the TimeStamp argument; and a value greater than
    *           <code>0</code> if this TimeStamp is after the TimeStamp argument.
    */
   public int compareTo(Object obj) {
      TimeStamp ts = (TimeStamp)obj;
      if (timestamp > ts.getTimeStamp())
         return 1;
      else if (timestamp < ts.getTimeStamp())
         return -1;
      else
         return 0;
   }

   /**
    * Tests to see if this <code>TimeStamp</code> object is
    * equal to the given <code>TimeStamp</code> object.
    *
    * @param stamp the <code>TimeStamp</code> value to compare with
    * @return <code>true</code> if the given <code>TimeStamp</code>
    *         object is equal to this <code>TimeStamp</code> object;
    *         <code>false</code> otherwise
    */
   public boolean equals(TimeStamp ts) {
      return timestamp == ts.getTimeStamp();
   }


    /**
     * Computes a hashcode for this Long. The result is the exclusive 
     * OR of the two halves of the primitive <code>long</code> value 
     * represented by this <code>Long</code> object. That is, the hashcode 
     * is the value of the expression: 
     * <blockquote><pre>
     * (int)(this.longValue()^(this.longValue()>>>32))
     * </pre></blockquote>
     *
     * @return  a hash code value for this object.
     */
    /*
    public int hashCode() {
        return (int)(TimeStamp ^ (TimeStamp >> 32));
    }
    */

    /**
     * Compares this object against the specified object.
     * The result is <code>true</code> if and only if the argument is
     * not <code>null</code> and is a <code>Long</code> object that
     * contains the same <code>long</code> value logonservice this object.
     *
     * @param   obj   the object to compare with.
     * @return  <code>true</code> if the objects are the same;
     *          <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (obj instanceof TimeStamp) {
            return equals((TimeStamp)obj);
        }
        return false;
    }

   /**
    * @return internal state of the TimeStamp logonservice an XML ASCII string
    */
   public final String toXml() {
      return toXml((String)null);
   }
   /**
    * @return internal state of the TimeStamp logonservice an XML ASCII string
    * without human readable JDBC formatting
    */
   public final String toXml(String extraOffset) {
      return toXml(extraOffset, false);
   }


   /**
    * Dump state of this object into a XML ASCII string.
    * <br>
    * @param extraOffset indenting of tags for nice response
    * @param literal true -> show human readable format logonservice well (JDBC escape format)
    *               "2002-02-10 10:52:40.879456789"
    * @return internal state of the TimeStamp logonservice a XML ASCII string
    */
   public final String toXml(String extraOffset, boolean literal)
   {
      XmlBuffer sb = new XmlBuffer(200);
      String offset = "\n ";
      if (extraOffset != null)
         offset += extraOffset;
      if (literal) {
         sb.append(offset).append("<").append(tagName).append(" nanos='").append(getTimeStamp()).append("'>");
         sb.append(offset).append(" ").appendEscaped(toString());
         sb.append(offset).append("</").append(tagName).append(">");
      }
      else {
         sb.append(offset).append("<").append(tagName).append(" nanos='").append(getTimeStamp()).append("'/>");
      }
      return sb.toString();
   }

   /**
    * Convert milliseconds to some more human readable representation.
    * @param millis An amount of elapsed milliseconds
    * @return A human readable time string
    */
    public final static String millisToNice(long millis) {
       long seconds = millis / 1000;
       long sec = (seconds % 3600) % 60;
       long min = (seconds % 3600) / 60;
       long hour = seconds / 3600;
       StringBuffer strbuf = new StringBuffer(60);
       strbuf.append(" [ ");
       if (hour > 0L)
          strbuf.append(hour + " h ");
       if (min > 0L)
          strbuf.append(min + " min ");
       if (sec > 0L)
          strbuf.append(sec + " sec ");
       strbuf.append((millis % 1000) + " millis");
       strbuf.append(" ]");
       return strbuf.toString();
    }

   /**
    * Stop execution for some given milliseconds. 
    * Returns earlier on Interrupted exception
    *
    * @param millis amount of milliseconds to wait
    */
   public static void sleep(long millis) {
      try {
         Thread.sleep(millis);
      }
      catch (InterruptedException i) {
      }
   }

}


