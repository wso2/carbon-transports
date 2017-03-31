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

import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

import javax.jms.JMSException;
import javax.jms.Session;

/**
 * Holds common fields and operations for a JMS callback implementation.
 */
public abstract class JMSCallback implements CarbonCallback {

    /**
     * The {@link Session} instance representing JMS Session related with this call back
     */
    private Session session;

    /**
     * The object which invoked the acknowledgment.
     */
    private final Object caller;

    /**
     * States whether the callback operation is completed.
     */
    private boolean operationComplete = false;

    /**
     * Creates a call back initializing the JMS session object and saving caller object to be notified.
     *
     * @param session JMS Session connected with this callback
     * @param caller  {@link Object} The caller object which needs to wait for the jms acknowledgement to be completed
     */
    public JMSCallback(Session session, Object caller) {
        this.session = session;
        this.caller = caller;
    }

    /**
     * States whether acknowledgement process has been completed. This is used by a caller who is waiting to be
     * notified by the callback.
     *
     * @return True if acknowledging process is completed.
     */
    public boolean isOperationComplete() {
        return operationComplete;
    }

    /**
     * Commits the JMS session.
     *
     * @throws JMSConnectorException if the JMS provider fails to commit the transaction due to
     */
    protected void commitSession() throws JMSConnectorException {
        try {
            session.commit();
        } catch (JMSException e) {
            throw new JMSConnectorException("Error while committing the session.");
        }
    }

    /**
     * Rollbacks the JMS session.
     *
     * @throws JMSConnectorException if the JMS provider fails to roll back the transaction
     */
    protected void rollbackSession() throws JMSConnectorException {
        try {
            session.rollback();
        } catch (JMSException e) {
            throw new JMSConnectorException("Error while rolling back the session.", e);
        }
    }

    /**
     * Recover the JMS session.
     *
     * @throws JMSConnectorException if the JMS provider fails to recover the session
     */
    protected void recoverSession() throws JMSConnectorException {
        try {
            session.recover();
        } catch (JMSException e) {
            throw new JMSConnectorException("Error while recovering the JMS session", e);
        }
    }

    /**
     * Mark as this callback operation is complete and notify the caller.
     */
    protected void markAsComplete() {
        operationComplete = true;
        synchronized (caller) {
            caller.notifyAll();
        }
    }
}
