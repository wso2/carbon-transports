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

package org.wso2.carbon.transport.jms.callback;

import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

import javax.jms.Session;

/**
 * Call back used for transacted sessions. To commit or rollback the sessions.
 */
public class TransactedSessionCallback extends JMSCallback {

    /**
     * Creates a call back for the transacted session.
     *
     * @param session JMS Session connected with this callback.
     */
    public TransactedSessionCallback(Session session) {
        super(session);
    }

    @Override
    public int getAcknowledgementMode() {
        return Session.SESSION_TRANSACTED;
    }

    /**
     * Update the status of the transaction to the side of the JMS transport by reading the status provided by the
     * Ballerina.
     */
    public void updateTransactionStatus() {
        try {
            if (isSuccess()) {
                commitSession();
            } else {
                rollbackSession();
            }
        } catch (JMSConnectorException e) {
            throw new RuntimeException("Error completing the transaction callback operation", e);
        }
    }
}
