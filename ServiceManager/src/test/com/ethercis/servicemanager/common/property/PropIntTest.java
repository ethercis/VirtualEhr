//Copyright
package com.ethercis.servicemanager.common.property;

import junit.framework.TestCase;

public class PropIntTest extends TestCase {

    /** java org.xmlBlaster.common.property.PropInt */
    public void testPropInt() {
        PropInt lifeTime = new PropInt("lifeTime", 123456);
        System.out.println(lifeTime.toXml());
    }

}