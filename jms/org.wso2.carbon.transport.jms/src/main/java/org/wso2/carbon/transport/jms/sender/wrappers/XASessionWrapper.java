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

import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.XASession;

/**
 * Wrapper Class for JMS Sessions. This wll also hold the MessageProducer instance created on the Session.
 * Instances of this class will be used as objects in the Session pool in the JMSClientConnectionFactory.
 */
public class XASessionWrapper extends SessionWrapper {
    private XASession xASession;

    public XASessionWrapper(XASession xASession, Session session, MessageProducer messageProducer) {
        super(session, messageProducer);
        this.xASession = xASession;
    }

    public XASession getXASession() {
        return xASession;
    }
}
