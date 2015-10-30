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


package com.ethercis.servicemanager.common;
import java.lang.ref.WeakReference;

/**
 * Helper holding the callback interface an some user data to be 
 * looped through.
 */
final class Container {
   private final boolean useWeakReference;
   private Object callback;
   private Object userData;
   final long creation;
   
   /** @param callback The handle to callback a client (is checked already to be not null) */
   Container(boolean useWeakReference, I_Timeout callback, Object userData) {
      this.useWeakReference = useWeakReference;
      if (this.useWeakReference) {
         this.callback = new WeakReference(callback);
         if (userData != null) 
            this.userData = new WeakReference(userData);
      }
      else {
         this.callback = callback;
         this.userData = userData;
      }
      this.creation = System.currentTimeMillis();
   }

   /** @return The callback handle can be null for weak references */
   I_Timeout getCallback() {
      if (this.useWeakReference) {
         WeakReference weak = (WeakReference)this.callback;
         return (I_Timeout)weak.get();
      }
      else {
         return (I_Timeout)this.callback;
      }
   }
   /** @return The userData, can be null for weak references */
   Object getUserData() {
      if (this.userData == null) {
         return null;
      }
      if (this.useWeakReference) {
         WeakReference weak = (WeakReference)this.userData;
         return weak.get();
      }
      else {
         return this.userData;
      }
   }

   void reset() {
      if (this.callback != null && useWeakReference) {
         ((WeakReference)this.callback).clear();
      }
      this.callback = null;

      if (this.userData != null && useWeakReference) {
         ((WeakReference)this.userData).clear();
      }
      this.userData = null;
   }
   
   public String toString() {
      return "callback=" + (callback==null?"null":callback.toString()) + " userData=" + userData;
   }
}


