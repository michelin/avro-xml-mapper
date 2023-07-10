package com.michelin.avroxmlmapper.utility;

import com.michelin.avroxmlmapper.constants.XMLUtilsConstants;
import com.michelin.avroxmlmapper.exception.AvroXmlMapperException;
import com.michelin.avroxmlmapper.mapper.XMLToAvroUtils;
import org.apache.avro.Schema;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

public final class GenericUtils {


    public static long stringParseCount = 0;

    private static XPath XPATH_STATIC;

    public static XPath getXpath() {
        if (XPATH_STATIC == null) {
            XPATH_STATIC = XPathFactory.newInstance().newXPath();
        }
        return XPATH_STATIC;
    }

    /**
     * Get the XML namespaces from a given XML schema
     *
     * @param schema            The current XML schema
     * @param namespaceSelector selector for multiple namespaces mapped in avro
     * @return A map containing the namespaces
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> xmlNamespaces(Schema schema, String namespaceSelector) {
        return (Map<String, String>) schema.getObjectProp(namespaceSelector);
    }

    /**
     * Get the XML namespaces from a given XML schema
     *
     * @param schema The current XML schema
     * @return A map containing the namespaces
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> xmlNamespaces(Schema schema) {
        return (Map<String, String>) schema.getObjectProp("xmlNamespaces");
    }

    /**
     * Converts a document to a String.
     *
     * @param document the document to convert
     * @return the result string
     * @throws TransformerException
     */
    public static String documentToString(Document document) throws TransformerException {
        StringWriter writer = new StringWriter();
        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Evaluate a string value as a org.w3c.dom.Document and update namespaces according to the target.
     *
     * @param strValue         the string value to evaluate as a Document
     * @param xmlNamespacesMap the target of namespaces (key : prefix ; value : URI), if null no update on namespaces.
     * @return the evaluated xml Document
     */
    public static Document stringToDocument(String strValue, Map<String, String> xmlNamespacesMap) {
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(strValue));
            document = builder.parse(is);

            if (xmlNamespacesMap == null) {
                return document;
            }

            // build a reverse map of namespaces : URI (K) -> list of prefixes (V)
            var mapOldNamespaces = XMLToAvroUtils.extractNamespaces(document.getDocumentElement(), new HashMap<>());
            XMLToAvroUtils.purgeNamespaces(document.getDocumentElement());
            XMLToAvroUtils.simplifyNamespaces(document, xmlNamespacesMap, mapOldNamespaces);

            return document;

        } catch (Exception e) {
            throw new AvroXmlMapperException("Failed to parse XML", e);
        }
    }

    /**
     * Handle exception for Node xPath evaluation
     *
     * @param node             the source node to evaluate
     * @param orphanNode       the source node to evaluate without parent nodes
     * @param xPathExpression  the xPathExpression to match
     * @param namespaceContext the namespace context
     * @return the list of matched nodes
     */
    public static NodeList xPathNodeListEvaluation(Node node, Node orphanNode, String xPathExpression, NamespaceContext namespaceContext) {
        NodeList result = null;

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
     * @param node             the source node to evaluate
     * @param xPathExpression  the xPathExpression to match
     * @param namespaceContext the namespace context
     * @return the list of matched values
     */
    public static List<String> xPathStringListEvaluation(Node node, Node orphanNode, String xPathExpression, NamespaceContext namespaceContext) {
        var nodeList = xPathNodeListEvaluation(node, orphanNode, xPathExpression, namespaceContext);
        return asList(nodeList).stream().map(Node::getTextContent).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    /**
     * Handle exception for Node xPath evaluation
     *
     * @param node             the source node to evaluate
     * @param orphanNode       the source node to evaluate without parent context
     * @param xPathExpression  the xPathExpression to match
     * @param namespaceContext the namespaceContext
     * @return the list of matched nodes
     */
    public static String xPathStringEvaluation(Node node, Node orphanNode, String xPathExpression, NamespaceContext namespaceContext) {
        String result = null;

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
        stringParseCount++;

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
            if (attrName.startsWith(XMLUtilsConstants.XMLNS + ":") || attrName.equals(XMLUtilsConstants.XMLNS)) {
                if (attrName.equals(XMLUtilsConstants.XMLNS))
                    mapPrefixes.put(XMLUtilsConstants.NO_PREFIX_NS, attr.getValue());
                else
                    mapPrefixes.put(attr.getNodeName().replace(XMLUtilsConstants.XMLNS + ":", ""), attr.getValue());
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
        return n.getLength() == 0 ?
                Collections.emptyList() : new NodeListWrapper(n);
    }


    /**
     * Try to parse a string value to the Java type based on Schema type.
     *
     * @param fieldType the schema type
     * @param value     the string value
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
     * This pattern creates a UNION type, with two sub-types. This method extracts the non-null type ("real type").
     *
     * @param schema the schema node wich can be a UNION
     * @return the non-null type
     */
    public static Schema extractRealType(Schema schema) {
        return schema.getType() != Schema.Type.UNION ?
                schema :
                schema.getTypes().stream()
                        .filter(s -> s.getType() != Schema.Type.NULL)
                        .findFirst().get();
    }


    /**
     * Custom class allowing the conversion of NodeList to iterable List of Node
     */
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
