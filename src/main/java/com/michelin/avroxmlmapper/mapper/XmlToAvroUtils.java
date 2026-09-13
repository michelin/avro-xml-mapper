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
package com.michelin.avroxmlmapper.mapper;

import static com.michelin.avroxmlmapper.constants.AvroXmlMapperConstants.*;
import static com.michelin.avroxmlmapper.utility.GenericUtils.*;

import com.michelin.avroxmlmapper.exception.AvroXmlMapperException;
import com.michelin.avroxmlmapper.utility.XPathFormatter;
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
import javax.xml.namespace.NamespaceContext;
import org.apache.avro.JsonProperties;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/** Utility class for converting XML to Avro. */
public final class XmlToAvroUtils {

    /** Private constructor. */
    private XmlToAvroUtils() {}

    /**
     * Recursively converts an XML node to a {@link SpecificRecordBase}.
     *
     * @param fullNode XML-node to convert
     * @param orphanNode XML-node to convert without parent context
     * @param clazz Class of the SpecificRecord to generate.
     * @param namespaceContext The namespace context.
     * @param baseNamespace Base namespace for the generated SpecificRecord classes.
     * @param xpathSelector The XPath selector used to find mappings in the Avro definition.
     * @param <T> The type of the Avro object
     * @return SpecificRecord generated.
     */
    static <T extends SpecificRecordBase> T convert(
            Node fullNode,
            Node orphanNode,
            Class<T> clazz,
            NamespaceContext namespaceContext,
            String baseNamespace,
            String xpathSelector) {
        try {
            T message = clazz.getDeclaredConstructor().newInstance();

            for (Schema.Field field : message.getSchema().getFields()) {
                Optional<Schema> fieldType = extractRealType(field.schema());

                if (fieldType.isEmpty()) {
                    continue;
                }

                switch (fieldType.get().getType()) {
                    case NULL, UNION, ENUM:
                        break;
                    case RECORD:
                        convertXMLRecordToAvro(
                                message,
                                fullNode,
                                orphanNode,
                                namespaceContext,
                                baseNamespace,
                                field,
                                fieldType.get(),
                                xpathSelector);
                        break;
                    case ARRAY:
                        convertXMLArrayToAvro(
                                message,
                                fullNode,
                                orphanNode,
                                namespaceContext,
                                baseNamespace,
                                field,
                                fieldType.get(),
                                xpathSelector);
                        break;
                    case MAP:
                        convertXMLMapToAvro(
                                message, fullNode, orphanNode, namespaceContext, field, fieldType.get(), xpathSelector);
                        break;
                    case LONG:
                        // Handle dates to a TimezonedTimestamp format
                        if (fieldType.get().getLogicalType() != null
                                && fieldType.get().getLogicalType().getName().equals("timestamp-millis")) {
                            convertXMLDateToAvro(message, fullNode, orphanNode, namespaceContext, field, xpathSelector);
                        }
                        break;
                    case BYTES:
                        convertXMLBytesToAvro(
                                message, fullNode, orphanNode, namespaceContext, field, fieldType.get(), xpathSelector);
                        break;
                    default:
                        // all other = primitive types
                        convertXMLPrimitiveTypeToAvro(
                                message, fullNode, orphanNode, namespaceContext, field, fieldType.get(), xpathSelector);
                }
            }
            return message;
        } catch (Exception e) {
            throw new AvroXmlMapperException("Failed to parse document", e);
        }
    }

