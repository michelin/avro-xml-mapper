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
package com.michelin.avroxmlmapper.utility;

import static com.michelin.avroxmlmapper.constants.AvroXmlMapperConstants.XML_NAMESPACE_SELECTOR_DEFAULT;

import com.michelin.avroxmlmapper.constants.AvroXmlMapperConstants;
import com.michelin.avroxmlmapper.exception.AvroXmlMapperException;
import com.michelin.avroxmlmapper.mapper.XmlToAvroUtils;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/** Generic utility class for conversions. */
@Slf4j
public final class GenericUtils {
    private GenericUtils() {}

    /**
     * Get a new XPath instance.
     *
     * @return A new XPath instance
     */
    public static XPath getXpath() {
        return XPathFactory.newInstance().newXPath();
    }

    /**
     * Get the XML namespaces from a given XML schema
     *
     * @param schema The current XML schema
     * @param namespaceSelector selector for multiple namespaces mapped in avro
     * @return A map containing the namespaces
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> xmlNamespaces(Schema schema, String namespaceSelector) {
        return (Map<String, String>) schema.getObjectProp(namespaceSelector);
    }

    /**
     * Get the default XML namespaces from a given XML schema ("xmlNamespaces" root attribute)
     *
     * @param schema The current XML schema
     * @return A map containing the namespaces
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> xmlNamespaces(Schema schema) {
        return (Map<String, String>) schema.getObjectProp(XML_NAMESPACE_SELECTOR_DEFAULT);
    }

    /**
     * Converts a document to a String.
     *
     * @param document the document to convert
     * @return the result string
     * @throws TransformerException if the conversion fails
     */
    public static String documentToString(Document document) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        StringWriter writer = new StringWriter();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Evaluate a string value as a org.w3c.dom.Document and update namespaces according to the target.
     *
     * @param strValue the string value to evaluate as a Document
     * @param xmlNamespacesMap the target of namespaces (key : prefix ; value : URI), if null no update on namespaces.
     * @return the evaluated xml Document
     */
    public static Document stringToDocument(String strValue, Map<String, String> xmlNamespacesMap) {
        Document document;
        try {
            // If no xmlNamespacesMap is provided, log a warning and initialize it
            if (xmlNamespacesMap == null) {
                xmlNamespacesMap = new HashMap<>();
                log.warn("No xmlNamespaces attribute provided in the avsc!");
            }

            // If no default namespace is present in the document, emulate one
            if (xmlNamespacesMap.get("null") == null) {
                // log a warning mentioning that no default xml namespace has been defined in the avsc, which could be
                // normal if no xml namespace is used / defined in the xml
                log.warn(
                        "No default xml namespace has been defined in the avsc, which could be normal if no xmlns is used / defined in the xml but could also be a mistake from the user");

                // Add a stub default namespace to the document root element to avoid NPE when evaluating xPath
                // expressions and add it to the xmlNamespacesMap
                strValue = addDefaultXMLNS(strValue);

                xmlNamespacesMap.put("null", "http://www.example.com/defaultUri");
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(strValue));
            document = builder.parse(is);

            // build a reverse map of namespaces : URI (K) -> list of prefixes (V)
            var namespacePrefixesByURI =
                    XmlToAvroUtils.extractNamespaces(document.getDocumentElement(), new HashMap<>());

            // Remove all namespace definitions
            XmlToAvroUtils.purgeNamespaces(document.getDocumentElement());

            // Unify all namespaces by keeping only the ones defined in the xmlNamespacesMap.
            // For instance, if the namespacePrefixesByURI map
            // contains {"http://www.openapplications.org/oagis/9", ["ns2", "ns9"]},
            // and the xmlNamespacesMap contains {"ns2", "http://www.openapplications.org/oagis/9"},
            // Then the only namespace left will be "ns2" and the prefix "ns9" will be removed.
            XmlToAvroUtils.simplifyNamespaces(document, xmlNamespacesMap, namespacePrefixesByURI);

            return document;

        } catch (Exception e) {
            throw new AvroXmlMapperException("Failed to parse XML", e);
        }
    }

    private static String addDefaultXMLNS(String xml) {
        int rootStart;
        int rootEnd;

        int declarationIndex = xml.indexOf("?>");
        if (declarationIndex == -1) {
            rootStart = xml.indexOf("<");
            rootEnd = xml.indexOf(">");
        } else {
            rootStart = xml.indexOf("<", declarationIndex + 2);
            rootEnd = xml.indexOf(">", declarationIndex + 2);
        }

        if (rootStart != -1 && rootEnd != -1) {
            String rootElement = xml.substring(rootStart, rootEnd + 1);

            if (!rootElement.contains("xmlns=")) {
                String modifiedRootElement =
                        rootElement.replaceFirst(">", " xmlns=\"http://www.example.com/defaultUri\">");
                xml = xml.substring(0, rootStart) + modifiedRootElement + xml.substring(rootEnd + 1);
            }
        }

        return xml;
    }

