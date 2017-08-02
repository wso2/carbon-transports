package org.wso2.carbon.connector.framework.http;

/**
 * Allows to get notifications.
 */
public interface Observer {
    void update(HTTPMessage httpMessage, Observer callback);
}
