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

public interface I_OrganizationCountry {

	@XmlElement
	public abstract Integer getCountryCd();

	public abstract void setCountryCd(Integer countryCd);

	@XmlElement
	public abstract String getCountryName();

	public abstract void setCountryName(String countryName);

	@XmlElement
	public abstract String getCountryFormalName();

	public abstract void setCountryFormalName(String countryFormalName);

	@XmlElement
	public abstract String getCountryAlpha2Code();

	public abstract void setCountryAlpha2Code(String countryAlpha2Code);

	@XmlElement
	public abstract String getCountryAlpha3Code();

	public abstract void setCountryAlpha3Code(String countryAlpha3Code);

	@XmlElement
	public abstract Integer getCountryIsoNumericalCode();

	public abstract void setCountryIsoNumericalCode(
			Integer countryIsoNumericalCode);

	@XmlElement
	public abstract String getCountryTelephoneCode();

	public abstract void setCountryTelephoneCode(String countryTelephoneCode);

	@XmlElement
	public abstract String getCountryCodeTLD();

	public abstract void setCountryCodeTLD(String countryCodeTLD);

}