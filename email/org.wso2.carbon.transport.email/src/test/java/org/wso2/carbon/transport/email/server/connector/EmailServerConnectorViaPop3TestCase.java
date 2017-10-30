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

package org.wso2.carbon.transport.email.server.connector;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.email.connector.factory.EmailConnectorFactoryImpl;
import org.wso2.carbon.transport.email.contract.EmailConnectorFactory;
import org.wso2.carbon.transport.email.contract.EmailServerConnector;
import org.wso2.carbon.transport.email.exception.EmailConnectorException;
import org.wso2.carbon.transport.email.utils.EmailTestConstant;
import org.wso2.carbon.transport.email.utils.TestEmailMessageListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Class implementing the server connector test cases. Pop3 protocol is used to receive mails.
 */
public class EmailServerConnectorViaPop3TestCase {

    private static final Logger log = Logger.getLogger(EmailServerConnectorViaPop3TestCase.class);
    private static final String PASSWORD = "carbon123";
    private static final String USER_NAME = "carbon";
    private static final String ADDRESS = "carbon@localhost.com";
    private static final String EMAIL_FROM = "someone@localhost.com";
    private static final String EMAIL_SUBJECT = "Test Pop3 server";
    private static final String EMAIL_TEXT = "This is a e-mail to test receive mails via pop3 server.";
    private static final String LOCALHOST = "127.0.0.1";
    private static final String STORE_TYPE = "pop3";
    private Map<String, String> emailProperties;
    /**
     * Server provided by Green mail to create local mail server.
     */
    private GreenMail mailServer;

    @BeforeMethod public void setUp() {
        setEmailSeverConnectorProperties();
        mailServer = new GreenMail(ServerSetupTest.POP3);
        mailServer.start();
    }

    @AfterMethod public void tearDown() {
        mailServer.stop();
    }

    @Test(description = "Test the scenario: receiving messages via pop3 server")
    public void receiveMailViaPop3Server()
            throws IOException, MessagingException, InterruptedException, ServerConnectorException {

        // create user on mail server
        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);

        // create an e-mail message using javax.mail ..
        MimeMessage message = new MimeMessage((Session) null);
        message.setFrom(new InternetAddress(EMAIL_FROM));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(ADDRESS));
        message.setSubject(EMAIL_SUBJECT);
        message.setContent(EMAIL_TEXT, EmailTestConstant.CONTENT_TYPE_TEXT_PLAIN);

        // use greenmail to store the message
        user.deliver(message);

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();

        Assert.assertEquals(messageListener.subject, EMAIL_SUBJECT);
        Thread.sleep(1000);
        Message[] messages = mailServer.getReceivedMessages();
        Assert.assertEquals(messages.length, 0, "Since the message is deleted by"
                + " the pop3 server after reading the message content, Number of messages in the"
                + " 'INBOX' after processing is 'zero'.");
        emailServerConnector.stop();
    }

    @Test(description = "Test the scenario: action after processes invalid one",
            expectedExceptions = EmailConnectorException.class)
    public void actionAfterProcessedIsInvalidTestCase()
            throws IOException, MessagingException, InterruptedException, ServerConnectorException {

        // create user on mail server
        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        emailProperties.put(EmailTestConstant.ACTION_AFTER_PROCESSED, "FLAGGED");

        // create an e-mail message using javax.mail ..
        MimeMessage message = new MimeMessage((Session) null);
        message.setFrom(new InternetAddress(EMAIL_FROM));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(ADDRESS));
        message.setSubject(EMAIL_SUBJECT);
        message.setContent(EMAIL_TEXT, EmailTestConstant.CONTENT_TYPE_TEXT_PLAIN);

        // use greenmail to store the message
        user.deliver(message);

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();

        Assert.assertEquals(messageListener.subject, EMAIL_SUBJECT);
        Thread.sleep(1000);
        Message[] messages = mailServer.getReceivedMessages();
        Assert.assertEquals(messages.length, 0, "Since the message is deleted by"
                + " the pop3 server after reading the message content, Number of messages in the"
                + " 'INBOX' after processing is 'zero'.");
        emailServerConnector.stop();
    }

    /**
     * Create client connector property map
     */
    public void setEmailSeverConnectorProperties() {
        emailProperties = new HashMap<>();
        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_USERNAME, USER_NAME);
        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_PASSWORD, PASSWORD);
        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_HOST_NAME, LOCALHOST);
        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_STORE_TYPE, STORE_TYPE);
        emailProperties.put(EmailTestConstant.MAIL_POP3_PORT, "3110");
        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_FOLDER_NAME, "INBOX");
        emailProperties.put(EmailTestConstant.POLLING_INTERVAL, "3000");
        emailProperties.put(EmailTestConstant.AUTO_ACKNOWLEDGE, "true");
    }
}
