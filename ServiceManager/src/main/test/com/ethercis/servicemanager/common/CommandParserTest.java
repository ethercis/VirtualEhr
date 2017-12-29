//Copyright
package com.ethercis.servicemanager.common;

import junit.framework.TestCase;

public class CommandParserTest extends TestCase {

    public void testParse(){
        CommandParser stest = new CommandParser();
        stest.parse("<usr><A type='1' value='val1'>asdad</A><B>nvb/xmv/bx</B><C type='tC1'>valC1</C><C type='tC2'>valC2</C></usr>");
        System.out.println(stest.dump());
        String bval = stest.getElementValue("B", 0);
        System.out.println("B-VAL:"+ bval);
        for (int i = 0; i < stest.getOccurences("C"); i++){
            bval = stest.getElementValue("C", i);
            System.out.println("C-VAL:"+bval);
        }
    }

}