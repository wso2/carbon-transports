/*
 * Copyright (c) 2017 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.transport.email.client.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.email.contract.EmailClientConnector;
import org.wso2.carbon.transport.email.contract.message.EmailBaseMessage;
import org.wso2.carbon.transport.email.contract.message.EmailTextMessage;
import org.wso2.carbon.transport.email.exception.EmailConnectorException;
import org.wso2.carbon.transport.email.utils.Constants;

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
 * Implementation of {@link EmailClientConnector}
 */
public class EmailClientConnectorImpl implements EmailClientConnector {

    /**
     * Logger to log the events.
     */
    private static final Logger logger = LoggerFactory.getLogger(EmailClientConnector.class);

    /**
     * Single thread scheduleExecutor to execute connection closing task.
     */
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Instance of ScheduledFuture<?>
     */
    private ScheduledFuture<?> waitTillTimeOut;

    /**
     * Session used to create the email connection via smtp host.
     */
    private Session session;

    /**
     * Instance of transport that is created using session.
     */
    private Transport transport;

    /**
     * Instance of Boolean value used to check the init method is called before send method is called.
     */
    private Boolean isInitMethodCalled = false;

    /**
     * int value used to store the time after which connection should close if it is ideal for a given time
     * in minutes.
     */
    private Integer waitTimeBeforeConnectionClose;

    public EmailClientConnectorImpl() {
    }

    @Override
    public void init(Map<String, String> properties) throws EmailConnectorException {
        Properties serverProperties = new Properties();

        //mail server properties start with mail.smtp. Get them from properties map and put into serverProperties
        try {
            properties.forEach((key, value) -> {
                if (key.startsWith("mail.smtp")) {
                    serverProperties.put(key, value);
                }
            });
        } catch (Exception e) {
            throw new EmailConnectorException(
                    "Error is encountered while" + " casting value of property map" + " into String." + e.getMessage(),
                    e);
        }

        String username = properties.get(Constants.MAIL_SENDER_USERNAME);
        String password = properties.get(Constants.MAIL_SENDER_PASSWORD);

        if (null == username || username.isEmpty()) {
            throw new EmailConnectorException("Username of the email account is" + " a mandatory parameter."
                    + "It is not given in the email property map.");
        }

        if (null == password || password.isEmpty()) {
            throw new EmailConnectorException("Password of the email account is" + " a mandatory parameter."
                    + "It is not given in the email property map.");
        }

        session = Session.getInstance(serverProperties, new EmailAuthenticator(username, password));

        try {
            transport = session.getTransport();
            transport.connect();
        } catch (NoSuchProviderException e) {
            throw new EmailConnectorException(
                    "Error is encountered while creating transport" + " using the session." + e.getMessage(), e);
        } catch (MessagingException e) {
            throw new EmailConnectorException(
                    "Error is encountered while creating " + " the connection using the session." + e.getMessage(), e);

        }

        if (null != properties.get(Constants.WAIT_TIME)) {
            //if wait time is not given by the user or defined as null then connection will keep forever.
            waitTimeBeforeConnectionClose = Integer.parseInt(properties.get(Constants.WAIT_TIME));
        }

        isInitMethodCalled = true;
    }

