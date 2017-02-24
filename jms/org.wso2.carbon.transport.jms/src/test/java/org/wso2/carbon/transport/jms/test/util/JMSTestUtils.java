/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.transport.jms.test.util;

import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * This class handles the utils methods that are needed for jms transport testing.
 */
public class JMSTestUtils {

    /**
     * Creates a parameter map for listening to a particular jms destination.
     *
     * @param destinationName   Particular jms destination name.
     * @param connectionFactory Connection Factory name.
     * @param destinationType   Destination type, whether queue or topic
     * @param jmsMode           jms acknowledgement mode.
     * @return a map of the jms properties that is needed to listen to a particular queue or topic.
     */
    public static Map<String, String> createJMSListeningParameterMap(String destinationName, String
            connectionFactory, String destinationType, String jmsMode) {
        HashMap<String, String> jmsDestinationListeningParameters = new HashMap<>();
        jmsDestinationListeningParameters.put(JMSConstants.DESTINATION_PARAM_NAME, destinationName);
        jmsDestinationListeningParameters
                .put(JMSConstants.CONNECTION_FACTORY_JNDI_PARAM_NAME, connectionFactory);
        jmsDestinationListeningParameters
                .put(JMSConstants.NAMING_FACTORY_INITIAL_PARAM_NAME, JMSTestConstants.ACTIVEMQ_FACTORY_INITIAL);
        jmsDestinationListeningParameters.put(JMSConstants.PROVIDER_URL_PARAM_NAME,
                JMSTestConstants.ACTIVEMQ_PROVIDER_URL);
        jmsDestinationListeningParameters
                .put(JMSConstants.CONNECTION_FACTORY_TYPE_PARAM_NAME, destinationType);
        jmsDestinationListeningParameters.put(JMSConstants.SESSION_ACK_MODE_PARAM_NAME, jmsMode);
        return jmsDestinationListeningParameters;
    }
}
