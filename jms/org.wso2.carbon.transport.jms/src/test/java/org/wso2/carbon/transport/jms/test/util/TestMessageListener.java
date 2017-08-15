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

import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.TextCarbonMessage;
import org.wso2.carbon.transport.jms.callback.JMSCallback;
import org.wso2.carbon.transport.jms.contract.JMSListener;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

/**
 * Message processor for testing purposes.
 */
public class TestMessageListener implements JMSListener {
    private int count = 0;

    /**
     * To get the count of the messages received
     *
     * @return Number of messages
     */
    public int getCount() {
        return count;
    }

    @Override
    public void onMessage(CarbonMessage jmsMessage, JMSCallback jmsCallback) {
        if (jmsMessage instanceof TextCarbonMessage) {
            count++;
            if (null != jmsCallback) {
                if (count <= 2) {
                    jmsMessage.setProperty(JMSConstants.JMS_MESSAGE_DELIVERY_STATUS,
                            JMSConstants.JMS_MESSAGE_DELIVERY_SUCCESS);

                } else {
                    jmsMessage.setProperty(JMSConstants.JMS_MESSAGE_DELIVERY_STATUS,
                            JMSConstants.JMS_MESSAGE_DELIVERY_ERROR);
                }
                jmsCallback.done(jmsMessage);
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {

    }
}
