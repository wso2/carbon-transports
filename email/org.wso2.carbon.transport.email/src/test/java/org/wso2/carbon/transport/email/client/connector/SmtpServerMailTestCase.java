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

package org.wso2.carbon.transport.email.client.connector;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.messaging.exceptions.ClientConnectorException;
import org.wso2.carbon.transport.email.connector.factory.EmailConnectorFactoryImpl;
import org.wso2.carbon.transport.email.contract.EmailClientConnector;
import org.wso2.carbon.transport.email.contract.EmailConnectorFactory;
import org.wso2.carbon.transport.email.contract.message.EmailBaseMessage;
import org.wso2.carbon.transport.email.contract.message.EmailTextMessage;
import org.wso2.carbon.transport.email.exception.EmailConnectorException;
import org.wso2.carbon.transport.email.utils.EmailTestConstant;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * Class implementing test cases to test email sender. GreenMail is used to create local smtp server.
 */
public class SmtpServerMailTestCase {
    private static final Logger log = Logger.getLogger(SmtpServerMailTestCase.class);
    private static final String PASSWORD = "carbon123";
    private static final String USERNAME = "carbon";
    private static final String ADDRESS = "carbon@localhost.com";
    private static final String HOST = "127.0.0.1";
    private Map<String, String> initProperties;
    private GreenMail mailServer;

    @BeforeMethod(description = "setup parameters need to create a smtp server.")
    public void setMailSenderParameters() {

        initProperties = new HashMap<>();
        initProperties.put(EmailTestConstant.MAIL_SENDER_USERNAME, USERNAME);
        initProperties.put(EmailTestConstant.MAIL_SENDER_PASSWORD, PASSWORD);
        //green mail server use port 3025 to create a connection to local email server.
        initProperties.put("mail.smtp.port", "3025");
        //It is required to set auth 'true' to create connection.
        initProperties.put("mail.smtp.auth", "true");
        initProperties.put("mail.smtp.host", HOST);

        //start the green mail server
        mailServer = new GreenMail(ServerSetupTest.SMTP);
        mailServer.start();
    }

    @AfterMethod(description = "stop the green mail server")
    public void tearDown() {
        mailServer.stop();
    }

    @Test(description = "Test case to send emails via smtp server.")
    public void sendingEmailViaSmtpServerTestCase()
            throws Exception {
        // create user on mail server
        mailServer.setUser(ADDRESS, USERNAME, PASSWORD);
        EmailConnectorFactory connectorFactory = new EmailConnectorFactoryImpl();
        EmailClientConnector clientConnector = connectorFactory.createEmailClientConnector();
        clientConnector.init(initProperties);
        EmailBaseMessage emailMessage = createEmailMessage("This is test message", "text/plain",
                "This is test message", "to@localhost", "cc@localhost", "bcc@localhost");
        emailMessage.setHeader(EmailTestConstant.MAIL_HEADER_IN_REPLY_TO, "ab@localHost");
        emailMessage.setHeader(EmailTestConstant.MAIL_HEADER_REPLY_TO, "replyto@localHost");
        emailMessage.setHeader(EmailTestConstant.MAIL_HEADER_MESSAGE_ID, "1234");
        clientConnector.send(emailMessage);
        Thread.sleep(1000);
        MimeMessage[] messages = mailServer.getReceivedMessages();
        Assert.assertEquals(messages.length, 3);
    }

    @Test(description = "Test case to send emails via smtp server with multiple to recipient.")
    public void sendingWithMultipleRecipients()
            throws IOException, MessagingException, ClientConnectorException, InterruptedException,
            EmailConnectorException {
        // create user on mail server
        mailServer.setUser(ADDRESS, USERNAME, PASSWORD);
        EmailConnectorFactory connectorFactory = new EmailConnectorFactoryImpl();
        EmailClientConnector clientConnector = connectorFactory.createEmailClientConnector();
        clientConnector.init(initProperties);
        EmailBaseMessage emailMessage = createEmailMessage("This is test message", "text/html",
                "This is test message", "to1@localhost, to2@localhost, to3@localhost", null, null);
        clientConnector.send(emailMessage);
        Thread.sleep(1000);
        MimeMessage[] messages = mailServer.getReceivedMessages();
        Assert.assertEquals(messages.length, 3);
    }

    @Test(description = "Test case to test invalid contentType is given",
            expectedExceptions = EmailConnectorException.class)
    public void invalidContentTypeIsGiven()
            throws IOException, MessagingException, ClientConnectorException, InterruptedException,
            EmailConnectorException {
        // create user on mail server
        mailServer.setUser(ADDRESS, USERNAME, PASSWORD);
        EmailConnectorFactory connectorFactory = new EmailConnectorFactoryImpl();
        EmailClientConnector clientConnector = connectorFactory.createEmailClientConnector();
        clientConnector.init(initProperties);
        EmailBaseMessage emailMessage = createEmailMessage("This is test message", "attachment",
                "This is test message", "to1@localhost, to2@localhost, to3@localhost", null, null);
        clientConnector.send(emailMessage);
    }

