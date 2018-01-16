//Copyright
package com.ethercis.servicemanager.common;

import junit.framework.TestCase;

import java.util.Date;

public class IsoDateParserTest extends TestCase {

    public static void test(String isodate) {
        System.out.println("----------------------------------");
        try {
            Date date = IsoDateParser.parse(isodate);
            System.out.println("GIVEN >> "+isodate);
            System.out.println("LOCAL >> "+date.toString()+" ["+date.getTime()+"]");
            System.out.println("UTC   >> "+ IsoDateParser.getUTCTimestamp(date));
            System.out.println("UTCT  >> "+ IsoDateParser.getUTCTimestampT(date));
            System.out.println("ISO   >> "+ IsoDateParser.getIsoDate(date));
        } catch (IllegalArgumentException ex) {
            System.err.println(isodate+" is invalid");
            System.err.println(ex.getMessage());
        }
        System.out.println("----------------------------------");
    }

    public static void test(Date date) {
        String isodate = null;
        System.out.println("----------------------------------");
        try {
            System.out.println("GIVEN >> "+date.toString()+" ["+date.getTime()+"]");
            isodate = IsoDateParser.getIsoDate(date);
            System.out.println("ISO   >> "+isodate);
            date = IsoDateParser.parse(isodate);
            System.out.println("PARSED>> "+date.toString()+" ["+date.getTime()+"]");
        } catch (IllegalArgumentException ex) {
            System.err.println(isodate+" is invalid");
            System.err.println(ex.getMessage());
        }
        System.out.println("----------------------------------");
    }

    // java org.xmlBlaster.common.IsoDateParser
    public void testParser() {
        test("1997-07-16T19:20:30.45-02:00");
        test("1997-07-16T19:20:30.678Z");
        test("2006-07-16T21:20:30.450+00:00");
        //test("2006-07-16T21:20:30.450+0000"); invalid

        test("1997-07-16 19:20:30.45-02:00");
        test("1997-07-16 19:20:30.678Z");
        test("2006-07-16 21:20:30.450+00:00");

        test("1997-07-16T19:20:30+01:00");
        test("1997-07-16T19:20:30+01:00");
        test("1997-07-16T12:20:30-06:00");
        test("1997-07-16T19:20");
        test("1997-07-16");
        test("1997-07");
        test("1997");
        test(new Date());
    }

}