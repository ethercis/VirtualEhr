//Copyright
package com.ethercis.servicemanager.common;

import junit.framework.TestCase;

public class TimeStampTest extends TestCase {


    /**
     * Test only.
     * <pre>
     *
     * Dump a nanosecond 'long' to a string representation:
     *
     *   java TimeStamp 1076677832527000001
     *
     * Dump a a string representation to a nanosecond 'long':
     *
     *   java TimeStamp "2004-02-13 14:10:32.527000001"
     * </pre>
     */
    public void testTimeStampLong() {

        String[] args = {};

        if (args.length > 0) {
            if (args[0].indexOf(":") != -1) {
                // 2004-02-13 14:10:32.527000001
                TimeStamp tt = TimeStamp.valueOf(args[0]);
                System.out.println(tt.toXml("", true));
            }
            else {
                long nanos = Long.valueOf(args[0]).longValue();
                TimeStamp tt = new TimeStamp(nanos);
                System.out.println(tt.toXml("", true));
            }
            System.exit(0);
        }

        int count = 5;
        StringBuffer buf = new StringBuffer(count * 120);
        for (int ii=0; ii<count; ii++)
            test(buf);
        System.out.println(buf);

        __testToString();
        __testToString();
        __testToString();

        System.out.println("TEST 1");

        __testValueOf();
        __testValueOf();
        __testValueOf();

        System.out.println("TEST 2");

        testToXml(false);
        testToXml(false);
        testToXml(false);

        System.out.println("TEST 3");

        testToXml(true);
        testToXml(true);
        testToXml(true);

        TimeStamp ts1 = new TimeStamp();
        TimeStamp ts2 = new TimeStamp();
        if (ts1.equals(ts2))
            System.out.println("ERROR: equals()");
        if (ts2.compareTo(ts1) < 1)
            System.out.println("ERROR: compareTo()");
        if (ts2.toString().equals(TimeStamp.valueOf(ts2.toString()).toString()) == false)
            System.out.println("ERROR: valueOf() ts2.toString()=" + ts2.toString() + " TimeStamp.valueOf(ts2.toString()).toString()=" + TimeStamp
                    .valueOf(ts2.toString()).toString());

        System.out.println(ts2.toXml(""));
        System.out.println(ts2.toXml("", true));
    }
    /** Test only */
    private static final TimeStamp test(StringBuffer buf) {
        TimeStamp ts = new TimeStamp();
        buf.append("TimeStamp toString()=" + ts.toString() +
                " getTimeStamp()=" + ts.getTimeStamp() +
                " getMillis()=" + ts.getMillis() +
                " getMillisOnly()=" + ts.getMillisOnly() +
                " getNanosOnly()=" + ts.getNanosOnly() +
                " getScannedAndDumped()=" + TimeStamp.valueOf(ts.toString()).toString() +
                "\n");
        return ts;
    }
    /** Test only */
    private static final void __testToString()
    {
        int count = 10000;
        long start = System.currentTimeMillis();
        TimeStamp ts = new TimeStamp();
        for (int ii=0; ii<count; ii++) {
            ts.toString();
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("toString(): " + count + " toString " + elapsed + " millisec -> " + ((elapsed*1000.*1000.)/count) + " nanosec/toString()");
    }
    /** Test only */
    private static final void __testValueOf()
    {
        int count = 10000;
        TimeStamp ts1 = new TimeStamp();
        String val = ts1.toString();
        long start = System.currentTimeMillis();
        for (int ii=0; ii<count; ii++) {
            TimeStamp.valueOf(val);
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("valueOf(): " + count + " valueOf " + elapsed + " millisec -> " + ((elapsed*1000.*1000.)/count) + " nanosec/valueOf()");
    }
    /** Test only */
    private static final void testToXml(boolean literal)
    {
        int count = 10000;
        long start = System.currentTimeMillis();
        TimeStamp ts = new TimeStamp();
        for (int ii=0; ii<count; ii++) {
            ts.toXml(null, literal);
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("toXml(" + literal + "): " + count + " toXml " + elapsed + " millisec -> " + ((elapsed*1000.*1000.)/count) + " nanosec/toXml()");
    }

}