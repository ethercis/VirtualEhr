//Copyright
package com.ethercis.servicemanager.common.def;

import junit.framework.TestCase;

public class MethodNameTest extends TestCase {


    /**
     */
    public void testMethodName () {
        try {
            MethodName.toMethodName((String)null);
            System.out.println("null should not return");
        }
        catch (Throwable e) {
            System.out.println("ERROR: " + e.toString());
        }
        MethodName methodName = MethodName.toMethodName("UpDaTe");
        System.out.println("OK: " + methodName);
    }

}