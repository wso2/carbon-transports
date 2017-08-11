package org.wso2.carbon.transport.jms.impl;

import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.transport.jms.callback.JMSCallback;
import org.wso2.carbon.transport.jms.contract.JMSListener;
import org.wso2.carbon.transport.jms.contract.JMSServerConnectorFuture;

/**
 * Server connector future implementation
 */
public class JMSServerConnectorFutureImpl implements JMSServerConnectorFuture {

    private JMSListener jmsListener;

    public JMSServerConnectorFutureImpl(JMSListener jmsListener) {
        this.jmsListener = jmsListener;
    }

    @Override
    public void notifyJMSListener(CarbonMessage jmsMessage, JMSCallback jmsCallback) {
        jmsListener.onMessage(jmsMessage, jmsCallback);
    }
}
