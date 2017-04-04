package org.wso2.carbon.transport.jms.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

import java.util.ArrayList;
import java.util.List;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;

/**
 * Listener that listens to the problem in the jms connection.
 */
public class JMSExceptionListener implements ExceptionListener {
    private static final Logger logger = LoggerFactory.getLogger(JMSExceptionListener.class);
    /**
     * List of {@link JMSMessageConsumer} instances that this handler listens to for exceptions
     * receiver.
     */
    private List<JMSMessageConsumer> messageConsumers = new ArrayList<>();

    /**
     * Creates a exception listening to track the exceptions in jms connection and to handle it.
     *
     * @param messageConsumer JMS consumer related with the particular connection
     */
    JMSExceptionListener(JMSMessageConsumer messageConsumer) {
        messageConsumers.add(messageConsumer);
    }

    public void addConsumer(JMSMessageConsumer messageConsumer) {
        messageConsumers.add(messageConsumer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onException(JMSException exception) {

        /*
            Quoting from JMS 2.0 Spec - https://jms-spec.java.net/2.0/apidocs/javax/jms/Session.html

            "Once a connection has been started, any session with one or more registered message listeners is
            dedicated to the thread of control that delivers messages to it. It is erroneous for client code to use
            this session or any of its constituent objects from another thread of control. The only exception to this
            rule is the use of the session or message consumer close method."

            Therefore we shouldn't do anything other than closing the consumers and restarting them within this method.
         */

        logger.error("Error in the JMS connection. " + exception.getMessage());

        for (JMSMessageConsumer messageConsumer : messageConsumers) {
            try {
                messageConsumer.closeAll();
            } catch (JMSConnectorException e) {
                // If an exception was triggered due to a connection issue, closing connection will also fail with a
                // connection exception
                logger.error("Error closing connection after exception", e);
            }

            try {
                messageConsumer.startConsuming();
            } catch (JMSConnectorException e) {
                throw new RuntimeException(
                        "Cannot establish the connection after retrying", e);

            }
        }
    }
}
