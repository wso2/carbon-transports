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
import java.util.Properties;

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
    public static Map<String, String> createJMSListeningParameterMap(String destinationName, String connectionFactory,
            String destinationType, String jmsMode) {
        HashMap<String, String> jmsDestinationListeningParameters = new HashMap<>();
        jmsDestinationListeningParameters.put(JMSConstants.PARAM_DESTINATION_NAME, destinationName);
        jmsDestinationListeningParameters.put(JMSConstants.PARAM_CONNECTION_FACTORY_JNDI_NAME, connectionFactory);
        jmsDestinationListeningParameters
                .put(JMSConstants.PARAM_NAMING_FACTORY_INITIAL, JMSTestConstants.ACTIVEMQ_FACTORY_INITIAL);
        jmsDestinationListeningParameters.put(JMSConstants.PARAM_PROVIDER_URL, JMSTestConstants.ACTIVEMQ_PROVIDER_URL);
        jmsDestinationListeningParameters.put(JMSConstants.PARAM_CONNECTION_FACTORY_TYPE, destinationType);
        jmsDestinationListeningParameters.put(JMSConstants.PARAM_ACK_MODE, jmsMode);
        return jmsDestinationListeningParameters;
    }

    /**
     * Method to convert Strings map to Properties object
     * @param stringsMap Map of string key, value pair
     * @return Properties object created with provided String key value pairs
     */
    public static Properties convertStringsToProperties(Map<String, String> stringsMap) {
        Properties properties = new Properties();
        if (stringsMap != null && !stringsMap.isEmpty()) {
            stringsMap.keySet().forEach(key -> properties.setProperty(key, stringsMap.get(key)));
        }
        return properties;
    }
}
