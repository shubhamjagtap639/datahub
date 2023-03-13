# Advantages of using SDKs and APIs

## When to use an API over the DataHub UI
Using programmatic ways to emit metadata to DataHub can provide a number of benefits over using the UI. One key advantage is automation, which can save time and effort by streamlining the process of emitting metadata. Additionally, programmatic ways of emitting metadata can offer greater flexibility and control over the metadata being emitted, enabling you to customize the metadata to meet your specific needs.

## Simple use-cases to get started with DataHub APIs
APIs offer a wide range of use cases when it comes to emitting metadata. 
Below are some simple use-cases to get you started:

### Basic Usage
* [Adding Tags](./adding-tags.md)
* [Adding Terms](./adding-terms.md)
* [Adding Ownership](./adding-ownerships.md)

### Advanced Usage 
Here our some examples of slightly more complex usage:
* Adding Tags on Entities Based on Entity Type
* Ingesting Entities from CSV Files
* Adding Column-level Lineage

## Our APIs
DataHub supports three APIs : GraphQL, SDKs and OpenAPI. Each method has different usage and format. 
Here's an overview of what each API can do. 

> Last Updated : Mar 10 2023

|                                       | GraphQL | SDK | OpenAPI |
|---------------------------------------|---|---|---|
| Add Tags/Terms/Ownership to a column of a dataset | ✅ |✅|✅|
| Add Tags/Terms/Ownership to a dataset | ✅ |✅|✅|
| Create Dataset                        |   |✅| ✅        |
| Delete Dataset                        |   | ✅       | ✅  |
| Search Dataset                        | ✅ |✅| ✅  |