    /**
     * Converts XML map structures to Avro map fields.
     *
     * @param message The target record.
     * @param fullNode The source XML node with full context.
     * @param orphanNode The source XML node without parent context.
     * @param namespaceContext The namespace context.
     * @param field The Avro field.
     * @param fieldType The Avro field schema.
     * @param xpathSelector The XPath selector property name.
     */
    private static void convertXMLMapToAvro(
            SpecificRecordBase message,
            Node fullNode,
            Node orphanNode,
            NamespaceContext namespaceContext,
            Schema.Field field,
            Schema fieldType,
            String xpathSelector) {
        // Initialize value Schema
        Schema valueSchema = fieldType.getValueType();
        String rootXpath = null;
        String keyXpath = null;
        String valueXpath = null;

        // Try to get the map xpath properties
        LinkedHashMap<String, String> mapXpathProperties =
                (LinkedHashMap<String, String>) field.getObjectProp(xpathSelector);

        if (mapXpathProperties != null) {
            // New scenarios
            rootXpath = XPathFormatter.format(mapXpathProperties.get(XPATH_MAP_ROOT_PROPERTY_NAME));
            keyXpath = XPathFormatter.format(mapXpathProperties.get(XPATH_MAP_KEY_PROPERTY_NAME));
            valueXpath = XPathFormatter.format(mapXpathProperties.get(XPATH_MAP_VALUE_PROPERTY_NAME));
        }

        if (rootXpath != null && keyXpath != null && valueXpath != null) {
            if (valueSchema.getType() == Schema.Type.STRING
                    || valueSchema.getType() == Schema.Type.INT
                    || valueSchema.getType() == Schema.Type.LONG
                    || valueSchema.getType() == Schema.Type.FLOAT
                    || valueSchema.getType() == Schema.Type.DOUBLE
                    || valueSchema.getType() == Schema.Type.BOOLEAN) {
                Map<String, Object> mapPrimitive = new HashMap<>();
                for (Node elementNode :
                        asList(xPathNodeListEvaluation(fullNode, orphanNode, rootXpath, namespaceContext))) {
                    Node orphanElementNode = elementNode.cloneNode(true);
                    String key = xPathStringEvaluation(elementNode, orphanElementNode, keyXpath, namespaceContext);

                    // Get the value to apply default if it isn't there
                    Object value = parseValue(
                            valueSchema.getType(),
                            xPathStringEvaluation(elementNode, orphanElementNode, valueXpath, namespaceContext));
                    if (value == null) {
                        value = fieldType.getObjectProps().get("default");
                    }

                    mapPrimitive.put(key, value);
                }

                if (!mapPrimitive.isEmpty()) {
                    message.put(field.name(), mapPrimitive);
                } else {
                    // Set avro default value if it's different from null
                    if (field.hasDefaultValue() && field.defaultVal() != JsonProperties.NULL_VALUE) {
                        message.put(field.name(), field.defaultVal());
                    }
                }
            } else { // For example a map<String, SpecificRecordBase>
                throw new NotImplementedException(
                        "Converting from XML to '" + valueSchema.getType() + "' type is not implemented yet");
            }
        } else {
            // Set avro default value if it's different from null
            if (field.hasDefaultValue() && field.defaultVal() != JsonProperties.NULL_VALUE) {
                message.put(field.name(), field.defaultVal());
            }
        }
    }

    /**
     * Converts XML array structures to Avro array fields.
     *
     * @param message The target record.
     * @param fullNode The source XML node with full context.
     * @param orphanNode The source XML node without parent context.
     * @param namespaceContext The namespace context.
     * @param baseNamespace The base namespace for generated classes.
     * @param field The Avro field.
     * @param fieldType The Avro field schema.
     * @param xpathSelector The XPath selector property name.
     * @throws ClassNotFoundException If the nested record class cannot be found.
     */
    private static void convertXMLArrayToAvro(
            SpecificRecordBase message,
            Node fullNode,
            Node orphanNode,
            NamespaceContext namespaceContext,
            String baseNamespace,
            Schema.Field field,
            Schema fieldType,
            String xpathSelector)
            throws ClassNotFoundException {
        Schema elementSchema = fieldType.getElementType();
        String xpath = XPathFormatter.format(field.getProp(xpathSelector));

        if (xpath != null) {
            Optional<Schema> schema = extractRealType(elementSchema);

            if (schema.isPresent() && schema.get().getType() == Schema.Type.RECORD) { // An array of records
                List<SpecificRecordBase> listRecords = new ArrayList<>();
                for (Node elementNode :
                        asList(xPathNodeListEvaluation(fullNode, orphanNode, xpath, namespaceContext))) {
                    listRecords.add(convert(
                            elementNode,
                            elementNode,
                            baseClass(baseNamespace, elementSchema.getName()),
                            namespaceContext,
                            baseNamespace,
                            xpathSelector));
                }
                message.put(field.name(), listRecords);
            } else if (schema.isPresent() && schema.get().getType() == Schema.Type.STRING) { // An array of string
                List<String> listValues =
                        new ArrayList<>(xPathStringListEvaluation(fullNode, orphanNode, xpath, namespaceContext));
                message.put(field.name(), listValues);
            } else { // An array of other primitive values
                throw new NotImplementedException(
                        "Converting XML to Avro using an array type different from record or String is not yet supported");
            }
        } else {
            // Set Avro default value if it is different from null
            if (field.hasDefaultValue() && field.defaultVal() != JsonProperties.NULL_VALUE) {
                message.put(field.name(), field.defaultVal());
            }
        }
    }

