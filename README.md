This library is meant as a way to easily convert from XML to Avro and vice versa, using specific attributes in the avsc file

Your avsc files become the only necessary reference for the functional mapping of your xml data to and from avro.

# Usage




# Annotations

## xpath

The **xpath** attribute is used to specify the path to the element in the XML file.


### Simple elements

<table>
<tr><th>XML</th><th>avsc</th></tr>
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
  "xpath": "objectRoot",
  "fields": [
    {"name": "element", "type": "string", "xpath": "element"}
  ]
}
```
</table>

#### Specifically handled logical-types
##### Dates
##### BigDecimal


### Lists

### Maps

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