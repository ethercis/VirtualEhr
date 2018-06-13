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

/**
 * Created by christian on 5/28/2018.
 */
public interface I_HttpServiceConfiguration {
    //HTTP
    String SERVER_HTTP_PORT = "server.http.port";
    String SERVER_HTTP_HOST = "server.http.host";
    String SERVER_IDLE_TIMEOUT = "server.idle_timeout";
    String SERVER_HTTP_SECURE_SCHEME = "server.http.secure_scheme";
//    String SERVER_HTTP_SECURE_PORT = "server.http.secure_port";
    String SERVER_HTTP_OUTPUT_BUFFER_SIZE = "server.http.output_buffer_size";
    String SERVER_HTTP_REQUEST_HEADER_SIZE = "server.http.request_header_size";
    String SERVER_HTTP_SET_RESPONSE_HEADER_SIZE = "server.http.response_header_size";
    String SERVER_HTTP_SEND_SERVER_VERSION = "server.http.send_server_version";
    String SERVER_HTTP_SEND_DATE_HEADER = "server.http.send_date_header";

    //SSL
    String SERVER_HTTPS_PORT = "server.https.port";
    String SERVER_HTTPS_HOST = "server.https.host";

    String SERVER_HTTPS_KEY_STORE_PATH = "server.https.key_store_path";
    String SERVER_HTTPS_KEY_STORE_PASSWORD = "server.https.key_store_password";
    String SERVER_HTTPS_KEY_MANAGER_PASSWORD = "server.https.key_manager_password";
    String SERVER_HTTPS_TRUST_STORE_PATH = "server.https.trust_store_path";
    String SERVER_HTTPS_TRUST_STORE_PASSWORD = "server.https.trust_store_password";
    String SERVER_HTTPS_EXCLUDE_CIPHER_SUITES = "server.https.exclude_cipher_suites";


    //Low resource monitor
    String SERVER_LOW_RESOURCES_MONITOR = "server.low_resources_monitor";
    String SERVER_LOW_RESOURCES_MONITOR_PERIOD = "server.low_resources_monitor.period";
    String SERVER_LOW_RESOURCES_MONITOR_IDLE_TIMEOUT = "server.low_resources_monitor.idle_timeout";
    String SERVER_LOW_RESOURCES_MONITOR_THREADS = "server.low_resources_monitor.threads";
    String SERVER_LOW_RESOURCES_MONITOR_MAX_CONNECTIONS = "server.low_resources_monitor.max_connections";
    String SERVER_LOW_RESOURCES_MONITOR_MAX_LOW_RESOURCES_TIME = "server.low_resources_monitor.max_low_resources_time";

    //Request log
    String SERVER_REQUEST_LOG = "server.request_log";
    String SERVER_REQUEST_LOG_FILENAME = "server.request_log.filename";
    String SERVER_REQUEST_LOG_FILENAME_DATE_FORMAT = "server.request_log.filename_date_format";
    String SERVER_REQUEST_LOG_RETAIN_DAY = "server.request_log.retain_day";
    String SERVER_REQUEST_LOG_APPEND = "server.request_log.append";
    String SERVER_REQUEST_LOG_EXTENDED = "server.request_log.extended";
    String SERVER_REQUEST_LOG_LOG_COOKIES = "server.request_log.log_cookies";
    String SERVER_REQUEST_LOG_LOG_TIMEZONE = "server.request_log.log_timezone";

    String SERVER_CORS_ALLOW_HEADERS = "server.cors.allow_headers";
    String SERVER_CORS_ALLOW_METHODS = "server.cors.allow_methods";

    //Use JMX
    String SERVER_JMX = "server.jmx";

    //Use statistics
    String SERVER_STATS = "server.stats";

    String SERVER_THREAD_POOL_MAX_THREADS = "server.thread_pool.max_threads";
}
