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

package org.wso2.carbon.transport.jms.jndi.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.transport.jms.jndi.utils.JMSConstants;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * Callback to be used when there is a need for acknowledgement or committing the session.
 */
public class AcknowledgementCallback implements CarbonCallback {
    private Message message;
    private Session session;
    private static final Log logger = LogFactory.getLog(AcknowledgementCallback.class.getName());

    AcknowledgementCallback(Message message, Session session) {
        this.message = message;
        this.session = session;
    }

    @Override
    public void done(CarbonMessage carbonMessage) {
        try {
            if (session.getAcknowledgeMode() == Session.CLIENT_ACKNOWLEDGE) {
                if (carbonMessage.getProperty(JMSConstants.JMS_MESSAGE_DELIVERY_STATUS).toString()
                        .equalsIgnoreCase(JMSConstants.JMS_MESSAGE_DELIVERY_SUCCESS)) {
                    try {
                        message.acknowledge();
                    } catch (JMSException e) {
                        logger.error("Error while acknowledging the message " + e.getMessage());
                    }
                } else {
                    try {
                        session.recover();
                    } catch (JMSException e) {
                        logger.error("Error while recovering the session " + e.getMessage());
                    }
                }
            } else if (carbonMessage.getProperty(JMSConstants.JMS_SESSION_COMMIT_OR_ROLLBACK)
                    .equals(JMSConstants.JMS_SESSION_COMMIT)) {
                try {
                    session.commit();
                } catch (JMSException e) {
                    logger.error("Error while committing the session " + e.getMessage());
                }
            } else {
                try {
                    session.rollback();
                } catch (JMSException e) {
                    logger.error("Error while rolling back the session " + e.getMessage());
                }
            }
        } catch (JMSException e) {
            logger.error("Error while getting the acknowledgement mode from the session " + e.getMessage());
        }
    }
}
