package org.wso2.carbon.transport.jms.contract;

import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

/**
 * Inlet of inbound messages
 */
public interface JMSServerConnector {

    /**
     * Start the server connector which actually starts listening for jms messages.
     */
    void start() throws JMSConnectorException;

    /**
     * Stops the server connector which actually closes the port.
     *
     * @return state of action.
     */
    boolean stop() throws JMSConnectorException;
}
