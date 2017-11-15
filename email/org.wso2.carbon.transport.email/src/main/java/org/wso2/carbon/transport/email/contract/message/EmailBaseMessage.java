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

package org.wso2.carbon.transport.email.contract.message;

import org.wso2.carbon.transport.email.contract.message.header.Headers;
import org.wso2.carbon.transport.email.contract.message.properties.Properties;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * This will work as base message for all the remote file system messages.
 */
public class EmailBaseMessage {

    /**
     * Countdown latch to wait for the acknowledgement from the application layer.
     */
    private CountDownLatch latch = new CountDownLatch(1);
    protected Headers headers = new Headers();
    protected Properties properties = new Properties();

    public EmailBaseMessage() {};

    /**
     * Method implemented to get the header value of given key
     * @param key Name of header which need to get.
     * @return Corresponding String value of the given header name.
     */
    public String getHeader(String key) {
        return this.headers.get(key);
    }

    /**
     * Method implemented to get all headers set in the message
     * @return Map which contains key as the header name and header value.
     */
    public Map<String, String> getHeaders() {
        return this.headers.getAllHeaders();
    }

    /**
     * Method implemented to add a header to the message.
     * @param key       Name of the header needed to be set.
     * @param value     Value of the header.
     */
    public void setHeader(String key, String value) {
        this.headers.set(key, value);
    }

    /**
     * Method implemented to add a headers to the message.
     * @param headerMap Map contains header name as key and header value.
     */
    public void setHeaders(Map<String, String> headerMap) {
        this.headers.setHeaders(headerMap);
    }

    /**
     * Method implemented to get the property object of given property name.
     * @param key Name of property which need to get.
     * @return Property object of the given name.
     */
    public Object getProperty(String key) {
        return this.properties.get(key);
    }

    /**
     * Method implemented to get all properties set in the message.
     * @return Map which contain property name and property as key value pairs.
     */
    public Map<String, Object> getProperties() {
        return this.properties.getAllProperties();
    }

    /**
     * Method implemented to add a property to the message.
     * @param key       Name of the property needed to be set.
     * @param value     Property as a object.
     */
    public void setProperty(String key, Object value) {
        this.properties.set(key, value);
    }

    /**
     * Method Implemented to add properties to the message .
     * @param properties Map contains property name as key and property object.
     */
    public void setProperties(Map<String, Object> properties) {
        this.properties.setProperties(properties);
    }


    /**
     * Waits until latch is count down by one. (It waits until 'done' is call by the message processor)
     * @throws InterruptedException
     */
    public void waitTillAck() throws InterruptedException {
        latch.await();
    }

    /**
     * Use to make countdown the latch.
     */
    public void sendAck() {
        latch.countDown();
    }
}
