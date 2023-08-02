package com.michelin.avroxmlmapper.constants;

public final class AvroXmlMapperConstants {
    // default property for xml conversion
    public final static String XPATH_DEFAULT = "xpath";
    public final static String XML_NAMESPACE_SELECTOR_DEFAULT = "xmlNamespaces";

    // default property names for map conversion
    public final static String XPATH_MAP_ROOT_PROPERTY_NAME = "rootXpath";
    public final static String XPATH_MAP_KEY_PROPERTY_NAME = "keyXpath";
    public final static String XPATH_MAP_VALUE_PROPERTY_NAME = "valueXpath";

    public final static String DEFAULT_NAMESPACE = "null";



    /**
     * Key to retrieve the format date properties on timestamp Avro attributes
     */
    public final static String FORMAT_PROPERTIES_KEY = "format";

    /**
     * Key to retrieve the time zone for date properties on timestamp Avro attributes
     */
    public final static String TIMEZONE_PROPERTIES_KEY = "timezone";
    public final static String SCALEOUT_PROPERTIES_KEY = "scaleOut";
    /**
     * Constant for the XML prefix "noprefixns"
     */
    public final static String NO_PREFIX_NS = "noprefixns";

    /**
     * Constant for the XML prefix "xmlns"
     */
    public final static String XMLNS = "xmlns";


    /**
     * Constant to symbolize the position of an element
     */
    public final static String XML_ATTRIBUTE_POSITION = "specialAttrPosition";

    public static final String REGEX_SPLIT_XPATH_LEVELS = "(?=/)(?![^\\[\\]]*])";
}
