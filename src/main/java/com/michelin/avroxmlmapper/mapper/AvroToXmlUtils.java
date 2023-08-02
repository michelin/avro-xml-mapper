package com.michelin.avroxmlmapper.mapper;

import com.michelin.avroxmlmapper.constants.AvroXmlMapperConstants;
import com.michelin.avroxmlmapper.exception.AvroXmlMapperException;
import org.apache.avro.JsonProperties;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

import static com.michelin.avroxmlmapper.constants.AvroXmlMapperConstants.*;
import static com.michelin.avroxmlmapper.utility.GenericUtils.*;

/**
 * Utility class for Avro to XML conversion
 */
public final class AvroToXmlUtils {

    /**
     * Create a Document from a SpecificRecordBase, using xpath property (Avro model) to build the XML structure.
     *
     * @param record            the global SpecificRecordBase containing the entire data to parse in XML
     * @param xpathSelector     Name of the variable defining the xpath of the avsc file that needs to be used
     * @param namespaceSelector Name of the variable defining xml namespaces of avsc file corresponding to record
     * @return the document produced
     */
    public static Document createDocumentfromAvro(SpecificRecordBase record, String xpathSelector, String namespaceSelector) {
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.newDocument();
            Map<String, String> mapNamespaces;
            if (namespaceSelector != null) {
                mapNamespaces = xmlNamespaces(record.getSchema(), namespaceSelector);
                mapNamespaces.put("", mapNamespaces.get(DEFAULT_NAMESPACE));
                mapNamespaces.remove(DEFAULT_NAMESPACE);
            } else {
                mapNamespaces = Collections.emptyMap();
            }
            String rootElementName = record.getSchema().getProp(xpathSelector).substring(1);// the first character, for the xpath of rootElement,// is '/'
            var rootElement = document.createElementNS(mapNamespaces.get(AvroToXmlUtils.getPrefix(rootElementName)), rootElementName);
            mapNamespaces.forEach((k, v) -> rootElement.setAttribute(k.isEmpty() ? AvroXmlMapperConstants.XMLNS : AvroXmlMapperConstants.XMLNS + ":" + k, v));
            AvroToXmlUtils.buildChildNodes(record, document, mapNamespaces, xpathSelector).forEach(n -> {
                if (n.getNodeType() == Node.ATTRIBUTE_NODE) rootElement.setAttributeNode((Attr) n);
                else rootElement.appendChild(n);
            });

            document.appendChild(rootElement);

        } catch (Exception e) {
            throw new AvroXmlMapperException("Failed to create document from avro", e);
        }

