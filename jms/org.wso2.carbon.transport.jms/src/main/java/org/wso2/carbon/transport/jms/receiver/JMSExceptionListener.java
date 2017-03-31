package org.wso2.carbon.transport.jms.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;

/**
 * Listener that listens to the problem in the jms connection.
 */
public class JMSExceptionListener implements ExceptionListener {
    private static final Logger logger = LoggerFactory.getLogger(JMSExceptionListener.class);
    /**
     * This {@link JMSServerConnector} instance represents the jms receiver that is related with this exception
     * receiver.
     */
    private JMSServerConnector jmsServerConnector;

    /**
     * Creates a exception listening to track the exceptions in jms connection and to handle it.
     *
     * @param jmsServerConnector JMS Server connector related with the particular connection
     */
    JMSExceptionListener(JMSServerConnector jmsServerConnector) {
        this.jmsServerConnector = jmsServerConnector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onException(JMSException exception) {
        logger.error("Error in the JMS connection. " + exception.getMessage());
        try {
            jmsServerConnector.closeAll();
        } catch (JMSConnectorException e) {
            /*
             * No need to throw the exception, as JMS provider has already informed that there is a problem in the
              * connection
             */
            logger.error("Error while closing the connection, session or consumer after receiving the exception call "
                    + "from jms provider. ", e);
        }
        try {
            jmsServerConnector.startConsuming();
        } catch (JMSConnectorException e) {
            throw new RuntimeException(
                    "Cannot establish the connection after retrying", e);

        }
    }
}
