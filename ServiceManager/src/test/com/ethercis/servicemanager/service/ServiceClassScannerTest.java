//Copyright
package com.ethercis.servicemanager.service;

import com.ethercis.servicemanager.annotation.Service;
import junit.framework.TestCase;

import java.util.List;

public class ServiceClassScannerTest extends TestCase {

    public void testScan() {
        ServiceClassScanner scanner=new ServiceClassScanner();
        List<Class<Service>> classes=scanner.getServiceClasses("com.ethercis");
        for(Class<Service> clazz:classes){
            System.out.println(clazz);
        }
    }

}