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

package org.wso2.carbon.transport.email.exception;

import org.wso2.carbon.messaging.exceptions.ServerConnectorException;

/**
 * Exception that happens in Email transport level.
 */
public class EmailConnectorException extends ServerConnectorException {
    //since polling framework is used in email server connector, EmailConnectorException class have to extended
    // by SeverConnectorException class which is come with carbon messaging dependency.
    //This will be removed in the future.

    public EmailConnectorException(String message) {
        super(message);
    }

    public EmailConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
