//Copyright
package com.ethercis.servicemanager.common;

import junit.framework.TestCase;

public class IsoDateJodaTest extends TestCase {

    public void testDateJoda() {
        {
            String utc = "1997-07-16T19:20:30.45+01:00";
            String period = IsoDateJoda.getDifferenceToNow(utc);
            System.out.println("now - " + utc + " = " + period);
        }
        {
            String utc = "1997-07-16 19:20:30.45+01:00";
            String period = IsoDateJoda.getDifferenceToNow(utc);
            System.out.println("now - " + utc + " = " + period);
        }
        System.out.println("3000->" + IsoDateJoda.getDifference(3000, true));
        System.out.println("380000->" + IsoDateJoda.getDifference(380000, true));
        System.out.println("5692439078->" + IsoDateJoda.getDifference(5692439078L, true));
    }
}