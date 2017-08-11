package org.wso2.carbon.transport.jms.contract;

import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.transport.jms.callback.JMSCallback;

/**
 * Allows to get notifications.
 */
public interface JMSListener {
    void onMessage(CarbonMessage jmsMessage, JMSCallback jmsCallback);
    void onError(Throwable throwable);
}
