package com.michelin.avroxmlmapper.mapper;

import com.michelin.avroxmlmapper.exception.AvroXmlMapperException;
import com.michelin.avroxmlmapper.utility.XPathFormatter;
import org.apache.avro.JsonProperties;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.namespace.NamespaceContext;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.*;

import static com.michelin.avroxmlmapper.constants.AvroXmlMapperConstants.*;
import static com.michelin.avroxmlmapper.utility.GenericUtils.*;

/**
 * Utility class for converting XML to Avro.
 */
public final class XmlToAvroUtils {

    /**
     * Converts, recursively, the content of an XML-node into SpecificRecord (avro).
     *
     * @param fullNode         XML-node to convert
     * @param orphanNode       XML-node to convert without parent context
     * @param clazz            class of the SpecificRecord to generate
     * @param namespaceContext the namespace context
     * @param baseNamespace    base namespace for the generated SpecificRecord classes
     * @param xpathSelector    the xpathSelector property used to search for the xpath mapping in the Avro definition
     * @param <T>              The type of the Avro object
     * @return SpecificRecord generated
     */
    static <T extends SpecificRecordBase> T convert(Node fullNode, Node orphanNode, Class<T> clazz, NamespaceContext namespaceContext, String baseNamespace, String xpathSelector) {
        try {
            T record = clazz.getDeclaredConstructor().newInstance();
            for (Schema.Field field : record.getSchema().getFields()) {
                Schema fieldType = extractRealType(field.schema());
                switch (fieldType.getType()) {
                    case NULL:
                    case UNION:
                    case ENUM:
                        // nothing
                        break;
                    case RECORD:
                        convertXMLRecordToAvro(record, fullNode, orphanNode, namespaceContext, baseNamespace, field, fieldType, xpathSelector);
                        break;
                    case ARRAY:
                        convertXMLArrayToAvro(record, fullNode, orphanNode, namespaceContext, baseNamespace, field, fieldType, xpathSelector);
                        break;
                    case MAP:
                        convertXMLMapToAvro(record, fullNode, orphanNode, namespaceContext, field, fieldType, xpathSelector);
                        break;
                    case LONG:
                        //Handle dates to a TimezonedTimestamp format
                        if (fieldType.getLogicalType() != null && fieldType.getLogicalType().getName().equals("timestamp-millis")) {
                            convertXMLDateToAvro(record, fullNode, orphanNode, namespaceContext, field, xpathSelector);
                        }
                        break;
                    case BYTES:
                        convertXMLBytesToAvro(record, fullNode, orphanNode, namespaceContext, field, fieldType, xpathSelector);
                        break;
                    default:
                        // all other = primitive types
                        convertXMLPrimitiveTypeToAvro(record, fullNode, orphanNode, namespaceContext, field, fieldType, xpathSelector);
                }
            }
            return record;
        } catch (Exception e) {
            throw new AvroXmlMapperException("Failed to parse document", e);
        }
    }

