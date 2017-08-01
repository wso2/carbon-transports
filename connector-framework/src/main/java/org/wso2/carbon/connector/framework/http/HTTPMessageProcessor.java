package org.wso2.carbon.connector.framework.http;

/**
 * Interface of the HTTP Message Processor which generates HTTP Carbon messages
 * from the wire payload. This basically represents the contract each HTTP transport consumer
 * has to adhere with.
 */
public interface HTTPMessageProcessor {
    /**
     * This method allows you to get hold of the HTTP request that comes from the
     * socket stream.
     *
     * @param httpRequest HTTP request received from the wire.
     * @param httpResponseFuture Used to send the response back to the client.
     */
    void handleInboudRequest(HTTPMessage httpRequest, HTTPResponseFuture httpResponseFuture);

    /**
     * This method allows you to send outbound HTTP request to the back-end.
     *
     * @param httpRequest HTTP request that needs be written to the wire.
     * @return A future the will get notify when there is a response.
     */
    HTTPResponseFuture handleOutboundRequest(HTTPMessage httpRequest);

    /**
     * This method allows you to handle errors/exceptions that triggered from the server connector.
     *
     * @param throwable Error/Exception that occurred from the server-connector.
     */
    void handleServerConnectorException(Throwable throwable);
}
