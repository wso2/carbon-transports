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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailSearchTermTestCase {
    private static final Logger log = Logger.getLogger(ImapServerMailTestCase.class);
    private static final String PASSWORD = "carbon123";
    private static final String USER_NAME = "carbon";
    private static final String ADDRESS = "carbon@localhost.com";
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
    private List<Message> getMessageListToSend() throws MessagingException {
        List<Message> messageList = new ArrayList<>();
        // create three messages with different parameters.
        MimeMessage message1 = new MimeMessage((Session) null);
        message1.setFrom(new InternetAddress("from1@localhost.com"));
        message1.addRecipient(Message.RecipientType.TO, new InternetAddress(ADDRESS));
        message1.addRecipient(Message.RecipientType.CC, new InternetAddress("cc1@localhost.com"));
        message1.addRecipient(Message.RecipientType.BCC, new InternetAddress("bcc1@localhost.com"));
        message1.setSubject("Test message one");
        message1.setContent("This is the first email message", EmailTestConstant.CONTENT_TYPE_TEXT_PLAIN);
        messageList.add(message1);

        MimeMessage message2 = new MimeMessage((Session) null);
        message2.setFrom(new InternetAddress("from2@localhost.com"));
        message2.addRecipient(Message.RecipientType.TO, new InternetAddress(ADDRESS));
        message2.addRecipient(Message.RecipientType.CC, new InternetAddress("cc2@localhost.com"));
        message2.addRecipient(Message.RecipientType.BCC, new InternetAddress("bcc2@localhost.com"));
        message2.setSubject("Test message two");
        message2.setContent("This is the second email message", EmailTestConstant.CONTENT_TYPE_TEXT_PLAIN);
        messageList.add(message2);

        MimeMessage message3 = new MimeMessage((Session) null);
        message3.setFrom(new InternetAddress("from3@localhost.com"));
        message3.addRecipient(Message.RecipientType.TO, new InternetAddress(ADDRESS));
        message3.addRecipient(Message.RecipientType.CC, new InternetAddress("cc3@localhost.com"));
        message3.addRecipient(Message.RecipientType.BCC, new InternetAddress("bcc3@localhost.com"));
        message3.setSubject("Test message Three");
        message3.setContent("This is the third email message" , EmailTestConstant.CONTENT_TYPE_TEXT_PLAIN);
        messageList.add(message3);

        return messageList;

    }

    @Test(description = "Test the scenario: receiving messages via "
            + "imap server when search term is 'subject:Test messge two'.")
    public void searchBySubjectTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        getMessageListToSend().forEach(message -> user.deliver((MimeMessage) message));

        emailProperties.put(EmailTestConstant.SEARCH_TERM, "subject:Test message two");
        EmailServerConnectorProvider emailServerConnectorProvider = new EmailServerConnectorProvider();
        ServerConnector connector = emailServerConnectorProvider.createConnector("testEmail", emailProperties);
        TestMessageProcessor testMessageProcessor = new TestMessageProcessor();
        connector.setMessageProcessor(testMessageProcessor);
        connector.start();
        testMessageProcessor.waitTillDone();
        Assert.assertEquals(testMessageProcessor.subject, "Test message two", "Only the message2 is"
                + " received since the it contain 'Test message two' in the subject.");
        connector.stop();
        Thread.sleep(1000);
    }

    @Test(description = "Test the scenario: receiving messages via "
            + "imap server when search term is 'from:from'.")
    public void searchByFromAddressTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        getMessageListToSend().forEach(message -> user.deliver((MimeMessage) message));

        emailProperties.put(EmailTestConstant.SEARCH_TERM, "from:from");
        EmailServerConnectorProvider emailServerConnectorProvider = new EmailServerConnectorProvider();
        ServerConnector connector = emailServerConnectorProvider.createConnector("testEmail", emailProperties);
        TestMessageProcessor testMessageProcessor = new TestMessageProcessor();
        connector.setMessageProcessor(testMessageProcessor);
        connector.start();
        testMessageProcessor.waitTillDone();
        Assert.assertEquals(testMessageProcessor.subject, "Test message one",
                "The message1 is received first, since the"
                + " message contains 'from' in the 'from address'.");
        connector.stop();
        Thread.sleep(1000);
    }


    @Test(description = "Test the scenario: receiving messages via "
            + "imap server when search term is 'from:from2@'.")
    public void searchByFromAddressWithAtSymbolTestCase()
            throws MessagingException, ServerConnectorException, InterruptedException {

        GreenMailUser user = mailServer.setUser(ADDRESS, USER_NAME, PASSWORD);
        getMessageListToSend().forEach(message -> user.deliver((MimeMessage) message));

        emailProperties.put(EmailTestConstant.SEARCH_TERM, "from: from2@");
        EmailServerConnectorProvider emailServerConnectorProvider = new EmailServerConnectorProvider();
        ServerConnector connector = emailServerConnectorProvider.createConnector("testEmail", emailProperties);
        TestMessageProcessor testMessageProcessor = new TestMessageProcessor();
        connector.setMessageProcessor(testMessageProcessor);
        connector.start();
        testMessageProcessor.waitTillDone();
        Assert.assertEquals(testMessageProcessor.subject, "Test message two",
                "Only the message2 is received since the messages' from address is start with 'from2@'.");
        connector.stop();
        Thread.sleep(1000);
    }



}
