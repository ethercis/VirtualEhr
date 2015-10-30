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

/*
This code is a refactoring and adaptation of the original
work provided by the XmlBlaster project (see http://xmlblaster.org)
for more details.
This code is therefore supplied under LGPL 2.1
 */

/**
 * Project: EtherCIS system application
 * 
 * @author <a href="mailto:christian@adoc.co.th">Christian Chevalley</a>
 * @author <a href="mailto:michele@laghi.eu">Michele Laghi</a>
 * @author <a href="mailto:xmlblast@marcelruff.info">Marcel Ruff</a>
 */


package com.ethercis.servicemanager.common;

/**
 * Same logonservice StringBuffer but has the additional method appendEscaped() which
 * escapes predefined XML identities.
 */
public class XmlBuffer {
        private StringBuffer buf;
        public XmlBuffer(int len) {
                this.buf = new StringBuffer(len);
        }

    /**
     * Escape predefined xml entities (&, <, >, ', ").
     * Additionally the '\0' is escaped.
     * @param text
     * @return The escaped text is appended to the StringBuffer.
     */
        public XmlBuffer appendEscaped(String text) {
                append(this.buf, text);
                return this;
        }

        /**
         * Escape predefined xml entities (', ", \r) for attributes.
         * Additionally the '\0' is escaped.
         * @param text
         * @return The escaped text is appended to the StringBuffer.
         */
            public XmlBuffer appendAttributeEscaped(String text) {
                    appendAttr(this.buf, text);
                    return this;
            }

        /**
         * Appends a tag name (e.g. "bla" of a tag called <bla>).
         * Currently is a normal append()
         * @param tagName Could in future escape invalid tokens  '<' and '&' in a tag name.
         * @return
         */
        public XmlBuffer appendTag(String tagName) {
                this.buf.append(tagName);
                return this;
        }

        /**
         * Sorround string with CDATA
         * @param str
         * @return
         */
        public XmlBuffer appendCdataEscaped(String str) {
        	this.buf.append("<![CDATA[").append(str).append("]]>");
        	return this;
        }

        /**
         * Supports only "," -> "&comma;" and ";" -> "%3B" 
         * @param text "bla,bla"
         * @param separator ','
         * @return "bla&comma;bla"
         */
        public XmlBuffer appendCsvEscaped(String text, char separator) {
            if (text == null) return this;
            int length = text.length();
            for (int i = 0; i < length; i++) {
                char c = text.charAt(i);
                if (c == separator) {
	                switch (separator) {
	                    case ';':
	            			// ';' is escaped logonservice %3B (logonservice in URL) and HTML logonservice &#59; (which unfortunately contains again a ;)
	                        buf.append("%3B");
	                        break;
	                    case ',':
                            buf.append("&comma;");
	                        break;
	                    default:
                            buf.append(c);
	                }
                }
                else {
                    buf.append(c);
                }
            }
        	return this;
        }

        /**
         * Aquivalent to a StringBuffer.append().
         */
        public XmlBuffer append(String str) {
                this.buf.append(str);
                return this;
        }

        /**
         * Aquivalent to a StringBuffer.append().
         */
        public XmlBuffer append(long ln) {
                this.buf.append(ln);
                return this;
        }

        /**
         * Aquivalent to a StringBuffer.append().
         */
        public XmlBuffer append(float ln) {
                this.buf.append(ln);
                return this;
        }

        /**
         * Aquivalent to a StringBuffer.append().
         */
        public XmlBuffer append(double ln) {
                this.buf.append(ln);
                return this;
        }

        /**
         * Aquivalent to a StringBuffer.append().
         */
        public XmlBuffer append(boolean b){
                this.buf.append(b);
                return this;
        }
        
        public StringBuffer getRawBuffer() {
        	return this.buf;
        }
        
        public int length() {
        	return this.buf.length();
        }
        
        /**
         * Removes all buffer entries.
         * Calling append fills new data to the beginning of the buffer. 
         */
        public void reset() {
        	this.buf.setLength(0);
        }