    private static void convertXMLMapToAvro(SpecificRecordBase record, Node fullNode, Node orphanNode, NamespaceContext namespaceContext, Schema.Field field, Schema fieldType, String xpathSelector) {

        // initialize value Schema
        Schema valueSchema = fieldType.getValueType();
        String rootXpath;
        String keyXpath;
        String valueXpath;
        // try to get the map xpath properties
        LinkedHashMap<String, String> mapXpathProperties = (LinkedHashMap<String, String>) field.getObjectProp(xpathSelector);

        if (mapXpathProperties != null) {
            // new scenarios
            rootXpath = XPathFormatter.format(mapXpathProperties.get("rootXpath"));
            keyXpath = XPathFormatter.format(mapXpathProperties.get("keyXpath"));
            valueXpath = XPathFormatter.format(mapXpathProperties.get("valueXpath"));
        } else {
            keyXpath = XPathFormatter.format(field.getProp(XPATHKEY_DEFAULT));
            rootXpath = XPathFormatter.format(field.getProp(XPATHMAP_DEFAULT));
            valueXpath = XPathFormatter.format(field.getProp(XPATHVALUE_DEFAULT));
        }

        if (rootXpath != null && keyXpath != null && valueXpath != null) {
            if (valueSchema.getType() == Schema.Type.STRING
                    || valueSchema.getType() == Schema.Type.INT
                    || valueSchema.getType() == Schema.Type.LONG
                    || valueSchema.getType() == Schema.Type.FLOAT
                    || valueSchema.getType() == Schema.Type.DOUBLE
                    || valueSchema.getType() == Schema.Type.BOOLEAN) {
                Map<String, Object> mapPrimitive = new HashMap<>();
                for (Node elementNode : asList(xPathNodeListEvaluation(fullNode, orphanNode, rootXpath, namespaceContext))) {
                    var orphanElementNode = elementNode.cloneNode(true);
                    mapPrimitive.put(xPathStringEvaluation(elementNode, orphanElementNode, keyXpath, namespaceContext), parseValue(valueSchema.getType(), xPathStringEvaluation(elementNode, orphanElementNode, valueXpath, namespaceContext)));
                }
                if(mapPrimitive.size() > 0) {
                    record.put(field.name(), mapPrimitive);
                }
                else{
                    // Set avro default value if it's different from null
                    if (field.hasDefaultValue() && field.defaultVal() != JsonProperties.NULL_VALUE) {
                        record.put(field.name(), field.defaultVal());
                    }
                }
            } else { // for example a map<String, SpecificRecordBase>
                throw new NotImplementedException("Converting from XML to '" + valueSchema.getType() + "' type is not implemented yet");
            }
        } else {
            // Set avro default value if it's different from null
            if (field.hasDefaultValue() && field.defaultVal() != JsonProperties.NULL_VALUE) {
                record.put(field.name(), field.defaultVal());
            }
        }
    }

    private static void convertXMLArrayToAvro(SpecificRecordBase record, Node fullNode, Node orphanNode, NamespaceContext namespaceContext, String baseNamespace, Schema.Field field, Schema fieldType, String xpathSelector) throws ClassNotFoundException {

        Schema elementSchema = fieldType.getElementType();
        String xpath = XPathFormatter.format(field.getProp(xpathSelector));
        if (xpath != null) {
            if (extractRealType(elementSchema).getType() == Schema.Type.RECORD) { // an array of records
                List<SpecificRecordBase> listRecords = new ArrayList<>();
                for (Node elementNode : asList(xPathNodeListEvaluation(fullNode, orphanNode, xpath, namespaceContext))) {
                    listRecords.add(convert(elementNode, elementNode, baseClass(baseNamespace, elementSchema.getName()), namespaceContext, baseNamespace, xpathSelector));
                }
                record.put(field.name(), listRecords);
            } else if (extractRealType(elementSchema).getType() == Schema.Type.STRING) { // an array of string
                List<String> listValues = new ArrayList<>(xPathStringListEvaluation(fullNode, orphanNode, xpath, namespaceContext));
                record.put(field.name(), listValues);
            } else { // an array of other primitive values
                throw new NotImplementedException("Converting xml to avro using an array type different from record or String is not yet supported");
            }
        } else {
            // Set avro default value if it's different from null
            if (field.hasDefaultValue() && field.defaultVal() != JsonProperties.NULL_VALUE) {
                record.put(field.name(), field.defaultVal());
            }
        }

    }

