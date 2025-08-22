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

import com.michelin.avroxmlmapper.utility.XPathFormatter;
import org.junit.jupiter.api.Test;

class XPathFormatterTest {

    @Test
    void testConvertionXpathWithMultipleAttributeTest() throws Exception {

        var sourceXPath =
                "DataArea/ns2:Shipment/ns2:ShipmentUnit[ns3:UserArea[@name='type']='DeliveryLeg' and ShipUnitSequenceID='1']/ns2:DocumentReference[@type='LoadNumber']/ns2:DocumentID/ID";
        assertEquals(
                "noprefixns:DataArea/ns2:Shipment/ns2:ShipmentUnit[ns3:UserArea[@*[translate(local-name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = 'name']='type']='DeliveryLeg' and noprefixns:ShipUnitSequenceID='1']/ns2:DocumentReference[@*[translate(local-name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = 'type']='LoadNumber']/ns2:DocumentID/noprefixns:ID",
                XPathFormatter.format(sourceXPath));
    }

    @Test
    void testConvertionAttributeValueWithSpaces() throws Exception {

        var sourceXPath =
                "ProcessShipment/DataArea/ns1:Shipment/ns1:ShipmentItem/Classification/Codes/Code[@name='Product Line']";
        assertEquals(
                "noprefixns:ProcessShipment/noprefixns:DataArea/ns1:Shipment/ns1:ShipmentItem/noprefixns:Classification/noprefixns:Codes/noprefixns:Code[@*[translate(local-name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = 'name']='Product Line']",
                XPathFormatter.format(sourceXPath));
    }

    @Test
    void testConvertionAttributeValueWithoutSpaces() throws Exception {

        var sourceXPath =
                "ProcessShipment/DataArea/ns1:Shipment/ns1:ShipmentItem/Classification/Codes/Code[@name='Commodity']";
        assertEquals(
                "noprefixns:ProcessShipment/noprefixns:DataArea/ns1:Shipment/ns1:ShipmentItem/noprefixns:Classification/noprefixns:Codes/noprefixns:Code[@*[translate(local-name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = 'name']='Commodity']",
                XPathFormatter.format(sourceXPath));
    }
}
