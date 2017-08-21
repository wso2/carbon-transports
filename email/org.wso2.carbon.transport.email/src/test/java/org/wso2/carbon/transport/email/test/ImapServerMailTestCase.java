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

package org.wso2.carbon.transport.email.test;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.messaging.ServerConnector;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;
import org.wso2.carbon.transport.email.provider.EmailServerConnectorProvider;
import org.wso2.carbon.transport.email.test.utils.EmailTestConstant;
import org.wso2.carbon.transport.email.test.utils.TestMessageProcessor;

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
 * Class implementing Imap server mail test case to test receiving mails via IMAP server.
 */
public class ImapServerMailTestCase {
    private static final Logger log = Logger.getLogger(ImapServerMailTestCase.class);
    private static final String PASSWORD = "carbon123";
    private static final String USER_NAME = "carbon";
    private static final String ADDRESS = "carbon@localhost.com";
    private static final String EMAIL_FROM = "someone@localhost.com";
    private static final String EMAIL_SUBJECT = "Test imap server";
    private static final String EMAIL_TEXT = "This is a e-mail to test receive mails via imap server.";
    private static final String LOCALHOST = "127.0.0.1";
    private static final String STORE_TYPE = "imap";
    private static Map<String, String> emailProperties = new HashMap<>();
    /**
     * Server provided by Green mail to create local mail server.
     */
    private GreenMail mailServer;

