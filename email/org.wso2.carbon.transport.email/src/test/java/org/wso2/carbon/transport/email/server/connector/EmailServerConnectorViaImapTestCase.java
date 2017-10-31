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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Class implementing the server connector test cases. Imap protocol is used to receive mails.
 */
public class EmailServerConnectorViaImapTestCase {

    private static final Logger log = Logger.getLogger(EmailServerConnectorViaImapTestCase.class);
    private static final String PASSWORD = "carbon123";
    private static final String USER_NAME = "carbon";
    private static final String ADDRESS = "carbon@localhost.com";
    private static final String EMAIL_SUBJECT = "Test message";
    private static final String LOCALHOST = "localhost";
    private static final String STORE_TYPE = "imap";
    private Map<String, String> emailProperties;
    /**
     * Server provided by Green mail to create local mail server.
     */
    private GreenMail mailServer;

    @BeforeMethod public void setUp() {
        emailProperties = new HashMap<>();
        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_USERNAME, USER_NAME);
        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_PASSWORD, PASSWORD);
        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_HOST_NAME, LOCALHOST);
        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_STORE_TYPE, STORE_TYPE);
        emailProperties.put(EmailTestConstant.MAIL_IMAP_PORT, EmailTestConstant.MAIL_IMAP_PORT_VALUE);
        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_FOLDER_NAME, EmailTestConstant.FOLDER_NAME);
        emailProperties.put(EmailTestConstant.POLLING_INTERVAL, EmailTestConstant.POLLING_INTERVAL_VALUE);
        emailProperties.put(EmailTestConstant.AUTO_ACKNOWLEDGE, EmailTestConstant.AUTO_ACKNOWLWDGE_VALUE);

        mailServer = new GreenMail(ServerSetupTest.IMAP);
        mailServer.start();
    }

    @AfterMethod public void tearDown() {
        mailServer.stop();
    }

    @Test(description = "Test the scenario: receiving messages via imap server when action after process is 'SEEN' and"
            + " check whether flag of the processed mail is set as 'SEEN'.")
    public void    imapServerWithActionAfterProcessedIsSeenTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        deliverMassage(user, "This is the a email message", "Test message", "from@localhost.com",
                "carbon@localhost.com", "cc@localhost.com", "bcc@localhost.com", "text/plain");

        emailProperties.put(EmailTestConstant.ACTION_AFTER_PROCESSED, "SEEN");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();
        Assert.assertEquals(messageListener.subject, EMAIL_SUBJECT);
        emailServerConnector.stop();
        Thread.sleep(1000);

        MimeMessage[] messages = mailServer.getReceivedMessages();
        Assert.assertTrue(messages[0].isSet(Flags.Flag.SEEN),
                "Flag of the message with subject: " + EMAIL_SUBJECT + ", has been set to 'SEEN'.");

    }

    @Test(description = "Test the scenario: receiving messages via imap server when action after process is"
            + " 'FLAGGED' and check whether flag of the processed mail is set as 'FLAGGED'.")
    public void imapServerWithActionAfterProcessedIsFlaggedTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        deliverMassage(user, "This is the a email message", "Test message", "from@localhost.com",
                "carbon@localhost.com", "cc@localhost.com", "bcc@localhost.com", "text/plain");

        emailProperties.put(EmailTestConstant.ACTION_AFTER_PROCESSED, "FLAGGED");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();
        Assert.assertEquals(messageListener.subject, EMAIL_SUBJECT);
        emailServerConnector.stop();
        Thread.sleep(1000);

        MimeMessage[] messages = mailServer.getReceivedMessages();
        Assert.assertTrue(messages[0].isSet(Flags.Flag.FLAGGED),
                "Flag of the message with subject: " + EMAIL_SUBJECT + ", has been set to 'FLAGGED'.");
    }

    @Test(description = "Test the scenario: receiving messages via imap server when action after process is 'ANSWERED' "
            + "and check whether flag of the processed message is set as 'ANSWERED'")
    public void imapServerWithActionAfterProcessedIsAnsweredTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        deliverMassage(user, "This is the a email message", "Test message", "from@localhost.com",
                "carbon@localhost.com", "cc@localhost.com", "bcc@localhost.com", "text/plain");

        emailProperties.put(EmailTestConstant.ACTION_AFTER_PROCESSED, "ANSWERED");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();
        Assert.assertEquals(messageListener.subject, EMAIL_SUBJECT);
        emailServerConnector.stop();
        Thread.sleep(1000);

        MimeMessage[] messages = mailServer.getReceivedMessages();
        Assert.assertTrue(messages[0].isSet(Flags.Flag.ANSWERED),
                "Flag of the message with subject: " + EMAIL_SUBJECT + ", has been set to 'ANSWERED'.");
    }

    @Test(description = "Test the scenario: receiving messages via imap server when action after process is"
            + " 'MOVE' and folder to move is 'ProcessedMailFolder' and check whether processed mail has been moved"
            + " to the given folder.")
    public void imapServerWithActionAfterProcessedIsMoveTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        deliverMassage(user, "This is the a email message", "Test message", "from@localhost.com",
                "carbon@localhost.com", "cc@localhost.com", "bcc@localhost.com", "text/plain");

        emailProperties.put(EmailTestConstant.ACTION_AFTER_PROCESSED, "MOVE");
        emailProperties.put(EmailTestConstant.MOVE_TO_FOLDER, "ProcessedMailFolder");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();
        Assert.assertEquals(messageListener.subject, EMAIL_SUBJECT);
        emailServerConnector.stop();
        Thread.sleep(1000);

        Message[] messages = getMessagesFromGreenMailFolder("ProcessedMailFolder");
        Assert.assertEquals(messages.length, 1, "Message count in the 'INBOX' is zero,"
                + " since message in the folder is moved to 'ProcessedMailFolder'.");
    }

    @Test(description = "Test the scenario: receiving messages via imap server when action after process is 'DELETE'"
            + " and check whether processed mail has been deleted.")
    public void imapServerWithActionAfterProcessedIsDeleteTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        deliverMassage(user, "This is the a email message", "Test message", "from@localhost.com",
                "carbon@localhost.com", "cc@localhost.com", "bcc@localhost.com", "text/plain");

        emailProperties.put(EmailTestConstant.ACTION_AFTER_PROCESSED, "DELETE");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();
        Assert.assertEquals(messageListener.subject, EMAIL_SUBJECT);
        emailServerConnector.stop();
        Thread.sleep(1000);

        MimeMessage[] messages = mailServer.getReceivedMessages();
        Assert.assertEquals(messages.length, 0, "Message count in the 'INBOX' is zero,"
                + " since messages in the folder have been deleted by the email server connector"
                + " after processing them.");
    }

    @Test(description = "Test the scenario: Receiving messages with autoAcknowledgement true.")
    public void imapServerWithAutoAckTrue()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        deliverMassage(user, "This is the a email message", "Test message", "from@localhost.com",
                "carbon@localhost.com", "cc@localhost.com", "bcc@localhost.com", "text/plain");
        deliverMassage(user, "This is the a email message", "Test message", "from1@localhost.com",
                "carbon@localhost.com", "cc1@localhost.com", "bcc1@localhost.com", "text/plain");

        emailProperties.put(EmailTestConstant.AUTO_ACKNOWLEDGE, "true");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();
        Assert.assertEquals(messageListener.subject, EMAIL_SUBJECT);
        emailServerConnector.stop();
        Thread.sleep(1000);

        MimeMessage[] messages = mailServer.getReceivedMessages();
        Assert.assertEquals(messages.length, 2);
    }

    @Test(description = "Test the scenario: receiving messages via imap server when search term is "
            + "'subject:Test message two'.") public void searchBySubjectTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        deliverMassage(user, "This is the first email message", "Test message one",
                "from1@localhost.com", "carbon@localhost.com", "cc1@localhost.com",
                "bcc1@localhost.com", "text/plain");
        deliverMassage(user, "This is the second email message", "Test message two",
                "from2@localhost.com", "carbon@localhost.com", "cc2@localhost.com",
                "bcc2@localhost.com", "text/plain");
        deliverMassage(user, "This is the third email message", "Test message three",
                "from3@localhost.com", "carbon@localhost.com", "cc3@localhost.com",
                "bcc3@localhost.com", "text/plain");

        emailProperties.put(EmailTestConstant.SEARCH_TERM, "subject:Test message two");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();

        Assert.assertEquals(messageListener.subject, "Test message two",
                "Only the message2 is" + " received since the it contain 'Test message two' in the subject.");
        emailServerConnector.stop();
        Thread.sleep(1000);
    }

    @Test(description = "Test the scenario: receiving messages via imap server when search term is 'from:from'.")
    public void searchByFromAddressTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        deliverMassage(user, "This is the first email message", "Test message one",
                "from1@localhost.com",
                "carbon@localhost.com", "cc1@localhost.com", "bcc1@localhost.com", "text/plain");
        deliverMassage(user, "This is the second email message", "Test message two",
                "from2@localhost.com",
                "carbon@localhost.com", "cc2@localhost.com", "bcc2@localhost.com", "text/plain");
        deliverMassage(user, "This is the third email message", "Test message three",
                "from3@localhost.com",
                "carbon@localhost.com", "cc3@localhost.com", "bcc3@localhost.com", "text/plain");

        emailProperties.put(EmailTestConstant.SEARCH_TERM, "from:from");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(3);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();

        Assert.assertEquals(messageListener.subject, "Test message three", "The message1"
                + " is received first, since the" + " message contains 'from' in the 'from address'.");
        emailServerConnector.stop();
        Thread.sleep(1000);
    }

    @Test(description = "Test the scenario: receiving messages via imap server when search term is 'from:from2@'.")
    public void searchByFromAddressWithAtSymbolTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        deliverMassage(user, "This is the first email message", "Test message one",
                "from1@localhost.com",
                "carbon@localhost.com", "cc1@localhost.com", "bcc1@localhost.com", "text/plain");
        deliverMassage(user, "This is the second email message", "Test message two",
                "from2@localhost.com",
                "carbon@localhost.com", "cc2@localhost.com", "bcc2@localhost.com", "text/plain");
        deliverMassage(user, "This is the third email message", "Test message three",
                "from3@localhost.com",
                "carbon@localhost.com", "cc3@localhost.com", "bcc3@localhost.com", "text/plain");

        emailProperties.put(EmailTestConstant.SEARCH_TERM, "from:from2@");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();

        Assert.assertEquals(messageListener.subject, "Test message two",
                "Only the message2 is received since the messages' from address is start with 'from2@'.");
        emailServerConnector.stop();
        Thread.sleep(1000);
    }

    @Test(description = "Test the scenario: receiving messages via imap server when multiple conditions"
            + " are defined in search term.")
    public void searchByMultipleConditionsTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        deliverMassage(user, "This is the first email message", "Test message",
                "from@localhost.com",
                "carbon@localhost.com", "cc1@localhost.com", "bcc@localhost.com", "text/plain");
        deliverMassage(user, "This is the second email message", "Test message",
                "from@localhost.com",
                "carbon@localhost.com", "cc@localhost.com", "bcc@localhost.com", "text/plain");
        deliverMassage(user, "This is the third email message", "Test message",
                "from3@localhost.com",
                "carbon@localhost.com", "cc@localhost.com", "bcc@localhost.com", "text/plain");
        deliverMassage(user, "This is the second email message", "Test",
                "from@localhost.com", "carbon@localhost.com",
                "cc@localhost.com", "bcc@localhost.com", "text/plain");

        emailProperties.put(EmailTestConstant.SEARCH_TERM,
                "from:from@, to:carbon@, cc:cc@, bcc:bcc@," + " subject:Test message");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();

        Assert.assertEquals(messageListener.subject, "Test message");
        Assert.assertEquals(messageListener.to, "carbon@localhost.com");
        Assert.assertEquals(messageListener.from, "from@localhost.com");
        Assert.assertEquals(messageListener.cc, "cc@localhost.com");
        Assert.assertEquals(messageListener.bcc, "bcc@localhost.com");
        emailServerConnector.stop();
        Thread.sleep(1000);
    }

    @Test(description = "Test the scenario: Search condition without @ symbol")
    public void searchByWithoutAtSymbolTestCase()
            throws MessagingException, InterruptedException, EmailConnectorException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        deliverMassage(user, "This is the first email message", "Test message one",
                "from1@localhost.com", "carbon@localhost.com", "cc1@localhost.com",
                "bcc1@localhost.com", "text/plain");
        deliverMassage(user, "This is the second email message", "Test message two",
                "from2@localhost.com", "carbon@localhost.com", "cc2@localhost.com",
                "bcc2@localhost.com", "text/plain");
        emailProperties.put(EmailTestConstant.SEARCH_TERM, "from:from, to:carbon, cc:cc, bcc:bcc,"
                + " subject: Test message");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(2);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();
        Assert.assertEquals(messageListener.numberOfEventArrived, 2, "Both messages should be"
                + "delivered");
        emailServerConnector.stop();
        Thread.sleep(1000);
    }

    @Test(description = "Test the scenario: Search condition when more than one recipients are defined in each type.")
    public void searchWithMultipleRecipientsTestCase()
            throws MessagingException, InterruptedException, EmailConnectorException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        deliverMassage(user, "This is the first email message", "Test message one",
                "from1@localhost.com", "carbon@localhost.com, carbon1@localhost.com",
                "cc@localhost.com,cc1@localhost", "bcc1@localhost.com,bcc@localhost.com",
                "text/plain");

        emailProperties.put(EmailTestConstant.SEARCH_TERM, "from:from1, to:carbon, cc:cc1, bcc:bcc1,"
                + " subject: Test message");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();
        Assert.assertEquals(messageListener.numberOfEventArrived, 1, "Both messages should be"
                + "delivered");
        emailServerConnector.stop();
        Thread.sleep(1000);
    }

    @Test(description = "Test the scenario: receiving messages via imap server when multiple conditions"
            + " are defined in search term in invalid format.", expectedExceptions = EmailConnectorException.class)
    public void searchByWrongConditionTestCase()
            throws MessagingException, InterruptedException, EmailConnectorException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);

        emailProperties.put(EmailTestConstant.SEARCH_TERM, "from:from@, to:carbon@, cc:cc@, bb:bcc@");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();
        emailServerConnector.stop();
        Thread.sleep(1000);
    }

    @Test(description = "Test the scenario: receiving messages via imap server when search term is 'from:from2@'.")
    public void htmlContentTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        deliverMassage(user, "This is the first email message", "Test message one",
                "from1@localhost.com",
                "carbon@localhost.com", "cc1@localhost.com", "bcc1@localhost.com", "text/plain");
        deliverMassage(user, "<h>This is the second email message</h>", "Test message two",
                "from2@localhost.com",
                "carbon@localhost.com", "cc2@localhost.com", "bcc2@localhost.com", "text/html");

        emailProperties.put(EmailTestConstant.CONTENT_TYPE, EmailTestConstant.CONTENT_TYPE_TEXT_HTML);

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();

        Assert.assertEquals(messageListener.subject, "Test message two",
                "Only the message2 is received since the messages content is text/html");
        emailServerConnector.stop();
        Thread.sleep(1000);
    }

    @Test(description = "Test the scenario: Test the scenario destroying the server connector")
    public void  destroyMethodTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        deliverMassage(user, "This is the a email message", "Test message", "from@localhost.com",
                "carbon@localhost.com", "cc@localhost.com", "bcc@localhost.com", "text/plain");
        deliverMassage(user, "This is the a email message", "Test message", "from1@localhost.com",
                "carbon@localhost.com", "cc1@localhost.com", "bcc1@localhost.com", "text/plain");
        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.destroy();
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();
        emailServerConnector.stop();
        emailServerConnector.destroy();
    }

    @Test(description = "Test the scenario: password is not given",
            expectedExceptions = EmailConnectorException.class)
    public void  userNameNotTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);

        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_USERNAME, null);

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
    }

    @Test(description = "Test the scenario: password is not given",
            expectedExceptions = EmailConnectorException.class)
    public void  passwordNotTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);

        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_PASSWORD, null);

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
    }

    @Test(description = "Test the scenario: host name is not given",
            expectedExceptions = EmailConnectorException.class)
    public void  hostNameNotTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);

        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_HOST_NAME, null);

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
    }

    @Test(description = "Test the scenario: store type name is not given",
            expectedExceptions = EmailConnectorException.class)
    public void  storeTypeNotTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);

        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_STORE_TYPE, null);

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
    }

    @Test(description = "Test the scenario: invalid content type is given",
            expectedExceptions = EmailConnectorException.class)
    public void  invalidContentTypeTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);

        emailProperties.put(EmailTestConstant.CONTENT_TYPE, "attachment");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
    }

    @Test(description = "Test the scenario: invalid server parameters are given",
            expectedExceptions = EmailConnectorException.class)
    public void  invalidServerParametersTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);

        emailProperties.put(EmailTestConstant.MAIL_IMAP_PORT, "100");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
    }

    @Test(description = "Test the scenario: invalid server parameters are with retryCount and retryInterval",
            expectedExceptions = EmailConnectorException.class)
    public void  invalidServerParameterAndRetryCountIsGivenTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);

        emailProperties.put(EmailTestConstant.MAIL_IMAP_PORT, "100");
        emailProperties.put(EmailTestConstant.MAX_RETRY_COUNT, "3");
        emailProperties.put(EmailTestConstant.RETRY_INTERVAL, "100");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
    }

    @Test(description = "Test the scenario: invalid retry Count is given",
            expectedExceptions = EmailConnectorException.class)
    public void  invalidRetryCountIsGivenTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);

        emailProperties.put(EmailTestConstant.MAX_RETRY_COUNT, "three");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
    }

    @Test(description = "Test the scenario: invalid retry interval is given",
            expectedExceptions = EmailConnectorException.class)
    public void  invalidRetryIntervalTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);

        emailProperties.put(EmailTestConstant.RETRY_INTERVAL, "1000K");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
    }

    @Test(description = "Test the scenario: invalid action after processed is given",
            expectedExceptions = EmailConnectorException.class)
    public void  invalidActionIsGiven()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);

        emailProperties.put(EmailTestConstant.ACTION_AFTER_PROCESSED, "remove");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
    }

    @Test(description = "Test the scenario: invalid action after processed is given move and folder to move"
            + " is not given",
            expectedExceptions = EmailConnectorException.class)
    public void  folderToMoveIsNotGiven()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);

        emailProperties.put(EmailTestConstant.ACTION_AFTER_PROCESSED, "MOVE");

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
    }

    @Test(description = "Test the scenario: Connect with minimum parameters required")
    public void connectWithMinimumParametersTestCase()
            throws MessagingException, InterruptedException, EmailConnectorException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        deliverMassage(user, "This is the first email message", "Test message one",
                "from1@localhost.com", "carbon@localhost.com, carbon1@localhost.com",
                "cc@localhost.com,cc1@localhost", "bcc1@localhost.com,bcc@localhost.com",
                "text/plain");

        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_FOLDER_NAME, null);
        emailProperties.put(EmailTestConstant.POLLING_INTERVAL, null);
        emailProperties.put(EmailTestConstant.AUTO_ACKNOWLEDGE, null);

        EmailConnectorFactory emailConnectorFactory = new EmailConnectorFactoryImpl();
        TestEmailMessageListener messageListener = new TestEmailMessageListener();
        messageListener.setNumberOfEvent(1);
        EmailServerConnector emailServerConnector = emailConnectorFactory
                .createEmailServerConnector("testEmail", emailProperties);
        emailServerConnector.init();
        emailServerConnector.start(messageListener);
        messageListener.waitForEvent();
        Assert.assertEquals(messageListener.numberOfEventArrived, 1, "Both messages should be"
                + "delivered");
        emailServerConnector.stop();
        Thread.sleep(1000);
    }

    /**
     * Method implemented to create and deliver the email to green mail server using given parameters.
     *
     * @param user        Instance of GreenMailUser which is going to use.
     * @param content     Content which is needed to be set in the email.
     * @param subject     Subject which is needed to be set in the email.
     * @param from        From Address of the email which is needed to be set in the email.
     * @param to          To Address which is needed to be set in the email.
     * @param cc          CC Address which is needed to be set in the email.
     * @param bcc         BCC Address which is needed to be set in the email.
     * @param contentType Content type which is needed to be set in the email.
     * @throws MessagingException Throw MessagingException if error is occurred while creating the Message.
     */
    private void deliverMassage(GreenMailUser user, String content, String subject, String from, String to, String cc,
            String bcc, String contentType) throws MessagingException {
        MimeMessage message = new MimeMessage((Session) null);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
        message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc));
        message.setSubject(subject);
        message.setContent(content, contentType);
        user.deliver(message);
    }

    /**
     * Method implementing to get the messages in the given folder.
     *
     * @param folderName Name of the folder to get mails.
     * @return Array of messages which contains inside the given folder.
     * @throws MessagingException MessagingException when fail to read the message from given folder.
     */
    private Message[] getMessagesFromGreenMailFolder(String folderName) throws MessagingException {
        Properties properties = new Properties();
        properties.put(EmailTestConstant.MAIL_IMAP_PORT, EmailTestConstant.MAIL_IMAP_PORT_VALUE);
        Session session = Session.getInstance(properties);
        Store store = session.getStore(STORE_TYPE);
        store.connect(LOCALHOST, USER_NAME, PASSWORD);
        Folder folder = store.getFolder(folderName);
        folder.open(Folder.READ_ONLY);
        Message[] messages = folder.getMessages();
        if (store.isConnected()) {
            store.close();
        }
        return messages;
    }
}
