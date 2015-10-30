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

public interface I_OrganizationCareteam {

	@XmlElement
	public abstract String getPhoneNumberExt();

	@XmlElement
	public abstract String getFaxNumber();

	@XmlElement
	public abstract String getEmail();

	@XmlElement
	public abstract long getId();

	@XmlElement
	public abstract int getPersonId();

	@XmlElement
	public abstract String getAddressLine1();

	@XmlElement
	public abstract String getAddressLine2();

	@XmlElement
	public abstract String getStreetNo();

	@XmlElement
	public abstract String getStreetNoSuffix();

	@XmlElement
	public abstract String getStreetName();

	@XmlElement
	public abstract String getStreetType();

	@XmlElement
	public abstract String getStreetDirectionId();

	@XmlElement
	public abstract String getMinorMunicipality();

	@XmlElement
	public abstract String getMajorMunicipality();

	@XmlElement
	public abstract String getPostalCode();

	@XmlElement
	public abstract String getGoverningDistrict();

	@XmlElement
	public abstract String getCountryCd();

	@XmlElement
	public abstract String getMobilePhoneNumber();

	@XmlElement
	public abstract String getBusinessMobilePhoneNumber();

	@XmlElement
	public abstract String getEmail2();

	@XmlElement
	public abstract String getTitle();

	@XmlElement
	public abstract String getMothersMaidenName();

	@XmlElement
	public abstract String getFamilyName();

	@XmlElement
	public abstract String getMiddleName();

	@XmlElement
	public abstract String getGivenName();

	@XmlElement
	public abstract String getGender();

	@XmlElement
	public abstract String getRelationship();

}