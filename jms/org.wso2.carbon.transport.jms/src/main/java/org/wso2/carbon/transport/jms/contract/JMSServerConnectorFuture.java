package org.wso2.carbon.transport.jms.contract;

import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.transport.jms.callback.JMSCallback;

/**
 * Allows to set listeners.
 */
public interface JMSServerConnectorFuture {
    void notifyJMSListener(CarbonMessage jmsMessage, JMSCallback jmsCallback);
}