    /**
     * Converts XML decimal values to Avro bytes logical decimals.
     *
     * @param message The target record.
     * @param fullNode The source XML node with full context.
     * @param orphanNode The source XML node without parent context.
     * @param namespaceContext The namespace context.
     * @param field The Avro field.
     * @param fieldType The Avro field schema.
     * @param xpathSelector The XPath selector property name.
     */
    private static void convertXMLBytesToAvro(
            SpecificRecordBase message,
            Node fullNode,
            Node orphanNode,
            NamespaceContext namespaceContext,
            Schema.Field field,
            Schema fieldType,
            String xpathSelector) {
        if (fieldType.getLogicalType() != null
                && fieldType.getLogicalType().getName().equals("decimal")) {
            String xpath = XPathFormatter.format(field.getProp(xpathSelector));
            BigDecimal result = null;
            int scale = ((LogicalTypes.Decimal) fieldType.getLogicalType()).getScale();
            MathContext mathContext =
                    new MathContext(((LogicalTypes.Decimal) fieldType.getLogicalType()).getPrecision());
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
            message.put(field.name(), result);
        }
    }

    /**
     * Converts XML date values to Avro timestamp fields.
     *
     * @param message The target record.
     * @param fullNode The source XML node with full context.
     * @param orphanNode The source XML node without parent context.
     * @param namespaceContext The namespace context.
     * @param field The Avro field.
     * @param xpathSelector The XPath selector property name.
     */
    private static void convertXMLDateToAvro(
            SpecificRecordBase message,
            Node fullNode,
            Node orphanNode,
            NamespaceContext namespaceContext,
            Schema.Field field,
            String xpathSelector) {
        String xpath = XPathFormatter.format(field.getProp(xpathSelector));
        Instant resultDate = null;
        if (xpath != null) {
            String dateTimeString = xPathStringEvaluation(fullNode, orphanNode, xpath, namespaceContext);
            if (dateTimeString != null && !dateTimeString.isEmpty()) {
                // convert to date
                resultDate = convertUnknownFormatDateToTimestamp(dateTimeString);
            }
        }
        if (resultDate == null && field.hasDefaultValue() && field.defaultVal() != JsonProperties.NULL_VALUE) {
            resultDate = Instant.ofEpochMilli((Long) field.defaultVal());
        }
        message.put(field.name(), resultDate);
    }

    /**
     * Converts XML nested record values to Avro record fields.
     *
     * @param message The target record.
     * @param fullNode The source XML node with full context.
     * @param orphanNode The source XML node without parent context.
     * @param namespaceContext The namespace context.
     * @param baseNamespace The base namespace for generated classes.
     * @param field The Avro field.
     * @param fieldType The Avro field schema.
     * @param xpathSelector The XPath selector property name.
     * @throws ClassNotFoundException If the nested record class cannot be found.
     */
    private static void convertXMLRecordToAvro(
            SpecificRecordBase message,
            Node fullNode,
            Node orphanNode,
            NamespaceContext namespaceContext,
            String baseNamespace,
            Schema.Field field,
            Schema fieldType,
            String xpathSelector)
            throws ClassNotFoundException {
        String xpath = XPathFormatter.format(field.getProp(xpathSelector));

        if (xpath != null) {
            List<Node> nodeList = asList(xPathNodeListEvaluation(fullNode, orphanNode, xpath, namespaceContext));

            if (!nodeList.isEmpty()) {
                Node currentNode = nodeList.get(0);
                message.put(
                        field.name(),
                        convert(
                                currentNode,
                                currentNode.cloneNode(true),
                                baseClass(baseNamespace, fieldType.getName()),
                                namespaceContext,
                                baseNamespace,
                                xpathSelector));
            }
        }
    }

