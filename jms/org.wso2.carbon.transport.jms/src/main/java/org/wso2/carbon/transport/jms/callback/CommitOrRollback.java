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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import javax.jms.JMSException;
import javax.jms.Session;

/**
 * Call back used for transacted sessions. To commit or rollback the sessions.
 */
public class CommitOrRollback implements CarbonCallback {
    private Session session;
    private static final Logger logger = LoggerFactory.getLogger(CommitOrRollback.class);

    public CommitOrRollback(Session session) {
        this.session = session;
    }

    @Override
    public void done(CarbonMessage carbonMessage) {
        if (carbonMessage.getProperty(JMSConstants.JMS_MESSAGE_DELIVERY_STATUS)
                .equals(JMSConstants.JMS_MESSAGE_DELIVERY_SUCCESS)) {
            try {
                session.commit();
            } catch (JMSException e) {
                logger.error("Error while committing the session. " + e.getMessage(), e);
                throw new RuntimeException("Error while committing the session. " + e.getMessage(), e);
            }
        } else {
            try {
                session.rollback();
            } catch (JMSException e) {
                logger.error("Error while rolling back the session. " + e.getMessage(), e);
                throw new RuntimeException("Error while rolling back the session. " + e.getMessage(), e);
            }
        }
    }
}
