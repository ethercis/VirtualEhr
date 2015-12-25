/*
 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ethercis.servicemanager.common;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

/*
 * You can now use also javax.mail.internet.MimeUtility
 * and sun.misc.BASE64Encoder.encode.
 * There is a non-public class in Java 1.4+ called java.common.prefs.Base64
 * 
 * CAUTION: Fails on ARM BigEndian like Android Phones, works fine for Intels Little Endian
 */
public class Base64 {

    static final char[] charTab = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray (); 

    
    public static String encode (byte [] data) {
        return encode (data, 0, data.length, null).toString ();
    }


    /** Encodes the part of the given byte array denoted by start and
        len to the Base64 format.  The encoded data is appended to the
        given StringBuffer. If no StringBuffer is given, a new one is
        created automatically. The StringBuffer is the return value of
        this method. */
 

    public static StringBuffer encode (byte [] data, int start, int len, StringBuffer buf) {

        if (buf == null) 
            buf = new StringBuffer (data.length * 3 / 2);

        int end = len - 3;
        int i = start;
        int n = 0;

        while (i <= end) {
            int d = (((data [i]) & 0x0ff) << 16) 
                | (((data [i+1]) & 0x0ff) << 8)
                | ((data [i+2]) & 0x0ff);

            buf.append (charTab [(d >> 18) & 63]);
            buf.append (charTab [(d >> 12) & 63]);
            buf.append (charTab [(d >> 6) & 63]);
            buf.append (charTab [d & 63]);

            i += 3;

            if (n++ >= 14) {
                n = 0;
                buf.append ("\r\n");
            }
        }


        if (i == start + len - 2) {
            int d = (((data [i]) & 0x0ff) << 16) 
                | (((data [i+1]) & 255) << 8);

            buf.append (charTab [(d >> 18) & 63]);
            buf.append (charTab [(d >> 12) & 63]);
            buf.append (charTab [(d >> 6) & 63]);
            buf.append ("=");
        }
        else if (i == start + len - 1) {
            int d = ((data [i]) & 0x0ff) << 16;

            buf.append (charTab [(d >> 18) & 63]);
            buf.append (charTab [(d >> 12) & 63]);
            buf.append ("==");
        }

        return buf;
    }


    static int decode (char c) {
        if (c >= 'A' && c <= 'Z') 
            return c - 65;
        else if (c >= 'a' && c <= 'z') 
            return c - 97 + 26;
        else if (c >= '0' && c <= '9')
            return c - 48 + 26 + 26;
        else switch (c) {
        case '+': return 62;
        case '/': return 63;
        case '=': return 0;
        default:
            throw new RuntimeException (new StringBuffer("unexpected code: ").append(c).toString());
        }
    }

    public static byte [] decode (byte[] b) {
    	try {
			return decode(new String(b, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return decode(new String(b));
		}
    }

    /** Decodes the given Base64 encoded String to a new byte array. 
     *  The byte array holding the decoded data is returned.
     *  @return never null
     */
    public static byte [] decode (String s) {
    	if (s == null)
    		return new byte[0];
        int i = 0;
        int len = s.length ();
        ByteArrayOutputStream bos = new ByteArrayOutputStream (len);
        
        while (true) { 
            while (i < len && s.charAt (i) <= ' ') i++;

            if (i == len) break;

            int tri = (decode (s.charAt (i)) << 18)
                + (decode (s.charAt (i+1)) << 12)
                + (decode (s.charAt (i+2)) << 6)
                + (decode (s.charAt (i+3)));
            
            bos.write ((tri >> 16) & 255);
            if (s.charAt (i+2) == '=') break;
            bos.write ((tri >> 8) & 255);
            if (s.charAt (i+3) == '=') break;
            bos.write (tri & 255);

            i += 4;
        }
        return bos.toByteArray ();
    }

}