    @BeforeClass
    public void setEmailSeverConnectorProperties() {
        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_USERNAME, USER_NAME);
        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_PASSWORD, PASSWORD);
        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_HOST_NAME, LOCALHOST);
        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_STORE_TYPE, STORE_TYPE);
        emailProperties.put(EmailTestConstant.MAIL_IMAP_PORT, EmailTestConstant.MAIL_IMAP_PORT_VALUE);
        emailProperties.put(EmailTestConstant.MAIL_RECEIVER_FOLDER_NAME, EmailTestConstant.FOLDER_NAME);
        emailProperties.put(EmailTestConstant.POLLING_INTERVAL, EmailTestConstant.POLLING_INTERVAL_VALUE);
        emailProperties.put(EmailTestConstant.AUTO_ACKNOWLEDGE, EmailTestConstant.AUTO_ACKNOWLWDGE_VALUE);
    }

    @BeforeMethod
    public void setUp() {
        mailServer = new GreenMail(ServerSetupTest.IMAP);
        mailServer.start();
    }

    @AfterMethod
    public void tearDown() {
        mailServer.stop();
    }



    @Test(description = "Test the scenario: receiving messages via "
            + "imap server when action after process is 'SEEN' and"
            + " check whether flag of the processed mail is set as 'SEEN'.")
    public void imapServerWithActionAfterProcessedIsSeenTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        user.deliver((MimeMessage) createMessage());

        emailProperties.put(EmailTestConstant.ACTION_AFTER_PROCESSED, "SEEN");
        EmailServerConnectorProvider emailServerConnectorProvider = new EmailServerConnectorProvider();
        ServerConnector connector = emailServerConnectorProvider.createConnector("testEmail", emailProperties);
        TestMessageProcessor testMessageProcessor = new TestMessageProcessor();
        connector.setMessageProcessor(testMessageProcessor);
        connector.start();
        testMessageProcessor.waitTillDone();
        Assert.assertEquals(testMessageProcessor.subject, EMAIL_SUBJECT);
        connector.stop();
        Thread.sleep(1000);
        MimeMessage[] messages = mailServer.getReceivedMessages();
        Assert.assertTrue(messages[0].isSet(Flags.Flag.SEEN), "Flag of the message with subject: " +
                EMAIL_SUBJECT + ", has been set to 'SEEN'.");

    }

    @Test(description = "Test the scenario: receiving messages via "
            + "imap server when action after process is 'FLAGGED' and"
            + " check whether flag of the processed mail is set as 'FLAGGED'.")
    public void imapServerWithActionAfterProcessedIsFlaggedTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        user.deliver((MimeMessage) createMessage());

        emailProperties.put(EmailTestConstant.ACTION_AFTER_PROCESSED, "FLAGGED");
        EmailServerConnectorProvider emailServerConnectorProvider = new EmailServerConnectorProvider();
        ServerConnector connector = emailServerConnectorProvider.createConnector("testEmail", emailProperties);
        TestMessageProcessor testMessageProcessor = new TestMessageProcessor();
        connector.setMessageProcessor(testMessageProcessor);
        connector.start();
        testMessageProcessor.waitTillDone();
        Assert.assertEquals(testMessageProcessor.subject, EMAIL_SUBJECT);
        connector.stop();
        Thread.sleep(1000);
        MimeMessage[] messages = mailServer.getReceivedMessages();
        Assert.assertTrue(messages[0].isSet(Flags.Flag.FLAGGED), "Flag of the message with subject: " +
        EMAIL_SUBJECT + ", has been set to 'FLAGGED'.");
    }

    @Test(description = "Test the scenario: receiving messages via "
            + "imap server when action after process is 'ANSWERED' and"
            + " check whether flag of the processed message is set as 'ANSWERED'")
    public void imapServerWithActionAfterProcessedIsAnsweredTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        user.deliver((MimeMessage) createMessage());

        emailProperties.put(EmailTestConstant.ACTION_AFTER_PROCESSED, "ANSWERED");
        EmailServerConnectorProvider emailServerConnectorProvider = new EmailServerConnectorProvider();
        ServerConnector connector = emailServerConnectorProvider.createConnector("testEmail", emailProperties);
        TestMessageProcessor testMessageProcessor = new TestMessageProcessor();
        connector.setMessageProcessor(testMessageProcessor);
        connector.start();
        testMessageProcessor.waitTillDone();
        Assert.assertEquals(testMessageProcessor.subject, EMAIL_SUBJECT);
        connector.stop();
        Thread.sleep(1000);
        MimeMessage[] messages = mailServer.getReceivedMessages();
        Assert.assertTrue(messages[0].isSet(Flags.Flag.ANSWERED), "Flag of the message with subject: " +
                EMAIL_SUBJECT + ", has been set to 'ANSWERED'.");

    }

    @Test(description = "Test the scenario: receiving messages via "
            + "imap server when action after process is 'MOVE' and folder to move is 'ProcessedMailFolder'"
            + " and check whether processed mail has been moved to the given folder.")
    public void imapServerWithActionAfterProcessedIsMoveTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        user.deliver((MimeMessage) createMessage());

        emailProperties.put(EmailTestConstant.ACTION_AFTER_PROCESSED, "MOVE");
        emailProperties.put(EmailTestConstant.MOVE_TO_FOLDER, "ProcessedMailFolder");
        EmailServerConnectorProvider emailServerConnectorProvider = new EmailServerConnectorProvider();
        ServerConnector connector = emailServerConnectorProvider.createConnector("testEmail", emailProperties);
        TestMessageProcessor testMessageProcessor = new TestMessageProcessor();
        connector.setMessageProcessor(testMessageProcessor);
        connector.start();
        testMessageProcessor.waitTillDone();
        Assert.assertEquals(testMessageProcessor.subject, EMAIL_SUBJECT);
        connector.stop();
        Thread.sleep(1000);

        Message[] messages = getMessagesFromGreenMailFolder("ProcessedMailFolder");
        Assert.assertEquals(messages.length, 1, "Message count in the 'INBOX' is zero,"
                + " since message in the folder is moved to 'ProcessedMailFolder'.");
    }

    @Test(description = "Test the scenario: receiving messages via "
            + "imap server when action after process is 'DELETE'"
            + " and check whether processed mail has been deleted.")
    public void imapServerWithActionAfterProcessedIsDeleteTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        user.deliver((MimeMessage) createMessage());

        emailProperties.put(EmailTestConstant.ACTION_AFTER_PROCESSED, "DELETE");
        EmailServerConnectorProvider emailServerConnectorProvider = new EmailServerConnectorProvider();
        ServerConnector connector = emailServerConnectorProvider.createConnector("testEmail", emailProperties);
        TestMessageProcessor testMessageProcessor = new TestMessageProcessor();
        connector.setMessageProcessor(testMessageProcessor);
        connector.start();
        testMessageProcessor.waitTillDone();
        Assert.assertEquals(testMessageProcessor.subject, EMAIL_SUBJECT);
        connector.stop();
        Thread.sleep(1000);
        MimeMessage[] messages = mailServer.getReceivedMessages();
        Assert.assertEquals(messages.length, 0, "Message count in the 'INBOX' is zero,"
                + " since messages in the folder have been deleted by the email server connector"
                + " after processing them.");

    }

    /**
     * Create a email message to deliver to the mail server.
     * @return Instance of message created.
     * @throws MessagingException MessagingException when fail to create a message.
     */
    private Message createMessage() throws MessagingException {
        MimeMessage message = new MimeMessage((Session) null);
        message.setFrom(new InternetAddress(EMAIL_FROM));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(ADDRESS));
        message.setSubject(EMAIL_SUBJECT);
        message.setContent(EMAIL_TEXT, EmailTestConstant.CONTENT_TYPE_TEXT_PLAIN);
        return message;
    }

    /**
     * Method implementing to get the messages in the given folder.
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
