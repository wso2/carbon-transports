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

package org.wso2.carbon.transport.jms.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.transport.jms.callback.AcknowledgementCallback;
import org.wso2.carbon.transport.jms.callback.CommitOrRollback;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.utils.JMSConstants;
import org.wso2.carbon.transport.jms.utils.JMSUtils;

import javax.jms.Message;
import javax.jms.Session;

/**
 * JMS Message Listener which listens to a queue/topic in asynchronous manner.
 */
class JMSMessageListener implements javax.jms.MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(JMSMessageListener.class);
    private CarbonMessageProcessor carbonMessageProcessor;
    private String serviceId;
    private int acknowledgementMode;
    private Session session;

    JMSMessageListener(CarbonMessageProcessor messageProcessor, String serviceId, int acknowledgementMode,
            Session session) {
        this.carbonMessageProcessor = messageProcessor;
        this.serviceId = serviceId;
        this.acknowledgementMode = acknowledgementMode;
        this.session = session;
    }

    /**
     * Override this method and add the operation which is needed to be done when a message is arrived.
     * @param message - the next received message
     */
    @Override
    public void onMessage(Message message) {
        try {
            CarbonMessage jmsCarbonMessage = JMSUtils.createJMSCarbonMessage(message);
            jmsCarbonMessage.setProperty(org.wso2.carbon.messaging.Constants.PROTOCOL, JMSConstants.PROTOCOL_JMS);
            jmsCarbonMessage.setProperty(JMSConstants.JMS_SERVICE_ID, serviceId);
            if (Session.CLIENT_ACKNOWLEDGE == this.acknowledgementMode) {
                carbonMessageProcessor.receive(jmsCarbonMessage, new AcknowledgementCallback(message, session));
            } else if (Session.SESSION_TRANSACTED == this.acknowledgementMode) {
                carbonMessageProcessor.receive(jmsCarbonMessage, new CommitOrRollback(session));
            } else {
                carbonMessageProcessor.receive(jmsCarbonMessage, null);
            }
        } catch (Exception e) {
            logger.error("Error while getting the message from jms server : " + e.getMessage(), e);
            throw new RuntimeException(new JMSConnectorException("Error while getting the message from jms "
                    + "server", e));
        }
    }

}
