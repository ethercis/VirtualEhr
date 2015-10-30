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
package com.ethercis.servicemanager.common.def;

import java.util.Hashtable;



/**
 * This class holds all method names to access POE. 
 *
 */
public final class MethodName implements java.io.Serializable, Comparable<Object>
{
	private static final long serialVersionUID = -6644144030401574462L;
	private final static Hashtable<String, MethodName> hash = new Hashtable<String, MethodName>(); // The key is the 'methodName' String and the value is an 'MethodName' instance
	private final String methodName;
	private final int argType;
	private final int returnType;

	private transient byte[] methodNameBytes; // for better performance in SOCKET protocol

	// The possible method return types, useful for SOCKET protocol (see requirement 'protocol.socket')
	public static final int RETURN_VOID = 0;
	public static final int RETURN_STRING = 1;
	public static final int RETURN_STRINGARR = 2;
	public static final int RETURN_PROPERTY = 3;
	public static final int RETURN_HTML = 4;
	public static final int RETURN_JSON = 5;
	public static final int RETURN_XML = 6;
	public static final int RETURN_BASE64 = 7;
	public static final int RETURN_XML_ARRAY = 8;
	public static final int RETURN_DYNA = 9;
	public static final int RETURN_UNDEFINED = -1;


	public static final String STR_RETURN_VOID = "Void";
	public static final String STR_RETURN_STRING = "String";
	public static final String STR_RETURN_STRINGARR = "StringArr";
	public static final String STR_RETURN_PROPERTY = "Property";
	public static final String STR_RETURN_HTML = "Html";
	public static final String STR_RETURN_JSON = "Json";
	public static final String STR_RETURN_XML = "Xml";
	public static final String STR_RETURN_XML_ARRAY = "XmlArray";
	public static final String STR_RETURN_BASE64 = "Base64";
	public static final String STR_RETURN_DYNA = "Dyna";
	public static final String STR_RETURN_UNDEFINED = "Undefined";	

	// The possible method argument types, useful for SOCKET protocol and persistence
	private static final int ARG_PROPERTY = 0;
	private static final int ARG_KEY = 1;
	private static final int ARG_MSGARR = 2;
	private static final int ARG_STR_MSGARR = 3;
	//private static final int ARG_MSG = 4;

    /*
       public final boolean isPublish() {
      return this.methodName == MethodName.PUBLISH;
   }

   public final boolean isSubscribe() {
      return this.methodName == MethodName.SUBSCRIBE;
   }

   public final boolean isUnSubscribe() {
      return this.methodName == MethodName.UNSUBSCRIBE;
   }

   public final boolean isErase() {
      return this.methodName == MethodName.ERASE;
   }

   public final boolean isGet() {
      return this.methodName == MethodName.GET;
   }

   public final boolean isUpdate() {
      return this.methodName == MethodName.UPDATE;
   }
     */

