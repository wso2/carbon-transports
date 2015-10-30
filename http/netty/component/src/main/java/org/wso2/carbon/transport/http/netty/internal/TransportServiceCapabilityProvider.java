/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.transport.http.netty.internal;

import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.kernel.startupresolver.CapabilityProvider;
import org.wso2.carbon.kernel.transports.CarbonTransport;
import org.wso2.carbon.transport.http.netty.internal.config.TransportConfigurationBuilder;

/**
 * Component which registers the CarbonTransport capability information.
 */
@Component(
        name = "org.wso2.carbon.transport.http.netty.internal.TransportServiceCapabilityProvider",
        immediate = true,
        service = CapabilityProvider.class
)
public class TransportServiceCapabilityProvider implements CapabilityProvider {

    @Override
    public String getName() {
        return CarbonTransport.class.getName();
    }

    @Override
    public int getCount() {
        return TransportConfigurationBuilder.build().getListenerConfigurations().size();
    }
}
