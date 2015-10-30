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
package com.ethercis.servicemanager.common.interfaces.data;

import javax.xml.bind.annotation.XmlElement;

public interface I_OrganizationEmergencyContact {

	@XmlElement
	public abstract String getPhoneNumberExt();

	public abstract void setPhoneNumberExt(String phonenumberext);

	@XmlElement
	public abstract String getFaxNumber();

	public abstract void setFaxNumber(String faxnumber);

	@XmlElement
	public abstract String getEmail();

	public abstract void setEmail(String email);

	@XmlElement
	public abstract long getId();

	public abstract void setId(long id);

	@XmlElement
	public abstract int getPersonId();

	public abstract void setPersonId(int personid);

	@XmlElement
	public abstract String getAddressLine1();

	public abstract void setAddressLine1(String addressline1);

	@XmlElement
	public abstract String getAddressLine2();

	public abstract void setAddressLine2(String addressline2);

	@XmlElement
	public abstract String getStreetNo();

	public abstract void setStreetNo(String streetno);

	@XmlElement
	public abstract String getStreetNoSuffix();

	public abstract void setStreetNoSuffix(String streetnosuffix);

	@XmlElement
	public abstract String getStreetName();

	public abstract void setStreetName(String streetname);

	@XmlElement
	public abstract String getStreetType();

	public abstract void setStreetType(String streetType);

	@XmlElement
	public abstract String getStreetDirectionId();

	public abstract void setStreetDirectionId(String streetdirectionid);

	@XmlElement
	public abstract String getMinorMunicipality();

	public abstract void setMinorMunicipality(String minorMunicipality);

	@XmlElement
	public abstract String getMajorMunicipality();

	public abstract void setMajorMunicipality(String majorMunicipality);

	@XmlElement
	public abstract String getPostalCode();

	public abstract void setPostalCode(String postalcode);

	@XmlElement
	public abstract String getGoverningDistrict();

	public abstract void setGoverningDistrict(String governingdistrict);

	@XmlElement
	public abstract String getCountryCd();

	public abstract void setCountryCd(String countrycd);

	@XmlElement
	public abstract String getMobilePhoneNumber();

	public abstract void setMobilePhoneNumber(String mobilephonenumber);

	@XmlElement
	public abstract String getBusinessMobilePhoneNumber();

	public abstract void setBusinessMobilePhoneNumber(
			String businessmobilephonenumber);

	@XmlElement
	public abstract String getEmail2();

	public abstract void setEmail2(String email2);

	@XmlElement
	public abstract String getTitle();

	public abstract void setTitle(String title);

	@XmlElement
	public abstract String getMothersMaidenName();

	public abstract void setMothersMaidenName(String mothersMaidenName);

	@XmlElement
	public abstract String getFamilyName();

	public abstract void setFamilyName(String familyName);

	@XmlElement
	public abstract String getMiddleName();

	public abstract void setMiddleName(String middleName);

	@XmlElement
	public abstract String getGivenName();

	public abstract void setGivenName(String givenName);

	@XmlElement
	public abstract String getGender();

	public abstract void setGender(String gender);

	@XmlElement
	public abstract String getRelationship();

	public abstract void setRelationship(String relationship);

}