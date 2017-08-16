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
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
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

    @Override
    public Object init(CarbonMessage carbonMessage, CarbonCallback carbonCallback, Map<String, Object> map)
            throws ClientConnectorException {
        throw new ClientConnectorException("Method not supported for Email.");
    }

    /**
     * @return false because, in this instance, the send method with a map parameter is required.
     */
    @Override
    public boolean send(CarbonMessage carbonMessage, CarbonCallback carbonCallback)
            throws ClientConnectorException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean  send(CarbonMessage carbonMessage, CarbonCallback carbonCallback,
            Map<String, String> emailProperties) throws ClientConnectorException {
        String username;
        String password;

        Properties serverProperties = new Properties();

        if (emailProperties.get(EmailConstants.MAIL_SENDER_USERNAME) != null &&
                !emailProperties.get(EmailConstants.MAIL_SENDER_USERNAME).isEmpty()) {
            username = emailProperties.get(EmailConstants.MAIL_SENDER_USERNAME);
        } else {
            throw new ClientConnectorException(
                    "Username (email address) of the email account is" + " a mandatory parameter."
                            + "It is not given in the email property map.");
        }

        if (emailProperties.get(EmailConstants.MAIL_SENDER_PASSWORD) != null &&
                !emailProperties.get(EmailConstants.MAIL_SENDER_PASSWORD).isEmpty()) {
            password = emailProperties.get(EmailConstants.MAIL_SENDER_PASSWORD);
        } else {
            throw new ClientConnectorException("Password of the email account is" + " a mandatory parameter."
                    + "It is not given in the email property map.");
        }


        if (!(carbonMessage instanceof TextCarbonMessage)) {
            throw new ClientConnectorException("Email client connector is support Text Carbon Message only.");
        }


        for (Map.Entry<String, String> entry : emailProperties.entrySet()) {
            if (entry.getKey().startsWith("mail.")) {
                serverProperties.put(entry.getKey(), entry.getValue());
            }
        }

        Session session = Session.getInstance(serverProperties, new EmailAuthenticator(username, password));

        Message message = createMessage(session, carbonMessage, emailProperties);

        try {
            Transport.send(message);
        } catch (MessagingException e) {
            throw new ClientConnectorException("Error occurred while sending the message.", e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Message is send successfully" + message.toString());
        }
        return false;
    }

    /**
     * Create email message from the carbon message and given properties.
     *
     * @param session Instance of the session relevant to smtp server
     * @param carbonMessage Carbon message which have o convert the email message
     * @param emailProperties Properties of the email client connector
     * @return Message is to be send to the given recipients
     * @throws ClientConnectorException ClientConnectorException when action is failed
     *                                 due to a email layer error.
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
                        "Email content type should be either '" + EmailConstants.CONTENT_TYPE_TEXT_PLAIN +
                                "' or '" + EmailConstants.CONTENT_TYPE_TEXT_HTML + "'. But found '"
                                + emailProperties.get(EmailConstants.CONTENT_TYPE) + "'");
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
            throw new ClientConnectorException("Error occurred while creating the email "
                    + "using given carbon message. " +  e.getMessage() , e);
        }

        return message;

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getProtocol() {
        return EmailConstants.PROTOCOL_MAIL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMessageProcessor(CarbonMessageProcessor carbonMessageProcessor) {

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