    @Override 
    public synchronized void send(EmailBaseMessage emailMessage) throws EmailConnectorException {

        if (!isInitMethodCalled) {
            throw new EmailConnectorException(
                    "Should call 'init' method first," + " before calling the 'send' method");
        }

        // cancel the thread which wait to close the connection
        if (null != waitTimeBeforeConnectionClose) {
            if (waitTillTimeOut != null && !waitTillTimeOut.isDone()) {
                waitTillTimeOut.cancel(true);
            }
        }

        Message message = createMessage(session, emailMessage);

        try {
            message.saveChanges();
        } catch (MessagingException e) {
            throw new EmailConnectorException("Error is encountered while saving the messages" + e.getMessage(), e);
        }

        if (transport.isConnected()) {
            try {
                transport.sendMessage(message, message.getAllRecipients());
            } catch (Exception e) {
                throw new EmailConnectorException(
                        "Error is encountered while sending" + " the message. " + e.getMessage(), e);
            }
        } else {
            try {
                transport.connect();
            } catch (MessagingException e) {
                throw new EmailConnectorException("Error is encountered while connecting " + e.getMessage(), e);
            }

            try {
                transport.sendMessage(message, message.getAllRecipients());
            } catch (MessagingException e) {
                throw new EmailConnectorException(
                        "Error is encountered while sending" + " the message." + e.getMessage(), e);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Message is send successfully" + message.toString());
        }

        //scheduler to close the connection after waitTimeBeforeConnectionClose minutes if send method is
        // not called during that period.
        //If send method is call during the period then schedule is reset.
        if (null != waitTimeBeforeConnectionClose) {
            waitTillTimeOut = scheduler.schedule(() -> {
                if (transport.isConnected()) {
                    try {
                        transport.close();
                    } catch (Exception e) {
                        logger.warn("Error is encountered while closing the connection after waiting " +
                                Constants.WAIT_TIME + " minutes. " + e.getMessage(), e);
                    }
                }
            }, waitTimeBeforeConnectionClose, TimeUnit.MINUTES);
        }
    }

    /**
     * Create a {@link javax.mail.Message} object using given Email message. Message headers can be
     *
     * @param session         Instance of the session relevant to smtp server
     * @param emailMessage   Carbon message which have to convert the email message
     * @return Message is to be send to the given recipients
     * @throws EmailConnectorException EmailConnectorException when action is failed
     *                                  due to a email layer error.
     */
    private Message createMessage(Session session, EmailBaseMessage emailMessage)
            throws EmailConnectorException {
        Message message = new MimeMessage(session);
        String contentType;
        String textData;

        if (emailMessage.getHeader(Constants.MAIL_HEADER_CONTENT_TYPE) != null) {
            if (emailMessage.getHeader(Constants.MAIL_HEADER_CONTENT_TYPE).
                    equalsIgnoreCase(Constants.CONTENT_TYPE_TEXT_PLAIN)) {
                contentType = Constants.CONTENT_TYPE_TEXT_PLAIN;
            } else if (emailMessage.getHeader(Constants.MAIL_HEADER_CONTENT_TYPE)
                    .equalsIgnoreCase(Constants.CONTENT_TYPE_TEXT_HTML)) {
                contentType = Constants.CONTENT_TYPE_TEXT_HTML;
            } else {
                throw new EmailConnectorException(
                        "Email content type should be either '" + Constants.CONTENT_TYPE_TEXT_PLAIN + "' or '"
                                + Constants.CONTENT_TYPE_TEXT_HTML + "'. But found '"
                                + emailMessage.getHeader(Constants.CONTENT_TYPE) + "'");
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Email content type is not given. Default value: is taken as a content type");
            }
            contentType = Constants.DEFAULT_CONTENT_TYPE;
        }

        if (emailMessage instanceof EmailTextMessage) {
            textData = ((EmailTextMessage) emailMessage).getText();
            if (textData == null) {
                throw new EmailConnectorException("Email message content couldn't be null");
            }
        } else {
            throw new EmailConnectorException("Email client connector only support EmailTextMessage");
        }

        try {
            message.setContent(textData, contentType);
            if (emailMessage.getHeader(Constants.MAIL_HEADER_SUBJECT) != null) {
                message.setSubject(emailMessage.getHeader(Constants.MAIL_HEADER_SUBJECT));
            } else {
                throw new EmailConnectorException("Subject of the email is not given");
            }

            if (emailMessage.getHeader(Constants.MAIL_HEADER_TO) != null) {
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(emailMessage.getHeader(Constants.MAIL_HEADER_TO)));
            } else {
                throw new EmailConnectorException("RecipientType 'to' of the email is not given."
                        + " It is a mandatory parameter in email client connector.");
            }

            if (emailMessage.getHeader(Constants.MAIL_HEADER_BCC) != null) {
                message.setRecipients(Message.RecipientType.BCC,
                        InternetAddress.parse(emailMessage.getHeader(Constants.MAIL_HEADER_BCC)));
            }

            if (emailMessage.getHeader(Constants.MAIL_HEADER_CC) != null) {
                message.setRecipients(Message.RecipientType.CC,
                        InternetAddress.parse(emailMessage.getHeader(Constants.MAIL_HEADER_CC)));
            }

            if (emailMessage.getHeader(Constants.MAIL_HEADER_FROM) != null) {
                message.setFrom(new InternetAddress(emailMessage.getHeader(Constants.MAIL_HEADER_FROM)));
            }

            if (emailMessage.getHeader(Constants.MAIL_HEADER_REPLY_TO) != null) {
                InternetAddress[] addresses = {
                        new InternetAddress(emailMessage.getHeader(Constants.MAIL_HEADER_REPLY_TO)) };
                message.setReplyTo(addresses);
            }

            if (emailMessage.getHeader(Constants.MAIL_HEADER_IN_REPLY_TO) != null) {
                message.setHeader(Constants.MAIL_HEADER_IN_REPLY_TO,
                        emailMessage.getHeader(Constants.MAIL_HEADER_IN_REPLY_TO));
            }

            if (emailMessage.getHeader(Constants.MAIL_HEADER_MESSAGE_ID) != null) {
                message.setHeader(Constants.MAIL_HEADER_MESSAGE_ID,
                        emailMessage.getHeader(Constants.MAIL_HEADER_MESSAGE_ID));
            }

            if (emailMessage.getHeader(Constants.MAIL_HEADER_REFERENCES) != null) {
                message.setHeader(Constants.MAIL_HEADER_REFERENCES,
                        emailMessage.getHeader(Constants.MAIL_HEADER_REFERENCES));
            }

        } catch (MessagingException e) {
            throw new EmailConnectorException(
                    "Error occurred while creating the email " + "using given carbon message. " + e.getMessage(), e);
        }

        return message;
    }

    /**
     * Inner class implemented by extending {@link javax.mail.Authenticator} to get password authentication
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
