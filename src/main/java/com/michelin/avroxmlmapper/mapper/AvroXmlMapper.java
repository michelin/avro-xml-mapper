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

import static com.michelin.avroxmlmapper.constants.AvroXmlMapperConstants.XML_NAMESPACE_SELECTOR_DEFAULT;
import static com.michelin.avroxmlmapper.constants.AvroXmlMapperConstants.XPATH_DEFAULT;
import static com.michelin.avroxmlmapper.mapper.AvroToXmlUtils.createDocumentfromAvro;
import static com.michelin.avroxmlmapper.utility.GenericUtils.*;

import java.lang.reflect.InvocationTargetException;
import javax.xml.transform.TransformerException;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.w3c.dom.Document;

/** Utility Class for XML parsing (Xpath) */
public final class AvroXmlMapper {

    /* *************************************************** */
    /* Build an Avro from an XML document in string Format */
    /* *************************************************** */

    /**
     * Converts an XML string into a SpecificRecordBase object. The mapping is based on the "xpath" property defined for
     * each of the fields in the original avsc file.
     *
     * <p>See README.md for more details.
     *
     * @param stringDocument The XML string to convert
     * @param clazz The Avro object to convert to
     * @param <T> The type of the Avro object
     * @return The SpecificRecordBase object.
     * @throws NoSuchMethodException If the method getClassSchema is not found
     * @throws InvocationTargetException If the method getClassSchema cannot be invoked
     * @throws IllegalAccessException If the method getClassSchema cannot be accessed
     */
    public static <T extends SpecificRecordBase> T convertXmlStringToAvro(String stringDocument, Class<T> clazz)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Schema schema = (Schema) (clazz.getDeclaredMethod("getClassSchema").invoke(null));
        var document = stringToDocument(stringDocument, xmlNamespaces(schema));
        return XmlToAvroUtils.convert(
                document.getDocumentElement(),
                document.getDocumentElement(),
                clazz,
                getNamespaceContext(document),
                schema.getNamespace(),
                XPATH_DEFAULT);
    }

    /**
     * Converts an XML string into a SpecificRecordBase object. The mapping is based on the chosen xpathSelector
     * property defined for each of the fields in the original avsc file. See README.md for more details.
     *
     * @param stringDocument The XML string to convert
     * @param clazz The Avro object to convert to
     * @param xpathSelector The xpathSelector property used to search for the xpathMapping in the Avro definition
     * @param <T> The type of the Avro object
     * @return the SpecificRecordBase object.
     * @throws NoSuchMethodException If the method getClassSchema is not found
     * @throws InvocationTargetException If the method getClassSchema cannot be invoked
     * @throws IllegalAccessException If the method getClassSchema cannot be accessed
     */
    public static <T extends SpecificRecordBase> T convertXmlStringToAvro(
            String stringDocument, Class<T> clazz, String xpathSelector)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Schema schema = (Schema) (clazz.getDeclaredMethod("getClassSchema").invoke(null));
        var document = stringToDocument(stringDocument, xmlNamespaces(schema));
        return XmlToAvroUtils.convert(
                document.getDocumentElement(),
                document.getDocumentElement(),
                clazz,
                getNamespaceContext(document),
                schema.getNamespace(),
                xpathSelector);
    }

    /**
     * Converts an XML string into a SpecificRecordBase object. The mapping is based on the chosen xpathSelector
     * property defined for each of the fields in the original avsc file. See README.md for more details.
     *
     * @param stringDocument The XML string to convert
     * @param clazz The Avro object to convert to
     * @param xpathSelector The xpathSelector property used to search for the xpathMapping in the Avro definition
     * @param xmlNamespacesSelector Name of the variable defining the xmlNamespaces of the avsc file that needs to be
     *     used for unifying namespace definitions
     * @param <T> The type of the Avro object
     * @return the SpecificRecordBase object.
     * @throws NoSuchMethodException If the method getClassSchema is not found
     * @throws InvocationTargetException If the method getClassSchema cannot be invoked
     * @throws IllegalAccessException If the method getClassSchema cannot be accessed
     */
    public static <T extends SpecificRecordBase> T convertXmlStringToAvro(
            String stringDocument, Class<T> clazz, String xpathSelector, String xmlNamespacesSelector)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Schema schema = (Schema) (clazz.getDeclaredMethod("getClassSchema").invoke(null));
        var document = stringToDocument(stringDocument, xmlNamespaces(schema, xmlNamespacesSelector));
        return XmlToAvroUtils.convert(
                document.getDocumentElement(),
                document.getDocumentElement(),
                clazz,
                getNamespaceContext(document),
                schema.getNamespace(),
                xpathSelector);
    }

    /* *************************************************** */
    /* Build an XML document in String format from an Avro */
    /* *************************************************** */

    /**
     * Create an XML in String format from a SpecificRecordBase, using default "xpath" and "xmlNamespaces" properties
     * defined in the Avro model to build the XML structure.
     *
     * @param record The SpecificRecordBase containing the entire data to parse in XML
     * @return The XML in String format
     * @throws TransformerException If the transformation fails
     */
    public static String convertAvroToXmlString(SpecificRecordBase record) throws TransformerException {
        return documentToString(createDocumentfromAvro(record, XPATH_DEFAULT, XML_NAMESPACE_SELECTOR_DEFAULT));
    }

    /**
     * Create an XML in String format from a SpecificRecordBase, using the provided xpathSelector and default
     * "xmlNamespaces" properties defined in the Avro model to build the XML structure.
     *
     * @param record The SpecificRecordBase containing the entire data to parse in XML
     * @param xpathSelector Name of the variable defining the xpath of the avsc file that needs to be used
     * @return The XML in String format
     * @throws TransformerException If the transformation fails
     */
    public static String convertAvroToXmlString(SpecificRecordBase record, String xpathSelector)
            throws TransformerException {
        return documentToString(createDocumentfromAvro(record, xpathSelector, XML_NAMESPACE_SELECTOR_DEFAULT));
    }

    /**
     * Create an XML in String format from a SpecificRecordBase, using the provided xpathSelector and
     * xmlNamespacesSelector properties defined in the Avro model to build the XML structure.
     *
     * @param record The SpecificRecordBase containing the entire data to parse in XML
     * @param xpathSelector Name of the variable defining the xpath of the avsc file that needs to be used
     * @param xmlNamespacesSelector Name of the variable defining the xmlNamespaces of the avsc file that needs to be
     *     used
     * @return The XML in String format
     * @throws TransformerException If the transformation fails
     */
    public static String convertAvroToXmlString(
            SpecificRecordBase record, String xpathSelector, String xmlNamespacesSelector) throws TransformerException {
        return documentToString(createDocumentfromAvro(record, xpathSelector, xmlNamespacesSelector));
    }

    /* ********************************** */
    /* Build an XML document from an Avro */
    /* ********************************** */

    /**
     * Create a Document from a SpecificRecordBase, using default "xpath" and "xmlNamespaces" properties defined in the
     * Avro model to build the XML structure.
     *
     * @param record The global SpecificRecordBase containing the entire data to parse in XML
     * @return The document produced
     */
    public static Document convertAvroToXmlDocument(SpecificRecordBase record) {
        return createDocumentfromAvro(record, XPATH_DEFAULT, XML_NAMESPACE_SELECTOR_DEFAULT);
    }

    /**
     * Create a Document from a SpecificRecordBase, using xpath property (Avro model) to build the XML structure.
     *
     * @param record The SpecificRecordBase containing the entire data to parse in XML
     * @param xpathSelector Name of the variable defining the xpath of the avsc file that needs to be used
     * @return The document produced
     */
    public static Document convertAvroToXmlDocument(SpecificRecordBase record, String xpathSelector) {
        return createDocumentfromAvro(record, xpathSelector, XML_NAMESPACE_SELECTOR_DEFAULT);
    }

    /**
     * Create a Document from a SpecificRecordBase, using the provided xpathSelector and xmlNamespacesSelector
     * properties defined in the Avro model to build the XML structure.
     *
     * @param record The SpecificRecordBase containing the entire data to parse in XML
     * @param xpathSelector Name of the variable defining the xpath of the avsc file that needs to be used
     * @param xmlNamespaceSelector Name of the variable defining the xmlNamespaces of the avsc file that needs to be
     *     used
     * @return The document produced
     */
    public static Document convertAvroToXmlDocument(
            SpecificRecordBase record, String xpathSelector, String xmlNamespaceSelector) {
        return createDocumentfromAvro(record, xpathSelector, xmlNamespaceSelector);
    }
}
