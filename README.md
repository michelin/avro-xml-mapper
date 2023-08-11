This library is meant as a way to easily convert from XML to Avro and vice versa, using specific attributes in the avsc file

Your avsc files become the only necessary reference for the functional mapping of your xml data to and from avro.

# Usage




# Annotations

## xpath

The **xpath** attribute is used to specify the path to the element in the XML file.


### Simple elements

<table style="width:100%">
<tr><th style="width:50%">XML</th><th style="width:50%">avsc</th></tr>
<td>

```xml
<objectRoot>
    <element>content</element>
</objectRoot>
```
</td>
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
</table>

#### Specifically handled logical-types
##### Dates

only the "timestamp-millis" long logical type is handled and has multiple accepted formats:
- ISO8601 date-time
- ISO8601 date
- Flat date (yyyyMMddz) which gets the UTC 12:00:00.000 time to avoid timezone issues
- FLat date-time (yyyyMMddHHmmssz) which gets the UTC timezone assigned
- ISO8601 date-time without offset
- ISO8601 date without offset
- Flat date without offset (yyyyMMdd) which gets the UTC 12:00:00.000 time to avoid timezone issues
- Flat date-time without offset (yyyyMMdd HHmmss) which gets the UTC timezone assigned
- Flat date-time without offset and without timezone (yyyy-MM-dd HH:mm:ss) which gets the UTC timezone assigned
- Flat date-time with offset (yyyy-MM-dd'T'HH:mm:ss'T'00:00)
They are all converted to the "Instant" java type.

##### BigDecimal

Only the "decimal" byte logical type is handled. It is converted to a BigDecimal java type.

### Lists

Lists can be applied to any repeating element in the XML file. The xpath attribute should point to the repeating element.

<table style="width:100%">
<tr><th style="width:50%">XML</th><th style="width:50%">avsc</th></tr>
<td>

``` xml
<objectRoot>
    <child>content1</child>
    <child>content2</child>
</objectRoot> 
```
</td>
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
      "type": {"type": "array", "items": "string" },
      "default": {}
    }
  ]
}
```
</table>

They can also define complex types like such:

<table style="width:100%">
<tr><th style="width:50%">XML</th><th style="width:50%">avsc</th></tr>
<td>

``` xml
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
</td>
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
</table>

### Maps

Maps have two accepted formats:
- A list of elements with a key attribute

<table style="width:100%">
<tr><th style="width:50%">XML</th><th style="width:50%">avsc</th></tr>
<td>

``` xml
<objectRoot>
    <element key="key1">content1</element>
    <element key="key2">content2</element>
</objectRoot> 
```
</td>
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
</table>

- A list of nodes with a key element and a value element

<table style="width:100%">
<tr><th style="width:50%">XML</th><th style="width:50%">avsc</th></tr>
<td>

``` xml
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
</td>
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
</table>

It can be noted that in both cases, the rootXpath attribute always point to the repeating element of the list.

## xmlNamespaces

The xmlNamespaces attribute defined at the root of the avsc file is used to specify the namespaces used in the XML file.

**It should be noted that this attribute is used in different ways depending on the conversion direction as described in the following sections.**

### XML to Avro 
The namespaces are used to "unify" the XML file. If multiple namespace definition refer to the same URI, only the one defined in the xmlNamespaces attribute will be kept during conversion.

For instance, with the given avsc and xml: 

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
Before conversion to avro, the initial Document is tweaked as such:

```xml
<noprefixns:objectRoot xmlns:noprefixns="http://namespace.uri/default"
            xmlns:ns1="http://namespace.uri/1">
    <noprefixns:element>content</noprefixns:element>
    <ns1:secondElement>second element content</ns1:secondElement>
    <ns1:thirdElement>third element content</ns1:thirdElement>
</noprefixns:objectRoot>
```

The root **xmlns** namespace is replaced with **xmlns:noprefixns** and the **ns1** is simply kept. 

The **ns2** namespace is removed because it refers to the same URI as the **ns1** namespace.


**Failing to provide xmlNamespaces for XML➡️Avro conversion simply means that namespaces in xpath have to be consistent.**

### Avro to XML
The namespaces are used for root namespaces' definition.

**Failing to provide xmlNamespaces for Avro➡️XML conversion means that no namespace should be used in the xpath attributes, as it would mean that the produced xml would be invalid.**

## keepEmptyTag
The keepEmptyTag attribute can be used to signify that the tag needs to be kept in the Avro to XML conversion in case the original avro field is null:

<table style="width:100%">
<tr><th style="width:50%">avsc</th><th style="width:50%">XML</th></tr>
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

``` xml
<objectRoot>
    <element/>
</objectRoot>
```
</td>

</table>


# Custom implementations

Using the provided method **AvroToXmlMapper#convertAvroToXmlDocument** allows for custom implementations and editing of the document before it is converted to String.

Conversion can be finalized using **GenericUtils.documentToString** method.

[example needed]

# Changelog
0.1.0-SNAPSHOT : First coherent snapshot
0.1.1-SNAPSHOT : Add support for non-provided xmlns
0.1.2-SNAPSHOT : Add keepEmptyTag attribute
0.1.3-SNAPSHOT : Handle "." syntax in lists