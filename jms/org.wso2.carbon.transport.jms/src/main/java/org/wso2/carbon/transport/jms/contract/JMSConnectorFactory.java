package org.wso2.carbon.transport.jms.contract;

import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

import java.util.Map;

/**
 * Allows you create JMS server and JMS client connectors
 */
public interface JMSConnectorFactory {

    /**
     * Returns an instance of the {@link JMSServerConnector} using the given serviceId.
     *
     * @param serviceId             id used to create the server connector instance.
     * @param connectorConfig       properties required for the {@link JMSServerConnector} class.
     * @param jmsListener           listener which gets triggered when message comes.
     * @return jmsServerConnector   newly created JMS server connector instance.
     * @throws JMSConnectorException
     */
    JMSServerConnector createServerConnector(String serviceId, Map<String, String> connectorConfig,
                                             JMSListener jmsListener) throws JMSConnectorException;

    /**
     * Returns an instance of the {@link JMSClientConnector} class.
     *
     * @return jmsClientConnector   instance.
     * @throws JMSConnectorException
     */
    JMSClientConnector createClientConnector() throws JMSConnectorException;
}
