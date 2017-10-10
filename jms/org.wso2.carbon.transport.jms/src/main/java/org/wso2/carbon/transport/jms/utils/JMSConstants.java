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

import javax.xml.namespace.QName;

/**
 * Common constants used by JMS transport.
 */
public class JMSConstants {

    /**
     * Enum for JMS destination type.
     */
    public enum JMSDestinationType {
        QUEUE, TOPIC
    }

    /**
     * JMS protocol.
     */
    public static final String PROTOCOL_JMS = "jms";
    /**
     * ID of the service, that this receiver is bounded to.
     */
    public static final String JMS_SERVICE_ID = "JMS_SERVICE_ID";

    /*
     * Session acknowledgement mode of the particular message.
     */
    public static final String JMS_SESSION_ACKNOWLEDGEMENT_MODE = "JMS_SESSION_ACKNOWLEDGEMENT_MODE";

    /**
     * A MessageContext property or client Option indicating the JMS message type.
     */
    public static final String JMS_MESSAGE_TYPE = "JMS_MESSAGE_TYPE";
    public static final String GENERIC_MESSAGE_TYPE = "Message";
    public static final String TEXT_MESSAGE_TYPE = "TextMessage";
    public static final String BYTES_MESSAGE_TYPE = "BytesMessage";
    public static final String OBJECT_MESSAGE_TYPE = "ObjectMessage";
    public static final String MAP_MESSAGE_TYPE = "MapMessage";
    public static final String STREAM_MESSAGE_TYPE = "StreamMessage";

    public static final String TOPIC_PREFIX = "topic.";
    public static final String QUEUE_PREFIX = "queue.";

    public static final String PARAM_CONNECTION_FACTORY_JNDI_NAME = "transport.jms.ConnectionFactoryJNDIName";
    public static final String PARAM_CONNECTION_FACTORY_TYPE = "transport.jms.ConnectionFactoryType";

    public static final String PARAM_DESTINATION_NAME = "transport.jms.Destination";
    public static final String DESTINATION_TYPE_QUEUE = "queue";
    public static final String DESTINATION_TYPE_TOPIC = "topic";
    public static final String SESSION_TRANSACTED = "transport.jms.SessionTransacted";
    public static final String PARAM_ACK_MODE = "transport.jms.SessionAcknowledgement";

    // Durable subscription related parameters.
    public static final String PARAM_SUB_DURABLE = "transport.jms.SubscriptionDurable";
    public static final String PARAM_DURABLE_SUB_ID = "transport.jms.DurableSubscriberName";
    public static final String PARAM_CLIENT_ID = "transport.jms.DurableSubscriberClientId";

    /**
     * Acknowledgements to client
     */
    public static final String JMS_MESSAGE_DELIVERY_ERROR = "ERROR";
    public static final String JMS_MESSAGE_DELIVERY_SUCCESS = "SUCCESS";
    public static final String JMS_MESSAGE_DELIVERY_STATUS = "JMS_MESSAGE_DELIVERY_STATUS";

    /**
     * Acknowledge Modes.
     */
    public static final String AUTO_ACKNOWLEDGE_MODE = "AUTO_ACKNOWLEDGE";
    public static final String CLIENT_ACKNOWLEDGE_MODE = "CLIENT_ACKNOWLEDGE";
    public static final String DUPS_OK_ACKNOWLEDGE_MODE = "DUPS_OK_ACKNOWLEDGE";
    public static final String SESSION_TRANSACTED_MODE = "SESSION_TRANSACTED";
    public static final String XA_TRANSACTED_MODE = "XA_TRANSACTED";

    /**
     * Number of concurrent consumers to be spawned when the server connector is starting.
     */
    public static final String CONCURRENT_CONSUMERS = "concurrentConsumers";

    /**
     * The behavioural nature of the connection factory to use.
     */
    public static final String CONNECTION_FACTORY_NATURE = "connectionFactoryNature";

    public static final String CONNECTION_USERNAME = "connectionUsername";
    public static final String CONNECTION_PASSWORD = "connectionPassword";
    public static final String TRANSPORT_HEADERS = "TransportHeaders";
    public static final String TEXT_DATA = "TextData";


    public static final String USE_RECEIVER = "useReceiver";