    @Test(description = "Test case to send emails via smtp server with wait time to close the connection is zero.")
    public void sendingWithWaitTimeToCloseConnectionIsZero()
            throws IOException, MessagingException, ClientConnectorException, InterruptedException,
            EmailConnectorException {

        initProperties.put(EmailTestConstant.WAIT_TIME, "0");
        // create user on mail server
        mailServer.setUser(ADDRESS, USERNAME, PASSWORD);
        EmailConnectorFactory connectorFactory = new EmailConnectorFactoryImpl();
        EmailClientConnector clientConnector = connectorFactory.createEmailClientConnector();
        clientConnector.init(initProperties);

        EmailBaseMessage emailMessage1 = createEmailMessage("This is test message",
                "text/plain", "This is test message", "to1@localhost", null, null);
        EmailBaseMessage emailMessage2 = createEmailMessage("This is second test message",
                "text/plain", "This is test message", "to2@localhost", null, null);

        clientConnector.send(emailMessage1);
        Thread.sleep(1000);
        clientConnector.send(emailMessage2);
        Thread.sleep(1000);
        MimeMessage[] messages = mailServer.getReceivedMessages();
        Assert.assertEquals(messages.length, 2);
    }

    @Test(description = "Test case user name is not given in server parameters.",
            expectedExceptions = EmailConnectorException.class)
    public void usernameIsNotGivenTestCase()
            throws IOException, MessagingException, ClientConnectorException, InterruptedException,
            EmailConnectorException {
        // create user on mail server
        mailServer.setUser(ADDRESS, USERNAME, PASSWORD);
        initProperties.put(EmailTestConstant.MAIL_SENDER_USERNAME, null);
        EmailConnectorFactory connectorFactory = new EmailConnectorFactoryImpl();
        EmailClientConnector clientConnector = connectorFactory.createEmailClientConnector();
        clientConnector.init(initProperties);
    }

    @Test(description = "Test case where password is not given in server parameters.",
            expectedExceptions = EmailConnectorException.class)
    public void passwordIsNotGivenTestCase()
            throws IOException, MessagingException, ClientConnectorException, InterruptedException,
            EmailConnectorException {
        // create user on mail server
        mailServer.setUser(ADDRESS, USERNAME, PASSWORD);
        initProperties.put(EmailTestConstant.MAIL_SENDER_PASSWORD, null);
        EmailConnectorFactory connectorFactory = new EmailConnectorFactoryImpl();
        EmailClientConnector clientConnector = connectorFactory.createEmailClientConnector();
        clientConnector.init(initProperties);
    }

    @Test(description = "Test case where invalid server is given in server parameters.",
            expectedExceptions = EmailConnectorException.class)
    public void invalidServerParameterIsGivenTestCase()
            throws IOException, MessagingException, ClientConnectorException, InterruptedException,
            EmailConnectorException {
        // create user on mail server
        mailServer.setUser(ADDRESS, USERNAME, PASSWORD);
        initProperties.put("mail.smtp.port", "3000");
        EmailConnectorFactory connectorFactory = new EmailConnectorFactoryImpl();
        EmailClientConnector clientConnector = connectorFactory.createEmailClientConnector();
        clientConnector.init(initProperties);
    }

    /**
     * Method implemented to create a email text message with relevant headers and properties.
     *
     * @param content       Content which need to be set in email body.
     * @param contentType   Content type of the email.
     * @param subject       Subject of the email.
     * @param to            To recipients of the email
     * @param cc            Cc recipients of the email
     * @param bcc           Bcc recipients of the email
     * @return              EmailBaseMessage created by setting headers and properties.
     */
    EmailBaseMessage createEmailMessage(String content, String contentType, String subject, String to,
            String cc, String bcc) {
        EmailBaseMessage emailMessage = new EmailTextMessage(content);
        emailMessage.setHeader(EmailTestConstant.MAIL_HEADER_CONTENT_TYPE, contentType);
        emailMessage.setHeader(EmailTestConstant.MAIL_HEADER_SUBJECT, subject);
        emailMessage.setHeader(EmailTestConstant.MAIL_HEADER_TO, to);
        emailMessage.setHeader(EmailTestConstant.MAIL_HEADER_CC, cc);
        emailMessage.setHeader(EmailTestConstant.MAIL_HEADER_BCC, bcc);

        return emailMessage;
    }
}
