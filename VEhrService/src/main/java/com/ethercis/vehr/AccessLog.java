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
package com.ethercis.vehr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

//TODO: make this guy a service
public class AccessLog {
	static Logger logger= LogManager.getLogger("ETHERCIS_AUDIT_LOG");
	static {
		String baseDir = System.getenv("ETHERCIS_HOME");
		if(baseDir==null){
			baseDir= System.getProperty("user.home");
		}
		File dir = new File(baseDir + File.separator + "logs");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		//TODO: configure externally

//		FileAppender fa = new FileAppender();
//		fa.setName(AccessLog.class.getCanonicalName());
//		fa.setFile(dir.getAbsolutePath() + File.separator + "access.log");
//		fa.setLayout(new PatternLayout("%d %m%n"));
//		fa.setThreshold(Level.INFO);
//		fa.setAppend(true);
//		fa.activateOptions();
//
//		// add appender to any Logger (here is root)
//		logger.addAppender(fa);
	}
	
	/**
	 * 
	 * @param user
	 * @param method
	 * @param path
	 * @param query
	 */
	public static void log(String user,String method,String path){
		logger.info(user + ":" +method +":" + path);		
	}
	
	public static void info(String msg)
	{
		logger.info(msg);
	}

}
