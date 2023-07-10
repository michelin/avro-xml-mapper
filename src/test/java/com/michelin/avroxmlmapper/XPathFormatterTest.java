package com.michelin.avroxmlmapper;

import com.michelin.avroxmlmapper.utility.XPathFormatter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XPathFormatterTest {

    private static final Logger log = LoggerFactory.getLogger(XPathFormatterTest.class);

    @Test
    void testConvertionXpathWithMultipleAttributeTest() throws Exception {

        var sourceXPath = "DataArea/ns2:Shipment/ns2:ShipmentUnit[ns3:UserArea[@name='type']='DeliveryLeg' and ShipUnitSequenceID='1']/ns2:DocumentReference[@type='LoadNumber']/ns2:DocumentID/ID";
        assertEquals("noprefixns:DataArea/ns2:Shipment/ns2:ShipmentUnit[ns3:UserArea[@*[translate(local-name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = 'name']='type']='DeliveryLeg' and noprefixns:ShipUnitSequenceID='1']/ns2:DocumentReference[@*[translate(local-name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = 'type']='LoadNumber']/ns2:DocumentID/noprefixns:ID", XPathFormatter.format(sourceXPath));
    }

    @Test
    void testConvertionAttributeValueWithSpaces() throws Exception {

        var sourceXPath = "ProcessShipment/DataArea/ns1:Shipment/ns1:ShipmentItem/Classification/Codes/Code[@name='Product Line']";
        assertEquals("noprefixns:ProcessShipment/noprefixns:DataArea/ns1:Shipment/ns1:ShipmentItem/noprefixns:Classification/noprefixns:Codes/noprefixns:Code[@*[translate(local-name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = 'name']='Product Line']", XPathFormatter.format(sourceXPath));
    }


    @Test
    void testConvertionAttributeValueWithoutSpaces() throws Exception {

        var sourceXPath = "ProcessShipment/DataArea/ns1:Shipment/ns1:ShipmentItem/Classification/Codes/Code[@name='Commodity']";
        assertEquals("noprefixns:ProcessShipment/noprefixns:DataArea/ns1:Shipment/ns1:ShipmentItem/noprefixns:Classification/noprefixns:Codes/noprefixns:Code[@*[translate(local-name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = 'name']='Commodity']", XPathFormatter.format(sourceXPath));
    }
}
