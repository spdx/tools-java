# SPDX Tools

[![Maven Central Version](https://img.shields.io/maven-central/v/org.spdx/tools-java)](https://central.sonatype.com/artifact/org.spdx/tools-java)
[![javadoc](https://javadoc.io/badge2/org.spdx/tools-java/javadoc.svg)](https://javadoc.io/doc/org.spdx/tools-java)

A command-line utility for creating, converting, comparing,
and validating SPDX documents across multiple formats.

The Software Package Data Exchange (SPDX) specification is a standard format for communicating the components, licenses and copyrights associated with a software package.

* [SPDX License List](https://spdx.org/licenses/)
* [SPDX Vocabulary Specification](https://spdx.org/specifications)

These tools are published by the SPDX Workgroup,
see <https://spdx.org/>

## Versions Supported

This utility supports versions 2.0, 2.1, 2.2, 2.3 and 3.0.1 of the SPDX specification.

## Code quality badges

[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=tools-java&metric=bugs)](https://sonarcloud.io/dashboard?id=tools-java)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=tools-java&metric=security_rating)](https://sonarcloud.io/dashboard?id=tools-java)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=tools-java&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=tools-java)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=tools-java&metric=sqale_index)](https://sonarcloud.io/dashboard?id=tools-java)

## Getting Starting

The SPDX Tools binaries can be downloaded from the [releases page](https://github.com/spdx/tools-java/releases) under the respective release.  The package is also available in [Maven Central](https://central.sonatype.com/artifact/org.spdx/tools-java) (organization `org.spdx`, artifact `tools-java`).

See the Syntax section below for the commands available.

If you are a developer, there are examples in the [examples folder](examples/org/spdx/examples).

## Syntax

The command line interface of the SPDX Tools can be used like this:

    java -jar tools-java-2.0.2-jar-with-dependencies.jar <function> <parameters>

## SPDX format converters

The following converter tools support SPDX format:

* Tag
* RDF/XML
* XLSX Spreadsheet
* XLS Spreadsheet
* JSON
* XML
* YAML
* JSON-LD (SPDX spec version 3.0.1)

Example to convert a SPDX file from Tag to RDF format:

    java -jar tools-java-2.0.2-jar-with-dependencies.jar Convert ../testResources/SPDXTagExample-v2.2.spdx TagToRDF.rdf

The file formats can optionally be provided as the 3rd and 4th parameter for the input and output formats respectively.  An optional 5th option `excludeLicenseDetails` will not copy the listed license properties to the output file.  An additional optional flag `--stable-ids` makes SPDX 2 to SPDX 3 conversions deterministic by preserving SPDX IDs when possible and otherwise using deterministic IDs. The following example will copy a JSON format to an RDF Turtle format without including the listed license properties:

    java -jar tools-java-2.0.2-jar-with-dependencies.jar Convert ../testResources/SPDXTagExample-v2.2.spdx TagToRDF.ttl TAG RDFTTL excludeLicenseDetails

To convert from SPDX 2 to SPDX 3.0.1:

* use the file extension `.jsonld.json` or `.jsonld`;
* or add the options for the from and to file types:

    java -jar tools-java-2.0.2-jar-with-dependencies.jar Convert hello.spdx hello.spdx.json TAG JSONLD

To convert from SPDX 2 to SPDX 3.0.1 with deterministic SPDX IDs:

    java -jar tools-java-2.0.2-jar-with-dependencies.jar Convert hello.spdx hello.spdx.json TAG JSONLD --stable-ids

## Compare utilities

The following tools can be used to compare one or more SPDX documents:

* CompareMultipleSpdxDocs with files

    Example to compare multiple SPDX files provided in RDF format and provide a spreadsheet with the results:

        java -jar tools-java-2.0.2-jar-with-dependencies.jar CompareDocs output.xlsx doc1 doc2 ... docN

* CompareMultipleSpdxDocs with directory

    Example to compare all SPDX documents in a directory "/home/me/spdxdocs" and provide a spreadsheet with the results:

        java -jar tools-java-2.0.2-jar-with-dependencies.jar CompareDocs output.xlsx /home/me/spdxdocs

## SPDX Viewer

The following tool can be used to "Pretty Print" an SPDX document.

* SPDXViewer

Sample usage:

    java -jar tools-java-2.0.2-jar-with-dependencies.jar SPDXViewer ../testResources/SPDXRdfExample-v2.2.spdx.rdf

## Verifier

The following tool can be used to verify an SPDX document:

* Verify

Sample usage:

    java -jar tools-java-2.0.2-jar-with-dependencies.jar Verify ../testResources/SPDXRdfExample-v2.2.spdx.rdf

## Generators

The following tool can be used to generate an SPDX verification code from a directory of source files:

* GenerateVerificationCode sourceDirectory

  Sample usage:

        java -jar tools-java-2.0.2-jar-with-dependencies.jar GenerateVerificationCode sourceDirectory [ignoredFilesRegex]

## SPDX Validation Tool

The SPDX Workgroup provides an online interface to validate, compare, and convert SPDX documents in addition to the command line options above.

The [SPDX Online Tools](https://tools.spdx.org/) is an all-in-one portal to upload and parse SPDX documents for validation, comparison and conversion and search the SPDX license list.

## License

A complete SPDX file is available including dependencies is available in the bintray and Maven repos.

    SPDX-License-Identifier: Apache-2.0
    PackageLicenseDeclared: Apache-2.0

## Development

### Build

You need [Apache Maven](http://maven.apache.org/) to build the project:

    mvn clean install

## Contributing

See the file [CONTRIBUTING.md](./CONTRIBUTING.md) for information on
making contributions to the SPDX tools.

## Issues

Report any security related issues by sending an email to [spdx-tools-security@lists.spdx.org](mailto:spdx-tools-security@lists.spdx.org)

Non-security related issues should be added to the [SPDX Tools issues list](https://github.com/spdx/tools-java/issues)
