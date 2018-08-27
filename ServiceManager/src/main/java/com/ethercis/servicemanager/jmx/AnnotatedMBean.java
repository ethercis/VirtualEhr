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
package com.ethercis.servicemanager.jmx;

import com.ethercis.servicemanager.common.def.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.*;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


/**
 * JMX helper class<p>
 * From article: http://weblogs.java.net/blog/emcmanus/archive/2005/07/adding_informat.html
 * @author Christian
 *
 */
public class AnnotatedMBean extends StandardMBean{
	private static Logger log = LogManager.getLogger(Constants.LOGGER_SYSTEM);
	   
    /** Instance where the MBean interface is implemented by another object. */
    public <T> AnnotatedMBean(T impl, Class<T> mbeanInterface) throws NotCompliantMBeanException {
        super(impl, mbeanInterface);
    }
    
    /** Instance where the MBean interface is implemented by this object. */
    protected AnnotatedMBean(Class<?> mbeanInterface)
            throws NotCompliantMBeanException {
        super(mbeanInterface);
    }

    @Override
    protected String getDescription(MBeanOperationInfo op) {
        String descr = op.getDescription();
        Method m = methodFor(getMBeanInterface(), op);
        if (m != null) {
            Description d = m.getAnnotation(Description.class);
            if (d != null)
                descr = d.value();
        }
        return descr;    	
    }

    @Override
    protected String getParameterName(MBeanOperationInfo op,
                                      MBeanParameterInfo param,
                                      int paramNo) {
        String name = param.getName();
        Method m = methodFor(getMBeanInterface(), op);
        if (m != null) {
            PName pname = getParameterAnnotation(m, paramNo, PName.class);
            if (pname != null)
                name = pname.value();
        }
        return name;
    }
    
    public static void RegisterMBean(String name, Class mbeanclass, Object object) {
    	MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    	try {
    	ObjectName objname = new ObjectName("com.ethercis:type="+name);
    	AnnotatedMBean mbean = new AnnotatedMBean(object, mbeanclass);
    	mbs.registerMBean(mbean, objname);
    	}
    	catch (Exception e){
    		log.warn("Coud not register JMX handler for "+name+", Exception: "+e);
    	}
    }
    
    //****************** HELPER *************************************************
    static <A extends Annotation> A getParameterAnnotation(Method m,
    		int paramNo,
    		Class<A> annot) {
    	for (Annotation a : m.getParameterAnnotations()[paramNo]) {
    		if (annot.isInstance(a))
    			return annot.cast(a);
    	}
    	return null;
    }
    
	static Class<?> classForName(String name, ClassLoader loader) throws ClassNotFoundException {
		Class<?> c = primitiveClasses.get(name);
		if (c == null)
			c = Class.forName(name, false, loader);
		return c;
	}

	private static final Map<String, Class<?>> primitiveClasses = new HashMap<String, Class<?>>();
	static {
		Class<?>[] prims = {
				byte.class, short.class, int.class, long.class,
				float.class, double.class, char.class, boolean.class,
		};
		for (Class<?> c : prims)
			primitiveClasses.put(c.getName(), c);
	}
	
	private static Method methodFor(Class<?> mbeanInterface, MBeanOperationInfo op) {
		final MBeanParameterInfo[] params = op.getSignature();
		final String[] paramTypes = new String[params.length];
		for (int i = 0; i < params.length; i++)
			paramTypes[i] = params[i].getType();

		return findMethod(mbeanInterface, op.getName(), paramTypes);
	}

	private static Method findMethod(Class<?> mbeanInterface, String name, String... paramTypes) {
		try {
			final ClassLoader loader = mbeanInterface.getClassLoader();
			final Class<?>[] paramClasses = new Class<?>[paramTypes.length];
			for (int i = 0; i < paramTypes.length; i++)
				paramClasses[i] = classForName(paramTypes[i], loader);
			return mbeanInterface.getMethod(name, paramClasses);
		} catch (RuntimeException e) {
//			avoid accidentally catching unexpected runtime exceptions
			throw e;
		} catch (Exception e) {
			return null;
		}
}	
}
