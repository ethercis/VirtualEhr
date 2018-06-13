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

import com.ethercis.ehrserver.definitions.x10.WeekPlan;

import java.util.BitSet;
import java.util.Calendar;

/**
 * Utility class to map events and activations in a Day<p>
 * The day plan consists of a bit map with a 1' resolution. That is,
 * a day plan is held into a bitmap of size 1440 (24*60). The mask is
 * set by setting to true a single time or a range. 
 * @author christian
 *
 */
public class DayPlanner {
	private BitSet mask;
	private static final int BITMAP_SIZE = 24*60 + 60;
	public static final int MAX = BITMAP_SIZE;
	public static final int MIN = 0;

	public DayPlanner(){
		 this.mask = new BitSet(BITMAP_SIZE);
	}
	
	public DayPlanner(BitSet m){
		this.mask = m;
	}
	
	public void setAbsoluteTime(int hour, int minute){
		try {
			int ofs = hour*60+minute;
			mask.set(ofs, true);
		} catch (Exception e){}; //swallow...
	}
	
	public BitSet getMask(){
		return mask;
	}
	
	public DayPlanner clone(){
		return new DayPlanner(this.mask);
	}
	/**
	 * check if a specified time is set
	 * @param h - hours (0 &lt= h &lt= 23) 
	 * @param m - minute (0 &lt= h &lt= 59)
	 * @return true if set, false if not set or bad parameters
	 */
	public boolean isSet(int h, int m){
		if (h < 0 || h > 23 || m < 0 || m > 59)
			return false;
		return isSet(time2offset(h,m));
	}
	
	public boolean isSet(int ofs){
		return mask.get(ofs);
	}
	/**
	 * set the bits in a specified range 
	 * @param ofs1 - start offset
	 * @param ofs2 - end offset
	 * @return false if parameters are inconsitents, true otherwise
	 */	
	public boolean setRange(int ofs1, int ofs2){
		if (ofs1 > ofs2) return false;
		
		mask.set(ofs1, ofs2+1);
		return true;
	}
	
	public boolean setRange(int h1, int m1, int h2, int m2){
		return setRange(time2offset(h1, m1), time2offset(h2, m2));
	}
	/**
	 * return the next set bit from location
	 * @param fromofs hr*min (ex. 17*60+30 to get next set bit from 17:30)
	 * @return hr*60+min ofs or -1 in none found
	 */
	public int getNextSet(int fromofs){
		return mask.nextSetBit(fromofs);
	}
	
	public int[] getNextSet(int h, int m){
		return offset2time(getNextSet(time2offset(h,m)));
	}
	/**
	 * return the next unset bit from location
	 * @param fromofs hr*min (ex. 17*60+30 to get next set bit from 17:30)
	 * @return hr*60+min ofs or -1 in none found
	 */
	public int getNextClear(int fromofs){
		return mask.nextClearBit(fromofs);
	}
	public int[] getNextClear(int h, int m){
		return offset2time(getNextClear(time2offset(h,m)));
	}
	
	public void clear(){
		mask.clear();
	}
//	some utilities to convert time from/to offset 	
	public static int time2offset(int hour, int minute){
		return hour*60 + minute;
	}
	
	public static int[] offset2time(int offset){
		int[] retints = new int[2];
		
		retints[0] = offset2hours(offset);
		retints[1] = offset2mins(offset);
		return retints;
	}
	
	public static int offset2hours(int offset){
		return (int)offset/60;
	}
	public static int offset2mins(int offset){
		return offset % 60;
	}
	
	public int cardinality(){
		return mask.cardinality();
	}
	
	public int size(){
		return mask.size();
	}
	
	/**
	 * returns the Day of Week object in WeekPlan for a day index<p>
	 * parameter dow follows the day indexing scheme of Calendar.
	 * @param dow day index
	 * @param p WeekPlan to get the DoW from
	 * @return  DoW definition or null if none
	 * @see Calendar
	 */
	public static com.ethercis.ehrserver.definitions.x10.DoW getDoW(int dow, WeekPlan p){
		switch(dow){
		case Calendar.MONDAY:
			return  p.getMonday();
		case Calendar.TUESDAY:
			return p.getTuesday(); 
		case Calendar.WEDNESDAY:
			return p.getWednesday();
		case Calendar.THURSDAY:
			return p.getThursday();
		case Calendar.FRIDAY:
			return p.getFriday();
		case Calendar.SATURDAY:
			return p.getSaturday();
		case Calendar.SUNDAY:
			return p.getSunday();
		}
		return null;
	}
	
	/**
	 * do a reverse lookup to find out the Calendar DAY_OF_WEEK from a DoW instance<p>
	 * @param dow
	 * @return
	 */
	public static int getCalendarDayOfWeek(com.ethercis.ehrserver.definitions.x10.DoW dow){
		String day = dow.getDomNode().getLocalName();
		if (day.compareTo("Monday")==0)
			return Calendar.MONDAY;
		if (day.compareTo("Tuesday")==0)
			return Calendar.TUESDAY;
		if (day.compareTo("Wednesday")==0)
			return Calendar.WEDNESDAY;
		if (day.compareTo("Thursday")==0)
			return Calendar.THURSDAY;
		if (day.compareTo("Friday")==0)
			return Calendar.FRIDAY;
		if (day.compareTo("Saturday")==0)
			return Calendar.SATURDAY;
		if (day.compareTo("Sunday")==0)
			return Calendar.SUNDAY;
		return -1;
	}
	/**
	 * set a DayPlanner bitmap<p>
	 * The time range is set according to the following rules:<br>
	 * <ul>
	 * <li>if both start and stop time are supplied, set the range accordingly
	 * <li>if start time is undef (null) assume this is 0:00
	 * <li>if stop time is undef (null) assume this is 23:59
	 * <ul>
	 * @param starttime the start time of the range to set
	 * @param stoptime the stop time of the range to set
	 */
	public DayPlanner setDayPlanner(Calendar starttime, Calendar stoptime){
		int lowofs, highofs;
		if (starttime == null)
			lowofs = DayPlanner.MIN;
		else
			lowofs = DayPlanner.time2offset(starttime.get(Calendar.HOUR_OF_DAY), starttime.get(Calendar.MINUTE));
		if (stoptime == null)
			highofs = DayPlanner.MAX;
		else
			highofs = DayPlanner.time2offset(stoptime.get(Calendar.HOUR_OF_DAY), stoptime.get(Calendar.MINUTE));

		setRange(lowofs, highofs);
		return this;
	}	
	
	/**
	 * set a DayPlanner bitmap ponctually<p>
	 * The a ponctual time<br>
	 * @param starttime the start time of the range to set
	 */
	public DayPlanner setPonctual(Calendar starttime){
		int ofs;
		if (starttime != null){
			ofs = DayPlanner.time2offset(starttime.get(Calendar.HOUR_OF_DAY), starttime.get(Calendar.MINUTE));
			mask.set(ofs);
		}
		return this;
	}		
}