    private static void convertXMLBytesToAvro(SpecificRecordBase record, Node fullNode, Node orphanNode, NamespaceContext namespaceContext, Schema.Field field, Schema fieldType, String xpathSelector) {
        if (fieldType.getLogicalType() != null && fieldType.getLogicalType().getName().equals("decimal")) {
            String xpath = XPathFormatter.format(field.getProp(xpathSelector));
            BigDecimal result = null;
            var scale = ((LogicalTypes.Decimal) fieldType.getLogicalType()).getScale();
            var mathContext = new MathContext(((LogicalTypes.Decimal) fieldType.getLogicalType()).getPrecision());
            if (xpath != null) {
                String value = xPathStringEvaluation(fullNode, orphanNode, xpath, namespaceContext);
                if (value != null) {
                    result = new BigDecimal(value)
                            .setScale(scale, RoundingMode.HALF_UP)
                            .round(mathContext);
                }
            }
            if (result == null && field.hasDefaultValue() && field.defaultVal() != JsonProperties.NULL_VALUE) {
                // Set avro default value if it's different from null
                result = new BigDecimal(new BigInteger((byte[]) field.defaultVal()), scale, mathContext);
            }
            record.put(field.name(), result);
        }
    }

    private static void convertXMLDateToAvro(SpecificRecordBase record, Node fullNode, Node orphanNode, NamespaceContext namespaceContext, Schema.Field field, String xpathSelector) {
        String xpath = XPathFormatter.format(field.getProp(xpathSelector));
        Instant resultDate = null;
        if (xpath != null) {
            String dateTimeString = xPathStringEvaluation(fullNode, orphanNode, xpath, namespaceContext);
            if (dateTimeString != null && dateTimeString.length() > 0) {
                //convert to date
                resultDate = convertUnknownFormatDateToTimestamp(dateTimeString);
            }
        }
        if (resultDate == null && field.hasDefaultValue() && field.defaultVal() != JsonProperties.NULL_VALUE) {
            resultDate = Instant.ofEpochMilli((Long) field.defaultVal());

        }
        record.put(field.name(), resultDate);
    }

    private static void convertXMLRecordToAvro(SpecificRecordBase record, Node fullNode, Node orphanNode, NamespaceContext namespaceContext, String baseNamespace, Schema.Field field, Schema fieldType, String xpathSelector) throws ClassNotFoundException {
        String xpath = XPathFormatter.format(field.getProp(xpathSelector));
        if (xpath != null) {
            List<Node> nodeList = asList(xPathNodeListEvaluation(fullNode, orphanNode, xpath, namespaceContext));
            if (nodeList.size() > 0) {

                var currentNode = nodeList.get(0);
                record.put(field.name(), convert(currentNode, currentNode.cloneNode(true), baseClass(baseNamespace, fieldType.getName()), namespaceContext, baseNamespace, xpathSelector));
            }
        }
    }

    private static void convertXMLPrimitiveTypeToAvro(SpecificRecordBase record, Node fullNode, Node orphanNode, NamespaceContext namespaceContext, Schema.Field field, Schema fieldType, String xpathSelector) {
        String xpath = XPathFormatter.format(field.getProp(xpathSelector));
        if (xpath != null) {
            Object value = parseValue(fieldType.getType(), xPathStringEvaluation(fullNode, orphanNode, xpath, namespaceContext));
            if (value != null) {
                record.put(field.name(), value);
                return;
            }
        }
        if (field.hasDefaultValue()) {
            var defaultVal = field.defaultVal();
            record.put(field.name(), defaultVal == JsonProperties.NULL_VALUE ? null : defaultVal);
        }
    }

    /**
     * Tries to convert the string date using a number of known patterns. Throws a DateTimeParseException if nothing worked
     *
     * @param date The string date to convert
     * @return The timestamp corresponding to the initial string
     */
    private static Instant convertUnknownFormatDateToTimestamp(String date) throws DateTimeParseException {
        try {
            return convertISO8601DateTimeToTimestamp(date);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return convertISO8601DateToTimestamp(date);
        } catch (DateTimeException ignored) {
        }
        try {
            return convertFlatDateToTimestamp(date);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return convertFlatDateTimeToTimestamp(date);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return convertISO8601DateTimeNoOffsetToTimestamp(date);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return convertISO8601DateNoOffsetToTimestamp(date);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return convertFlatDateNoOffsetToTimestamp(date);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return convertFlatDateTimeNoOffsetToTimestamp(date);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return convertFlatDateTimeNoOffsetWithoutZoneToTimestamp(date);
        } catch (ParseException ignored) {
        }
        try {
            return convertFlatDateTimeWithOffsetZoneToTimestamp(date);
        } catch (ParseException ignored) {
        }
        return null;
    }

