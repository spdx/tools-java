# Overview
The Software Package Data Exchange (SPDX) specification is a standard format for communicating the components, licenses and copyrights associated with a software package.

  * [SPDX License List](http://spdx.org/licenses/)
  * [SPDX Vocabulary Specification](http://spdx.org/rdf/terms)

These tools are published by the SPDX Workgroup
see [http://spdx.org/](http://spdx.org/)

## Versions Supported
This utility supports versions 2.0, 2.1, 2.2, 2.3 and 3.0.1 of the SPDX specification.

# Code quality badges

|   [![Bugs](https://sonarcloud.io/api/project_badges/measure?project=tools-java&metric=bugs)](https://sonarcloud.io/dashboard?id=tools-java)    | [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=tools-java&metric=security_rating)](https://sonarcloud.io/dashboard?id=tools-java) | [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=tools-java&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=tools-java) | [![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=tools-java&metric=sqale_index)](https://sonarcloud.io/dashboard?id=tools-java) |

## Getting Starting

The SPDX Tool binaries can be downloaded from the [releases page](https://github.com/spdx/tools-java/releases) under the respective release.  The package is also available in [Maven Central](https://search.maven.org/artifact/org.spdx/tools-java) (organization org.spdx, artifact tools-java).

See the Syntax section below for the commands available.

If you are a developer, there are examples in the [examples folder](examples/org/spdx/examples).

## Contributing
See the file CONTRIBUTING.md for information on making contributions to the SPDX tools.

## Issues
Report any security related issues by sending an email to [spdx-tools-security@lists.spdx.org](mailto:spdx-tools-security@lists.spdx.org)

Non-security related issues should be added to the [SPDX tools issues list](https://github.com/spdx/tools-java/issues)

## Syntax
The command line interface of the spdx tools can be used like this:

    java -jar tools-java-2.0.0-Alpha-jar-with-dependencies.jar <function> <parameters>

## SPDX format converters
The following converter tools support spdx format:

  * Tag
  * RDF/XML
  * XLSX Spreadsheet
  * XLS Spreadsheet
  * JSON
  * XML
  * YAML
  * JSON-LD (SPDX spec version 3.0.1)

Example to convert a SPDX file from tag to rdf format:

    java -jar tools-java-2.0.0-Alpha-jar-with-dependencies.jar Convert ../testResources/SPDXTagExample-v2.2.spdx TagToRDF.rdf

The file formats can optionally be provided as the 3rd and 4th parameter for the input and output formats respectively.  An optional 5th option `excludeLicenseDetails` will not copy the listed license properties to the output file.  The following example will copy a JSON format to an RDF Turtle format without including the listed license properties:

    java -jar tools-java-2.0.0-Alpha-jar-with-dependencies.jar Convert ../testResources/SPDXTagExample-v2.2.spdx TagToRDF.ttl TAG RDFTTL excludeLicenseDetails

To convert from SPDX 2 to SPDX 3.0.1:
* use the file extension `.jsonld.json` or `.jsonld`;
* or add the options for the from and to file types:
```
java -jar tools-java-2.0.0-Alpha-jar-with-dependencies.jar Convert hello.spdx hello.spdx.json TAG JSONLD
```

## Compare utilities
The following  tools can be used to compare one or more SPDX documents:

  * CompareMultipleSpdxDocs with files

    Example to compare multiple SPDX files provided in rdf format and provide a spreadsheet with the results:

        java -jar tools-java-2.0.0-Alpha-jar-with-dependencies.jar CompareDocs output.xlsx doc1 doc2 ... docN

  * CompareMultipleSpdxDocs with directory

    Example to compare all SPDX documents in a directory "/home/me/spdxdocs" and provide a spreadsheet with the results:

        java -jar tools-java-2.0.0-Alpha-jar-with-dependencies.jar CompareDocs output.xlsx /home/me/spdxdocs

## SPDX Viewer
The following tool can be used to "Pretty Print" an SPDX document.

  * SPDXViewer

Sample usage:

    java -jar tools-java-2.0.0-Alpha-jar-with-dependencies.jar SPDXViewer ../testResources/SPDXRdfExample-v2.2.spdx.rdf

## Verifier
The following tool can be used to verify an SPDX document:

  * Verify

Sample usage:

    java -jar tools-java-2.0.0-Alpha-jar-with-dependencies.jar Verify ../testResources/SPDXRdfExample-v2.2.spdx.rdf

## Generators
The following tool can be used to generate an SPDX verification code from a directory of source files:

  * GenerateVerificationCode sourceDirectory

  Sample usage:

        java -jar tools-java-2.0.0-Alpha-jar-with-dependencies.jar GenerateVerificationCode sourceDirectory [ignoredFilesRegex]

## SPDX Validation Tool
The SPDX Workgroup provides an online interface to validate, compare, and convert SPDX documents in addition to the command line options above. The [SPDX Validation Tool](https://tools.spdx.org/app/validate/) is an all-in-one portal to upload and parse SPDX documents for validation, comparison and conversion and search the SPDX license list.

# License
A complete SPDX file is available including dependencies is available in the bintray and Maven repos.

    SPDX-License-Identifier:	Apache-2.0
    PackageLicenseDeclared:	Apache-2.0

# Development

## Build
You need [Apache Maven](http://maven.apache.org/) to build the project:

    mvn clean install