        return document;
    }

    /**
     * Build all child nodes of an element (with type record in avsc) and return it as list.
     *
     * @param record        the record corresponding to the parent element
     * @param document      the target document (necessary to create nodes)
     * @param namespaces    map containing all namespaces (K : prefix ; V : URI)
     * @param xpathSelector Name of the variable defining the xpath of the avsc file that needs to be used
     * @return the list of all child nodes built
     */
    private static List<Node> buildChildNodes(SpecificRecordBase record, Document document, Map<String, String> namespaces, String xpathSelector) {
        List<Node> childNodes = new ArrayList<>();
        for (Schema.Field field : record.getSchema().getFields()) {
            Schema fieldType = extractRealType(field.schema());
            String xpath;
            switch (fieldType.getType()) {
                case NULL:
                case UNION:
                case ENUM:
                    break;
                case RECORD:
                    xpath = field.getProp(xpathSelector);
                    if (xpath != null) {
                        var subRecord = (SpecificRecordBase) record.get(field.name());
                        if (subRecord != null) {
                            Node node = createNode(xpath, childNodes, document, namespaces);
                            buildChildNodes(subRecord, document, namespaces, xpathSelector).forEach(n -> {
                                if (n.getNodeType() == Node.ATTRIBUTE_NODE) ((Element) node).setAttributeNode((Attr) n);
                                else node.appendChild(n);
                            });
                        }
                    }
                    break;
                case ARRAY:
                    Schema elementSchema = fieldType.getElementType();
                    xpath = field.getProp(xpathSelector);
                    if (xpath != null) {
                        var list = (List) record.get(field.name());
                        if (list != null && !list.isEmpty()) {
                            if (extractRealType(elementSchema).getType() == Schema.Type.RECORD) { // an array of records
                                for (SpecificRecordBase item : (List<SpecificRecordBase>) list) {
                                    Node node = createNode(xpath, childNodes, document, namespaces);
                                    buildChildNodes(item, document, namespaces, xpathSelector).forEach(n -> {
                                        if (n.getNodeType() == Node.ATTRIBUTE_NODE)
                                            ((Element) node).setAttributeNode((Attr) n);
                                        else node.appendChild(n);
                                    });
                                }
                            } else if (extractRealType(elementSchema).getType() == Schema.Type.STRING) { // an array of string
                                for (String item : (List<String>) list) {
                                    Node node = createNode(xpath, childNodes, document, namespaces);
                                    node.appendChild(document.createTextNode(item));
                                }
                            } else {
                                throw new NotImplementedException("Array implementation with value types other than records or String are not yet supported");
                            }
                        }
                    }
                    break;
                case MAP:
                    buildMapChildNodes(record, document, childNodes, namespaces, field, fieldType, xpathSelector);
                    break;
                default:
                    // all other = primitive types
                    var xpathList = getXpathList(field, xpathSelector);

                    String fieldValue = record.get(field.name()) != null ? record.get(field.name()).toString() : "";
                    if (!fieldValue.isEmpty()) {
                        xpathList.forEach(x -> {
                            Node node = createNode(x, childNodes, document, namespaces);
                            node.appendChild(document.createTextNode(formatStringWithSchemaType(fieldType.getType(), record.get(field.name()), field.schema())));
                        });
                    }
            }
        }

        childNodes.forEach(AvroToXmlUtils::removeSpecialAttributes);
        return childNodes;

    }

    private static void buildMapChildNodes(SpecificRecordBase record, Document document, List<Node> childNodes, Map<String, String> namespaces, Schema.Field field, Schema fieldType, String xpathSelector) {

        // initialize value Schema
        Schema valueSchema = fieldType.getValueType();

        // try to get the map xpath properties
        LinkedHashMap<String, String> mapXpathProperties = (LinkedHashMap<String, String>) field.getObjectProp(xpathSelector);
        String rootXpath, keyXpath, valueXpath;

        if (mapXpathProperties != null) {
            // new scenarios
            rootXpath = mapXpathProperties.get(XPATH_MAP_ROOT_PROPERTY_NAME);
            keyXpath = mapXpathProperties.get(XPATH_MAP_KEY_PROPERTY_NAME);
            valueXpath = mapXpathProperties.get(XPATH_MAP_VALUE_PROPERTY_NAME);

            if (rootXpath != null && keyXpath != null && valueXpath != null) {
                if (!keyXpath.contains("@")) {
                    buildMapChildNodesFromScenario1(record, document, childNodes, namespaces, field, valueSchema, rootXpath, keyXpath, valueXpath);
                } else {
                    buildMapChildNodesFromScenario2(record, document, childNodes, namespaces, field, valueSchema, rootXpath, keyXpath, valueXpath);
                }
            }
        }
    }

    /**
     * xpath = "root#key#value"
     *
     * <mapMarkup>
     * <key>key1</key>
     * <value>value</value>
     * </mapMarkup>
     * <mapMarkup>
     * <key>key2</key>
     * <value>value</value>
     * </mapMarkup>
     */
    private static void buildMapChildNodesFromScenario1(SpecificRecordBase record, Document document, List<Node> childNodes, Map<String, String> namespaces, Schema.Field field, Schema valueSchema, String rootXpath, String keyXpath, String valueXpath) {
        if (valueSchema.getType() == Schema.Type.STRING) {
            var map = (Map<String, String>) record.get(field.name());
            if (map != null) {
                for (var keyValue : map.entrySet()) {
                    Node node = createNode(rootXpath, childNodes, document, namespaces);
                    var hackEmptyList = new ArrayList<Node>();
                    Node keyNode = createNode(keyXpath, hackEmptyList, document, namespaces);
                    Node valueNode = createNode(valueXpath, hackEmptyList, document, namespaces);
                    keyNode.appendChild(document.createTextNode(formatStringWithSchemaType(valueSchema.getType(), keyValue.getKey(), field.schema())));
                    valueNode.appendChild(document.createTextNode(formatStringWithSchemaType(valueSchema.getType(), keyValue.getValue(), field.schema())));
                    node.appendChild(keyNode);
                    node.appendChild(valueNode);
                }
            }
        } else {
            throw new NotImplementedException("Map implementation with value types other than String are not yet supported");
        }
    }

    /**
     * xpath="root/entry#@key#."
     *
     * <mapMarkup>
     * <entry key="key1">value</entry>
     * <entry key="key2">value</entry>
     * </mapMarkup>
     */
    private static void buildMapChildNodesFromScenario2(SpecificRecordBase record, Document document, List<Node> childNodes, Map<String, String> namespaces, Schema.Field field, Schema valueSchema, String rootXpath, String keyXpath, String valueXpath) {
        if (!".".equals(valueXpath)) {
            throw new NotImplementedException("Using a valueXpath different from '.' while using an attribute key is not yet supported.");
        }
        if (valueSchema.getType() == Schema.Type.STRING) {
            var map = (Map<String, String>) record.get(field.name());
            if (map != null) {
                for (var keyValue : map.entrySet()) {
                    Node entry = createNode(rootXpath, childNodes, document, namespaces);
                    addDynamicAttribute(entry, keyXpath.replace("@", ""), keyValue.getKey());
                    entry.appendChild(document.createTextNode(formatStringWithSchemaType(valueSchema.getType(), keyValue.getValue(), field.schema())));
                }
            }
        } else {
            throw new NotImplementedException("Map implementation with value types other than String are not yet supported");
        }
    }

    /**
     * This method creates a node according to the xpath provided.
     * If the xpath contains more than one level, for each intermediate level :
     * * if the intermediate node already exists in the list, it is retrieved
     * * if the intermediate node does not exist in the list, it is created
     * The new node is appended to the intermediate node (retrieved or created).
     * This method uses filters based on attributes to set attributes when necessary
     * * Note : filters must respect constraints : only one value per attribute (which excludes "!=" operator), only "and" operator between conditions, not multi-levels filter (like "[element/@attribute='foo']")
     * The type of the returned node can be element or attribute
     *
     * @param xpath      the relative xpath with all intermediate elements, including attribute filters
     * @param nodeList   the nodes already created ; all nodes created by this method are added to this list
     * @param document   the target document (necessary to create nodes)
     * @param namespaces map containing all namespaces (K : prefix ; V : URI)
     * @return the node created
     */
    private static Node createNode(String xpath, List<Node> nodeList, Document
            document, Map<String, String> namespaces) {
        Node resultNode = null;
        Node parentNode = null;
        String[] xmlLevels = xpath.split(REGEX_SPLIT_XPATH_LEVELS);
        int i = 0;
        for (String xmlLevel : xmlLevels) {
            if (xmlLevel.startsWith("/"))
                xmlLevel = xmlLevel.substring(1); // remove the '/' if present at beginning
            if (i == 0 && xmlLevels.length > 1) { // first level, we search in the list if the element already exists
                for (Node node : nodeList) {
                    // xPathNodeListEvaluation
                    if (isNodeMatching(node, xmlLevel)) {
                        parentNode = node;
                        break;
                    }
                }
                if (parentNode == null) {
                    parentNode = createElement(xmlLevel, document, namespaces);
                    nodeList.add(parentNode);
                }
            } else if (i < xmlLevels.length - 1 && xmlLevels.length > 2) { // intermediate level, we search from the parent node
                boolean alreadyExist = false;
                for (Node node : asList(parentNode.getChildNodes())) {
                    if (isNodeMatching(node, xmlLevel)) {
                        parentNode = node;
                        alreadyExist = true;
                        break;
                    }
                }
                if (!alreadyExist) {
                    Node newNode = createElement(xmlLevel, document, namespaces);
                    parentNode.appendChild(newNode);
                    parentNode = newNode;
                }
            } else { // last level
                if (xmlLevel.startsWith("@")) { // attribute
                    resultNode = document.createAttribute(xmlLevel.substring(1));
                } else { // element
                    resultNode = createElement(xmlLevel, document, namespaces);
                }
                if (parentNode == null) {
                    nodeList.add(resultNode);
                } else {
                    if (resultNode.getNodeType() == Node.ATTRIBUTE_NODE)
                        ((Element) parentNode).setAttributeNode((Attr) resultNode);
                    else parentNode.appendChild(resultNode);
                }
            }
            i++;
        }
        return resultNode;
    }

    private static List<String> getXpathList(Schema.Field field, String xpathSelector) {
        Object xpath1 = field.getObjectProp(xpathSelector);
        var xpathList = new ArrayList<String>();

        if (xpath1 == null || JsonProperties.NULL_VALUE.equals(xpath1)) {
            return xpathList;
        }

        //test if xpath is an array
        if (xpath1.getClass().getSimpleName().equals("ArrayList")) {
            xpathList.addAll((Collection<? extends String>) xpath1);
        } else {
            xpathList.add((String) xpath1);
        }

        return xpathList;
    }

    /**
     * Try to parse an Object value to a String depending on Schema type (special rules to convert a float to String).
     *
     * @param fieldType The schema type
     * @param value     The field value as typed Object
     * @return the result of parsing, with formatting specificities.
     */
    private static String formatStringWithSchemaType(Schema.Type fieldType, Object value, Schema schema) {
        String result;

        if (value.toString() == null) {
            result = null;
        } else {
            try {
                switch (fieldType) {
                    case BYTES -> {
                        if (value instanceof BigDecimal) {
                            int maxFractionDigit = 2;
                            Optional<Schema> schemaOptional = schema.getTypes()
                                    .stream()
                                    .filter(type -> type.getType().equals(Schema.Type.BYTES))
                                    .findAny();
                            if (schemaOptional.isPresent()) {
                                String scaleOut = schemaOptional.get().getProp(SCALEOUT_PROPERTIES_KEY);
                                if (!StringUtils.isEmpty(scaleOut)) {
                                    maxFractionDigit = Integer.parseInt(scaleOut);
                                }
                            }

                            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
                            DecimalFormat df = new DecimalFormat();
                            df.setDecimalFormatSymbols(symbols);
                            df.setMaximumFractionDigits(maxFractionDigit);


                            df.setMinimumFractionDigits(0);
                            df.setRoundingMode(RoundingMode.HALF_UP);

                            df.setGroupingUsed(false);

                            result = df.format(value);
                        } else {
                            result = value.toString();
                        }
                    }
                    case STRING, INT, LONG -> {
                        result = value.toString();
                        if (value instanceof Instant) {
                            Optional<Schema> dateRecord = schema.getTypes()
                                    .stream()
                                    .filter(type -> type.getType().equals(Schema.Type.LONG))
                                    .findAny();

                            if (dateRecord.isPresent()) {
                                String format = dateRecord.get().getProp(FORMAT_PROPERTIES_KEY);

                                if (!StringUtils.isBlank(format)) {
                                    Date toDate = Date.from((Instant) value);
                                    SimpleDateFormat formatter = new SimpleDateFormat(format);

                                    String timeZone = dateRecord.get().getProp(TIMEZONE_PROPERTIES_KEY);
                                    if (!StringUtils.isBlank(timeZone)) {
                                        formatter.setTimeZone(TimeZone.getTimeZone(timeZone));
                                    }

                                    result = formatter.format(toDate);
                                }
                            }
                        }
                    }
                    case BOOLEAN -> result = value.toString().toLowerCase();
                    case DOUBLE, FLOAT -> {
                        // it is not very elegant, but the most common case is an integer value, in this case the value must appear as integer (not xx.0)
                        // it is possible that some applications do not support float value
                        int indexOfDecimal = value.toString().indexOf(".");
                        if (indexOfDecimal >= 0) {
                            result = value.toString().substring(indexOfDecimal).equals(".0") ? value.toString().substring(0, indexOfDecimal) : value.toString();
                        } else {
                            result = value.toString();
                        }
                    }
                    default -> result = null;
                }
            } catch (Exception e) {
                result = null;
            }
        }

        return result;
    }

    private static void addDynamicAttribute(Node node, String attributeKey, String value) {
        ((Element) node).setAttribute(attributeKey, value);
    }

    private static void removeSpecialAttributes(Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            ((Element) node).removeAttribute(XML_ATTRIBUTE_POSITION);
        }
        asList(node.getChildNodes()).forEach(AvroToXmlUtils::removeSpecialAttributes);
    }

    /**
     * Compares a node, including attributes, with the xpath level
     *
     * @param node     the node to compare
     * @param xmlLevel the xpath level
     * @return true if matching, false otherwise
     */
    private static boolean isNodeMatching(Node node, String xmlLevel) {
        if (node.getNodeName().equals(extractElementName(xmlLevel))) { // same element name
            var xmlSubLevel = getSubLevelFromFilter(xmlLevel);
            if (!xmlSubLevel.isEmpty()) {
                for (Node childNode : asList(node.getChildNodes())) {
                    if (isNodeMatching(childNode, xmlSubLevel)) return true;
                }
            } else {
                NamedNodeMap nodeAttributes = node.getAttributes();
                for (var attr : extractAttributes(xmlLevel).entrySet()) {
                    Node nodeAttribute = nodeAttributes.getNamedItem(attr.getKey());
                    if (nodeAttribute == null || !nodeAttribute.getNodeValue().equals(attr.getValue())) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * This method create an Element with optional attributes based on xpath filter (included in xmlLevel)
     *
     * @param xmlLevel   the element name and, optionally, a filter based on attributes
     * @param document   the document (necessary to create element)
     * @param namespaces the map of namespaces
     * @return the node created
     */
    private static Node createElement(String xmlLevel, Document document, Map<String, String> namespaces) {
        Node resultNode;
        var xmlSubLevel = getSubLevelFromFilter(xmlLevel);
        if (!xmlSubLevel.isEmpty()) {
            resultNode = createElement(extractElementName(xmlLevel), document, namespaces);
        } else {
            String elementName = extractElementName(xmlLevel);
            resultNode = document.createElementNS(namespaces.get(getPrefix(elementName)), elementName);
            for (var attribute : extractAttributes(xmlLevel).entrySet()) {
                ((Element) resultNode).setAttribute(attribute.getKey(), attribute.getValue());
            }
        }
        return resultNode;
    }

    private static String getSubLevelFromFilter(String xmlLevel) {
        var subLevel = new StringBuilder();
        if (xmlLevel.contains("[")) {
            String filter = xmlLevel.substring(xmlLevel.indexOf('[') + 1, xmlLevel.indexOf(']'));
            if (!filter.startsWith("@") && !filter.matches("\\d+")) {
                var subLevels = filter.split(REGEX_SPLIT_XPATH_LEVELS);
                subLevel.append(subLevels[0]);
                if (subLevels.length > 1) {
                    subLevel.append("[").append(subLevels[1].substring(1));
                    for (int i = 2; i < subLevels.length; i++) {
                        subLevel.append(subLevels[i]);
                    }
                    subLevel.append("]");
                }
            }
        }
        return subLevel.toString();
    }

    /**
     * Extract element name from the xpath level. For example, if xmlLevel = foo:bar[@toto='titi'] the result is foo:bar.
     *
     * @param xmlLevel the xmlLevel from which the name is extracted
     * @return the element name
     */
    private static String extractElementName(String xmlLevel) {
        if (xmlLevel.contains("[")) { // element with filter on attribute(s)
            return xmlLevel.substring(0, xmlLevel.indexOf('['));
        } else {
            return xmlLevel;
        }
    }


    /**
     * Extract the prefix (namespace) of an element name.
     *
     * @param qualifiedName the element name, including the prefix
     * @return the prefix or an empty string in no prefix is present
     */
    private static String getPrefix(String qualifiedName) {
        String[] parts = qualifiedName.split(":");
        if (parts.length == 1) { // no prefix
            return "";
        } else {
            return parts[0];
        }
    }


    /**
     * Extract attributes from filters contained in the xpath level. The result is a Map of K = attributeName ; V = attributeValue
     *
     * @param xmlLevel the fragment of xpath
     * @return the result Map of attributes
     */
    private static Map<String, String> extractAttributes(String xmlLevel) {
        Map<String, String> attributes = new HashMap<>(); // K = attributeName ; V = attributeValue
        if (xmlLevel.contains("[")) { // element with filter on attribute(s)
            String filter = xmlLevel.substring(xmlLevel.indexOf('[') + 1, xmlLevel.indexOf(']'));
            if (filter.startsWith("@")) {
                for (String attribute : filter.split("and")) {
                    attributes.put(attribute.split("=")[0].substring(1), attribute.split("=")[1].replace("'", "")); // key.substring(1) -> to remove the initial @
                }
            } else if (filter.matches("\\d+")) {
                attributes.put(XML_ATTRIBUTE_POSITION, filter); // this pseudo-attribute allows to filter on position
            } // else : nothing

        }
        return attributes;
    }

}
