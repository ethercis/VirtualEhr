//Copyright
package com.ethercis.servicemanager.common;

import junit.framework.TestCase;

public class jcryptTest extends TestCase {
    //jcrypt.main(new String[]{"yZ","demo123"});
    public void testCrypt()
    {
        String[] args = new String[]{"yZ","demo123"};
        if(args.length == 2)
        {
            System.out.println
                    (
                            "[" + args[0] + "] [" + args[1] + "] => [" +
                                    jcrypt.crypt(args[0], args[1]) + "]"
                    );
        }
        else if(args.length == 3)
        {
            String salt = args[1];
            String pepper = args[2];
            System.out.println(jcrypt.crypt(salt, pepper));
        }
        else {
            System.out.println("Usage:\njava ....jcrypt <salt> <password>");
        }
    }

}