    private static Instant convertISO8601DateTimeToTimestamp(String s) {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME);

        return zonedDateTime.toInstant();
    }

    private static Instant convertISO8601DateTimeNoOffsetToTimestamp(String s) {
        return convertISO8601DateTimeToTimestamp(s + "Z");
    }

    private static Instant convertISO8601DateToTimestamp(String s) {

        TemporalAccessor parsed = DateTimeFormatter.ISO_DATE.parse(s);
        ZoneId zone = ZoneId.from(parsed);
        String noonFormattedDate = s.replace(zone.getId(), "T12:00Z");
        return convertISO8601DateTimeToTimestamp(noonFormattedDate);
    }

    private static Instant convertISO8601DateNoOffsetToTimestamp(String s) {
        return convertISO8601DateTimeToTimestamp(s + "T00:00Z");
    }

    private static Instant convertFlatDateToTimestamp(String s) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddz");
        TemporalAccessor parsed = formatter.parse(s);
        ZoneId zone = ZoneId.from(parsed);
        String noonFormattedDate = s.replace(zone.getId(), "120000Z");

        return convertFlatDateTimeToTimestamp(noonFormattedDate);
    }

    private static Instant convertFlatDateTimeToTimestamp(String s) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssz");
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(s, formatter);
        return zonedDateTime.toInstant();
    }

    private static Instant convertFlatDateNoOffsetToTimestamp(String s) {
        return convertFlatDateTimeToTimestamp(s + "120000Z");
    }

    private static Instant convertFlatDateTimeNoOffsetToTimestamp(String s) {
        return convertFlatDateTimeToTimestamp(s + "Z");
    }

    private static Instant convertFlatDateTimeNoOffsetWithoutZoneToTimestamp(String s) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        var result = formatter.parse(s);

        return result.toInstant();
    }

    private static Instant convertFlatDateTimeWithOffsetZoneToTimestamp(String s) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'T'00:00");
        var result = formatter.parse(s);
        return result.toInstant();
    }


    @SuppressWarnings("unchecked")
    private static Class<SpecificRecordBase> baseClass(String baseNamespace, String typeName) throws ClassNotFoundException {
        return (Class<SpecificRecordBase>) Class.forName(baseNamespace + "." + typeName);
    }

    /**
     * <p>Redefines all xml namespaces used in the xml document at the root markup.</p>
     * <p>Tries to match avsc-defined namespaces with the actual xml namespaces and deduplicates if there are any namespaces pointing to the same URI</p>
     *
     * @param document         the xml document
     * @param xmlNamespacesMap the map of namespaces defined in the avsc schema
     * @param mapOldNamespaces the map of namespaces defined in the xml document
     */
    public static void simplifyNamespaces(Document document, Map<String, String> xmlNamespacesMap, Map<String, List<String>> mapOldNamespaces) {
        // all namespaces are redefined on root element, matching old namespaces and target namespaces on URI
        for (Map.Entry<String, String> entry : xmlNamespacesMap.entrySet()) {
            // Check xml and avsc match on namespaces definitions
            // if the namespace is the main namespace without prefix (xmlns=...), we use the "null" key
            if (DEFAULT_NAMESPACE.equalsIgnoreCase(entry.getKey())) {
                if (mapOldNamespaces.get(entry.getValue()) == null) {
                    throw new NullPointerException("The default namespace uri provided in the avsc schema (\"" + entry.getValue() + "\") is not defined in the XML document. Either fix your avsc schema to match the default namespace defined in the xml, or make sure that the xml document you are converting is not faulty.");
                }
                document.getDocumentElement().setAttribute(XMLNS + ":" + NO_PREFIX_NS, entry.getValue());
                for (String prefixToReplace : mapOldNamespaces.get(entry.getValue())) {
                    if (prefixToReplace.equals(NO_PREFIX_NS)) {
                        prefixToReplace = null;
                    }
                    replacePrefixNodeRecursively(document.getDocumentElement(), prefixToReplace, NO_PREFIX_NS);
                }
            } else {
                document.getDocumentElement().setAttribute(XMLNS + ":" + entry.getKey(), entry.getValue());

                var prefixesForNamespace = mapOldNamespaces.get(entry.getValue());

                if (prefixesForNamespace == null) {
                    continue;
                }

                for (String prefixToReplace : mapOldNamespaces.get(entry.getValue())) {
                    replacePrefixNodeRecursively(document.getDocumentElement(), prefixToReplace, entry.getKey());
                }
            }
        }
    }

    /**
     * Replace the old prefix by a new prefix. For the main namespace without prefix (xmlns=...), oldPrefix is null.
     *
     * @param node      the node to update
     * @param oldPrefix the prefix to replace (can be null).
     * @param newPrefix the prefix to use instead of the old.
     */
    public static void replacePrefixNodeRecursively(Node node, String oldPrefix, String newPrefix) {
        if (node.getNodeType() == Node.ELEMENT_NODE && Objects.equals(node.getPrefix(), oldPrefix)) {
            node.setPrefix(newPrefix);
        }

        asList(node.getChildNodes()).forEach(n -> replacePrefixNodeRecursively(n, oldPrefix, newPrefix));
    }

    /**
     * <p>Recursively removes all namespace definitions from the given node and its children.</p>
     * <p>Namespaces definition are found by searching for attributes starting with the "xmlns" char sequence.</p>
     *
     * @param node The node to purge
     */
    public static void purgeNamespaces(Node node) {

        asList(node.getChildNodes()).forEach(XmlToAvroUtils::purgeNamespaces);

        var attributes = node.getAttributes();
        if (attributes == null) {
            return;
        }

        var markedForDeletion = new ArrayList<String>();

        for (int i = 0; i < attributes.getLength(); i++) {
            var attribute = attributes.item(i);

            if (attribute.getNodeName().startsWith(XMLNS)) {
                markedForDeletion.add(attribute.getNodeName());
            }
        }

        markedForDeletion.forEach(attributes::removeNamedItem);

    }

    /**
     * Extracts
     *
     * @param node
     * @param oldNamespaces
     * @return
     */
    public static Map<String, List<String>> extractNamespaces(Node node, Map<String, List<String>> oldNamespaces) {
        asList(node.getChildNodes()).forEach(childNode -> extractNamespaces(childNode, oldNamespaces));

        var attributes = node.getAttributes();

        if (attributes == null) {
            return oldNamespaces;
        }

        // this loop extract all "xmlns[:...]" attributes of each node
        for (int i = 0; i < attributes.getLength(); i++) {
            var attribute = attributes.item(i);

            if (attribute.getNodeName().startsWith(XMLNS)) {

                var namespace = attribute.getNodeValue();

                if (StringUtils.isEmpty(namespace)) {
                    continue;
                }
                var prefix = attribute.getNodeName().equals(XMLNS) ? NO_PREFIX_NS : attribute.getNodeName().replace(XMLNS + ":", "");
                var namespacePrefixes = oldNamespaces.get(namespace);

                if (namespacePrefixes != null && !namespacePrefixes.contains(prefix)) {
                    namespacePrefixes.add(prefix);
                    oldNamespaces.put(namespace, namespacePrefixes);
                } else {
                    var newList = new ArrayList<String>();
                    newList.add(prefix);
                    oldNamespaces.put(namespace, newList);
                }
            }
        }

        return oldNamespaces;
    }
}
