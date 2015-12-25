//Copyright
package com.ethercis.servicemanager.common.property;

import junit.framework.TestCase;

public class PropDoubleTest extends TestCase {


    public void testPropDoubleTest() {
        PropDouble lifeTime = new PropDouble("lifeTime", 123456.789);
        System.out.println(lifeTime.toXml());
    }

}