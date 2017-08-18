/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.transport.email.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.ClientConnector;
import org.wso2.carbon.messaging.TextCarbonMessage;
import org.wso2.carbon.messaging.exceptions.ClientConnectorException;
import org.wso2.carbon.transport.email.utils.EmailConstants;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Class is implementing email client connector
 */
public class EmailClientConnector implements ClientConnector {
    private static final Logger logger = LoggerFactory.getLogger(EmailClientConnector.class);
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> waitTillTimeOut;
    private Session session;
    private Transport transport;
    private Boolean isInitMethodCalled = false;

    @Override public Object init(CarbonMessage carbonMessage, CarbonCallback carbonCallback,
            Map<String, Object> properties) throws ClientConnectorException {
        Properties serverProperties = new Properties();

        //mail server properties start with mail.smtp. Get them from properties map and put into serverProperties
        try {
            properties.forEach((key, value) -> {
                if (key.startsWith("mail.smtp")) {
                    serverProperties.put(key, (String) value);
                }
            });
        } catch (Exception e) {
            throw new ClientConnectorException(
                    "Error is encountered while" + " casting value of property map" + " into String." + e.getMessage(),
                    e);
        }

        String username = (String) properties.get(EmailConstants.MAIL_SENDER_USERNAME);
        String password = (String) properties.get(EmailConstants.MAIL_SENDER_PASSWORD);

        if (null == username || username.isEmpty()) {
            throw new ClientConnectorException("Username of the email account is" + " a mandatory parameter."
                    + "It is not given in the email property map.");
        }

        if (null == password || password.isEmpty()) {
            throw new ClientConnectorException("Password of the email account is" + " a mandatory parameter."
                    + "It is not given in the email property map.");
        }

        session = Session.getInstance(serverProperties, new EmailAuthenticator(username, password));

        try {
            transport = session.getTransport();
            transport.connect();
        } catch (NoSuchProviderException e) {
            throw new ClientConnectorException(
                    "Error is encountered while creating transport" + " using the session." + e.getMessage(), e);
        } catch (MessagingException e) {
            throw new ClientConnectorException(
                    "Error is encountered while creating " + " the connection using the session." + e.getMessage(), e);

        }

        isInitMethodCalled = true;
        return true;
    }