	public static final MethodName CONNECT = new MethodName("connect", ARG_PROPERTY, RETURN_PROPERTY);
	public static final MethodName DISCONNECT = new MethodName("disconnect", ARG_PROPERTY, RETURN_PROPERTY);
	public static final MethodName GET = new MethodName("get", ARG_PROPERTY, RETURN_PROPERTY);
	public static final MethodName POST = new MethodName("post", ARG_PROPERTY, RETURN_PROPERTY);
	public static final MethodName READ = new MethodName("read", ARG_PROPERTY, RETURN_PROPERTY);
	public static final MethodName EXECUTE = new MethodName("execute", ARG_PROPERTY, RETURN_PROPERTY);
	public static final MethodName CHANGE = new MethodName("change", ARG_PROPERTY, RETURN_PROPERTY);
	public static final MethodName DELETE = new MethodName("delete", ARG_PROPERTY, RETURN_PROPERTY);
	public static final MethodName EXTEND = new MethodName("extend", ARG_PROPERTY, RETURN_PROPERTY);
	public static final MethodName EXIST = new MethodName("exist", ARG_PROPERTY, RETURN_PROPERTY);
	public static final MethodName COMMUNICATE = new MethodName("communicate", ARG_PROPERTY, RETURN_PROPERTY);
	public static final MethodName QUERY = new MethodName("query", ARG_PROPERTY, RETURN_PROPERTY); 	
	public static final MethodName PING = new MethodName("ping", ARG_PROPERTY, RETURN_PROPERTY);
    public static final MethodName PUBLISH = new MethodName("publish", ARG_PROPERTY, RETURN_PROPERTY);
    public static final MethodName SUBSCRIBE = new MethodName("subscribe", ARG_PROPERTY, RETURN_PROPERTY);
    public static final MethodName UNSUBSCRIBE = new MethodName("unsubscribe", ARG_PROPERTY, RETURN_PROPERTY);
    public static final MethodName ERASE = new MethodName("erase", ARG_PROPERTY, RETURN_PROPERTY);
    public static final MethodName UPDATE = new MethodName("update", ARG_PROPERTY, RETURN_PROPERTY);
	public static final MethodName DUMMY = new MethodName("dummy", ARG_PROPERTY, RETURN_PROPERTY);
	public static final MethodName UNKNOWN = new MethodName("unknown", ARG_PROPERTY, RETURN_PROPERTY);
	public static final MethodName EXCEPTION = new MethodName("exception", ARG_PROPERTY, RETURN_PROPERTY);
	public static final MethodName VIEW = new MethodName("view", ARG_PROPERTY, RETURN_PROPERTY);
	public static final MethodName PUT = new MethodName("put", ARG_PROPERTY, RETURN_PROPERTY);
    public static final MethodName CREATE = new MethodName("create", ARG_PROPERTY, RETURN_PROPERTY);

    //EhrScape stuff
    public static final MethodName SESSION = new MethodName("session", ARG_PROPERTY, RETURN_PROPERTY);


	/**
	 * @exception IllegalArgumentException if the given methodName is null
	 */
	private MethodName(String methodName, int argType, int returnType) {
		if (methodName == null)
			throw new IllegalArgumentException("Your given methodName is null");
		this.methodName = methodName;
		this.argType = argType;
		this.returnType = returnType;
		hash.put(methodName, this);
	}

	public static MethodName[] getAll() {
		return new MethodName[] { CONNECT, DISCONNECT, GET, POST, READ, EXECUTE, CHANGE, DELETE, EXTEND, EXIST, COMMUNICATE, QUERY,
				PING, VIEW, PUT, SESSION, CREATE /*, DUMMY, UNKNOWN, EXCEPTION*/ };
	}


	/**
	 * @return the comma separated methodNames, never null 
	 */
	public static String toString(MethodName[] nameArr) {
		if (nameArr == null || nameArr.length < 1) return "";
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<nameArr.length; i++) {
			if (i>0) sb.append(",");
			sb.append(nameArr[i]);
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "MethodName [methodName=" + methodName + ", argType=" + argType
				+ ", returnType=" + returnType + "]";
	}

	/**
	 * Returns the methodName. 
	 */
	public String getMethodName() {
		return this.methodName;
	}

	public boolean isConnect() {
		return this == MethodName.CONNECT || this.methodName.equals(MethodName.CONNECT);
	}

	public boolean isDisconnect() {
		return this == MethodName.DISCONNECT || this.methodName.equals(MethodName.DISCONNECT);
	}


	public boolean isGet() {
		return this == MethodName.GET || this.methodName.equals(MethodName.GET);
	}

	public boolean isChange() {
		return this == MethodName.CHANGE || this.methodName.equals(MethodName.CHANGE);
	}

	public boolean isExtend() {
		return this == MethodName.EXTEND || this.methodName.equals(MethodName.EXTEND);
	}

	public boolean isErase() {
    return this == MethodName.DELETE || this.methodName.equals(MethodName.DELETE);
}

