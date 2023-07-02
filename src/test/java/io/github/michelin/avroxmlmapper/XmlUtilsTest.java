//package io.michelin.choreography.utils;
//
//import io.michelin.choreography.avro.test.SubXMLTestModel;
//import io.michelin.choreography.avro.test.SubXMLTestModelMultipleXpath;
//import io.michelin.choreography.avro.test.TestModelXMLDefaultXpath;
//import io.michelin.choreography.avro.test.TestModelXMLMultipleXpath;
//import org.apache.commons.io.IOUtils;
//import org.junit.jupiter.api.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.w3c.dom.Document;
//import org.w3c.dom.Node;
//import org.xml.sax.InputSource;
//import org.xmlunit.builder.DiffBuilder;
//import org.xmlunit.diff.DefaultNodeMatcher;
//import org.xmlunit.diff.Diff;
//import org.xmlunit.diff.ElementSelectors;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.transform.OutputKeys;
//import javax.xml.transform.TransformerFactory;
//import javax.xml.transform.dom.DOMSource;
//import javax.xml.transform.stream.StreamResult;
//import java.io.StringReader;
//import java.io.StringWriter;
//import java.math.BigDecimal;
//import java.nio.charset.StandardCharsets;
//import java.time.Instant;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class XmlUtilsTest {
//
//    private static final Logger log = LoggerFactory.getLogger(XmlUtilsTest.class);
//    public static final String NODE_VALUE = "bunch of monkeys";
//    public static final String NODE_ATTR = "42";
//
//    @Test
//    void xPathStringEvaluationForDoc() throws Exception {
//        var doc = sampleDoc(NODE_ATTR, NODE_VALUE);
//        var xpath = String.format("/bookings/booking[@id='%s']", NODE_ATTR);
//
//        var result = GenericUtils.xPathStringEvaluation(doc, xpath);
//        assertEquals(NODE_VALUE, result);
//    }
//
//
//    @Test
//    void testNamespaceExtraction() throws Exception {
//
//        final String originalShipmentRequest = FileUtils.readFile("/originalShipmentRequest.xml");
//        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        factory.setNamespaceAware(true);
//        DocumentBuilder builder = factory.newDocumentBuilder();
//        InputSource is = new InputSource(new StringReader(originalShipmentRequest));
//        var document = builder.parse(is);
//
//        var mapNamespaces = new HashMap<String, List<String>>();
//        var result = XMLToAvroUtils.extractNamespaces(document.getDocumentElement(), mapNamespaces);
//
//        assertEquals(Map.of("http://www.gic.michelin.com/oagis/9/michelin/1", List.of("ns5"),
//                "http://www.openapplications.org/oagis/9", List.of(XMLUtilsConstants.NO_PREFIX_NS, "imp1")), result);
//    }
//
//    @Test
//    void testUniformizeNamespaces() throws Exception {
//
//        final String originalShipmentRequest = FileUtils.readFile("/originalShipmentRequest.xml");
//        final String targetShipmentRequest = FileUtils.readFile("/simplifiedShipmentRequest.xml");
//
//        String fixedXmlContent = originalShipmentRequest.replaceAll("xmlns=\"\"", "");
//
//        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        factory.setNamespaceAware(true);
//        DocumentBuilder builder = factory.newDocumentBuilder();
//        InputSource is = new InputSource(new StringReader(fixedXmlContent));
//        var document = builder.parse(is);
//
//        XMLToAvroUtils.purgeNamespaces(document.getDocumentElement());
//        XMLToAvroUtils.simplifyNamespaces(document, Map.of(
//                "null", "http://www.openapplications.org/oagis/9",
//                "gic", "http://www.gic.michelin.com/oagis/9/michelin/1"), Map.of(
//                "http://www.gic.michelin.com/oagis/9/michelin/1", List.of("ns5"),
//                "http://www.openapplications.org/oagis/9", List.of(XMLUtilsConstants.NO_PREFIX_NS, "imp1")
//        ));
//
//        Diff diffXml = DiffBuilder.compare(XmlUtils.documentToString(document))
//                .withTest(targetShipmentRequest)
//                .checkForSimilar()
//                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.or(ElementSelectors.byNameAndText)))
//                .ignoreWhitespace()
//                .normalizeWhitespace()
//                .ignoreElementContentWhitespace()
//                .build();
//
//        assertFalse(diffXml.hasDifferences(), diffXml.toString());
//    }
//
//    @Test
//    void xPathStringEvaluationWhenInvalidXpath() throws Exception {
//        var doc = sampleDoc(NODE_ATTR, NODE_VALUE);
//        var xpath = String.format("/bookings/booking[@id='%s']", 100500);
//
//        var result = GenericUtils.xPathStringEvaluation(doc, xpath);
//        assertNull(result);
//    }
//
//    @Test
//    void xPathStringEvaluationWhenNullValue() throws Exception {
//        var doc = sampleDoc(NODE_ATTR, null);
//        var xpath = String.format("/bookings/booking[@id='%s']", NODE_ATTR);
//
//        var result = GenericUtils.xPathStringEvaluation(doc, xpath);
//        assertNull(result);
//    }
//
//    @Test
//    void xPathStringEvaluationWhenEmptyValue() throws Exception {
//        var doc = sampleDoc(NODE_ATTR, "");
//        var xpath = String.format("/bookings/booking[@id='%s']", NODE_ATTR);
//
//        var result = GenericUtils.xPathStringEvaluation(doc, xpath);
//        assertNull(result);
//    }
//
//    @Test
//    void xPathStringEvaluationWhenBlankValue() throws Exception {
//        var doc = sampleDoc(NODE_ATTR, "     ");
//        var xpath = String.format("/bookings/booking[@id='%s']", NODE_ATTR);
//
//        var result = GenericUtils.xPathStringEvaluation(doc, xpath);
//        assertNull(result);
//    }
//
//    @Test
//    void conversionTest() throws Exception {
//        String doc = IOUtils.toString(XmlUtilsTest.class.getResourceAsStream("/testDoc.xml"), StandardCharsets.UTF_8);
//        Map<String, String> xmlNamespaces = new HashMap<>();
//        xmlNamespaces.put(XMLUtilsConstants.NO_PREFIX_NS, "toto1");
//        xmlNamespaces.put("ns1", "toto2");
//        Document testDoc = XmlUtils.stringToDocument(doc, xmlNamespaces);
//
//        ExhaustiveXMLTestModel actualValue = (ExhaustiveXMLTestModel) XmlUtils.convert(testDoc.getDocumentElement(), ExhaustiveXMLTestModel.class, XmlUtils.getNamespaceContext(testDoc), "io.michelin.choreography.utils");
//        assertEquals(buildTestDocAvro(), actualValue);
//    }
//
//    @Test
//    void getSubLevelFromFilterTest() {
//        assertEquals("Codes[Code/@name='Commodity']",
//                AvroToXMLUtils.getSubLevelFromFilter("Classification[Codes/Code/@name='Commodity']"));
//        assertEquals("Code[@name='Commodity']",
//                AvroToXMLUtils.getSubLevelFromFilter("Codes[Code/@name='Commodity']"));
//        assertEquals("",
//                AvroToXMLUtils.getSubLevelFromFilter("Code[@name='Commodity']"));
//        assertEquals("",
//                AvroToXMLUtils.getSubLevelFromFilter("Code[1]"));
//        assertEquals("",
//                AvroToXMLUtils.getSubLevelFromFilter("Code"));
//    }
//
//    @Test
//    void isNodeMatchingTest() throws Exception {
//        var doc = sampleDoc("foo", "bar"); // doc is only created for base of node
//        // we create this tree : Classification[Codes/Code/@name='Commodity']
//        var nodeClassification = doc.createElement("Classification");
//        var nodeCodes = doc.createElement("Codes");
//        var nodeCode = doc.createElement("Code");
//        nodeCode.setAttribute("name", "Commodity");
//        nodeCodes.appendChild(nodeCode);
//        nodeClassification.appendChild(nodeCodes);
//        assertTrue(AvroToXMLUtils.isNodeMatching(nodeCode, "Code[@name='Commodity']"));
//        assertFalse(AvroToXMLUtils.isNodeMatching(nodeCode, "Code[@name='Product Line']"));
//        assertTrue(AvroToXMLUtils.isNodeMatching(nodeClassification, "Classification[Codes/Code/@name='Commodity']"));
//        assertFalse(AvroToXMLUtils.isNodeMatching(nodeClassification, "Classification[Codes/Code/@name='Product Line']"));
//        assertTrue(AvroToXMLUtils.isNodeMatching(nodeClassification, "Classification"));
//    }
//
//    private static Document sampleDoc(String attributeValue, String textContent) throws Exception {
//        final var docFactory = DocumentBuilderFactory.newInstance();
//        final var docBuilder = docFactory.newDocumentBuilder();
//
//        final var doc = docBuilder.newDocument();
//
//        final var root = doc.createElement("bookings");
//        doc.appendChild(root);
//
//        final var booking = doc.createElement("booking");
//        root.appendChild(booking);
//
//        final var attr = doc.createAttribute("id");
//        attr.setValue(attributeValue);
//
//        booking.setAttributeNode(attr);
//        booking.setTextContent(textContent);
//
//        log.info(nodeToString(doc));
//
//        return doc;
//    }
//
//    private static String nodeToString(Node node) throws Exception {
//        final var writer = new StringWriter();
//
//        final var transformer = TransformerFactory.newInstance().newTransformer();
//        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//        transformer.setOutputProperty(OutputKeys.INDENT, "false");
//        transformer.transform(new DOMSource(node), new StreamResult(writer));
//
//        return writer.toString();
//    }
//
//    private static ExhaustiveXMLTestModel buildTestDocAvro() {
//        return ExhaustiveXMLTestModel.newBuilder()
//                .setDate1(Instant.ofEpochMilli(1583412805459L))
//                .setDate2(Instant.ofEpochMilli(1583409600000L))
//                .setDate3(Instant.ofEpochMilli(1583420005000L))
//                .setDate4(Instant.ofEpochMilli(1583409600000L))
//                .setDate5(Instant.ofEpochMilli(1583416405000L))
//                .build();
//    }
//    /* -------------------------------------------------------------------------------------------- */
//
//    @Test
//    public void shouldConvertToAvroAndBackToXML() throws Exception {
//        final String xmlInput = FileUtils.readFile("/XML_With_Default_XPath.xml");
//        var result = (TestModelXMLDefaultXpath) XmlUtils.convertStringDocumentToAvro(xmlInput, TestModelXMLDefaultXpath.class);
//        var expectedModel = TestModelXMLDefaultXpath.newBuilder()
//                .setBooleanField(true)
//                .setDateField(Instant.ofEpochMilli(1766620800000L))
//                .setQuantityField(BigDecimal.valueOf(52L).setScale(4))
//                .setStringField("lorem ipsum")
//                .setStringMapLegacyScenario(Map.of("key1", "value1", "key2", "value2", "key3", "value3"))
//                .setStringMapScenario1(Map.of("key1", "value1", "key2", "value2", "key3", "value3"))
//                .setStringMapScenario2(Map.of("key1", "value1", "key2", "value2", "key3", "value3"))
//                .setStringList(List.of("item1", "item2", "item3"))
//                .setRecordListWithDefault(List.of(
//                        SubXMLTestModel.newBuilder().setSubStringField("item1").setSubIntField(1).setSubStringFieldFromAttribute("attribute1").build(),
//                        SubXMLTestModel.newBuilder().setSubStringField("item2").setSubIntField(2).setSubStringFieldFromAttribute("attribute2").build(),
//                        SubXMLTestModel.newBuilder().setSubStringField("item3").setSubIntField(3).setSubStringFieldFromAttribute("attribute3").build()
//                ))
//                .build();
//
//        assertEquals(expectedModel, result);
//
//        var documentXMLResult = XmlUtils.createDocumentfromAvro(result);
//        var stringXMLResult = XmlUtils.documentToString(documentXMLResult);
//        assertEquals("<root xmlns=\"namespace1\" xmlns:ns2=\"namespace2\"><ns2:stringField>lorem ipsum</ns2:stringField><stringFieldWithDefault>defaultString</stringFieldWithDefault><booleanField>true</booleanField><quantityField>52</quantityField><quantityFieldWithDefault>51.2</quantityFieldWithDefault><dateField>2025-12-25T00:00:00Z</dateField><dateFieldWithDefault>1970-01-01T00:00:00Z</dateFieldWithDefault><stringMapScenario1><key>key1</key><value>value1</value></stringMapScenario1><stringMapScenario1><key>key2</key><value>value2</value></stringMapScenario1><stringMapScenario1><key>key3</key><value>value3</value></stringMapScenario1><stringMapScenario2><entry key=\"key1\">value1</entry><entry key=\"key2\">value2</entry><entry key=\"key3\">value3</entry></stringMapScenario2><stringList><listItem>item1</listItem><listItem>item2</listItem><listItem>item3</listItem></stringList><recordList><listItem><subStringField>item1</subStringField><subIntField attribute=\"attribute1\">1</subIntField></listItem><listItem><subStringField>item2</subStringField><subIntField attribute=\"attribute2\">2</subIntField></listItem><listItem><subStringField>item3</subStringField><subIntField attribute=\"attribute3\">3</subIntField></listItem></recordList></root>",
//                stringXMLResult);
//
//    }
//
//
//    @Test
//    public void shouldConvertToAvroAndBackToXMLUsingCustomXpath() throws Exception {
//        final String xmlInput = FileUtils.readFile("/XML_With_Default_XPath.xml");
//        var result = (TestModelXMLMultipleXpath) XmlUtils.convertStringDocumentToAvro(xmlInput, TestModelXMLMultipleXpath.class, "xpath2");
//        var expectedModel = TestModelXMLMultipleXpath.newBuilder()
//                .setQuantityField(BigDecimal.valueOf(52L).setScale(4))
//                .setStringField("lorem ipsum")
//                .setStringMapLegacyScenario(Map.of("key1", "value1", "key2", "value2", "key3", "value3"))
//                .setStringMapScenario1(Map.of("key1", "value1", "key2", "value2", "key3", "value3"))
//                .setStringList(List.of("item1", "item2", "item3"))
//                .setRecordListWithDefault(List.of(
//                        SubXMLTestModelMultipleXpath.newBuilder().setSubStringField("item1").setSubStringFieldFromAttribute("attribute1").build(),
//                        SubXMLTestModelMultipleXpath.newBuilder().setSubStringField("item2").setSubStringFieldFromAttribute("attribute2").build(),
//                        SubXMLTestModelMultipleXpath.newBuilder().setSubStringField("item3").setSubStringFieldFromAttribute("attribute3").build()
//                ))
//                .build();
//
//        assertEquals(expectedModel, result);
//
//        var documentXMLResult = XmlUtils.createDocumentfromAvro(result, "xpath2");
//        var stringXMLResult = XmlUtils.documentToString(documentXMLResult);
//        assertEquals("<root xmlns=\"namespace1\" xmlns:ns2=\"namespace2\"><ns2:stringField>lorem ipsum</ns2:stringField><stringFieldWithDefault>defaultString</stringFieldWithDefault><quantityField>52</quantityField><quantityFieldWithDefault>51.2</quantityFieldWithDefault><dateFieldWithDefault>1970-01-01T00:00:00Z</dateFieldWithDefault><stringMapScenario1><key>key1</key><value>value1</value></stringMapScenario1><stringMapScenario1><key>key2</key><value>value2</value></stringMapScenario1><stringMapScenario1><key>key3</key><value>value3</value></stringMapScenario1><stringList><listItem>item1</listItem><listItem>item2</listItem><listItem>item3</listItem></stringList><recordList><listItem><subStringField>item1</subStringField><subIntField attribute=\"attribute1\"/></listItem><listItem><subStringField>item2</subStringField><subIntField attribute=\"attribute2\"/></listItem><listItem><subStringField>item3</subStringField><subIntField attribute=\"attribute3\"/></listItem></recordList></root>",
//                stringXMLResult);
//
//    }
//}
