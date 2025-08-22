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
package com.michelin.avroxmlmapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.michelin.avro.AltListItem;
import com.michelin.avro.EmbeddedRecord;
import com.michelin.avro.SubXMLTestModel;
import com.michelin.avro.SubXMLTestModelMultipleXpath;
import com.michelin.avro.TestModelEmptyNamespace;
import com.michelin.avro.TestModelParentRecord;
import com.michelin.avro.TestModelXMLDefaultXpath;
import com.michelin.avro.TestModelXMLMultipleXpath;
import com.michelin.avroxmlmapper.exception.AvroXmlMapperException;
import com.michelin.avroxmlmapper.mapper.AvroXmlMapper;
import com.michelin.avroxmlmapper.utility.GenericUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class AvroXmlMapperTest {
    @Test
    void shouldConvertXmlStringToAvro() throws Exception {
        String input = IOUtils.toString(
                Objects.requireNonNull(AvroXmlMapperTest.class.getResourceAsStream("/xmlDefaultXpath.xml")),
                StandardCharsets.UTF_8);

        TestModelXMLDefaultXpath result = AvroXmlMapper.convertXmlStringToAvro(input, TestModelXMLDefaultXpath.class);

        assertEquals(buildDefaultXpathTestModel(), result);
    }

    @Test
    void shouldConvertXmlStringToAvroWithCustomXpathSelector() throws Exception {
        String input = IOUtils.toString(
                Objects.requireNonNull(AvroXmlMapperTest.class.getResourceAsStream("/xmlXpathCustom1.xml")),
                StandardCharsets.UTF_8);

        TestModelXMLMultipleXpath result =
                AvroXmlMapper.convertXmlStringToAvro(input, TestModelXMLMultipleXpath.class, "customXpath1");

        assertEquals(buildMultiXpathTestModel(), result);
    }

    @Test
    void shouldConvertXmlStringToAvroWithCustomXpathSelectorAndCustomXmlNamespacesSelector() throws Exception {
        String input = IOUtils.toString(
                Objects.requireNonNull(
                        AvroXmlMapperTest.class.getResourceAsStream("/xmlXpathCustom2AndCustomXmlNamespaces.xml")),
                StandardCharsets.UTF_8);

        TestModelXMLMultipleXpath result = AvroXmlMapper.convertXmlStringToAvro(
                input, TestModelXMLMultipleXpath.class, "customXpath2", "xmlNamespacesCustom2");

        assertEquals(buildMultiXpathTestModel2(), result);
    }

    @Test
    void shouldConvertAvroToXml() throws Exception {
        TestModelXMLDefaultXpath expectedModel = buildDefaultXpathTestModel();
        String xmlResult = AvroXmlMapper.convertAvroToXmlString(expectedModel);
        String expected = IOUtils.toString(
                        Objects.requireNonNull(AvroXmlMapperTest.class.getResourceAsStream("/xmlDefaultXpath.xml")),
                        StandardCharsets.UTF_8)
                .replaceAll("[\r\n]+", "")
                .replaceAll("(?m)^[ \\t]*", "")
                .replaceAll("(?m)[ \\t]*$", "")
                .replaceAll(">\\s+<", "><")
                .trim();

        assertEquals(expected, xmlResult);
    }

    @Test
    void shouldConvertAvroToXmlWithCustomXpathSelector() throws Exception {
        TestModelXMLMultipleXpath expectedModel = buildMultiXpathTestModel();

        String xmlResult = AvroXmlMapper.convertAvroToXmlString(expectedModel, "customXpath1");
        String expected = IOUtils.toString(
                        Objects.requireNonNull(AvroXmlMapperTest.class.getResourceAsStream("/xmlXpathCustom1.xml")),
                        StandardCharsets.UTF_8)
                .replaceAll("[\r\n]+", "")
                .replaceAll("(?m)^[ \\t]*", "")
                .replaceAll("(?m)[ \\t]*$", "")
                .replaceAll(">\\s+<", "><")
                .replaceAll("\\s+", " ")
                .trim();

        assertEquals(expected, xmlResult);
    }

    @Test
    void shouldConvertAvroToXmlWithCustomXpathSelectorAndCustomXmlNamespacesSelector() throws Exception {
        TestModelXMLMultipleXpath expectedModel = buildMultiXpathTestModel2();

        String xmlResult = AvroXmlMapper.convertAvroToXmlString(expectedModel, "customXpath2", "xmlNamespacesCustom2");
        String expected = IOUtils.toString(
                        Objects.requireNonNull(AvroXmlMapperTest.class.getResourceAsStream(
                                "/xmlXpathCustom2AndCustomXmlNamespaces.xml")),
                        StandardCharsets.UTF_8)
                .replaceAll("[\r\n]+", "")
                .replaceAll("(?m)^[ \\t]*", "")
                .replaceAll("(?m)[ \\t]*$", "")
                .replaceAll(">\\s+<", "><")
                .replaceAll("\\s+", " ")
                .trim();

        assertEquals(expected, xmlResult);
    }

    @Test
    void shouldConvertAvroToXmlDocument() throws Exception {
        TestModelXMLDefaultXpath inputModel = buildDefaultXpathTestModel();

        Document xmlResult = AvroXmlMapper.convertAvroToXmlDocument(inputModel);
        String expectedStringUncleaned = IOUtils.toString(
                Objects.requireNonNull(AvroXmlMapperTest.class.getResourceAsStream("/xmlDefaultXpath.xml")),
                StandardCharsets.UTF_8);
        Document expectedDocument =
                XMLUnit.getWhitespaceStrippedDocument(XMLUnit.buildControlDocument(expectedStringUncleaned));

        assertEquals(GenericUtils.documentToString(expectedDocument), GenericUtils.documentToString(xmlResult));
    }

    @Test
    void shouldConvertAvroToXmlDocumentWithCustomXpathSelector() throws Exception {
        TestModelXMLMultipleXpath inputModel = buildMultiXpathTestModel();

        Document xmlResult = AvroXmlMapper.convertAvroToXmlDocument(inputModel, "customXpath1");
        String expectedStringUncleaned = IOUtils.toString(
                Objects.requireNonNull(AvroXmlMapperTest.class.getResourceAsStream("/xmlXpathCustom1.xml")),
                StandardCharsets.UTF_8);
        Document expectedDocument =
                XMLUnit.getWhitespaceStrippedDocument(XMLUnit.buildControlDocument(expectedStringUncleaned));

        assertEquals(GenericUtils.documentToString(expectedDocument), GenericUtils.documentToString(xmlResult));
    }

    @Test
    void shouldConvertAvroToXmlDocumentWithCustomXpathSelectorAndCustomXmlNamespacesSelector() throws Exception {
        TestModelXMLMultipleXpath inputModel = buildMultiXpathTestModel2();

        Document xmlResult = AvroXmlMapper.convertAvroToXmlDocument(inputModel, "customXpath2", "xmlNamespacesCustom2");

        String expectedStringUncleaned = IOUtils.toString(
                Objects.requireNonNull(
                        AvroXmlMapperTest.class.getResourceAsStream("/xmlXpathCustom2AndCustomXmlNamespaces.xml")),
                StandardCharsets.UTF_8);
        Document expectedDocument =
                XMLUnit.getWhitespaceStrippedDocument(XMLUnit.buildControlDocument(expectedStringUncleaned));

        assertEquals(GenericUtils.documentToString(expectedDocument), GenericUtils.documentToString(xmlResult));
    }

    @Test
    void shouldThrowExceptionWhenConvertingFaultyNamespaceXmlToAvro() throws Exception {
        String input = IOUtils.toString(
                Objects.requireNonNull(AvroXmlMapperTest.class.getResourceAsStream("/xmlFaultyNamespace.xml")),
                StandardCharsets.UTF_8);

        AvroXmlMapperException e = assertThrows(
                AvroXmlMapperException.class,
                () -> AvroXmlMapper.convertXmlStringToAvro(input, TestModelXMLDefaultXpath.class));

        assertEquals("Failed to parse XML", e.getMessage());
        assertEquals(
                "The default namespace uri provided in the avsc schema (\"http://namespace.uri/default\") is not defined in the XML document. Either fix your avsc schema to match the default namespace defined in the xml, or make sure that the xml document you are converting is not faulty.",
                e.getCause().getMessage());
    }

    @Test
    void shouldConvertEmptyDefaultNamespaceXmlToAvro() throws Exception {
        String input = IOUtils.toString(
                Objects.requireNonNull(AvroXmlMapperTest.class.getResourceAsStream("/xmlWithoutDefaultNamespace.xml")),
                StandardCharsets.UTF_8);
        TestModelEmptyNamespace result = AvroXmlMapper.convertXmlStringToAvro(
                input, TestModelEmptyNamespace.class, "specificXpath", "specificXmlNamespaces");

        assertEquals(
                TestModelEmptyNamespace.newBuilder()
                        .setStringField("Hello")
                        .setThirdStringField("World")
                        .build(),
                result);
    }

    @Test
    void shouldConvertEmptyNamespaceXmlToAvro() throws Exception {
        String input = IOUtils.toString(
                Objects.requireNonNull(AvroXmlMapperTest.class.getResourceAsStream("/xmlWithoutNamespace.xml")),
                StandardCharsets.UTF_8);
        TestModelEmptyNamespace result = AvroXmlMapper.convertXmlStringToAvro(input, TestModelEmptyNamespace.class);

        assertEquals(
                TestModelEmptyNamespace.newBuilder()
                        .setStringField("Hello")
                        .setOtherStringField("Hello")
                        .setThirdStringField("World")
                        .build(),
                result);
    }

    @Test
    void shouldConvertEmbeddedRecordXMLToAvro() throws Exception {
        TestModelParentRecord expectedModel = TestModelParentRecord.newBuilder()
                .setEmbeddedRecord(EmbeddedRecord.newBuilder()
                        .setStringField("Hello")
                        .setOtherStringField("World")
                        .build())
                .build();

        String input = IOUtils.toString(
                Objects.requireNonNull(AvroXmlMapperTest.class.getResourceAsStream("/xmlWithEmbeddedRecord.xml")),
                StandardCharsets.UTF_8);
        TestModelParentRecord result = AvroXmlMapper.convertXmlStringToAvro(input, TestModelParentRecord.class);

        assertEquals(expectedModel, result);
    }

    @Test
    void shouldConvertEmbeddedRecordAvroToXML() throws Exception {
        TestModelParentRecord inputModel = TestModelParentRecord.newBuilder()
                .setEmbeddedRecord(EmbeddedRecord.newBuilder()
                        .setStringField("Hello")
                        .setOtherStringField("World")
                        .setThirdStringField("toto")
                        .build())
                .build();

        String expectedString = IOUtils.toString(
                Objects.requireNonNull(AvroXmlMapperTest.class.getResourceAsStream("/xmlWithEmbeddedRecord.xml")),
                StandardCharsets.UTF_8);
        Document result = AvroXmlMapper.convertAvroToXmlDocument(inputModel);
        Document expectedDocument = XMLUnit.getWhitespaceStrippedDocument(XMLUnit.buildControlDocument(expectedString));
        assertEquals(GenericUtils.documentToString(expectedDocument), GenericUtils.documentToString(result));
    }

    private TestModelXMLDefaultXpath buildDefaultXpathTestModel() {
        Map<String, String> mapResult = new HashMap<>();
        mapResult.put("key1", "value1");
        mapResult.put("key2", "value2");
        mapResult.put("key3", "value3");

        return TestModelXMLDefaultXpath.newBuilder()
                .setBooleanField(true)
                .setDateField(Instant.ofEpochMilli(1766620800000L))
                .setQuantityField(BigDecimal.valueOf(52L).setScale(4, RoundingMode.HALF_UP))
                .setStringField("lorem ipsum")
                .setStringMapScenario1(mapResult)
                .setStringMapScenario2(mapResult)
                .setStringList(List.of("item1", "item2", "item3"))
                .setAltList(List.of(
                        AltListItem.newBuilder()
                                .setListItemAttribute("attrToto")
                                .setListItemContent("toto")
                                .build(),
                        AltListItem.newBuilder()
                                .setListItemAttribute("attrTutu")
                                .setListItemContent("tutu")
                                .build()))
                .setRecordListWithDefault(List.of(
                        SubXMLTestModel.newBuilder()
                                .setSubStringField("item1")
                                .setSubIntField(1)
                                .setSubStringFieldFromAttribute("attribute1")
                                .build(),
                        SubXMLTestModel.newBuilder()
                                .setSubStringField("item2")
                                .setSubIntField(2)
                                .setSubStringFieldFromAttribute("attribute2")
                                .build(),
                        SubXMLTestModel.newBuilder()
                                .setSubStringField("item3")
                                .setSubIntField(3)
                                .setSubStringFieldFromAttribute("attribute3")
                                .build()))
                .build();
    }

    private TestModelXMLMultipleXpath buildMultiXpathTestModel() {
        Map<String, String> mapResult = new HashMap<>();
        mapResult.put("key1", "value1");
        mapResult.put("key2", "value2");
        mapResult.put("key3", "value3");

        return TestModelXMLMultipleXpath.newBuilder()
                .setBooleanField(true)
                .setDateField(Instant.ofEpochMilli(1766620800000L))
                .setQuantityField(BigDecimal.valueOf(52L).setScale(4, RoundingMode.HALF_UP))
                .setStringField("lorem ipsum")
                .setStringMapScenario1(mapResult)
                .setStringMapScenario2(mapResult)
                .setStringList(List.of("item1", "item2", "item3"))
                .setRecordListWithDefault(List.of(
                        SubXMLTestModelMultipleXpath.newBuilder()
                                .setSubStringField("item1")
                                .setSubIntField(1)
                                .setSubStringFieldFromAttribute("attribute1")
                                .build(),
                        SubXMLTestModelMultipleXpath.newBuilder()
                                .setSubStringField("item2")
                                .setSubIntField(2)
                                .setSubStringFieldFromAttribute("attribute2")
                                .build(),
                        SubXMLTestModelMultipleXpath.newBuilder()
                                .setSubStringField("item3")
                                .setSubIntField(3)
                                .setSubStringFieldFromAttribute("attribute3")
                                .build()))
                .build();
    }

    private TestModelXMLMultipleXpath buildMultiXpathTestModel2() {
        Map<String, String> mapResult = new HashMap<>();
        mapResult.put("key1", "value1");
        mapResult.put("key2", "value2");
        mapResult.put("key3", "value3");

        return TestModelXMLMultipleXpath.newBuilder()
                .setDateField(Instant.ofEpochMilli(1766620800000L))
                .setStringField("lorem ipsum")
                .setStringMapScenario1(mapResult)
                .setStringMapScenario2(mapResult)
                .setStringList(List.of("item1", "item2", "item3"))
                .setRecordListWithDefault(List.of(
                        SubXMLTestModelMultipleXpath.newBuilder()
                                .setSubStringField("item1")
                                .setSubIntField(1)
                                .setSubStringFieldFromAttribute("attribute1")
                                .build(),
                        SubXMLTestModelMultipleXpath.newBuilder()
                                .setSubStringField("item2")
                                .setSubIntField(2)
                                .setSubStringFieldFromAttribute("attribute2")
                                .build(),
                        SubXMLTestModelMultipleXpath.newBuilder()
                                .setSubStringField("item3")
                                .setSubIntField(3)
                                .setSubStringFieldFromAttribute("attribute3")
                                .build()))
                .build();
    }
}
