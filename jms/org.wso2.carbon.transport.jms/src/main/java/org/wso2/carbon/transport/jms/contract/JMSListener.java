package org.wso2.carbon.transport.jms.contract;

import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.transport.jms.callback.JMSCallback;

/**
 * Allows to get notifications.
 */
public interface JMSListener {
    /**
     * Transport will trigger this method when for each jms message.
     *
     * @param jmsMessage contains the msg data.
     */
    void onMessage(CarbonMessage jmsMessage, JMSCallback jmsCallback);

    /**
     * If there are errors, transport will trigger this method.
     *
     * @param throwable contains the error details of the event.
     */
    void onError(Throwable throwable);
}