    public boolean isPublish() {
        return this == MethodName.PUBLISH || this.methodName.equals(MethodName.PUBLISH);
    }

    public boolean isSubscribe() {
        return this == MethodName.SUBSCRIBE || this.methodName.equals(MethodName.SUBSCRIBE);
    }

    public boolean isUnsubscribe() {
        return this == MethodName.UNSUBSCRIBE || this.methodName.equals(MethodName.UNSUBSCRIBE);
    }

    public boolean isUpdate() {
        return this == MethodName.UPDATE || this.methodName.equals(MethodName.UPDATE);
    }


    public boolean isExist() {
		return this == MethodName.EXIST || this.methodName.equals(MethodName.EXIST);
	}
	
	public boolean isView() {
		return this == MethodName.VIEW || this.methodName.equals(MethodName.VIEW);
	}
	
	public boolean isExecute() {
		return this == MethodName.EXECUTE || this.methodName.equals(MethodName.EXECUTE);
	}

    public boolean isSession() {
        return this == MethodName.SESSION || this.methodName.equals(MethodName.SESSION);
    }

    public boolean isCreate() {
        return this == MethodName.CREATE || this.methodName.equals(MethodName.CREATE);
    }

    /**
	 * When you compare two methodName with == and they are
	 * loaded by different Classloaders it will fail (return false even
	 * if they are the same method), using
	 * this equals() method is safe under such circumstances
	 */
	public boolean equals(MethodName other) {
		if (other == null) return false;
		if (this == other) return true; // same classloader
		return getMethodName().equals(other.getMethodName());
		/*
         Class local = MethodName.class;
         Class other = reference.getReferencedObject().getClass();
         System.err.println( "LOCAL: " + System.identityHashCode( local ) );
         System.err.println( "other: " + System.identityHashCode( other ) );

         URL localURL = local.getProtectionDomain().getCodeSource().getLocation();
         URL otherURL = other.getProtectionDomain().getCodeSource().getLocation();
         System.err.println( "LOCAL-URL: " + localURL );
         System.err.println( "other-URL: " + otherURL );
		 */
	}

	/**
	 * When you compare two methodName with == and they are
	 * loaded by different Classloaders it will fail (return false even
	 * if they are the same method), using
	 * this equals() method is safe under such circumstances
	 */
	public boolean equals(String other) {
		return getMethodName().equals(other);
	}

	// For TreeSet
	public int compareTo(Object other) {
		return getMethodName().compareTo(((MethodName)other).getMethodName());
	}

	/**
	 * For better performance in SOCKET protocol. 
	 * @return methodName dumped to a byte[]
	 */
	public byte[] getMethodNameBytes() {
		if (this.methodNameBytes == null) {
			this.methodNameBytes = this.methodName.getBytes();
		}
		return this.methodNameBytes;
	}

	public boolean wantsQosArg() {
		return this.argType == ARG_PROPERTY;
	}

	public boolean wantsKeyQosArg() {
		return this.argType == ARG_KEY;
	}

	public boolean wantsMsgArrArg() {
		return this.argType == ARG_MSGARR;
	}

	public boolean wantsStrMsgArrArg() {
		return this.argType == ARG_STR_MSGARR;
	}

	public boolean returnsVoid() {
		return this.returnType == RETURN_VOID;
	}

	public boolean returnsString() {
		return this.returnType == RETURN_STRING;
	}

	public boolean returnsStringArr() {
		return this.returnType == RETURN_STRINGARR;
	}

	public boolean returnsBase64() {
		return this.returnType == RETURN_BASE64;
	}

	public boolean returnsHtml() {
		return this.returnType == RETURN_HTML;
	}

	public boolean returnsXml() {
		return this.returnType == RETURN_XML;
	}

	public boolean returnsXmlArray() {
		return this.returnType == RETURN_XML_ARRAY;
	}
	
	
	public boolean returnsJson() {
		return this.returnType == RETURN_JSON;
	}

