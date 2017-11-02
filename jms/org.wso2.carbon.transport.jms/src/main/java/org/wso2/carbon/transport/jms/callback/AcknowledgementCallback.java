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

package org.wso2.carbon.transport.jms.callback;

import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * Callback to be used when there is a need for acknowledgement.
 */
public class AcknowledgementCallback extends JMSCallback {
    /**
     * The {@link Message} instance representing JMS Message related with this callback.
     */
    private Message message;

    /**
     * Creates a acknowledgement call back to acknowledge or recover messages in client acknowledgement mode.
     *
     * @param session {@link Session} JMS session related with this callback.
     * @param message {@link Message} JMS message related with this callback.
     */
    public AcknowledgementCallback(Session session, Message message) {
        super(session);
        this.message = message;
    }

    @Override
    public int getAcknowledgementMode() {
        return Session.CLIENT_ACKNOWLEDGE;
    }

    /**
     * Update the status of the transaction to the side of the JMS transport by reading the status provided by the
     * Ballerina.
     */
    public void updateAcknowledgementStatus() {
        try {
            if (isSuccess()) {
                message.acknowledge();
            } else {
                recoverSession();
            }
        } catch (JMSException | JMSConnectorException e) {
            throw new RuntimeException("Error completing the transaction callback operation", e);
        }
    }
}
