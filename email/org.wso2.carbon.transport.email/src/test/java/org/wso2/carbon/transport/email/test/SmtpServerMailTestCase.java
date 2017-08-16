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

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.ClientConnector;
import org.wso2.carbon.messaging.TextCarbonMessage;
import org.wso2.carbon.messaging.exceptions.ClientConnectorException;
import org.wso2.carbon.transport.email.sender.EmailClientConnector;
import org.wso2.carbon.transport.email.test.utils.EmailTestConstant;

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
    private static final String TO_RECEPIENTS = "someone1@localhost.com";
    private static final String HOST = "127.0.0.1";
    private static final String SUBJECT = "This is smtp server test email";
    private static final String CONTENT = "This is mail send to test the carbon transport";
    private static Map<String, String> properties = new HashMap<>();
    private GreenMail mailServer;

    @BeforeClass(description = "setup parameters need to create a smtp server.")
    public void setMailSenderParameters() {
        properties.put(EmailTestConstant.MAIL_SENDER_HOST_NAME, HOST);
        properties.put(EmailTestConstant.MAIL_SENDER_USERNAME, USERNAME);
        properties.put(EmailTestConstant.MAIL_SENDER_PASSWORD, PASSWORD);
        properties.put(EmailTestConstant.MAIL_HEADER_FROM, ADDRESS);
        properties.put(EmailTestConstant.MAIL_HEADER_TO, TO_RECEPIENTS);
        properties.put(EmailTestConstant.MAIL_HEADER_SUBJECT, SUBJECT);
        properties.put(EmailTestConstant.MAIL_HEADER_CONTENT_TYPE, EmailTestConstant.CONTENT_TYPE_TEXT_HTML);
        //green mail server use port 3025 to create a connection to local email server.
        properties.put("mail.smtp.port", "3025");
        //It is required to set auth 'true' to create connection.
        properties.put("mail.smtp.auth", "true");
    }

    @BeforeMethod(description = "start the green mail server")
    public void setUp() {
        mailServer = new GreenMail(ServerSetupTest.SMTP);
        mailServer.start();
    }

    @AfterMethod(description = "stop the green mail server")
    public void tearDown() {
        mailServer.stop();
    }

    @Test(description = "Test case to send emails via smtp server.")
    public void sendingEmailViaSmtpServerTestCase1() throws IOException, MessagingException, ClientConnectorException {
        // create user on mail server
        mailServer.setUser(ADDRESS, USERNAME, PASSWORD);
        CarbonMessage emailCarbonMessage = new TextCarbonMessage(CONTENT);
        ClientConnector emailClientConnector = new EmailClientConnector();
        emailClientConnector.send(emailCarbonMessage, null, properties);

        MimeMessage[] messages = mailServer.getReceivedMessages();
        Assert.assertEquals(messages.length, 1);
    }

}
