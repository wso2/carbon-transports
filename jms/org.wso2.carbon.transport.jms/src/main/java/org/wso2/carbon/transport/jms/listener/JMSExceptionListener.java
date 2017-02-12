package org.wso2.carbon.transport.jms.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;

/**
 * Listener that listens to the problem in the jms connection.
 */
class JMSExceptionListener implements ExceptionListener {
    private static final Logger logger = LoggerFactory.getLogger(JMSExceptionListener.class);
    private JMSServerConnector jmsServerConnector;
    private long retryInterval;
    private int maxRetryCount;

    /**
     * Creates a exception listening to track the exceptions in jms connection and to handle it.
     *
     * @param jmsServerConnector JMS Server connector related with the particular connection
     * @param retryInterval      Retry interval in milli seconds
     * @param maxRetryCount      Maximum retry count to try to connect
     */
    JMSExceptionListener(JMSServerConnector jmsServerConnector, long retryInterval, int maxRetryCount) {
        this.jmsServerConnector = jmsServerConnector;
        this.retryInterval = retryInterval;
        this.maxRetryCount = maxRetryCount;
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
            jmsServerConnector.createMessageListener();
        } catch (JMSConnectorException e) {
            JMSConnectionRetryHandler jmsConnectionRetryHandler = new JMSConnectionRetryHandler(jmsServerConnector,
                    retryInterval, maxRetryCount);
            try {
                jmsConnectionRetryHandler.retry();
            } catch (JMSConnectorException e1) {
                throw new RuntimeException(
                        "Cannot establish the connection again after retrying for " + maxRetryCount + " times", e1);
            }

        }
    }
}
