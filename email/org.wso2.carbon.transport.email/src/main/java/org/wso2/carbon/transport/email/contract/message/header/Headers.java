/*
 * Copyright (c) 2017 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.transport.email.contract.message.header;

import java.util.Map;
import java.util.TreeMap;

/**
 * Class implementing to set and get headers of the messages.
 */
public class Headers {
    private Map<String, String> headerMap;

    public Headers() {
        this.headerMap = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Method implemented to get the header value of given key
     * @param key Name of header which need to get.
     * @return Corresponding String value of the given header name.
     */
    public String get(String key) {
        return this.headerMap.get(key);
    }

    /**
     * Method implemented to get header Map
     * @return Header Map which contains key as the header name and header value.
     */
    public Map<String, String> getAllHeaders() {
        return this.headerMap;
    }

    /**
     * Method implemented to add a header to header map.
     * @param key       Name of the header needed to be set.
     * @param value     Value of the header.
     */
    public void set(String key, String value) {
        this.headerMap.put(key, value);
    }

    /**
     * Method implemented to add headers to header map.
     * @param headers Map contains header name as key and header value.
     */
    public void setHeaders(Map<String, String> headers) {
        headers.forEach(this::set);
    }
}
