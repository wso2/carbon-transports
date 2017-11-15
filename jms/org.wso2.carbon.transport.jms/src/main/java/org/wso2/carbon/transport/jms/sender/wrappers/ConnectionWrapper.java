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

package org.wso2.carbon.transport.jms.sender.wrappers;

import java.util.concurrent.atomic.AtomicInteger;
import javax.jms.Connection;

/**
 * Wrapper class for JMS Connection objects. This class will hold JMSConnection and a counter to maintain the
 * number of JMSSessions currently active based on the Connection.
 */
public class ConnectionWrapper {
    private Connection connection;
    private AtomicInteger sessionCount;

    public ConnectionWrapper(Connection connection) {
        this.connection = connection;
        sessionCount = new AtomicInteger(0);
    }

    public void incrementSessionCount() {
        sessionCount.incrementAndGet();
    }

    public void decrementSessionCount() {
        sessionCount.decrementAndGet();
    }

    public Connection getConnection() {
        return connection;
    }

    public AtomicInteger getSessionCount() {
        return sessionCount;
    }
}
