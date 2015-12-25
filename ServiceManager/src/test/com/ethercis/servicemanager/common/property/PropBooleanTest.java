//Copyright
package com.ethercis.servicemanager.common.property;

import junit.framework.TestCase;

public class PropBooleanTest extends TestCase {

    public void testPropBooleanTest() {
        PropBoolean forceDestroy = new PropBoolean("forceDestroy", true);
        System.out.println(forceDestroy.toXml());
    }

}