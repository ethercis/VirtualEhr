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

import com.ethercis.servicemanager.cluster.ContextNode;

import javax.management.ObjectInstance;

/**
 * Container to hold the ObjectInstance,mBean and ContextNode triple. 
 */
public class JmxMBeanHandle {
   private ObjectInstance objectInstance;
   private ContextNode contextNode;
   private Object mBean;

   public JmxMBeanHandle(ObjectInstance objectInstance, ContextNode contextNode, Object mBean) {
      this.objectInstance = objectInstance;
      this.contextNode = contextNode;
      this.mBean = mBean;
   }

   /**
   * @return Returns the ObjectInstance.
   */
   public ObjectInstance getObjectInstance() {
      return this.objectInstance;
   }

   /**
   * @param objectInstance The ObjectInstance to set.
   */
   public void setObjectInstance(ObjectInstance objectInstance) {
      this.objectInstance = objectInstance;
   }

   /**
   * @return Returns the contextNode.
   */
   public ContextNode getContextNode() {
      return this.contextNode;
   }

   /**
   * @param contextNode The contextNode to set.
   */
   public void setContextNode(ContextNode contextNode) {
      this.contextNode = contextNode;
   }

   /**
   * @return Returns the mBean.
   */
   public Object getMBean() {
      return this.mBean;
   }

   /**
   * @param mBean The mBean to set.
   */
   public void setMBean(Object mBean) {
      this.mBean = mBean;
   }
}