    /**
     * Converts XML primitive values to Avro primitive fields.
     *
     * @param message The target record.
     * @param fullNode The source XML node with full context.
     * @param orphanNode The source XML node without parent context.
     * @param namespaceContext The namespace context.
     * @param field The Avro field.
     * @param fieldType The Avro field schema.
     * @param xpathSelector The xpath selector property name.
     */
    private static void convertXMLPrimitiveTypeToAvro(
            SpecificRecordBase message,
            Node fullNode,
            Node orphanNode,
            NamespaceContext namespaceContext,
            Schema.Field field,
            Schema fieldType,
            String xpathSelector) {
        String xpath = XPathFormatter.format(field.getProp(xpathSelector));
        if (xpath != null) {
            Object value = parseValue(
                    fieldType.getType(), xPathStringEvaluation(fullNode, orphanNode, xpath, namespaceContext));
            if (value != null) {
                message.put(field.name(), value);
                return;
            }
        }
        if (field.hasDefaultValue()) {
            Object defaultVal = field.defaultVal();
            message.put(field.name(), defaultVal == JsonProperties.NULL_VALUE ? null : defaultVal);
        }
    }

    /**
     * Converts a date string using known patterns.
     *
     * @param date The string date to convert
     * @return The timestamp corresponding to the initial string
     */
    private static Instant convertUnknownFormatDateToTimestamp(String date) throws DateTimeParseException {
        try {
            return convertISO8601DateTimeToTimestamp(date);
        } catch (DateTimeParseException ignored) {
            // Do nothing
        }

        try {
            return convertISO8601DateToTimestamp(date);
        } catch (DateTimeException ignored) {
            // Do nothing
        }

        try {
            return convertFlatDateToTimestamp(date);
        } catch (DateTimeParseException ignored) {
            // Do nothing
        }

        try {
            return convertFlatDateTimeToTimestamp(date);
        } catch (DateTimeParseException ignored) {
            // Do nothing
        }

        try {
            return convertISO8601DateTimeNoOffsetToTimestamp(date);
        } catch (DateTimeParseException ignored) {
            // Do nothing
        }

        try {
            return convertISO8601DateNoOffsetToTimestamp(date);
        } catch (DateTimeParseException ignored) {
            // Do nothing
        }

        try {
            return convertFlatDateNoOffsetToTimestamp(date);
        } catch (DateTimeParseException ignored) {
            // Do nothing
        }

        try {
            return convertFlatDateTimeNoOffsetToTimestamp(date);
        } catch (DateTimeParseException ignored) {
            // Do nothing
        }

        try {
            return convertFlatDateTimeNoOffsetWithoutZoneToTimestamp(date);
        } catch (ParseException ignored) {
            // Do nothing
        }

        try {
            return convertFlatDateTimeWithOffsetZoneToTimestamp(date);
        } catch (ParseException ignored) {
            // Do nothing
        }

        return null;
    }

