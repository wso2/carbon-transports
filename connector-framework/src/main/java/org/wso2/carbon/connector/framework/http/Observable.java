package org.wso2.carbon.connector.framework.http;

/**
 * Allows to set listeners.
 */
public interface Observable {
    void setObserver(Observer listener);
    void removeObserver(Observer listener);
    void notifyObserver(HTTPMessage httpMessage);
}
