//Copyright
package com.ethercis.servicemanager.service;

import com.ethercis.servicemanager.annotation.Service;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import junit.framework.TestCase;

import java.util.List;

public class ServiceClassScannerTest extends TestCase {

    public void testScan() throws ServiceManagerException {
        RunTimeSingleton global = RunTimeSingleton.instance();
        ServiceClassScanner scanner=new ServiceClassScanner();
        String serviceClasses = global.getProperty().get(I_ServiceRunMode.SERVER_SERVICE_CLASS_DEF, "com.ethercis");
        List<Class<Service>> classes=scanner.getServiceClasses(global, serviceClasses);
        for(Class<Service> clazz:classes){
            System.out.println(clazz);
        }
    }

}