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
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.wso2.carbon.transport.jms.factory;

/**
 * Connection key that is used as the key for the {@link org.apache.commons.pool2.KeyedObjectPool}.
 */
public class PooledConnectionKey {

    /**
     * Connection username.
     */
    private final String username;

    /**
     * Connection password.
     */
    private final String password;

    private int hashCode;

    /**
     * Initializing key with username, password and generating hashcode.
     *
     * @param username The username for the connection
     * @param password The password for the connection
     */
    public PooledConnectionKey(String username, String password) {
        this.username = username;
        this.password = password;
        generateHashCode();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Pre-populate hashCode.
     */
    private void generateHashCode() {
        // Why 31? - http://stackoverflow.com/questions/299304/why-does-javas-hashcode-in-string-use-31-as-a-multiplier
        hashCode = username.hashCode() + 31 * password.hashCode();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        PooledConnectionKey that = (PooledConnectionKey) object;

        if (hashCode != that.hashCode) {
            return false;
        }

        if (username != null) {
            if (that.username == null) {
                return false;
            } else if (!username.equals(that.username)) {
                return false;
            }
        }

        if (password != null) {
            if (that.password == null) {
                return false;
            } else if (password.equals(that.password)) {
                return false;
            }
        }

        return true;
    }
}
