//Copyright
package com.ethercis.servicemanager.common.property;

import junit.framework.TestCase;

public class PropStringTest extends TestCase {


    /** java org.xmlBlaster.common.property.PropString */
    public void testString() {
        PropString dummy = new PropString("propName", null);
        System.out.println(dummy.toXml());
    }

}