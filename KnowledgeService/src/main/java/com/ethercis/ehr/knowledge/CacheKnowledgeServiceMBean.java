/*
 * Copyright (c) Ripple Foundation CIC Ltd, UK, 2017
 * Author: Christian Chevalley
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


package com.ethercis.ehr.knowledge;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 6/1/2015.
 */
public interface CacheKnowledgeServiceMBean {
    /**
     * show list of available commands
     * @return
     */
    public String usage();

    /**
     * reload the cache (with force caching depending on settings)
     * @return
     */
    public String reload();

    /**
     * get the current cache statistics
     * @return
     */
    public String statistics();

    /**
     * show the list of found archetypes
     * @return
     */
    String showArcheypes();

    /**
     * show the list of templates
     * @return
     */
    String showTemplates();

    /**
     * show the list of operational templates
     * @return
     */
    String showOPT();

    /**
     * enable/disable force caching on reload
     * @param set
     * @return
     */
    String setForceCache(boolean set);

    String settings();

    String errors();
}
