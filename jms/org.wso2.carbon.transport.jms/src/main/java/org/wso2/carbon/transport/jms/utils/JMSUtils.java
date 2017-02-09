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

package org.wso2.carbon.transport.jms.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.MapCarbonMessage;
import org.wso2.carbon.messaging.SerializableCarbonMessage;
import org.wso2.carbon.messaging.TextCarbonMessage;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

import java.util.Enumeration;
import java.util.Properties;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.Reference;

/**
 * This class is maintains the common methods used by JMS transport.
 */
public class JMSUtils {
    private static final Logger logger = LoggerFactory.getLogger(JMSUtils.class);

    /**
     * Return the JMS destination with the given destination name looked up from the context.
     *
     * @param context         the Context to lookup
     * @param destinationName name of the destination to be looked up
     * @param destinationType type of the destination to be looked up
     * @return the JMS destination, or null if it does not exist
     */
    public static Destination lookupDestination(Context context, String destinationName, String destinationType)
            throws NamingException {
        if (null == destinationName) {
            return null;
        }
        try {
            return JMSUtils.lookup(context, Destination.class, destinationName);
        } catch (NameNotFoundException e) {
            Properties initialContextProperties = new Properties();
            initialContextProperties.put(JMSConstants.NAMING_FACTORY_INITIAL,
                    context.getEnvironment().get(JMSConstants.NAMING_FACTORY_INITIAL));
            initialContextProperties
                    .put(JMSConstants.PROVIDER_URL, context.getEnvironment().get(JMSConstants.PROVIDER_URL));
            if (null != context.getEnvironment().get(JMSConstants.CONNECTION_STRING)) {
                initialContextProperties.put(JMSConstants.CONNECTION_STRING,
                        context.getEnvironment().get(JMSConstants.CONNECTION_STRING));
            }
            if (JMSConstants.DESTINATION_TYPE_TOPIC.equalsIgnoreCase(destinationType)) {
                initialContextProperties.put(JMSConstants.TOPIC_PREFIX + destinationName, destinationName);
            } else if (JMSConstants.DESTINATION_TYPE_QUEUE.equalsIgnoreCase(destinationType)
                    || JMSConstants.DESTINATION_TYPE_GENERIC.equalsIgnoreCase(destinationType)) {
                initialContextProperties.put(JMSConstants.QUEUE_PREFIX + destinationName, destinationName);
            }
            InitialContext initialContext = new InitialContext(initialContextProperties);
            try {
                return JMSUtils.lookup(initialContext, Destination.class, destinationName);
            } catch (NamingException e1) {
                return JMSUtils.lookup(context, Destination.class,
                        (JMSConstants.DESTINATION_TYPE_TOPIC.equalsIgnoreCase(destinationType) ?
                                "dynamicTopics/" :
                                "dynamicQueues/") + destinationName);
            }

        }
    }

    /**
     * JNDI look up in the context.
     *
     * @param context Context that need to looked up
     * @param clazz   Class of the object that need to be found
     * @param name    Name of the object that need to be looked up
     * @param <T>     Class of the Object that need to be found
     * @return the relevant object, if found in the context
     * @throws NamingException, if the found object is different from the expected object
     */
    private static <T> T lookup(Context context, Class<T> clazz, String name) throws NamingException {
        Object object = context.lookup(name);
        try {
            return clazz.cast(object);
        } catch (ClassCastException ex) {
            // Instead of a ClassCastException, throw an exception with some
            // more information.
            if (object instanceof Reference) {
                Reference ref = (Reference) object;
                String errorMessage =
                        "JNDI failed to de-reference Reference with name " + name + "; is the " + "factory " + ref
                                .getFactoryClassName() + " in your classpath?";
                throw new NamingException(errorMessage);
            } else {
                String errorMessage =
                        "JNDI lookup of name " + name + " returned a " + object.getClass().getName() + " while a "
                                + clazz + " was expected";
                logger.error(errorMessage);
                throw new NamingException(errorMessage);
            }
        }
    }

    /**
     * Change a jms message to carbon message.
     *
     * @param message JMS message that need to be changed as carbon message
     * @return the carbon message converted from jms message
     */
    public static CarbonMessage createJMSCarbonMessage(Message message) throws JMSConnectorException {
        CarbonMessage jmsCarbonMessage = null;
        try {
            if (message instanceof TextMessage) {
                jmsCarbonMessage = new TextCarbonMessage(((TextMessage) message).getText());
                jmsCarbonMessage.setProperty(JMSConstants.JMS_MESSAGE_TYPE, JMSConstants.TEXT_MESSAGE_TYPE);
            } else if (message instanceof MapMessage) {
                MapCarbonMessage mapCarbonMessage = new MapCarbonMessage();
                MapMessage mapMessage = (MapMessage) message;
                Enumeration<String> mapKeys = ((MapMessage) message).getMapNames();
                while (mapKeys.hasMoreElements()) {
                    String mapKey = mapKeys.nextElement();
                    String mapValue = mapMessage.getString(mapKey);
                    if (null != mapValue) {
                        mapCarbonMessage.setValue(mapKey, mapValue);
                    }
                }
                jmsCarbonMessage = mapCarbonMessage;
            } else if (message instanceof ObjectMessage) {
                jmsCarbonMessage = (SerializableCarbonMessage) ((ObjectMessage) message).getObject();
            } else {
                jmsCarbonMessage = new TextCarbonMessage(((BytesMessage) message).readUTF());
            }
            String messageId = message.getJMSMessageID();
            if (null != messageId) {
                jmsCarbonMessage.setHeader(JMSConstants.JMS_MESSAGE_ID, messageId);
            }
            jmsCarbonMessage.setHeader(JMSConstants.JMS_DELIVERY_MODE, String.valueOf(message.getJMSDeliveryMode()));
            jmsCarbonMessage.setHeader(JMSConstants.JMS_PRIORITY, String.valueOf(message.getJMSPriority()));
            jmsCarbonMessage.setHeader(JMSConstants.JMS_RE_DELIVERED, String.valueOf(message.getJMSRedelivered()));
            jmsCarbonMessage.setHeader(JMSConstants.JMS_TIME_STAMP, String.valueOf(message.getJMSTimestamp()));

            Enumeration<String> properties = message.getPropertyNames();
            while (properties.hasMoreElements()) {
                String name = properties.nextElement();
                jmsCarbonMessage.setHeader(name, message.getStringProperty(name));
            }
            return jmsCarbonMessage;
        } catch (JMSException e) {
            logger.error("Error while changing the jms message to carbon message");
            throw new JMSConnectorException("Error while changing the jms message to carbon message", e);
        }
    }
}
