package org.wso2.carbon.transport.jms.contract;

import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

import java.util.Map;

/**
 * Allows to send outbound messages
 */
public interface JMSClientConnector {

    /**
     * Message sending logic to send message to a backend endpoint. Additionally, this method accepts a map of
     * parameters that is used as data to create the connection and construct the message to be send.
     *
     * @param message the carbon message used with sending the a message to backend.
     * @param propertyMap data passed from application level to be used with creating the message.
     * @return return true if the sending was successful, false otherwise.
     * @throws JMSConnectorException on error while trying to send message to backend.
     */
    boolean send(CarbonMessage message, Map<String, String> propertyMap) throws JMSConnectorException;
}
