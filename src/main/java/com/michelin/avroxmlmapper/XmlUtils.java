package com.michelin.avroxmlmapper;

import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * Utility Class for XML parsing (Xpath)
 */
public final class XmlUtils {

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
            throw new XmlUtilsException("Failed to parse XML", e);
        }
    }

    /* *************************************************** */
    /* Build an Avro from an XML document in string Format */
    /* *************************************************** */

    /**
     * @param stringDocument
     * @param clazz
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static <T extends SpecificRecordBase> T convertStringDocumentToAvro(String stringDocument, Class<T> clazz) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Schema schema = (Schema) (clazz.getDeclaredMethod("getClassSchema").invoke(null));
        var document = stringToDocument(stringDocument, xmlNamespaces(schema));
        return (T)convert(document.getDocumentElement(), clazz, getNamespaceContext(document), schema.getNamespace());
    }

    /**
     * converts a string input containing XML into the corresponding Avro class, using th4e xpathSelector given for mapping.
     *
     * @param stringDocument
     * @param clazz
     * @param xpathSelector
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static SpecificRecordBase convertStringDocumentToAvro(String stringDocument, Class<? extends SpecificRecordBase> clazz, String xpathSelector) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Schema schema = (Schema) (clazz.getDeclaredMethod("getClassSchema").invoke(null));
        var document = stringToDocument(stringDocument, xmlNamespaces(schema));
        return convert(document.getDocumentElement(), clazz, getNamespaceContext(document), schema.getNamespace(), xpathSelector);
    }

    /* ********************************** */
    /* Build an Avro from an XML document */
    /* ********************************** */

    /**
     * Converts, recursively, the content of an XML-node into SpecificRecord (avro).
     *
     * @param node             XML-node to convert
     * @param clazz            class of the SpecificRecord to generate
     * @param namespaceContext the namespace context
     * @return SpecificRecord generated
     */
    public static SpecificRecordBase convert(Node node, Class<? extends SpecificRecordBase> clazz, NamespaceContext namespaceContext) {

        return XMLToAvroUtils.convert(node, node.cloneNode(true), clazz, namespaceContext, "io.michelin.choreography.avro", "xpath");
    }

    /**
     * Converts, recursively, the content of an XML-node into SpecificRecord (avro).
     *
     * @param fullNode         XML-node to convert
     * @param clazz            class of the SpecificRecord to generate
     * @param namespaceContext the namespace context
     * @param baseNamespace    base namespace for the generated SpecificRecord classes
     * @return SpecificRecord generated
     */
    public static SpecificRecordBase convert(Node fullNode, Class<? extends SpecificRecordBase> clazz, NamespaceContext namespaceContext, String baseNamespace) {
        return XMLToAvroUtils.convert(fullNode, fullNode, clazz, namespaceContext, baseNamespace, "xpath");
    }

    /**
     * Converts, recursively, the content of an XML-node into SpecificRecord (avro).
     *
     * @param fullNode         XML-node to convert
     * @param clazz            class of the SpecificRecord to generate
     * @param namespaceContext the namespace context
     * @param baseNamespace    base namespace for the generated SpecificRecord classes
     * @param xpathSelector    the xpath to get
     * @return SpecificRecord generated
     */
    public static SpecificRecordBase convert(Node fullNode, Class<? extends SpecificRecordBase> clazz, NamespaceContext namespaceContext, String baseNamespace, String xpathSelector) {
        return XMLToAvroUtils.convert(fullNode, fullNode, clazz, namespaceContext, baseNamespace, xpathSelector);
    }

    /* ********************************** */
    /* Build an XML document from an Avro */
    /* ********************************** */

    /**
     * Create a Document from a SpecificRecordBase, using xpath property (Avro model) to build the XML structure.
     *
     * @param record the global SpecificRecordBase containing the entire data to parse in XML
     * @return the document produced
     */
    public static Document createDocumentfromAvro(SpecificRecordBase record) {
        return createDocumentfromAvro(record, XMLUtilsConstants.XPATH_DEFAULT, Optional.of(XMLUtilsConstants.XML_NAMESPACE_SELECTOR_DEFAULT));
    }

    /**
     * Create a Document from a SpecificRecordBase, using xpath property (Avro model) to build the XML structure.
     *
     * @param record        the global SpecificRecordBase containing the entire data to parse in XML
     * @param xpathSelector Name of the variable defining the xpath of the avsc file that needs to be used
     * @return the document produced
     */
    public static Document createDocumentfromAvro(SpecificRecordBase record, String xpathSelector) {
        return createDocumentfromAvro(record, xpathSelector, Optional.of(XMLUtilsConstants.XML_NAMESPACE_SELECTOR_DEFAULT));
    }


    /**
     * Create a Document from a SpecificRecordBase, using xpath property (Avro model) to build the XML structure.
     *
     * @param record            the global SpecificRecordBase containing the entire data to parse in XML
     * @param xpathSelector     Name of the variable defining the xpath of the avsc file that needs to be used
     * @param namespaceSelector Name of the variable defining xml namespaces of avsc file corresponding to record
     * @return the document produced
     */
    public static Document createDocumentfromAvro(SpecificRecordBase record, String xpathSelector, Optional<String> namespaceSelector) {
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.newDocument();
            Map<String, String> mapNamespaces;
            if (namespaceSelector.isPresent()) {
                mapNamespaces = xmlNamespaces(record.getSchema(), namespaceSelector.get());
                mapNamespaces.put("", mapNamespaces.get("null"));
                mapNamespaces.remove("null");
            } else {
                mapNamespaces = Collections.emptyMap();
            }
            String rootElementName = record.getSchema().getProp(xpathSelector).substring(1);// the first character, for the xpath of rootElement,// is '/'
            var rootElement = document.createElementNS(mapNamespaces.get(AvroToXMLUtils.getPrefix(rootElementName)), rootElementName);
            mapNamespaces.forEach((k, v) -> rootElement.setAttribute(k.isEmpty() ? XMLUtilsConstants.XMLNS : XMLUtilsConstants.XMLNS + ":" + k, v));
            AvroToXMLUtils.buildChildNodes(record, document, mapNamespaces, xpathSelector).forEach(n -> {
                if (n.getNodeType() == Node.ATTRIBUTE_NODE) rootElement.setAttributeNode((Attr) n);
                else rootElement.appendChild(n);
            });

            document.appendChild(rootElement);

        } catch (Exception e) {
            throw new XmlUtilsException("Failed to create document from avro", e);
        }

        return document;
    }


    /* *************** */
    /* Other utilities */
    /* *************** */

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
     * @param document
     * @return
     * @deprecated
     */
    @Deprecated
    public static NamespaceContext getNamespaceContext(Document document) {
        return GenericUtils.getNamespaceContext(document);
    }

    /**
     * Get the XML namespaces from a given XML schema
     *
     * @param doc the document parsed
     * @param tagName xml tag name to searched
     * @param attributeName xml attribute to be found form xml
     * @return String Value of the attribute
     */
    public static String getAttributeFromXML(Document doc, String tagName, String attributeName) {
        NodeList listOfTagElements = doc.getElementsByTagNameNS("*", tagName);
        if (listOfTagElements.getLength() == 0) {
            return null;
        }
        String result = null;
        for (int i = 0; i < listOfTagElements.getLength(); i++) {
            if (listOfTagElements.item(i).getAttributes().item(0).getTextContent().trim().equals(attributeName)) {
                if (listOfTagElements.item(i).getTextContent() != null) {
                    result = listOfTagElements.item(i).getTextContent().trim();
                    break;
                }
            }
        }
        return result;
    }
}
