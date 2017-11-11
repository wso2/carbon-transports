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

/**
 * Constants that are used within the test scope.
 */
public class JMSTestConstants {
    public static final String QUEUE_CONNECTION_FACTORY = "QueueConnectionFactory";
    public static final String TOPIC_CONNECTION_FACTORY = "TopicConnectionFactory";
    public static final String XA_CONNECTION_FACTORY = "XAConnectionFactory";
    public static final String ACTIVEMQ_PROVIDER_URL = "vm://localhost?broker.persistent=false";
    public static final String ACTIVEMQ_PROVIDER_URL_2 = "tcp://localhost:61617";
    public static final String QUEUE_NAME = "testqueue";
    public static final String QUEUE_NAME_1 = "testqueue1";
    public static final String QUEUE_NAME_2 = "testqueue2";
    public static final String QUEUE_NAME_3 = "testqueue3";
    public static final String TOPIC_NAME = "testtopic";
    public static final String TOPIC_NAME_1 = "testtopic1";
    public static final String TOPIC_NAME_2 = "testtopic2";

    public static final String TEST_LOG_DIR = "./target/logs";

    public static final String ACTIVEMQ_FACTORY_INITIAL = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
    public static final String ACTIVEMQ_LOGIN_CONFIG = "java.security.auth.login.config";
    public static final String ACTIVEMQ_LOGIN_CONFIG_DIR =  "conf/login.config";

    public static final String ATOMIKOS_BASE_DIRECTORY_PROP = "com.atomikos.icatch.log_base_dir";

}