    /**
     * @return false because, in this instance, the send method with a map parameter is required.
     */
    @Override public boolean send(CarbonMessage carbonMessage, CarbonCallback carbonCallback)
            throws ClientConnectorException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override public synchronized boolean send(CarbonMessage carbonMessage, CarbonCallback carbonCallback,
            Map<String, String> emailProperties) throws ClientConnectorException {

        if (!isInitMethodCalled) {
            throw new ClientConnectorException(
                    "Should call 'init' method first," + " before calling the 'send' method");
        }

        // cancel the thread which wait to close the connection
        if (waitTillTimeOut != null && !waitTillTimeOut.isDone()) {
            waitTillTimeOut.cancel(true);
        }

        Message message = createMessage(session, carbonMessage, emailProperties);

        try {
            message.saveChanges();
        } catch (MessagingException e) {
            throw new ClientConnectorException("Error is encountered while saving the messages" + e.getMessage(), e);
        }

        if (transport.isConnected()) {
            try {
                transport.sendMessage(message, message.getAllRecipients());
            } catch (Exception e) {
                throw new ClientConnectorException(
                        "Error is encountered while sending" + " the message. " + e.getMessage(), e);
            }
        } else {
            try {
                transport.connect();
            } catch (MessagingException e) {
                throw new ClientConnectorException("Error is encountered while connecting " + e.getMessage(), e);
            }

            try {
                transport.sendMessage(message, message.getAllRecipients());
            } catch (MessagingException e) {
                throw new ClientConnectorException(
                        "Error is encountered while sending" + " the message." + e.getMessage(), e);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Message is send successfully" + message.toString());
        }

        //scheduler to close the connection after 5 minutes if send method is not called during that period.
        //If send method is call during the period then schedule is reset.
        waitTillTimeOut = scheduler.schedule(() -> {
            if (transport.isConnected()) {
                try {
                    transport.close();
                } catch (Exception e) {
                    logger.warn("Error is encountered while closing the connection after waiting "
                            + EmailConstants.WAIT_TIME + " minutes. " + e.getMessage(), e);
                }
            }
        }, EmailConstants.WAIT_TIME, TimeUnit.MINUTES);

        return false;
    }

    /**
     * Create email message from the carbon message and given properties.
     *
     * @param session         Instance of the session relevant to smtp server
     * @param carbonMessage   Carbon message which have o convert the email message
     * @param emailProperties Properties of the email client connector
     * @return Message is to be send to the given recipients
     * @throws ClientConnectorException ClientConnectorException when action is failed
     *                                  due to a email layer error.
     */
    private Message createMessage(Session session, CarbonMessage carbonMessage, Map<String, String> emailProperties)
            throws ClientConnectorException {
        Message message = new MimeMessage(session);
        String contentType;

        if (emailProperties.get(EmailConstants.MAIL_HEADER_CONTENT_TYPE) != null) {
            if (emailProperties.get(EmailConstants.MAIL_HEADER_CONTENT_TYPE).
                    equalsIgnoreCase(EmailConstants.CONTENT_TYPE_TEXT_PLAIN)) {
                contentType = EmailConstants.CONTENT_TYPE_TEXT_PLAIN;
            } else if (emailProperties.get(EmailConstants.MAIL_HEADER_CONTENT_TYPE)
                    .equalsIgnoreCase(EmailConstants.CONTENT_TYPE_TEXT_HTML)) {
                contentType = EmailConstants.CONTENT_TYPE_TEXT_HTML;
            } else {
                throw new ClientConnectorException(
                        "Email content type should be either '" + EmailConstants.CONTENT_TYPE_TEXT_PLAIN + "' or '"
                                + EmailConstants.CONTENT_TYPE_TEXT_HTML + "'. But found '" + emailProperties
                                .get(EmailConstants.CONTENT_TYPE) + "'");
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Email content type is not given. Default value: is taken as a content type");
            }
            contentType = EmailConstants.DEFAULT_CONTENT_TYPE;
        }

        String textData = ((TextCarbonMessage) carbonMessage).getText();

        try {
            message.setContent(textData, contentType);
            if (emailProperties.get(EmailConstants.MAIL_HEADER_SUBJECT) != null) {
                message.setSubject(emailProperties.get(EmailConstants.MAIL_HEADER_SUBJECT));
            } else {
                throw new ClientConnectorException("Subject of the email is not given");
            }

            if (emailProperties.get(EmailConstants.MAIL_HEADER_TO) != null) {
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(emailProperties.get(EmailConstants.MAIL_HEADER_TO)));
            } else {
                throw new ClientConnectorException("RecipientType 'to' of the email is not given."
                        + " It is a mandatory parameter in email client connector.");
            }

            if (emailProperties.get(EmailConstants.MAIL_HEADER_BCC) != null) {
                message.setRecipients(Message.RecipientType.BCC,
                        InternetAddress.parse(emailProperties.get(EmailConstants.MAIL_HEADER_BCC)));
            }

            if (emailProperties.get(EmailConstants.MAIL_HEADER_CC) != null) {
                message.setRecipients(Message.RecipientType.CC,
                        InternetAddress.parse(emailProperties.get(EmailConstants.MAIL_HEADER_CC)));
            }

            if (emailProperties.get(EmailConstants.MAIL_HEADER_FROM) != null) {
                message.setFrom(new InternetAddress(emailProperties.get(EmailConstants.MAIL_HEADER_FROM)));
            }

            if (emailProperties.get(EmailConstants.MAIL_HEADER_REPLY_TO) != null) {
                InternetAddress[] addresses = {
                        new InternetAddress(emailProperties.get(EmailConstants.MAIL_HEADER_REPLY_TO)) };
                message.setReplyTo(addresses);
            }

            if (emailProperties.get(EmailConstants.MAIL_HEADER_IN_REPLY_TO) != null) {
                message.setHeader(EmailConstants.MAIL_HEADER_IN_REPLY_TO,
                        emailProperties.get(EmailConstants.MAIL_HEADER_IN_REPLY_TO));
            }

            if (emailProperties.get(EmailConstants.MAIL_HEADER_MESSAGE_ID) != null) {
                message.setHeader(EmailConstants.MAIL_HEADER_MESSAGE_ID,
                        emailProperties.get(EmailConstants.MAIL_HEADER_MESSAGE_ID));
            }

            if (emailProperties.get(EmailConstants.MAIL_HEADER_REFERENCES) != null) {
                message.setHeader(EmailConstants.MAIL_HEADER_REFERENCES,
                        emailProperties.get(EmailConstants.MAIL_HEADER_REFERENCES));
            }

        } catch (MessagingException e) {
            throw new ClientConnectorException(
                    "Error occurred while creating the email " + "using given carbon message. " + e.getMessage(), e);
        }

        return message;

    }

    /**
     * {@inheritDoc}
     */
    @Override public String getProtocol() {
        return EmailConstants.PROTOCOL_MAIL;
    }

    /**
     * {@inheritDoc}
     */
    @Override public void setMessageProcessor(CarbonMessageProcessor carbonMessageProcessor) {

    }

    /**
     * Inner class implemented by extending Authenticator to get password authentication
     */
    private static class EmailAuthenticator extends Authenticator {
        private final String username;
        private final String password;

        public EmailAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
        }
    }
}
