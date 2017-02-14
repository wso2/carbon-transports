package org.wso2.carbon.transport.file.connector.server;

import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.ServerConnectorErrorHandler;
import org.wso2.carbon.transport.file.connector.server.util.Constants;

/**
 * Error handler for the file server connector.
 */
public class FileServerConnectorErrorHandler implements ServerConnectorErrorHandler {
    @Override
    public void handleError(Exception e, CarbonMessage carbonMessage, CarbonCallback carbonCallback)
            throws Exception {
        carbonCallback.done(carbonMessage);
    }

    @Override
    public String getProtocol() {
        return Constants.PROTOCOL_NAME;
    }
}
