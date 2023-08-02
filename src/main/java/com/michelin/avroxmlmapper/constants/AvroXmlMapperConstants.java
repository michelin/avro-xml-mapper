package com.michelin.avroxmlmapper.constants;

/**
 * Constants class for the AvroXmlMapper library
 */
public final class AvroXmlMapperConstants {

    /**
     * Default xpath property name.
     */
    public final static String XPATH_DEFAULT = "xpath";

    /**
     * <p>Default XML namespace property name.</p>
     * <p>The value assigned to this property contains all the namespaces used in the XML document.</p>
     * <p>Namespaces keys have to match the ones used in the xpath of the avsc fields, but not necessarily the ones defined in the XML document.</p>
     * <p>Namespaces values (URI) have to match the ones defined in the XML document</p>
     */
    public final static String XML_NAMESPACE_SELECTOR_DEFAULT = "xmlNamespaces";

    /**
     * Property name for the root of a map entry. The value assigned to this property is the xpath to the root (recurring element) of the map entry.
     */
    public final static String XPATH_MAP_ROOT_PROPERTY_NAME = "rootXpath";

    /**
     * Property name for the key of a map entry.
     */
    public final static String XPATH_MAP_KEY_PROPERTY_NAME = "keyXpath";

    /**
     * Property name for the value of a map entry.
     */
    public final static String XPATH_MAP_VALUE_PROPERTY_NAME = "valueXpath";

    /**
     * Default namespace key. Corresponds to the base empty namespace defined in the XML document with xmlns="..."
     */
    public final static String DEFAULT_NAMESPACE = "null";



    /**
     * Key to retrieve the format date properties on timestamp Avro attributes
     */
    public final static String FORMAT_PROPERTIES_KEY = "format";

    /**
     * Key to retrieve the time zone for date properties on timestamp Avro attributes
     */
    public final static String TIMEZONE_PROPERTIES_KEY = "timezone";

    /**
     * Key to retrieve the scaleOut properties on decimal Avro attributes
     */
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

    /**
     * Regex to split an xpath into levels
     */
    public static final String REGEX_SPLIT_XPATH_LEVELS = "(?=/)(?![^\\[\\]]*])";
}
