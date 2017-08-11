package org.wso2.carbon.transport.jms.contract;

import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

/**
 * Inlet of inbound messages
 */
public interface JMSServerConnector {
    void start() throws JMSConnectorException;
    boolean stop() throws JMSConnectorException;
}
