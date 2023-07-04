package com.michelin.avroxmlmapper;

import org.apache.avro.Schema;
import org.w3c.dom.*;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
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
     * Handle exception Document xPath evaluation
     *
     * @param document        the source xml Document
     * @param xPathExpression the xPathExpression to match
     * @return the list of matyched nodes
     */
    public static NodeList xPathNodeListEvaluation(Document document, String xPathExpression) {
        NodeList result = null;
        try {
            XPath xPath = getXpath();
            xPath.setNamespaceContext(getNamespaceContext(document));
            result = (NodeList) getXpath().compile(xPathExpression).evaluate(document, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new XmlUtilsException("Failed to execute xpath " + xPathExpression, e);
        }

        return result;
    }

    public static NodeList xPathNodeListEvaluation(Node node, String xPathExpression, NamespaceContext namespaceContext) {
        return xPathNodeListEvaluation(node, node, xPathExpression, namespaceContext);
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
            throw new XmlUtilsException("Failed to execute xpath " + xPathExpression, e);
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
     * Handle exception for Document xPath evaluation
     *
     * @param document        the source xml Document
     * @param xPathExpression the xPathExpression to match
     * @return the list of matyched nodes
     */
    public static String xPathStringEvaluation(Document document, String xPathExpression) {
        return xPathStringEvaluation(document, xPathExpression, getNamespaceContext(document));
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
            throw new XmlUtilsException("Failed to execute xpath " + xPathExpression, e);
        }
        stringParseCount++;

        return result != null && !result.isBlank() ? result : null;
    }


    public static String xPathStringEvaluation(Node node, String xPathExpression, NamespaceContext namespaceContext) {
        return xPathStringEvaluation(node, node, xPathExpression, namespaceContext);
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
    protected static Object parseValue(Schema.Type fieldType, String value) {
        Object result;
        try {
            switch (fieldType) {
                case STRING:
                    result = value;
                    break;
                case INT:
                    result = Integer.valueOf(value);
                    break;
                case LONG:
                    result = Long.valueOf(value);
                    break;
                case FLOAT:
                    result = Float.valueOf(value);
                    break;
                case DOUBLE:
                    result = Double.valueOf(value);
                    break;
                case BOOLEAN:
                    result = Boolean.valueOf(value);
                    break;
                default:
                    result = null;
            }
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
