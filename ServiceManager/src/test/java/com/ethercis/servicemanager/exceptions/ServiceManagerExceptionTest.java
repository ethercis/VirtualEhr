//Copyright
package com.ethercis.servicemanager.exceptions;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import junit.framework.TestCase;

public class ServiceManagerExceptionTest extends TestCase {

    /**
     */
    public void testSME() {
        RunTimeSingleton glob = RunTimeSingleton.instance().getClone(new String[] {});
        ServiceManagerException e = new ServiceManagerException(glob, SysErrorCode.INTERNAL, "LOC", "Bla bla");
        System.out.println(e.toXml());
        byte[] serial = e.toByteArr();
        System.out.println("\n" + new String(serial));
        ServiceManagerException back = ServiceManagerException.parseByteArr(glob, serial);
        System.out.println("BACK\n" + back.toXml());
        System.out.println("\ngetMessage:\n" + back.getMessage());

        e = new ServiceManagerException(glob, SysErrorCode.INTERNAL_UNKNOWN, "LOC", "Bla bla");
        System.out.println("\ngetMessage:\n" + e.getMessage());

        e = ServiceManagerException.convert(glob, null, null, new IllegalArgumentException("wrong args"));
        System.out.println("\ngetMessage:\n" + e.getMessage());
    }


}