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

package org.wso2.carbon.transport.email.utils;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.pop3.POP3Folder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.email.contract.message.EmailBaseMessage;
import org.wso2.carbon.transport.email.contract.message.EmailTextMessage;
import org.wso2.carbon.transport.email.exception.EmailConnectorException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessageRemovedException;
import javax.mail.UIDFolder;

/**
 * This class is used to create carbon message from the corresponding email message at the email receiver side
 * and to decide the action for processed mail.
 */
public class EmailUtils {
    private static final Logger log = LoggerFactory.getLogger(EmailUtils.class);

    /**
     * Return enum relevant to the user given action for processed mail.
     *
     * @param action       User given Action
     * @param isImapFolder Whether folder is IMAP folder or not
     * @return ActionAfterProcessed enum of ActionAfterProcessed
     */
    public static Constants.ActionAfterProcessed getActionAfterProcessed(String action, Boolean isImapFolder)
            throws EmailConnectorException {
        String actionInUpperCase;
        if (action != null) {
            actionInUpperCase = action.toUpperCase(Locale.ENGLISH);
            if (isImapFolder) {
                switch (actionInUpperCase) {
                case "SEEN":
                    return Constants.ActionAfterProcessed.SEEN;
                case "FLAGGED":
                    return Constants.ActionAfterProcessed.FLAGGED;
                case "ANSWERED":
                    return Constants.ActionAfterProcessed.ANSWERED;
                case "DELETE":
                    return Constants.ActionAfterProcessed.DELETE;
                case "MOVE":
                    return Constants.ActionAfterProcessed.MOVE;
                default:
                    throw new EmailConnectorException(
                            " action '" + action + "' is not supported by email server connector.");
                }
            } else {
                switch (actionInUpperCase) {
                case "DELETE":
                    return Constants.ActionAfterProcessed.DELETE;
                default:
                    throw new EmailConnectorException("Action '" + action + "' is not supported by POP3Folder.");
                }
            }
        } else {
            if (isImapFolder) {
                if (log.isDebugEnabled()) {
                    log.warn("Action after processed mail parameter is not defined." + " Get default action : SEEN.");
                }
                return Constants.ActionAfterProcessed.SEEN;

            } else {
                if (log.isDebugEnabled()) {
                    log.warn("Action after processed mail parameter is not defined" + " Get default action : DELETE.");
                }
                return Constants.ActionAfterProcessed.DELETE;
            }
        }
    }

    /**
     * Create the carbon message using corresponding email.
     *
     * @param message             Instance of Email message
     * @param folder              Instance of the folder in which Mail contains
     * @param emailMessageContent Message content
     * @param serviceId           Unique id of the service
     * @return an instance of EmailBaseMessage created
     * @throws EmailConnectorException EmailConnectorException when action is fail
     *                                       due to a email layer error.
     */
    public static EmailBaseMessage createEmailMessage(Message message, Folder folder, String emailMessageContent,
            String serviceId) throws EmailConnectorException {

        try {
            EmailTextMessage emailMessage = new EmailTextMessage(emailMessageContent);

            //get headers of the email message and put them as carbon message headers
            Enumeration headers = message.getAllHeaders();
            while (headers.hasMoreElements()) {
                Header h = (Header) headers.nextElement();
                emailMessage.setHeader(h.getName(), h.getValue());
            }

            //put the service name as a property
            emailMessage.setProperty(Constants.SERVICE_NAME, serviceId);
            //put the email message number as a property
            emailMessage.setProperty(Constants.MAIL_PROPERTY_MESSAGE_NUMBER, message.getMessageNumber());

            if (folder instanceof IMAPFolder) {
                List<String> flagList = new ArrayList<>();
                //due to the reading the content of the message, flag "SEEN" is already set in every message.
                if (message.isSet(Flags.Flag.SEEN)) {
                    flagList.add("SEEN");
                }
                if (message.isSet(Flags.Flag.ANSWERED)) {
                    flagList.add("ANSWERED");
                }
                if (message.isSet(Flags.Flag.FLAGGED)) {
                    flagList.add("FLAGGED");
                }
                if (message.isSet(Flags.Flag.DELETED)) {
                    flagList.add("DELETED");
                }

                String flags = String.join(",", flagList);

                emailMessage.setProperty(Constants.MAIL_PROPERTY_FLAGS, flags);
                //put the uid (Long value) of the message as a property.
                emailMessage.setProperty(Constants.MAIL_PROPERTY_UID, ((UIDFolder) folder).getUID(message));
            } else {
                if (folder instanceof POP3Folder) {
                    //put the uid (String value) of the message as a property.
                    emailMessage.setProperty(Constants.MAIL_PROPERTY_UID, ((POP3Folder) folder).getUID(message));
                }
            }

            return emailMessage;

        } catch (MessageRemovedException e) {
            throw new EmailConnectorException("Error is encountered while creating the carbon message since it"
                    + " has been deleted by another thread." + e.getMessage(), e);
        } catch (Exception e) {
            throw new EmailConnectorException("Error is encountered while creating a carbon message." +
                    e.getMessage(), e);
        }
    }
}
