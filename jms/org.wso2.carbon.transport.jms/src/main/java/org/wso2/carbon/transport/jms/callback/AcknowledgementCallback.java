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

import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * Callback to be used when there is a need for acknowledgement.
 */
public class AcknowledgementCallback implements CarbonCallback {
    /**
     * The {@link Message} instance representing JMS Message related with this callback.
     */
    private Message message;
    /**
     * The {@link Session} instance representing JMS Session related with this call back
     */
    private Session session;

    /**
     * Creates a acknowledgement call back to acknowledge or recover messages in client acknowledgement mode
     *
     * @param message {@link Message} JMS message related with this call back
     * @param session {@link Session} JMS session related with this call back
     */
    public AcknowledgementCallback(Message message, Session session) {
        this.message = message;
        this.session = session;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void done(CarbonMessage carbonMessage) {
        if (carbonMessage.getProperty(JMSConstants.JMS_MESSAGE_DELIVERY_STATUS).toString()
                .equalsIgnoreCase(JMSConstants.JMS_MESSAGE_DELIVERY_SUCCESS)) {
            try {
                message.acknowledge();
            } catch (JMSException e) {
                throw new RuntimeException("Error while acknowledging the message. ", e);
            }
        } else {
            try {
                session.recover();
            } catch (JMSException e) {
                throw new RuntimeException("Error while recovering the session. ", e);
            }
        }
    }
}
