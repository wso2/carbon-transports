package org.wso2.carbon.transport.jms.impl;

import org.wso2.carbon.transport.jms.contract.JMSClientConnector;
import org.wso2.carbon.transport.jms.contract.JMSConnectorFactory;
import org.wso2.carbon.transport.jms.contract.JMSListener;
import org.wso2.carbon.transport.jms.contract.JMSServerConnector;
import org.wso2.carbon.transport.jms.contract.JMSServerConnectorFuture;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.receiver.JMSServerConnectorImpl;
import org.wso2.carbon.transport.jms.sender.JMSClientConnectorImpl;

import java.util.Map;

/**
 * Implementation of HTTPConnectorFactory interface
 */
public class JMSConnectorFactoryImpl implements JMSConnectorFactory {

    @Override
    public JMSServerConnector createServerConnector(String serviceId, Map<String, String> connectorConfig,
                                                    JMSListener jmsListener) throws JMSConnectorException {
        JMSServerConnectorFuture jmsServerConnectorFuture = new JMSServerConnectorFutureImpl(jmsListener);
        return new JMSServerConnectorImpl(serviceId, connectorConfig, jmsServerConnectorFuture);
    }

    @Override
    public JMSClientConnector createClientConnector() throws JMSConnectorException {
        return new JMSClientConnectorImpl();
    }
}
