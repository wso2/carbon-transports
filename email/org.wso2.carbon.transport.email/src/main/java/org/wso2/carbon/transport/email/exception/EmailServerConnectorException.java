/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.transport.email.exception;

import org.wso2.carbon.messaging.exceptions.ServerConnectorException;

/**
 * Exception that happens in Email transport level.
 */
public class EmailServerConnectorException extends ServerConnectorException {

    /**
     * Creates a Email Connector Exception.
     *
     * @param message Corresponding exception message
     */
    public EmailServerConnectorException(String message) {
        super(message);
    }

    /**
     * Creates a Email Connector Exception.
     *
     * @param message Corresponding exception message
     * @param cause   Exception object, that has the details of the relevant exception
     */
    public EmailServerConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
