package com.michelin.avroxmlmapper.mapper;

import com.michelin.avroxmlmapper.constants.XMLUtilsConstants;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.TransformerException;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import static com.michelin.avroxmlmapper.mapper.AvroToXMLUtils.createDocumentfromAvro;
import static com.michelin.avroxmlmapper.utility.GenericUtils.*;


/**
 * Utility Class for XML parsing (Xpath)
 */
public final class AvroXmlMapper {

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
    public static <T extends SpecificRecordBase> T convertXMLStringToAvro(String stringDocument, Class<T> clazz) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Schema schema = (Schema) (clazz.getDeclaredMethod("getClassSchema").invoke(null));
        var document = stringToDocument(stringDocument, xmlNamespaces(schema));
        return (T) XMLToAvroUtils.convert(document.getDocumentElement(), document.getDocumentElement(), clazz, getNamespaceContext(document), schema.getNamespace(), XMLUtilsConstants.XPATH_DEFAULT);
    }

    /**
     * converts a string input containing XML into the corresponding Avro class, using the xpathSelector given for mapping.
     *
     * @param stringDocument
     * @param clazz
     * @param xpathSelector
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static <T extends SpecificRecordBase> T convertXMLStringToAvro(String stringDocument, Class<T> clazz, String xpathSelector) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Schema schema = (Schema) (clazz.getDeclaredMethod("getClassSchema").invoke(null));
        var document = stringToDocument(stringDocument, xmlNamespaces(schema));
        return (T) XMLToAvroUtils.convert(document.getDocumentElement(), document.getDocumentElement(), clazz, getNamespaceContext(document), schema.getNamespace(), xpathSelector);
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
    public static <T extends SpecificRecordBase> T convertXMLDocumentToAvro(Node node, Class<T> clazz, NamespaceContext namespaceContext) {
        return (T) XMLToAvroUtils.convert(node, node.cloneNode(true), clazz, namespaceContext, "io.michelin.choreography.avro", "xpath");
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
    private static <T extends SpecificRecordBase> T convertXMLDocumentToAvro(Node fullNode, Class<? extends SpecificRecordBase> clazz, NamespaceContext namespaceContext, String baseNamespace) {
        return (T) XMLToAvroUtils.convert(fullNode, fullNode, clazz, namespaceContext, baseNamespace, "xpath");
    }



    /* *************************************************** */
    /* Build an XML document in String format from an Avro */
    /* *************************************************** */

    public static String convertAvroToXMLString(SpecificRecordBase record) throws TransformerException {
        return documentToString(createDocumentfromAvro(record, XMLUtilsConstants.XPATH_DEFAULT, Optional.of(XMLUtilsConstants.XML_NAMESPACE_SELECTOR_DEFAULT)));
    }

    public static String convertAvroToXMLString(SpecificRecordBase record, String xpathSelector) throws TransformerException {
        return documentToString(createDocumentfromAvro(record, xpathSelector, Optional.of(XMLUtilsConstants.XML_NAMESPACE_SELECTOR_DEFAULT)));
    }


    /* *************************************************** */
    /* Build an XML document in String format from an Avro */
    /* *************************************************** */

    /**
     * Create a Document from a SpecificRecordBase, using xpath property (Avro model) to build the XML structure.
     *
     * @param record the global SpecificRecordBase containing the entire data to parse in XML
     * @return the document produced
     */
    private static Document convertAvroToXMLDocument(SpecificRecordBase record) {
        return createDocumentfromAvro(record, XMLUtilsConstants.XPATH_DEFAULT, Optional.of(XMLUtilsConstants.XML_NAMESPACE_SELECTOR_DEFAULT));
    }

    /**
     * Create a Document from a SpecificRecordBase, using xpath property (Avro model) to build the XML structure.
     *
     * @param record        the global SpecificRecordBase containing the entire data to parse in XML
     * @param xpathSelector Name of the variable defining the xpath of the avsc file that needs to be used
     * @return the document produced
     */
    private static Document convertAvroToXMLDocument(SpecificRecordBase record, String xpathSelector) {
        return createDocumentfromAvro(record, xpathSelector, Optional.of(XMLUtilsConstants.XML_NAMESPACE_SELECTOR_DEFAULT));
    }
}
