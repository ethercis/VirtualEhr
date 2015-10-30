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

/*
This code is a refactoring and adaptation of the original
work provided by the XmlBlaster project (see http://xmlblaster.org)
for more details.
This code is therefore supplied under LGPL 2.1
 */

/**
 * Project: EtherCIS system application
 * 
 * @author <a href="mailto:christian@adoc.co.th">Christian Chevalley</a>
 * @author <a href="mailto:michele@laghi.eu">Michele Laghi</a>
 * @author <a href="mailto:xmlblast@marcelruff.info">Marcel Ruff</a>
 */


package com.ethercis.servicemanager.jmx;

import javax.management.*;
import java.lang.reflect.Method;

/**
 * MethodInvocation is wrapper for Invocation of Methods and the returning
 * Objects
 * TODO: Implement check before invoke if TargetHost is current Host!
 */
public class MethodInvocation implements java.io.Serializable {

  private static final long serialVersionUID = 7235369610255097138L;

  /* Status values. */
  public final static int OK    = 1;
  public final static int ERROR = -99;

  /* MBean server reference. */
  private transient MBeanServer server  = null;

  /* MethodInvocation 'payload'. */
  private String objectName   = null;
  private String methodName   = null;
  private String targetHost   = "";
  private Object[] params     = null;
  private Object returnValue  = null;
  private int status          = 0;

  //key that identify Method to callback
  private String ID = "";

  public MethodInvocation() {}

  public MethodInvocation(Method m) {
    setMethodName(m.getName());
  }

  public void setParams(Object[] params) {
    if (params == null || params.length < 1)  return;
    this.params = params;
  }

  public Object[] getParams() {
    return this.params;
  }

  public void setMBeanServer(MBeanServer server) {
    this.server = server;
  }

  public Object getReturnValue() {
    return returnValue;
  }

  public int getStatus() {
    return status;
  }

  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }


  public void setId(String id) {
    this.ID=id;
  }

  public String getId() {
    return this.ID;
  }

  public void setTargetHost(String target) {
    this.targetHost = target;
  }

  public String getTargetHost() {
    return this.targetHost;
  }


  /**
   *  Invoke implementation
   */

  public void invoke() {

  try {

      if (methodName.equals("createMBean")) {
        if (params.length == 2) {
          returnValue = server.createMBean(
              (String)params[0],
              (ObjectName)params[1]
          );
        }

        else if (params.length == 3) {
          returnValue = server.createMBean(
              (String)params[0],
              (ObjectName)params[1],
              (ObjectName)params[2]
          );
        }

        else if (params.length == 4) {
          returnValue = server.createMBean(
              (String)params[0],
              (ObjectName)params[1],
              (Object[])params[2],
              (String[])params[3]
          );
        }

        else if (params.length == 5) {
          returnValue = server.createMBean(
              (String)params[0],
              (ObjectName)params[1],
              (ObjectName)params[2],
              (Object[])params[3],
              (String[])params[4]
          );
        }

        else {}
      }

      else if (methodName.equals("unregisterMBean")) {
        server.unregisterMBean((ObjectName)params[0]);
      }

      else if (methodName.equals("getObjectInstance")) {
        returnValue = server.getObjectInstance(
            (ObjectName)params[0]
        );
      }

      else if (methodName.equals("isRegistered")) {
        returnValue = new Boolean(server.isRegistered(
            (ObjectName)params[0]
        ));
      }

      else if (methodName.equals("getMBeanCount")) {
        returnValue = server.getMBeanCount();
      }

      else if (methodName.equals("getAttribute")) {
        returnValue = server.getAttribute(
            (ObjectName)params[0],
            (String)params[1]
        );
      }

      else if (methodName.equals("getAttributes")) {
        returnValue = server.getAttributes(
            (ObjectName)params[0],
            (String[])params[1]
        );
      }

      else if (methodName.equals("setAttribute")) {
        server.setAttribute(
            (ObjectName)params[0],
            (Attribute)params[1]
        );
      }

      else if (methodName.equals("setAttributes")) {
        returnValue = server.setAttributes(
            (ObjectName)params[0],
            (AttributeList)params[1]
        );
      }

      else if (methodName.equals("invoke")) {
        returnValue = server.invoke(
            (ObjectName)params[0],
            (String)params[1],
            (Object[])params[2],
            (String[])params[3]
        );
      }

      else if (methodName.equals("getDefaultDomain")) {
        returnValue = server.getDefaultDomain();
      }

      else if (methodName.equals("addNotificationListener")) {
      }

      else if (methodName.equals("removeNotificationListener")) {
      }

      else if (methodName.equals("getMBeanInfo")) {
        returnValue = server.getMBeanInfo(
            (ObjectName)params[0]
        );
      }

      else if (methodName.equals("isInstanceOf")) {
        returnValue = new Boolean(server.isInstanceOf(
            (ObjectName)params[0],
            (String)params[1]
        ));
      }

      else if (methodName.equals("queryNames")) {
        returnValue = server.queryNames(
            (ObjectName)params[0],
            (QueryExp)params[1]
        );
      }

      else if (methodName.equals("queryMBeans")) {
        returnValue = server.queryMBeans(
            (ObjectName)params[0],
            (QueryExp)params[1]
        );
      }

      else {}

    }
    catch (Throwable t) {
      returnValue = t;
      status = ERROR;
    }
  }
}