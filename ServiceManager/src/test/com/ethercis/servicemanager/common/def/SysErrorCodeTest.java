//Copyright
package com.ethercis.servicemanager.common.def;

import com.ethercis.servicemanager.common.FileLocator;
import junit.framework.TestCase;

public class SysErrorCodeTest extends TestCase {

    /**
     * Generate a requirement file for all error codes.
     * Used by build.xml, change with care!
     * <pre>
     *  java ....def.ErrorCode <HtmlFileName>
     *  java ....def.ErrorCode verifySerialization
     *  java ....def.ErrorCode toPlainList
     * </pre>
     */
    public static void main(String [] args) {
        String file = "doc/requirements/admin.errorcodes.listing.xml";
        if (args.length > 0) {
            file = args[0];
        }
        if ("verifySerialization".equals(file)) {
            SysErrorCode.verifySerialization();
        }
        else if ("toPlainList".equals(file)) {
            System.out.println(SysErrorCode.toPlainList());
        }
        else {
            String req = SysErrorCode.toRequirement();
            try {
                FileLocator.writeFile(file, req);
                System.out.println("Created requirement file '" + file + "'");
            }
            catch (Exception e) {
                System.out.println("Writing file '" + file + "' failed: " + e.toString());
            }
        }
    }

}