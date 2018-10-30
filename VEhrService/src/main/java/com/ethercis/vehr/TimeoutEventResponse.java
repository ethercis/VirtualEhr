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

package com.ethercis.vehr;

import javax.servlet.AsyncEvent;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by christian on 10/29/2018.
 */
public class TimeoutEventResponse extends FailedQueryEventResponse{

    public TimeoutEventResponse(AsyncEvent event) {
        super(event, HttpServletResponse.SC_REQUEST_TIMEOUT, "timeout");
    }

}
