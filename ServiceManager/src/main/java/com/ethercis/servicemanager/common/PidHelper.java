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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.apache.log4j.Logger;


/**
 * Helper file for JVM PID file<p>
 * NB: this should work only with a Sun JVM!
 * @author Christian
 *
 */
public class PidHelper {
    private final static String ME = "PidHelper";
	private final String partitionId;
	private final String path;
    private static Logger log = Logger.getLogger("PidHelper");
	
	/**
	 * new instance
	 * @param locpath location of pid file
	 * @param id name of file (will be suffixed by '.pid')
	 */
	public PidHelper(String locpath, String id){
		this.partitionId = id;
		this.path = locpath;
	}
	/**
	 * return the pid part of the jvm instance name<p>
	 * The jvm instance name is formatted logonservice: <code><i>pid</i>@<i>name</i></code>
	 * @return
	 */
	private String pid(){
		String pid = ManagementFactory.getRuntimeMXBean().getName().split("\\@")[0];
		log.info("Running instance name is:"+ManagementFactory.getRuntimeMXBean().getName());
		return pid;
	}
	/**
	 * create a new pid file<p>
	 * The file name is location/id.pid
	 * @throws IOException
	 */
	public void createPidFile() throws IOException{
		String filename = path+"/"+partitionId+".pid";
		BufferedWriter out = new BufferedWriter(new FileWriter(filename));
		
		out.write(pid());
		out.flush();
		out.close();
	}
	/**
	 * delete the PID file on JVM exit<p>
	 */
	public void deletePidFile(){
		File file = new File(path+"/"+partitionId+".pid");
		file.deleteOnExit();
	}
}
