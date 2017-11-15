package org.wso2.carbon.transport.email.contract.message.properties;

import java.util.Map;
import java.util.TreeMap;

/**
 * Class implementing to set and get properties of the messages.
 */
public class Properties {
    private Map<String, Object> propertyMap;

    public Properties() {
        this.propertyMap = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Method implemented to get the property object of given property name.
     * @param key Name of property which need to get.
     * @return Property object of the given name.
     */
    public Object get(String key) {
        return this.propertyMap.get(key);
    }

    /**
     * Method implemented to get propertyMap
     * @return propertyMap which contain property name and property as key value pairs.
     */
    public Map<String, Object> getAllProperties() {
        return this.propertyMap;
    }

    /**
     * Method implemented to add a property to the property map.
     * @param key       Name of the property needed to be set.
     * @param value     Property as a object.
     */
    public void set(String key, Object value) {
        this.propertyMap.put(key, value);
    }

    /**
     * Method Implemented to add properties to property map.
     * @param properties Property Map contains property name as key and property object.
     */
    public void setProperties(Map<String, Object> properties) {
       properties.forEach(this::set);
    }
}

