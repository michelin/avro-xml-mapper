<div align="center">

# Avro XML Mapper

[![GitHub Build](https://img.shields.io/github/actions/workflow/status/michelin/avro-xml-mapper/build.yml?branch=main&logo=github&style=for-the-badge)](https://img.shields.io/github/actions/workflow/status/michelin/avro-xml-mapper/build.yml)
[![Maven Central](https://img.shields.io/nexus/r/com.michelin/avro-xml-mapper?server=https%3A%2F%2Fs01.oss.sonatype.org%2F&style=for-the-badge&logo=apache-maven&label=Maven%20Central)](https://central.sonatype.com/search?q=com.michelin.avro-xml-mapper&sort=name)
![Supported Java Versions](https://img.shields.io/badge/Java-17--21-blue.svg?style=for-the-badge&logo=openjdk)
[![GitHub Stars](https://img.shields.io/github/stars/michelin/avro-xml-mapper?logo=github&style=for-the-badge)](https://github.com/michelin/avro-xml-mapper)
[![SonarCloud Coverage](https://img.shields.io/sonar/coverage/michelin_avro-xml-mapper?logo=sonarcloud&server=https%3A%2F%2Fsonarcloud.io&style=for-the-badge)](https://sonarcloud.io/component_measures?id=michelin_avro-xml-mapper&metric=coverage&view=list)
[![SonarCloud Tests](https://img.shields.io/sonar/tests/michelin_avro-xml-mapper/main?server=https%3A%2F%2Fsonarcloud.io&style=for-the-badge&logo=sonarcloud)](https://sonarcloud.io/component_measures?metric=tests&view=list&id=michelin_avro-xml-mapper)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?logo=apache&style=for-the-badge)](https://opensource.org/licenses/Apache-2.0)

Turn XML into Avro and vice versa.

Use attributes in Avro files as a single source of truth for effortless data mapping.

</div>

## Table of Contents

* [Dependency](#dependency)
* [Usage](#usage)
  * [XPath](#xpath)
  * [Structure](#structure)
    * [Single Element](#single-element)
    * [List](#list)
    * [Map](#map)
  * [Logical Type](#logical-type)
    * [Date](#date)
    * [Big Decimal](#big-decimal)
  * [XML Namespace](#xml-namespace)
  * [Keywords](#keywords)
    * [keepEmptyTag](#keepemptytag)
  * [Custom Implementations](#custom-implementations)
* [Contribution](#contribution)

## Dependency

[![javadoc](https://javadoc.io/badge2/com.michelin/avro-xml-mapper/javadoc.svg?style=for-the-badge)](https://javadoc.io/doc/com.michelin/avro-xml-mapper)

The Avro XML Mapper dependency is compatible with Java 17 and 21.

```xml
<dependency>
    <groupId>com.michelin</groupId>
    <artifactId>avro-xml-mapper</artifactId>
    <version>${avro-xml-mapper.version}</version>
</dependency>
```

## Usage

### XPath

The XPath attribute is used to specify the path of the element in the XML file.

### Structure

#### Single Element

A single element is represented as follows:

<table style="width:100%">
<tr><th style="width:50%">AVSC</th><th style="width:50%">XML</th></tr>
<td>

```avro schema
{
  "name": "Object",
  "type": "record",
  "namespace": "com.example",
  "xpath": "/objectRoot",
  "fields": [
    {"name": "element", "type": "string", "xpath": "element"}
  ]
}
```

</td>
<td>

```xml
<objectRoot>
    <element>content</element>
</objectRoot>
```

</table>

#### List

Lists can be applied to any repeating element in the XML file. The XPath attribute should point to the repeating element.

<table style="width:100%">
<tr><th style="width:50%">AVSC</th><th style="width:50%">XML</th></tr>
<td>

```avro schema
{
  "name": "Object",
  "type": "record",
  "namespace": "com.example",
  "xpath": "/objectRoot",
  "fields": [
    {
      "name": "stringList",
      "xpath": "child",
      "type": {"type": "array", "items": "string"},
      "default": {}
    }
  ]
}
```

</td>
<td>

```xml
<objectRoot>
    <child>content1</child>
    <child>content2</child>
</objectRoot> 
```

</table>

Complex types can also be defined as follows:

<table style="width:100%">
<tr><th style="width:50%">AVSC</th><th style="width:50%">XML</th></tr>
<td>

```avro schema
{
  "name": "Object",
  "type": "record",
  "namespace": "com.example",
  "xpath": "/objectRoot",
  "fields": [
    {
      "name": "recordList",
      "xpath": "recordList/listItem",
      "type": {
        "type": "array",
        "items": {
          "type": "record",
          "name": "SubXMLTestModelMultipleXpath",
          "fields": [
            {"name": "subStringField", "type" : ["null","string"], "default": null, "customXpath1": "subStringField", "customXpath2": "altSubStringField"},
            {"name": "subIntField", "type" : ["null","int"], "default": null, "customXpath1": "subIntField", "customXpath2": "altSubIntField"},
            {"name": "subStringFieldFromAttribute", "type" : ["null","string"], "default": null, "customXpath1": "subIntField/@attribute", "customXpath2": "altSubIntField/@attribute"}
          ],
          "default": {}
        }
      },
      "default": []
    }
  ]
}
```

</td>
<td>

```xml
<objectRoot>
    <recordList>
        <listItem>
            <subStringField>item1</subStringField>
            <subIntField attribute="attribute1">1</subIntField>
        </listItem>
        <listItem>
            <subStringField>item2</subStringField>
            <subIntField attribute="attribute2">2</subIntField>
        </listItem>
        <listItem>
            <subStringField>item3</subStringField>
            <subIntField attribute="attribute3">3</subIntField>
        </listItem>
    </recordList>
</objectRoot>
```

</table>

#### Map

Maps have two accepted formats:
- A list of elements with a key attribute

<table style="width:100%">
<tr><th style="width:50%">AVSC</th><th style="width:50%">XML</th></tr>
<td>

```avro schema
{
  "name": "Object",
  "type": "record",
  "namespace": "com.example",
  "xpath": "/objectRoot",
  "fields": [
    {
      "name": "stringMapFormat1",
      "xpath": { "rootXpath": "element", "keyXpath": "@key", "valueXpath": "." },
      "type": { "type": "map", "values": "string" },
      "default": {}
    }
  ]
}
```

</td>
<td>

```xml
<objectRoot>
    <element key="key1">content1</element>
    <element key="key2">content2</element>
</objectRoot> 
```

</table>

- A list of nodes with a key element and a value element

<table style="width:100%">
<tr><th style="width:50%">AVSC</th><th style="width:50%">XML</th></tr>
<td>

```avro schema
{
  "name": "Object",
  "type": "record",
  "namespace": "com.example",
  "xpath": "/objectRoot",
  "fields": [
    {
      "name": "stringMapFormat2",
      "xpath": { "rootXpath": "element", "keyXpath": "key", "valueXpath": "value" },
      "type": { "type": "map", "values": "string" },
      "default": {}
    }
  ]
}
```

</td>
<td>

```xml
<objectRoot>
    <element>
        <key>key1</key>
        <value>content1</value>
    </element>
    <element>
        <key>key2</key>
        <value>content2</value>
    </element>
</objectRoot>
```

</table>

In both cases, the `rootXpath` attribute always points to the repeating element of the list.

### Logical Type

#### Date

Only the `timestamp-millis` Long logical type is handled and has multiple accepted formats:

- ISO8601 date-time
- ISO8601 date
- Flat date (yyyyMMddz) which gets the UTC 12:00:00.000 time to avoid timezone issues
- Flat date-time (yyyyMMddHHmmssz) which gets the UTC timezone assigned
- ISO8601 date-time without offset
- ISO8601 date without offset
- Flat date without offset (yyyyMMdd) which gets the UTC 12:00:00.000 time to avoid timezone issues
- Flat date-time without offset (yyyyMMdd HHmmss) which gets the UTC timezone assigned
- Flat date-time without offset and without timezone (yyyy-MM-dd HH:mm:ss) which gets the UTC timezone assigned
- Flat date-time with offset (yyyy-MM-dd'T'HH:mm:ss'T'00:00)

They are all converted to the `Instant` Java type.

#### Big Decimal

Only the `Decimal` byte logical type is handled. It is converted to a `BigDecimal` Java type.

### XML Namespace

The `xmlNamespaces` attribute defined at the root of the AVSC file is used to specify the namespaces used in the XML file.

> It should be noted that this attribute is used in different ways depending on the conversion direction as described in the following sections.

#### XML to Avro 

The namespaces are used to unify the XML file. 
If multiple namespace definitions refer to the same URI, only the one defined in the `xmlNamespaces` attribute will be kept during conversion.

For instance, with the given AVSC and XML: 

```avro schema
{
  "name": "Object",
  "type": "record",
  "namespace": "com.example",
  "xpath": "objectRoot",
  "xmlNamespaces": {
    "null": "http://namespace.uri/default",
    "ns1": "http://namespace.uri/1"
  },
  "fields": [
    {"name": "element", "type": "string", "xpath": "element"},
    {"name": "secondElement", "type": "string", "xpath": "ns1:secondElement"},
    {"name": "thirdElement", "type": "string", "xpath": "ns1:thirdElement"}
  ]
}
```

```xml 
<objectRoot xmlns="http://namespace.uri/default"
            xmlns:ns1="http://namespace.uri/1">
    <element>content</element>
    <ns1:secondElement>second element content</ns1:secondElement>
    <ns2:thirdElement xmlns:ns2="http://namespace.uri/1">third element content</ns2:thirdElement>
</objectRoot>
```

Before conversion to Avro, the initial document is tweaked as such:

```xml
<noprefixns:objectRoot xmlns:noprefixns="http://namespace.uri/default"
            xmlns:ns1="http://namespace.uri/1">
    <noprefixns:element>content</noprefixns:element>
    <ns1:secondElement>second element content</ns1:secondElement>
    <ns1:thirdElement>third element content</ns1:thirdElement>
</noprefixns:objectRoot>
```

The root `xmlns` namespace is replaced with `xmlns:noprefixns` and the `ns1` is simply preserved.

The `ns2` namespace is removed because it refers to the same URI as the `ns1` namespace.

> Failing to provide `xmlNamespaces` for XML to Avro conversion simply means that namespaces in XPath have to be consistent.

#### Avro to XML

The namespaces are used for root namespace definition.

> Failing to provide `xmlNamespaces` for Avro to XML conversion means that no namespace should be used in the XPath attributes, as it would mean that the produced XML would be invalid.

## Keywords

### keepEmptyTag

The `keepEmptyTag` attribute can be used to signify that the tag needs to be kept in the Avro to XML conversion in case the original Avro field is null:

<table style="width:100%">
<tr><th style="width:50%">AVSC</th><th style="width:50%">XML</th></tr>
<td>

```avro schema
{
  "name": "Object",
  "type": "record",
  "namespace": "com.example",
  "xpath": "/objectRoot",
  "fields": [
    {
      "name": "emptyElement",
      "xpath": "element",
      "keepEmptyTag": true,
      "type": ["null","string"],
      "default": null
    }
  ]
}
```

</td>

<td>

```xml
<objectRoot>
    <element />
</objectRoot>
```

</td>
</table>

### Custom Implementations

Using the provided method `AvroToXmlMapper#convertAvroToXmlDocument` allows for custom implementations and editing of the document before it is converted to String.

Conversion can be finalized using `GenericUtils#documentToString` method.

## Contribution

We welcome contributions from the community! Before you get started, please take a look at
our [contribution guide](https://github.com/michelin/avro-xml-mapper/blob/main/CONTRIBUTING.md) to learn about our guidelines
and best practices. We appreciate your help in making Avro XML Mapper a better tool for everyone.