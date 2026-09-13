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
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/** Generic utility class for conversions. */
public final class GenericUtils {
    private static final Logger LOG = LoggerFactory.getLogger(GenericUtils.class);

    /** Private constructor. */
    private GenericUtils() {}

    /**
     * Gets a new XPath instance.
     *
     * @return A new XPath instance
     */
    public static XPath getXpath() {
        return XPathFactory.newInstance().newXPath();
    }

    /**
     * Gets the XML namespaces from a schema.
     *
     * @param schema The current XML schema
     * @param namespaceSelector Selector for multiple namespaces mapped in Avro.
     * @return A map containing the namespaces
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> xmlNamespaces(Schema schema, String namespaceSelector) {
        return (Map<String, String>) schema.getObjectProp(namespaceSelector);
    }

    /**
     * Gets the default XML namespaces from a schema's {@code xmlNamespaces} root attribute.
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
     * @param document The document to convert.
     * @return The result string.
     * @throws TransformerException If the conversion fails.
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
     * Parses a string as a {@link Document} and updates its namespaces.
     *
     * @param strValue The string value to evaluate as a Document.
     * @param xmlNamespacesMap The target namespaces map (key: prefix, value: URI). If null, no namespace update is
     *     applied.
     * @return The evaluated XML Document.
     */
    public static Document stringToDocument(String strValue, Map<String, String> xmlNamespacesMap) {
        Document document;
        try {
            // If no xmlNamespacesMap is provided, log a warning and initialize it
            if (xmlNamespacesMap == null) {
                xmlNamespacesMap = new HashMap<>();
                LOG.warn("No xmlNamespaces attribute provided in the avsc!");
            }

            // If no default namespace is present in the document, emulate one
            if (xmlNamespacesMap.get("null") == null) {
                // log a warning mentioning that no default xml namespace has been defined in the avsc, which could be
                // normal if no xml namespace is used / defined in the xml
                LOG.warn(
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
            Map<String, List<String>> namespacePrefixesByURI =
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

    /**
     * Adds a default namespace to the root element when none is present.
     *
     * @param xml The XML content to update.
     * @return The XML content containing a default namespace declaration.
     */
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
     * Evaluates an XPath expression and returns the matching nodes.
     *
     * @param node The source node to evaluate.
     * @param orphanNode The source node to evaluate without parent nodes.
     * @param xPathExpression The XPath expression to match.
     * @param namespaceContext The namespace context.
     * @return The list of matched nodes.
     */
    public static NodeList xPathNodeListEvaluation(
            Node node, Node orphanNode, String xPathExpression, NamespaceContext namespaceContext) {
        NodeList result;

        Node nodeToParse = node;

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
     * Evaluates an XPath expression and returns the matching values.
     *
     * @param node The source node to evaluate
     * @param orphanNode The node to evaluate without parent nodes.
     * @param xPathExpression The XPath expression to match.
     * @param namespaceContext The namespace context
     * @return The list of matched values.
     */
    public static List<String> xPathStringListEvaluation(
            Node node, Node orphanNode, String xPathExpression, NamespaceContext namespaceContext) {
        NodeList nodeList = xPathNodeListEvaluation(node, orphanNode, xPathExpression, namespaceContext);
        return asList(nodeList).stream()
                .map(Node::getTextContent)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Evaluates an XPath expression and returns the matching value.
     *
     * @param node The source node to evaluate.
     * @param orphanNode The source node to evaluate without parent context.
     * @param xPathExpression The XPath expression to match.
     * @param namespaceContext The namespace context.
     * @return The matched value.
     */
    public static String xPathStringEvaluation(
            Node node, Node orphanNode, String xPathExpression, NamespaceContext namespaceContext) {
        String result;

        Node nodeToParse = node;

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
     * Builds a simple {@link NamespaceContext} for a document.
     *
     * @param document The document to analyze.
     * @return The namespace context.
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
            /**
             * Gets the namespace URI associated with a prefix.
             *
             * @param prefix The namespace prefix.
             * @return The namespace URI.
             */
            @Override
            public String getNamespaceURI(String prefix) {
                return mapPrefixes.get(prefix);
            }

            /**
             * Gets one prefix associated with a namespace URI.
             *
             * @param namespaceURI The namespace URI.
             * @return The matching prefix.
             */
            @Override
            public String getPrefix(String namespaceURI) {
                return null;
            }

            /**
             * Gets all prefixes associated with a namespace URI.
             *
             * @param namespaceURI The namespace URI.
             * @return The matching prefixes iterator.
             */
            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                return null;
            }
        };
    }

    /**
     * Converts a {@link NodeList} to a list of nodes.
     *
     * @param n The list to convert.
     * @return An equivalent List of Node.
     */
    public static List<Node> asList(NodeList n) {
        return n.getLength() == 0 ? Collections.emptyList() : new NodeListWrapper(n);
    }

    /**
     * Parses a string value according to its schema type.
     *
     * @param fieldType The schema type.
     * @param value The string value.
     * @return The result of parsing. In case of exception (for example NumberFormatException), the result is null.
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
     * Extracts the non-null type from a nullable union schema.
     *
     * @param schema The schema node, which can be a UNION.
     * @return The non-null type
     */
    public static Optional<Schema> extractRealType(Schema schema) {
        return schema.getType() != Schema.Type.UNION
                ? Optional.of(schema)
                : schema.getTypes().stream()
                        .filter(s -> s.getType() != Schema.Type.NULL)
                        .findFirst();
    }

    /** Wraps a {@link NodeList} as a list of nodes. */
    public static final class NodeListWrapper extends AbstractList<Node> implements RandomAccess {
        private final NodeList list;

        /**
         * Constructs a wrapper around a {@link NodeList}.
         *
         * @param l The wrapped node list.
         */
        NodeListWrapper(NodeList l) {
            list = l;
        }

        /**
         * Gets the node at a given index.
         *
         * @param index The position of the node to retrieve.
         * @return The node at the requested index.
         */
        @Override
        public Node get(int index) {
            return list.item(index);
        }

        /**
         * Gets the number of wrapped nodes.
         *
         * @return The number of wrapped nodes.
         */
        @Override
        public int size() {
            return list.getLength();
        }
    }
}
