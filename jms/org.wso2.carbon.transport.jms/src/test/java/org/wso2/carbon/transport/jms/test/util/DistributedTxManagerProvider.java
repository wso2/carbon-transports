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
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.wso2.carbon.transport.jms.test.util;

import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

/**
 * This class is used for creating and XA transaction Manager for testing XA transaction flow implementation
 * of the JMS transport.
 */
public class DistributedTxManagerProvider {
    private static final Logger logger = LoggerFactory.getLogger(DistributedTxManagerProvider.class);
    private static DistributedTxManagerProvider distributedTxManagerProvider = null;
    private TransactionManager transactionManager;

    protected DistributedTxManagerProvider() {
        Properties properties = new Properties();
        properties.setProperty(JMSTestConstants.ATOMIKOS_BASE_DIRECTORY_PROP, JMSTestConstants.TEST_LOG_DIR);

        try {
            UserTransactionServiceImp service = new UserTransactionServiceImp(properties);
            service.init();
            UserTransactionManager atomikosTransactionManager = new UserTransactionManager();
            atomikosTransactionManager.setStartupTransactionService(false);
            atomikosTransactionManager.init();
            this.transactionManager = atomikosTransactionManager;
        } catch (SystemException e) {
            logger.error("Error when creating the XA provider ", e);
        }
    }

    public static DistributedTxManagerProvider getInstance() {
        if (distributedTxManagerProvider == null) {
            synchronized (DistributedTxManagerProvider.class) {
                if (distributedTxManagerProvider == null) {
                    distributedTxManagerProvider = new DistributedTxManagerProvider();
                }
            }
        }
        return distributedTxManagerProvider;
    }

    public TransactionManager getTransactionManager() {
        return this.transactionManager;
    }
}