    /**
     * Converts an ISO-8601 datetime string to an Instant.
     *
     * @param s The datetime string.
     * @return The corresponding instant.
     */
    private static Instant convertISO8601DateTimeToTimestamp(String s) {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME);
        return zonedDateTime.toInstant();
    }

    /**
     * Converts an ISO-8601 datetime string without offset to an Instant.
     *
     * @param s The datetime string without offset.
     * @return The corresponding instant.
     */
    private static Instant convertISO8601DateTimeNoOffsetToTimestamp(String s) {
        return convertISO8601DateTimeToTimestamp(s + "Z");
    }

    /**
     * Converts an ISO-8601 date string with zone to an Instant.
     *
     * @param s The date string with zone.
     * @return The corresponding instant.
     */
    private static Instant convertISO8601DateToTimestamp(String s) {

        TemporalAccessor parsed = DateTimeFormatter.ISO_DATE.parse(s);
        ZoneId zone = ZoneId.from(parsed);
        String noonFormattedDate = s.replace(zone.getId(), "T12:00Z");
        return convertISO8601DateTimeToTimestamp(noonFormattedDate);
    }

    /**
     * Converts an ISO-8601 date string without zone to an Instant.
     *
     * @param s The date string without zone.
     * @return The corresponding instant.
     */
    private static Instant convertISO8601DateNoOffsetToTimestamp(String s) {
        return convertISO8601DateTimeToTimestamp(s + "T00:00Z");
    }

    /**
     * Converts a flat date string with zone to an Instant.
     *
     * @param s The flat date string.
     * @return The corresponding instant.
     */
    private static Instant convertFlatDateToTimestamp(String s) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddz");
        TemporalAccessor parsed = formatter.parse(s);
        ZoneId zone = ZoneId.from(parsed);
        String noonFormattedDate = s.replace(zone.getId(), "120000Z");

        return convertFlatDateTimeToTimestamp(noonFormattedDate);
    }

    /**
     * Converts a flat datetime string with zone to an Instant.
     *
     * @param s The flat datetime string.
     * @return The corresponding instant.
     */
    private static Instant convertFlatDateTimeToTimestamp(String s) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssz");
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(s, formatter);
        return zonedDateTime.toInstant();
    }

    /**
     * Converts a flat date string without zone to an Instant.
     *
     * @param s The flat date string without zone.
     * @return The corresponding instant.
     */
    private static Instant convertFlatDateNoOffsetToTimestamp(String s) {
        return convertFlatDateTimeToTimestamp(s + "120000Z");
    }

    /**
     * Converts a flat datetime string without zone to an Instant.
     *
     * @param s The flat datetime string without zone.
     * @return The corresponding instant.
     */
    private static Instant convertFlatDateTimeNoOffsetToTimestamp(String s) {
        return convertFlatDateTimeToTimestamp(s + "Z");
    }

    /**
     * Converts a yyyy-MM-dd HH:mm:ss string to an Instant.
     *
     * @param s The datetime string.
     * @return The corresponding instant.
     * @throws ParseException If parsing fails.
     */
    private static Instant convertFlatDateTimeNoOffsetWithoutZoneToTimestamp(String s) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date result = formatter.parse(s);

        return result.toInstant();
    }

    /**
     * Converts a yyyy-MM-dd'T'HH:mm:ss'T'00:00 string to an Instant.
     *
     * @param s The datetime string.
     * @return The corresponding instant.
     * @throws ParseException If parsing fails.
     */
    private static Instant convertFlatDateTimeWithOffsetZoneToTimestamp(String s) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'T'00:00");
        Date result = formatter.parse(s);
        return result.toInstant();
    }

    /**
     * Resolves the generated SpecificRecord class for a schema type.
     *
     * @param baseNamespace The base namespace.
     * @param typeName The record type name.
     * @return The generated SpecificRecord class.
     * @throws ClassNotFoundException If the class cannot be found.
     */
    @SuppressWarnings("unchecked")
    private static Class<SpecificRecordBase> baseClass(String baseNamespace, String typeName)
            throws ClassNotFoundException {
        return (Class<SpecificRecordBase>) Class.forName(baseNamespace + "." + typeName);
    }

    /**
     * Redefines all XML namespaces at the document root.
     *
     * <p>Matches schema-defined namespaces with the XML namespaces and deduplicates namespaces with the same URI.
     *
     * @param document The XML document.
     * @param xmlNamespacesMap The map of namespaces defined in the AVSC schema.
     * @param mapOldNamespaces The map of namespaces defined in the XML document.
     */
    public static void simplifyNamespaces(
            Document document, Map<String, String> xmlNamespacesMap, Map<String, List<String>> mapOldNamespaces) {
        // all namespaces are redefined on root element, matching old namespaces and target namespaces on URI
        for (Map.Entry<String, String> entry : xmlNamespacesMap.entrySet()) {
            // Check xml and avsc match on namespaces definitions
            // if the namespace is the main namespace without prefix (xmlns=...), we use the "null" key
            if (DEFAULT_NAMESPACE.equalsIgnoreCase(entry.getKey())) {
                if (mapOldNamespaces.get(entry.getValue()) == null) {
                    throw new NullPointerException(
                            "The default namespace uri provided in the avsc schema (\"" + entry.getValue()
                                    + "\") is not defined in the XML document. Either fix your avsc schema to match the default namespace defined in the xml, or make sure that the xml document you are converting is not faulty.");
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

                List<String> prefixesForNamespace = mapOldNamespaces.get(entry.getValue());

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
     * Replaces an old prefix with a new prefix. For the default namespace ({@code xmlns="..."}) {@code oldPrefix} is
     * null.
     *
     * @param node The node to update.
     * @param oldPrefix The prefix to replace (can be null).
     * @param newPrefix The prefix to use instead of the old.
     */
    public static void replacePrefixNodeRecursively(Node node, String oldPrefix, String newPrefix) {
        if (node.getNodeType() == Node.ELEMENT_NODE && Objects.equals(node.getPrefix(), oldPrefix)) {
            node.setPrefix(newPrefix);
        }

        asList(node.getChildNodes()).forEach(n -> replacePrefixNodeRecursively(n, oldPrefix, newPrefix));
    }

    /**
     * Recursively removes all namespace definitions from the given node and its children.
     *
     * <p>Namespace definitions are found by searching for attributes starting with {@code xmlns}.
     *
     * @param node The node to purge.
     */
    public static void purgeNamespaces(Node node) {

        asList(node.getChildNodes()).forEach(XmlToAvroUtils::purgeNamespaces);

        NamedNodeMap attributes = node.getAttributes();
        if (attributes == null) {
            return;
        }

        List<String> markedForDeletion = new ArrayList<>();

        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);

            if (attribute.getNodeName().startsWith(XMLNS)) {
                markedForDeletion.add(attribute.getNodeName());
            }
        }

        markedForDeletion.forEach(attributes::removeNamedItem);
    }

    /**
     * Recursively extracts all namespaces from the given node and its children.
     *
     * @param node The node from which to extract namespaces.
     * @param oldNamespaces The namespace map to update.
     * @return The updated namespace map.
     */
    public static Map<String, List<String>> extractNamespaces(Node node, Map<String, List<String>> oldNamespaces) {
        asList(node.getChildNodes()).forEach(childNode -> extractNamespaces(childNode, oldNamespaces));

        NamedNodeMap attributes = node.getAttributes();

        if (attributes == null) {
            return oldNamespaces;
        }

        // this loop extract all "xmlns[:...]" attributes of each node
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);

            if (attribute.getNodeName().startsWith(XMLNS)) {

                String namespace = attribute.getNodeValue();

                if (StringUtils.isEmpty(namespace)) {
                    continue;
                }
                String prefix = attribute.getNodeName().equals(XMLNS)
                        ? NO_PREFIX_NS
                        : attribute.getNodeName().replace(XMLNS + ":", "");
                List<String> namespacePrefixes = oldNamespaces.get(namespace);

                if (namespacePrefixes != null && !namespacePrefixes.contains(prefix)) {
                    namespacePrefixes.add(prefix);
                    oldNamespaces.put(namespace, namespacePrefixes);
                } else {
                    List<String> newList = new ArrayList<>();
                    newList.add(prefix);
                    oldNamespaces.put(namespace, newList);
                }
            }
        }

        return oldNamespaces;
    }
}
