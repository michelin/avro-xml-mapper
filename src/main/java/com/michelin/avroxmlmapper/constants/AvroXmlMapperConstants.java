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

/** Constants for the AvroXmlMapper library. */
public final class AvroXmlMapperConstants {

    /** Default XPath property name. */
    public static final String XPATH_DEFAULT = "xpath";

    /**
     * Default XML namespace property name.
     *
     * <p>The value assigned to this property contains all the namespaces used in the XML document.
     *
     * <p>Namespace keys must match those used in the XPath of the AVSC fields, but not necessarily those defined in the
     * XML document.
     *
     * <p>Namespaces values (URI) have to match the ones defined in the XML document.
     */
    public static final String XML_NAMESPACE_SELECTOR_DEFAULT = "xmlNamespaces";

    /** Property name for the root of a map entry. Its value is the XPath to the recurring root element. */
    public static final String XPATH_MAP_ROOT_PROPERTY_NAME = "rootXpath";

    /** Property name for the key of a map entry. */
    public static final String XPATH_MAP_KEY_PROPERTY_NAME = "keyXpath";

    /** Property name for the value of a map entry. */
    public static final String XPATH_MAP_VALUE_PROPERTY_NAME = "valueXpath";

    /** Default namespace key, corresponding to the empty namespace in the XML document. */
    public static final String DEFAULT_NAMESPACE = "null";

    /** Key for date format properties on Avro timestamp fields. */
    public static final String FORMAT_PROPERTIES_KEY = "format";

    /** Key for time zone properties on Avro timestamp fields. */
    public static final String TIMEZONE_PROPERTIES_KEY = "timezone";

    /** Key for scale-out properties on Avro decimal fields. */
    public static final String SCALEOUT_PROPERTIES_KEY = "scaleOut";

    /** Constant for the XML prefix "noprefixns". */
    public static final String NO_PREFIX_NS = "noprefixns";

    /** Constant for the XML prefix "xmlns". */
    public static final String XMLNS = "xmlns";

    /** Constant to symbolize the position of an element. */
    public static final String XML_ATTRIBUTE_POSITION = "specialAttrPosition";

    /** Regular expression for splitting an XPath into levels. */
    public static final String REGEX_SPLIT_XPATH_LEVELS = "(?=/)(?![^\\[\\]]*])";

    /** Private constructor. */
    private AvroXmlMapperConstants() {}
}
