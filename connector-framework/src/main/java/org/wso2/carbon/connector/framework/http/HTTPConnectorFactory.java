package org.wso2.carbon.connector.framework.http;

import java.util.Properties;

/**
 * Allows you create server and client connectors
 */
public interface HTTPConnectorFactory {
    Observable getServerConnector(Properties connectorConfig);
    Observer getClientConnector(Properties connectorConfig);
}