	public boolean returnsProperty() {
		return this.returnType == RETURN_PROPERTY;
	}

	public boolean returnsDyna() {
		return this.returnType == RETURN_DYNA;
	}


	public boolean returnsUndefined() {
		return this.returnType == RETURN_UNDEFINED;
	}
	
	//some usefull string to constants mapping
	public static String returnTypeAsString(int retcode){
		switch (retcode) {
		case RETURN_BASE64:
			return STR_RETURN_BASE64;
		case RETURN_HTML:
			return STR_RETURN_HTML;
		case RETURN_JSON:
			return STR_RETURN_JSON;
		case RETURN_PROPERTY:
			return STR_RETURN_PROPERTY;
		case RETURN_STRING:
			return STR_RETURN_STRING;
		case RETURN_STRINGARR:
			return STR_RETURN_STRINGARR;
		case RETURN_VOID:
			return STR_RETURN_VOID;
		case RETURN_XML:
			return STR_RETURN_XML;
		case RETURN_XML_ARRAY:
			return STR_RETURN_XML_ARRAY;
		case RETURN_DYNA:
			return STR_RETURN_DYNA;
		default:
			return STR_RETURN_UNDEFINED;
		}
	}
	
	public static int returnTypeAsInt(String retcode){
		if (retcode.equals(STR_RETURN_BASE64))
			return RETURN_BASE64;
		if (retcode.equals(STR_RETURN_HTML))
			return RETURN_HTML;
		if (retcode.equals(STR_RETURN_JSON))
			return RETURN_JSON;
		if (retcode.equals(STR_RETURN_PROPERTY))
			return RETURN_PROPERTY;
		if (retcode.equals(STR_RETURN_STRING))
			return RETURN_STRING;
		if (retcode.equals(STR_RETURN_STRINGARR))
			return RETURN_STRINGARR;
		if (retcode.equals(STR_RETURN_VOID))
			return RETURN_VOID;
		if (retcode.equals(STR_RETURN_XML))
			return RETURN_XML;
		if (retcode.equals(STR_RETURN_XML_ARRAY))
			return RETURN_XML_ARRAY;
		if (retcode.equals(STR_RETURN_DYNA))
			return RETURN_DYNA;		
		return RETURN_UNDEFINED;
	}

	/**
	 * Returns the MethodName object for the given String. 
	 * @param methodName The String code to lookup
	 * @return The enumeration object for this methodName
	 * @exception IllegalArgumentException if the given methodName is invalid
	 */
	public static final MethodName toMethodName(String methodName) throws IllegalArgumentException {
		if (methodName == null)
			throw new IllegalArgumentException("MethodName: A 'null' methodName is invalid");
		Object entry = hash.get(methodName);
		if (entry != null)
			return (MethodName)entry;

		// 2. try case insensitive: Buggy logonservice it does not work for "unSubscribe" with big letter 'S'
		methodName = methodName.toLowerCase();
		entry = hash.get(methodName);
		if (entry == null)
			throw new IllegalArgumentException("MethodName: The given methodName=" + methodName + " is unknown");
		return (MethodName)entry;
	}

	public static final MethodName toMethodName(byte[] methodNameBytes) throws IllegalArgumentException {
		return toMethodName(Constants.toUtf8String(methodNameBytes)); // tuning possible by doing it ourself?
	}

	///////////////
	// This code is a helper for serialization so that after
	// deserial the check
	//   MethodName.PUBLISH == instance
	// is still usable (the singleton is assured when deserializing)
	public Object writeReplace() throws java.io.ObjectStreamException {
		return new SerializedForm(this.getMethodName());
	}
	private static class SerializedForm implements java.io.Serializable {
		private static final long serialVersionUID = -4747332683196055305L;
		String methodName;
		SerializedForm(String methodName) { this.methodName = methodName; }
		Object readResolve() throws java.io.ObjectStreamException {
			return MethodName.toMethodName(methodName);
		}
	}
	///////////////END

}