    /**
     * Handle exception for Node xPath evaluation
     *
     * @param node the source node to evaluate
     * @param orphanNode the source node to evaluate without parent nodes
     * @param xPathExpression the xPathExpression to match
     * @param namespaceContext the namespace context
     * @return the list of matched nodes
     */
    public static NodeList xPathNodeListEvaluation(
            Node node, Node orphanNode, String xPathExpression, NamespaceContext namespaceContext) {
        NodeList result;

        var nodeToParse = node;

        // Isolate current node from the whole document for performance when full context is not necessary
        if (!xPathExpression.contains("//")) {
            nodeToParse = orphanNode;
        }

        try {
            XPath xPath = getXpath();
            xPath.setNamespaceContext(namespaceContext);
            result = (NodeList) xPath.compile(xPathExpression).evaluate(nodeToParse, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new AvroXmlMapperException("Failed to execute xpath " + xPathExpression, e);
        }

        return result;
    }

    /**
     * Handle exception for Node xPath evaluation
     *
     * @param node The source node to evaluate
     * @param orphanNode The node to evaluate without parent nodes
     * @param xPathExpression The xPathExpression to match
     * @param namespaceContext The namespace context
     * @return The list of matched values
     */
    public static List<String> xPathStringListEvaluation(
            Node node, Node orphanNode, String xPathExpression, NamespaceContext namespaceContext) {
        var nodeList = xPathNodeListEvaluation(node, orphanNode, xPathExpression, namespaceContext);
        return asList(nodeList).stream()
                .map(Node::getTextContent)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Handle exception for Node xPath evaluation
     *
     * @param node the source node to evaluate
     * @param orphanNode the source node to evaluate without parent context
     * @param xPathExpression the xPathExpression to match
     * @param namespaceContext the namespaceContext
     * @return the list of matched nodes
     */
    public static String xPathStringEvaluation(
            Node node, Node orphanNode, String xPathExpression, NamespaceContext namespaceContext) {
        String result;

        var nodeToParse = node;

        // Isolate current node from the whole document for performance when full context is not necessary
        if (!xPathExpression.contains("//")) {
            nodeToParse = orphanNode;
        }

        try {
            XPath xPath = getXpath();
            xPath.setNamespaceContext(namespaceContext);
            result = (String) xPath.compile(xPathExpression).evaluate(nodeToParse, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new AvroXmlMapperException("Failed to execute xpath " + xPathExpression, e);
        }

        return result != null && !result.isBlank() ? result : null;
    }

    /**
     * Build a simple NamespaceContext in order to make Xpath usable for a document
     *
     * @param document the document to analyze
     * @return the namespace context
     */
    public static NamespaceContext getNamespaceContext(Document document) {
        NamedNodeMap mapAttributes = document.getDocumentElement().getAttributes();

        Map<String, String> mapPrefixes = new HashMap<>();
        for (int i = 0; i < mapAttributes.getLength(); i++) {
            Attr attr = (Attr) mapAttributes.item(i);
            String attrName = attr.getNodeName();
            if (attrName.startsWith(AvroXmlMapperConstants.XMLNS + ":")
                    || attrName.equals(AvroXmlMapperConstants.XMLNS)) {
                if (attrName.equals(AvroXmlMapperConstants.XMLNS))
                    mapPrefixes.put(AvroXmlMapperConstants.NO_PREFIX_NS, attr.getValue());
                else
                    mapPrefixes.put(
                            attr.getNodeName().replace(AvroXmlMapperConstants.XMLNS + ":", ""), attr.getValue());
            }
        }

        return new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                return mapPrefixes.get(prefix);
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return null;
            }

            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                return null;
            }
        };
    }

    /**
     * Convert a NodeList to an IterableList of Node
     *
     * @param n the list to convert
     * @return an equivalent List of Node
     */
    public static List<Node> asList(NodeList n) {
        return n.getLength() == 0 ? Collections.emptyList() : new NodeListWrapper(n);
    }

    /**
     * Try to parse a string value to the Java type based on Schema type.
     *
     * @param fieldType the schema type
     * @param value the string value
     * @return the result of parsing. In case of Exception (for ex NumberFormatException) the result is null.
     */
    public static Object parseValue(Schema.Type fieldType, String value) {
        Object result;
        try {
            result = switch (fieldType) {
                case STRING -> value;
                case INT -> Integer.valueOf(value);
                case LONG -> Long.valueOf(value);
                case FLOAT -> Float.valueOf(value);
                case DOUBLE -> Double.valueOf(value);
                case BOOLEAN -> Boolean.valueOf(value);
                default -> null;
            };
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    /**
     * Frequently the type is defined in avsc with this pattern : "type" : [ "null", "realType"] to allow a null value.
     * This pattern creates a UNION type, with two subtypes. This method extracts the non-null type ("real type").
     *
     * @param schema the schema node which can be a UNION
     * @return The non-null type
     */
    public static Optional<Schema> extractRealType(Schema schema) {
        return schema.getType() != Schema.Type.UNION
                ? Optional.of(schema)
                : schema.getTypes().stream()
                        .filter(s -> s.getType() != Schema.Type.NULL)
                        .findFirst();
    }

    /** Custom class allowing the conversion of NodeList to iterable List of Node */
    public static final class NodeListWrapper extends AbstractList<Node> implements RandomAccess {
        private final NodeList list;

        NodeListWrapper(NodeList l) {
            list = l;
        }

        public Node get(int index) {
            return list.item(index);
        }

        public int size() {
            return list.getLength();
        }
    }
}
