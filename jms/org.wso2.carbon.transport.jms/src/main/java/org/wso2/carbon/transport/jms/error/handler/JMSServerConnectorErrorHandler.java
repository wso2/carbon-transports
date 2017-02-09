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

package org.wso2.carbon.transport.jms.error.handler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.ServerConnectorErrorHandler;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

/**
 * Error handler for jms listener.
 */
public class JMSServerConnectorErrorHandler implements ServerConnectorErrorHandler {
    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressFBWarnings({ "BC_UNCONFIRMED_CAST" })
    public void handleError(Throwable throwable,
            CarbonMessage carbonMessage, CarbonCallback carbonCallback) {
        if (null != carbonCallback) {
            carbonMessage
                    .setProperty(JMSConstants.JMS_MESSAGE_DELIVERY_STATUS, JMSConstants.JMS_MESSAGE_DELIVERY_ERROR);
            carbonCallback.done(carbonMessage);
        } else {
            throw (RuntimeException) throwable;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProtocol() {
        return JMSConstants.PROTOCOL_JMS;
    }
}


