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
import static com.michelin.avroxmlmapper.mapper.AvroToXmlUtils.createDocumentFromAvro;
import static com.michelin.avroxmlmapper.utility.GenericUtils.*;

import java.lang.reflect.InvocationTargetException;
import javax.xml.transform.TransformerException;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.w3c.dom.Document;

/** Utility class for XML parsing with XPath. */
public final class AvroXmlMapper {
    private static final String GET_CLASS_SCHEMA_METHOD = "getClassSchema";

    /** Private constructor. */
    private AvroXmlMapper() {}

    /**
     * Converts an XML string to a {@link SpecificRecordBase} using each field's {@code xpath} property.
     *
     * <p>See the README for details.
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
        Schema schema =
                (Schema) (clazz.getDeclaredMethod(GET_CLASS_SCHEMA_METHOD).invoke(null));
        Document document = stringToDocument(stringDocument, xmlNamespaces(schema));
        return XmlToAvroUtils.convert(
                document.getDocumentElement(),
                document.getDocumentElement(),
                clazz,
                getNamespaceContext(document),
                schema.getNamespace(),
                XPATH_DEFAULT);
    }

    /**
     * Converts an XML string to a {@link SpecificRecordBase} using the selected XPath property.
     *
     * @param stringDocument The XML string to convert
     * @param clazz The Avro object to convert to
     * @param xpathSelector The XPath selector property used to find mappings in the Avro definition.
     * @param <T> The type of the Avro object
     * @return The SpecificRecordBase object.
     * @throws NoSuchMethodException If the method getClassSchema is not found
     * @throws InvocationTargetException If the method getClassSchema cannot be invoked
     * @throws IllegalAccessException If the method getClassSchema cannot be accessed
     */
    public static <T extends SpecificRecordBase> T convertXmlStringToAvro(
            String stringDocument, Class<T> clazz, String xpathSelector)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Schema schema =
                (Schema) (clazz.getDeclaredMethod(GET_CLASS_SCHEMA_METHOD).invoke(null));
        Document document = stringToDocument(stringDocument, xmlNamespaces(schema));
        return XmlToAvroUtils.convert(
                document.getDocumentElement(),
                document.getDocumentElement(),
                clazz,
                getNamespaceContext(document),
                schema.getNamespace(),
                xpathSelector);
    }

    /**
     * Converts an XML string to a {@link SpecificRecordBase} using the selected XPath and namespace properties.
     *
     * @param stringDocument The XML string to convert
     * @param clazz The Avro object to convert to
     * @param xpathSelector The XPath selector property used to find mappings in the Avro definition.
     * @param xmlNamespacesSelector Name of the variable defining the XML namespaces in the AVSC file for unifying
     *     namespace definitions.
     * @param <T> The type of the Avro object
     * @return The SpecificRecordBase object.
     * @throws NoSuchMethodException If the method getClassSchema is not found
     * @throws InvocationTargetException If the method getClassSchema cannot be invoked
     * @throws IllegalAccessException If the method getClassSchema cannot be accessed
     */
    public static <T extends SpecificRecordBase> T convertXmlStringToAvro(
            String stringDocument, Class<T> clazz, String xpathSelector, String xmlNamespacesSelector)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Schema schema =
                (Schema) (clazz.getDeclaredMethod(GET_CLASS_SCHEMA_METHOD).invoke(null));
        Document document = stringToDocument(stringDocument, xmlNamespaces(schema, xmlNamespacesSelector));
        return XmlToAvroUtils.convert(
                document.getDocumentElement(),
                document.getDocumentElement(),
                clazz,
                getNamespaceContext(document),
                schema.getNamespace(),
                xpathSelector);
    }

    /**
     * Creates an XML string from a {@link SpecificRecordBase} using the default {@code xpath} and {@code xmlNamespaces}
     * properties.
     *
     * @param message The SpecificRecordBase containing the entire data to parse in XML
     * @return The XML in String format
     * @throws TransformerException If the transformation fails
     */
    public static String convertAvroToXmlString(SpecificRecordBase message) throws TransformerException {
        return documentToString(createDocumentFromAvro(message, XPATH_DEFAULT, XML_NAMESPACE_SELECTOR_DEFAULT));
    }

    /**
     * Creates an XML string from a {@link SpecificRecordBase} using the selected XPath and default
     * {@code xmlNamespaces} properties.
     *
     * @param message The SpecificRecordBase containing the entire data to parse in XML
     * @param xpathSelector Name of the variable defining the XPath in the AVSC file.
     * @return The XML in String format
     * @throws TransformerException If the transformation fails
     */
    public static String convertAvroToXmlString(SpecificRecordBase message, String xpathSelector)
            throws TransformerException {
        return documentToString(createDocumentFromAvro(message, xpathSelector, XML_NAMESPACE_SELECTOR_DEFAULT));
    }

    /**
     * Creates an XML string from a {@link SpecificRecordBase} using the selected XPath and namespace properties.
     *
     * @param message The SpecificRecordBase containing the entire data to parse in XML
     * @param xpathSelector Name of the variable defining the XPath in the AVSC file.
     * @param xmlNamespacesSelector Name of the variable defining the XML namespaces in the AVSC file.
     * @return The XML in String format
     * @throws TransformerException If the transformation fails
     */
    public static String convertAvroToXmlString(
            SpecificRecordBase message, String xpathSelector, String xmlNamespacesSelector)
            throws TransformerException {
        return documentToString(createDocumentFromAvro(message, xpathSelector, xmlNamespacesSelector));
    }

    /**
     * Creates an XML document from a {@link SpecificRecordBase} using the default {@code xpath} and
     * {@code xmlNamespaces} properties.
     *
     * @param message The SpecificRecordBase containing the data to parse as XML.
     * @return The document produced
     */
    public static Document convertAvroToXmlDocument(SpecificRecordBase message) {
        return createDocumentFromAvro(message, XPATH_DEFAULT, XML_NAMESPACE_SELECTOR_DEFAULT);
    }

    /**
     * Creates an XML document from a {@link SpecificRecordBase} using the selected XPath property.
     *
     * @param message The SpecificRecordBase containing the entire data to parse in XML
     * @param xpathSelector Name of the variable defining the XPath in the AVSC file.
     * @return The document produced
     */
    public static Document convertAvroToXmlDocument(SpecificRecordBase message, String xpathSelector) {
        return createDocumentFromAvro(message, xpathSelector, XML_NAMESPACE_SELECTOR_DEFAULT);
    }

    /**
     * Creates an XML document from a {@link SpecificRecordBase} using the selected XPath and namespace properties.
     *
     * @param message The SpecificRecordBase containing the entire data to parse in XML
     * @param xpathSelector Name of the variable defining the XPath in the AVSC file.
     * @param xmlNamespaceSelector Name of the variable defining the XML namespaces in the AVSC file.
     * @return The document produced
     */
    public static Document convertAvroToXmlDocument(
            SpecificRecordBase message, String xpathSelector, String xmlNamespaceSelector) {
        return createDocumentFromAvro(message, xpathSelector, xmlNamespaceSelector);
    }
}
