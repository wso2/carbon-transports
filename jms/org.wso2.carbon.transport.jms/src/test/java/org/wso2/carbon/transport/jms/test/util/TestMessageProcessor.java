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

package org.wso2.carbon.transport.jms.test.util;

import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.ClientConnector;
import org.wso2.carbon.messaging.TextCarbonMessage;
import org.wso2.carbon.messaging.TransportSender;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

/**
 * Message processor for testing purposes.
 */
public class TestMessageProcessor implements CarbonMessageProcessor {
    private int count = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback) throws Exception {
        if (carbonMessage instanceof TextCarbonMessage) {
            count++;
            if (null != carbonCallback) {
                if (count <= 2) {
                    carbonMessage.setProperty(JMSConstants.JMS_MESSAGE_DELIVERY_STATUS,
                            JMSConstants.JMS_MESSAGE_DELIVERY_SUCCESS);

                } else {
                    carbonMessage.setProperty(JMSConstants.JMS_MESSAGE_DELIVERY_STATUS,
                            JMSConstants.JMS_MESSAGE_DELIVERY_ERROR);
                }
                carbonCallback.done(carbonMessage);
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTransportSender(TransportSender transportSender) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setClientConnector(ClientConnector clientConnector) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return null;
    }

    /**
     * To get the count of the messages received.
     *
     * @return Number of messages.
     */
    public int getCount() {
        return count;
    }
}