    /**
     * Namespace for JMS map payload representation.
     */
    public static final String JMS_MAP_NS = "http://axis.apache.org/axis2/java/transports/jms/map-payload";
    /**
     * Root element name of JMS Map message payload representation
     */
    public static final String JMS_MAP_ELEMENT_NAME = "JMSMap";
    public static final String SET_ROLLBACK_ONLY = "SET_ROLLBACK_ONLY";
    public static final QName JMS_MAP_QNAME = new QName(JMS_MAP_NS, JMS_MAP_ELEMENT_NAME, "");
    /**
     * Constant that holds the name of the environment property
     * for specifying configuration information for the service provider
     * to use. The value of the property should contain a URL string
     * (e.g. "ldap://somehost:389").
     * This property may be specified in the environment,
     * an applet parameter, a system property, or a resource file.
     * If it is not specified in any of these sources,
     * the default configuration is determined by the service provider.
     * <p>
     * The value of this constant is "java.naming.provider.url".
     */
    public static final String PARAM_PROVIDER_URL = "java.naming.provider.url";
    public static final String DESTINATION_TYPE_GENERIC = "generic";
    /**
     * Naming factory initial.
     */
    public static final String PARAM_NAMING_FACTORY_INITIAL = "java.naming.factory.initial";
    /**
     * Default Connection Factory.
     */
    public static final String CONNECTION_STRING = "connectionfactory.QueueConnectionFactory";

    /**
     * The parameter indicating the JMS API specification to be used - if this
     * is "1.1" the JMS 1.1 API would be used, else the JMS 1.0.2B
     */
    public static final String PARAM_JMS_SPEC_VER = "transport.jms.JMSSpecVersion";

    /**
     * A message selector to be used when messages are sought for this service
     */
    public static final String PARAM_MSG_SELECTOR = "transport.jms.MessageSelector";
    /**
     * Should a pub-sub connection receive messages published by itself?
     */
    public static final String PARAM_PUBSUB_NO_LOCAL = "transport.jms.PubSubNoLocal";

    /**
     * JMS 2.0 Parameters
     */
    public static final String PARAM_IS_SHARED_SUBSCRIPTION = "transport.jms.SharedSubscription";

    public static final String JMS_SPEC_VERSION_1_0 = "1.0";

    public static final String JMS_SPEC_VERSION_1_1 = "1.1";

    public static final String JMS_SPEC_VERSION_2_0 = "2.0";

    // JMS Message Headers
    public static final String JMS_MESSAGE_ID = "JMS_MESSAGE_ID";
    public static final String JMS_TIMESTAMP = "JMS_TIMESTAMP";
    public static final String JMS_CORRELATION_ID_AS_BYTES = "JMS_CORRELATION_ID_AS_BYTES";
    public static final String JMS_CORRELATION_ID = "JMS_CORRELATION_ID";
    public static final String JMS_REPLY_TO = "JMS_REPLY_TO";
    public static final String JMS_DESTINATION = "JMS_DESTINATION";
    public static final String JMS_DELIVERY_MODE = "JMS_DELIVERY_MODE";
    public static final String JMS_REDELIVERED = "JMS_REDELIVERED";
    public static final String JMS_TYPE = "JMS_TYPE";
    public static final String JMS_EXPIRATION = "JMS_EXPIRATION";
    public static final String JMS_DELIVERY_TIME = "JMS_DELIVERY_TIME";
    public static final String JMS_PRIORITY = "JMS_PRIORITY";

    public static final String RETRY_INTERVAL = "retryInterval";
    public static final String MAX_RETRY_COUNT = "maxRetryCount";

    public static final int DEFAULT_PRIORITY = 4;

    /**
     * Parameter to be passed in to select default connection factory nature.
     */
    public static final String DEFAULT_CONNECTION_FACTORY = "default";

    public static final String PERSISTENT_DELIVERY_MODE = "1";
    public static final String NON_PERSISTENT_DELIVERY_MODE = "2";

    /**
     * Parameter to be passed in to select cached connection factory nature.
     */
    public static final String CACHED_CONNECTION_FACTORY = "cached";

    /**
     * Parameter to be passed in to select pooled connection factory nature.
     */
    public static final String POOLED_CONNECTION_FACTORY = "pooled";

    /**
     * Parameter for passing in cache in client mode
     */
    public static final String PARAM_JMS_CACHING = "transport.jms.caching";

    /**
     * Client side pooling configurations
     */
    public static final String PARAM_MAX_CONNECTIONS = "connectionCount";

    public static final String PARAM_MAX_SESSIONS_ON_CONNECTION = "sessionCount";



}
