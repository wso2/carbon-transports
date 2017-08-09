/*
* Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.wso2.carbon.connector.framework.server.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.connector.framework.ConnectorManager;
import org.wso2.carbon.kernel.CarbonRuntime;
import org.wso2.carbon.kernel.startupresolver.RequiredCapabilityListener;
import org.wso2.carbon.kernel.startupresolver.StartupServiceUtils;
import org.wso2.carbon.messaging.ServerConnectorProvider;
import org.wso2.carbon.messaging.exceptions.ServerConnectorException;

import java.util.Map;

/**
 * This is the ServerConnector Service Component. This will listen for all the {@link ServerConnectorProvider}
 * and start all the registered connectors.
 */
@Component(
        name = "org.wso2.carbon.serverconnector.framework.internal.ServerConnectorSC",
        immediate = true,
        property = {
                "componentName=wso2-server-connector-sc"
        }
)
public class ServerConnectorSC implements RequiredCapabilityListener {
    private static final Logger log = LoggerFactory.getLogger(ServerConnectorSC.class);
    private ConnectorManager connectorManager = new ConnectorManager();

    @Reference(
            name = "server.connector.providers",
            service = ServerConnectorProvider.class,
            cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterServerConnectorProvider"
    )
    protected void registerServerConnectorProvider(ServerConnectorProvider serverConnectorProvider, Map properties) {
        connectorManager.registerServerConnectorProvider(serverConnectorProvider);
        StartupServiceUtils.updateServiceCache("wso2-server-connector-sc", ServerConnectorProvider.class);
    }

    protected void unregisterServerConnectorProvider(ServerConnectorProvider serverConnectorProvider, Map properties) {
    }

    @Reference(
            name = "carbonRuntime",
            service = CarbonRuntime.class,
            cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterCarbonRuntime"
    )
    protected void registerCarbonRuntime(CarbonRuntime carbonRuntime, Map properties) {
        // No use of the CarbonRuntime reference. We just need this for OSGi startup order resolving.
    }

    protected void unregisterCarbonRuntime(CarbonRuntime carbonRuntime, Map properties) {
    }

    @Override
    public void onAllRequiredCapabilitiesAvailable() {
        try {
            connectorManager.startConnectors();
        } catch (ServerConnectorException e) {
            log.error("Error while starting connectors. " + e.getMessage(), e);
        }
    }
}
