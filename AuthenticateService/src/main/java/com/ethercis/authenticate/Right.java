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
//Copyright
package com.ethercis.authenticate;

import com.ethercis.servicemanager.common.security.I_Right;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.math.BigInteger;



@XmlRootElement
@XmlType(name = "Right", propOrder = { "name", "mask" })
public class Right implements Serializable, I_Right {

	private static final long serialVersionUID = 6560022242276409226L;
	private String name;
	private String mask;

	public Right() {
	}

	public Right(String name, String mask) {
		this.name = name;
		this.mask = mask;
	}

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
					.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	public static String byteArrayToHexString(byte[] b) {
		int len = b.length;
		String data = new String();

		for (int i = 0; i < len; i++) {
			data += Integer.toHexString((b[i] >> 4) & 0xf);
			data += Integer.toHexString(b[i] & 0xf);
		}
		return data;
	}


	public byte[] getMaskByte() {
		return hexStringToByteArray(mask);
	}

	/* (non-Javadoc)
	 * @see com.ethercis.party.model.I_Right#getMaskBI()
	 */
	@Override
	public BigInteger getMaskBI() {
		if (mask == null)
			return BigInteger.ZERO;
		return new BigInteger(getMaskByte());
	}

	/* (non-Javadoc)
	 * @see com.ethercis.party.model.I_Right#getUserId()
	 */
	@Override
	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}



	public String getMask() {
		return mask;
	}


	public void setMask(String mask) {
		this.mask = mask;
	}

	/**
	 * returns the bit mask associated to a right
	 * <p>
	 * 
	 * @param r
	 *            a right
	 * @return BigInteger bit mask
	 */
	public static BigInteger getMask(I_Right r) {
		byte[] ba = r.getMaskByte();
		return new BigInteger(ba);
	}

	@Override
	public String toString() {
		return "Right [name=" + name + ", maskr=" + mask + "]";
	}
	
	public static void main(String[] args) {
		System.out.println(new BigInteger(hexStringToByteArray("0000000000010000")));
		System.out.println(byteArrayToHexString(hexStringToByteArray("0000000000010000")));
		System.out.println(byteArrayToHexString(new BigInteger("65536").toByteArray()));
		
	}

}
