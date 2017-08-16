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

package org.wso2.carbon.transport.email.receiver;

import org.wso2.carbon.transport.email.exception.EmailServerConnectorException;
import org.wso2.carbon.transport.email.utils.EmailConstants;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessageRemovedException;
import javax.mail.UIDFolder;

/**
 * Class implemented action for processed mail
 */
public class ActionForProcessedMail {

    /**
     * Carryout the given action for the processed mail
     *
     * @param message Email message which needed to carry out the action.
     * @param folder Folder name which message is contained.
     * @param action Instance which contain the relevant action to carryout
     * @param folderToMove Folder to move the processed mail if action is move.
     * @throws EmailServerConnectorException EmailServerConnectorException when action is failed
     *                                       due to a email layer error.
     */
    static void carryOutAction(Message message, Folder folder,
            EmailConstants.ActionAfterProcessed action, Folder folderToMove)
            throws EmailServerConnectorException {
        // folder create at the constructor of the consume
        Message[] messages = { message };
        try {
            switch (action) {
            case MOVE:
                if (!folderToMove.isOpen()) {
                    folderToMove.open(Folder.READ_WRITE);
                }
                folder.copyMessages(messages, folderToMove);
                message.setFlag(Flags.Flag.DELETED, true);
                folder.expunge();
                break;
            case SEEN:
                message.setFlag(Flags.Flag.SEEN, true);
                break;
            case DELETE:
                message.setFlag(Flags.Flag.DELETED, true);
                if (folder instanceof UIDFolder) {
                    folder.expunge();
                }
                break;
            case FLAGGED:
                message.setFlag(Flags.Flag.FLAGGED, true);
                break;
            case ANSWERED:
                message.setFlag(Flags.Flag.ANSWERED, true);
                break;
            }
        } catch (MessageRemovedException e) {
            throw new EmailServerConnectorException("Error is encountered while carrying out the action '"
                    + action + "'for processed mail since it has been deleted by another thread."
                    + e.getMessage() , e);
        } catch (Exception e) {
            throw new EmailServerConnectorException("Error is encountered while carrying out the action '"
                    + action + "'for processed mail." + e.getMessage(), e);
        }
    }
}