        public String toString() {
                return this.buf.toString();
        }

        // 5 predefined XML entities plus some extra escapers
    private static final char[] AMP = "&amp;".toCharArray();
    private static final char[] LT = "&lt;".toCharArray();
    private static final char[] GT = "&gt;".toCharArray();
    private static final char[] QUOT = "&quot;".toCharArray();
    private static final char[] APOS = "&apos;".toCharArray();

    private static final char[] SLASH_R = "&#x0D;".toCharArray();
    private static final char[] NULL = "&#x0;".toCharArray();

    /**
     * Escape predefined xml entities (&, <, >, ', ").
     * Additionally the '\0' is escaped.
     * @param text e.g. "Hello < and &"
     * @return "Hello &lt; and &amp;"
     */
    public static String escape(String text) {
        if (text == null || text.length() < 1)
                return text;
        StringBuffer buf = new StringBuffer((int)(text.length()*1.2));
        append(buf, text);
        return buf.toString();
    }

    /**
     * Escape predefined xml entities (&, <, >, ', ").
     * Additionally the '\0' is escaped.
     * @param text
     * @return The escaped text is appended to the given StringBuffer.
     */
    public static void append(StringBuffer buf, String text) {
        if (text == null) return;
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\0':
                    buf.append(NULL);
                    break;
                case '&':
                        buf.append(AMP);
                    break;
                case '<':
                        buf.append(LT);
                    break;
                case '>':
                        buf.append(GT);
                    break;
                case '"':
                        buf.append(QUOT);
                    break;
                case '\'':
                        buf.append(APOS);
                    break;
                case '\r':
                        buf.append(SLASH_R);
                    break;
                default:
                        buf.append(c);
            }
        }
    }
    /**
     * Escape predefined xml entities (\0, ', ", \r). for attribute notation
     * Additionally the '\0' is escaped.
     * @param text
     * @return The escaped text is appended to the given StringBuffer.
     */
    public static void appendAttr(StringBuffer buf, String text) {
        if (text == null) return;
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\0':
                    buf.append(NULL);
                    break;
                case '&':
                        buf.append(AMP);
                    break;
                case '<':
                        buf.append(LT);
                    break;
                /*
                case '>':
                        buf.append(GT);
                    break;
                */
                case '"':
                        buf.append(QUOT);
                    break;
                case '\'':
                        buf.append(APOS);
                    break;
                case '\r':
                        buf.append(SLASH_R);
                    break;
                default:
                        buf.append(c);
            }
        }
    }
    
    private final static boolean startsWith(String xml, char[] ch, int pos) {
       return xml.indexOf(new String(ch), pos) == pos;
    }
    
    public final static String unEscapeXml(String xml) {
       StringBuffer buf = new StringBuffer();
       int i, len;
       if (xml == null)
          return null;

       len = xml.length();
       
       for (i = 0; i < len; i++) {
          char ch = xml.charAt(i); 
          if (ch != '&') {
             buf.append(ch);
             continue;
          }
          if (startsWith(xml, AMP, i)) {
             buf.append('&');
             i += AMP.length - 1;
          } 
          else if (startsWith(xml, LT, i)) {
             buf.append('<');
             i += LT.length - 1;
          } 
          else if (startsWith(xml, GT, i)) {
             buf.append('>');
             i +=  GT.length - 1;
          } 
          else if (startsWith(xml, QUOT, i)) {
             buf.append('"');
             i += QUOT.length - 1;
          } 
          else if (startsWith(xml, APOS, i)) {
             buf.append('\'');
             i += APOS.length - 1;
          } 
          else if (startsWith(xml, SLASH_R, i)) {
             buf.append('\r');
             i += SLASH_R.length - 1;
          } 
          else if (startsWith(xml, NULL, i)) {
             buf.append('\0');
             i += NULL.length - 1;
          }
       }
       return buf.toString();
    }

}
