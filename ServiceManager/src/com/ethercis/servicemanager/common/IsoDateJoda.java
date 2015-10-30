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

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;

/**
 * Some Joda time utitility methods, depends on lib/joda-time.jar
 * http://www.w3.org/TR/1998/NOTE-datetime-19980827
 */
public class IsoDateJoda {
   
   /**
    * Calculate the difference from the given time to now. 
    * ISO 8601 states: Durations are represented by the format P[n]Y[n]M[n]DT[n]H[n]M[n]S
    * @param utc Given time, e.g. "1997-07-16T19:20:30.45+01:00"
    * @return The ISO 8601 Period like "P3Y6M4DT12H30M17S"
    */
   public static String getDifferenceToNow(String utc) {
	   if (utc == null) return "";
	   utc = ReplaceVariable.replaceAll(utc, " ", "T");
	   DateTime now = new DateTime();
	   DateTimeFormatter f = ISODateTimeFormat.dateTimeParser();
	   DateTime other = f.parseDateTime(utc);
	   Period period = new Period(other, now); // Period(ReadableInstant startInstant, ReadableInstant endInstant)
	   return period.toString();
   }

   /**
    * Calculate the difference of given millis.
    * @param diffMillis The elapsed time
    * @param trimDateIfPossible true:
    * <pre> 
    * 3000->00:00:03
    * 380000->00:06:20
    * 5692439078->1581:13:59.078
    * </pre>
    * false: 
    * <pre> 
	*   3000->P0000-00-00T00:00:03
	*   5692439078->P0000-00-00T1581:13:59.078
    * </pre>
    * 
    * @return The ISO 8601 Period like "P3Y6M4DT12H30M17S"
    */
   public static String getDifference(long diffMillis, boolean trimDateIfPossible) {
	   if (diffMillis == 0) return "";
	   Period period = new Period(diffMillis);
	   /*
	   PeriodFormatter myFormatter = new PeriodFormatterBuilder()
	     .printZeroAlways()
	     .appendYears()
	     .appendSuffix(" year", " years")
	     .appendSeparator(" and ")
	     .appendMonths()
	     .appendSuffix(" month", " months")
	     .toFormatter();
	   */
	   
	   /*if (true) */{
	//	   3000->P0000-00-00T00:00:03
	//	   5692439078->P0000-00-00T1581:13:59.078
	   PeriodFormatter formatter = ISOPeriodFormat.alternateExtended();
	   String periodStr = formatter.print(period);
	   if (trimDateIfPossible) {
		   if (periodStr.startsWith("P0000-00-00T"))
			   periodStr = periodStr.substring("P0000-00-00T".length());
	   }
	   return periodStr;
	   }
	   /*
	   else {
	   // 3000->PT3S
	   // 5692439078->PT1581H13M59.078S
	   return period.toString();
	   }
	   */
   }


}

