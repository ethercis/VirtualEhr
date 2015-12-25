//Copyright
package com.ethercis.servicemanager.common;

import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import junit.framework.TestCase;

public class Base64Test extends TestCase {

    public void setUp() throws Exception {
        super.setUp();

    }

    /**
     * java org.xmlBlaster.common.Base64 -file MyFile.jpg
     * java org.xmlBlaster.common.Base64 HelloWorld -> SGVsbG9Xb3JsZA==
     * java org.xmlBlaster.common.Base64 -decodeFile MyFile.base64
     * java org.xmlBlaster.common.Base64 -decode Q2lBOGEyVjVJRzlwWkQwblNHVnNiRzhuSUdOdmJuUmxiblJOYVcxbFBTZDBaWGgwTDNodGJDY2dZMjl1ZEdWdWRFMXBiV1ZGZUhSbGJtUmxaRDBuTVM0d0p6NEtJQ0E4YjNKbkxuaHRiRUpzWVhOMFpYSStQR1JsYlc4dE16NDhMMlJsYlc4dE16NDhMMjl5Wnk1NGJXeENiR0Z6ZEdWeVBnb2dQQzlyWlhrKw==
     * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
     */
    //TODO: complete test
    public void testEncode() throws ServiceManagerException {
//        if (args.length == 2) {
//            if (args[0].equals("-decode")) {
//                String base64 = args[1];
//                byte[] back = Base64.decode(base64);
//                System.out.println("Decoded to '" + new String(back) + "'");
//                return;
//            }
//            else if (args[0].equals("-decodeFile")) {
//                String fileName = args[1];
//                byte[] base64 = FileLocator.readFile(fileName);
//                byte[] back = Base64.decode(base64);
//                String fn = fileName+".bin";
//                FileLocator.writeFile(fn, back);
//                System.out.println("Decoded to file '" + fn + "'");
//                return;
//            }
//            else if (args[0].equals("-file")) {
//                String fileName = args[1];
//                byte[] bytes = FileLocator.readFile(fileName);
//                String base64 = Base64.encode(bytes);
//                System.out.print(base64);
//                return;
//            }
//         /* FileLocator is not known in J2ME:
//         if (args[0].equals("-fn")) {
//            try {
//               byte[] bb = FileLocator.readFile(args[1]);
//               String base64 = Base64.encode(bb);
//               System.out.println("Content of '" + args[1] + "' encoded to base64 '" + base64 + "'");
//               return;
//            }
//            catch (Exception e) {
//               e.printStackTrace();
//            }
//         }
//         */
//        }
//        {
//            String hello = args.length > 0 ? args[0] : "Hello World";
//            String base64 = Base64.encode(hello.getBytes());
//            byte[] back = Base64.decode(base64);
//            System.out.println("Before Base64 '" + hello + "' base64='" + (new String(base64)) + "' after '" + new String(back) + "'");
//        }
//        {
//            //javax.mail.internet.MimeUtility.decode(arg0, arg1)
//            //sun.misc.BASE64Encoder.encode.
//            //java.common.prefs.Base64
//        }
    }
}