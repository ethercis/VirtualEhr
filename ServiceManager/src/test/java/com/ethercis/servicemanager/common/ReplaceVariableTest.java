//Copyright
package com.ethercis.servicemanager.common;

import junit.framework.TestCase;

public class ReplaceVariableTest extends TestCase {

    /**
     * Method for testing only.
     * <br />
     * Invoke:
     * java org.xmlBlaster.common.ReplaceVariable -template 'Hello ${A} and ${B}, ${A${B}}' -A ZZXX -B OO -AOO OK
     * java org.xmlBlaster.common.ReplaceVariable -template 'Hello ${A} and ${B}, ${A${B}}' -A ZZXX -B OO -AOO OK
     * java org.xmlBlaster.common.ReplaceVariable -startToken @ -endToken @ -template '@A@' -A aa -B bb -Abb WRONG -aabb OK
     */
    //TODO: complete test
    public void testReplace() {
//        String template = "Hello ${A} and ${B}, ${A${B}}";
//        String startToken = "${";
//        String endToken = "}";
//        for (int i=0; i<args.length-1; i++) { // Add all "-key value" command line
//            String key = args[i].substring(1);
//            if (key.equals("template"))
//                template = args[i+1];
//            else if (key.equals("startToken"))
//                startToken = args[i+1];
//            else if (key.equals("endToken"))
//                endToken = args[i+1];
//            else
//                System.setProperty(key, args[i+1]);
//            i++;
//        }
//        System.out.println("Using startToken=" + startToken + " and endToken=" + endToken);
//
//        ReplaceVariable r = new ReplaceVariable(startToken, endToken);
//        String result = r.replace(template,
//                new I_ReplaceVariable() {
//                    public String get(String key) {
//                        return System.getProperty(key);
//                    }
//                });
//        System.out.println("INPUT : '" + template + "'");
//        System.out.println("OUTPUT: '" + result + "'");
    }

}