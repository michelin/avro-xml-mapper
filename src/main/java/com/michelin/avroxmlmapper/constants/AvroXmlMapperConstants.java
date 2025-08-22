/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.michelin.avroxmlmapper.constants;

/** Constants class for the AvroXmlMapper library */
public final class AvroXmlMapperConstants {

    /** Default xpath property name. */
    public static final String XPATH_DEFAULT = "xpath";

    /**
     * Default XML namespace property name.
     *
     * <p>The value assigned to this property contains all the namespaces used in the XML document.
     *
     * <p>Namespaces keys have to match the ones used in the xpath of the AVSC fields, but not necessarily the ones
     * defined in the XML document.
     *
     * <p>Namespaces values (URI) have to match the ones defined in the XML document
     */
    public static final String XML_NAMESPACE_SELECTOR_DEFAULT = "xmlNamespaces";

    /**
     * Property name for the root of a map entry. The value assigned to this property is the xpath to the root
     * (recurring element) of the map entry.
     */
    public static final String XPATH_MAP_ROOT_PROPERTY_NAME = "rootXpath";

    /** Property name for the key of a map entry. */
    public static final String XPATH_MAP_KEY_PROPERTY_NAME = "keyXpath";

    /** Property name for the value of a map entry. */
    public static final String XPATH_MAP_VALUE_PROPERTY_NAME = "valueXpath";

    /** Default namespace key. Corresponds to the base empty namespace defined in the XML document with xmlns="..." */
    public static final String DEFAULT_NAMESPACE = "null";

    /** Key to retrieve the format date properties on timestamp Avro attributes */
    public static final String FORMAT_PROPERTIES_KEY = "format";

    /** Key to retrieve the time zone for date properties on timestamp Avro attributes */
    public static final String TIMEZONE_PROPERTIES_KEY = "timezone";

    /** Key to retrieve the scaleOut properties on decimal Avro attributes */
    public static final String SCALEOUT_PROPERTIES_KEY = "scaleOut";

    /** Constant for the XML prefix "noprefixns" */
    public static final String NO_PREFIX_NS = "noprefixns";

    /** Constant for the XML prefix "xmlns" */
    public static final String XMLNS = "xmlns";

    /** Constant to symbolize the position of an element */
    public static final String XML_ATTRIBUTE_POSITION = "specialAttrPosition";

    /** Regex to split an xpath into levels */
    public static final String REGEX_SPLIT_XPATH_LEVELS = "(?=/)(?![^\\[\\]]*])";
}
