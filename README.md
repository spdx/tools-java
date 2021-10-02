# Overview
The Software Package Data Exchange (SPDX) specification is a standard format for communicating the components, licenses and copyrights associated with a software package.

  * [SPDX License List](http://spdx.org/licenses/)
  * [SPDX Vocabulary Specification](http://spdx.org/rdf/terms)

These tools are published by the SPDX Workgroup
see [http://spdx.org/](http://spdx.org/)

# Code quality badges

|   [![Bugs](https://sonarcloud.io/api/project_badges/measure?project=tools-java&metric=bugs)](https://sonarcloud.io/dashboard?id=tools-java)    | [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=tools-java&metric=security_rating)](https://sonarcloud.io/dashboard?id=tools-java) | [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=tools-java&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=tools-java) | [![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=tools-java&metric=sqale_index)](https://sonarcloud.io/dashboard?id=tools-java) |

## Getting Starting

The SPDX Tool binaries can be downloaded from the [releases page](https://github.com/spdx/tools-java/releases) under the respective release.  The package is also available in [Maven Central](https://search.maven.org/artifact/org.spdx/tools-java) (organization org.spdx, artifact tools-java).

See the Syntax section below for the commands available.

## Contributing
See the file CONTRIBUTING.md for information on making contributions to the SPDX tools.

## Issues
Report any security related issues by sending an email to [spdx-tools-security@lists.spdx.org](mailto:spdx-tools-security@lists.spdx.org)

Non-security related issues should be added to the [SPDX tools issues list](https://github.com/spdx/tools-java/issues)

## Syntax
The command line interface of the spdx tools can be used like this:

    java -jar spdx-tools-jar-with-dependencies.jar <function> <parameters> 

## SPDX format converters
The following converter tools support spdx format:

  * Tag
  * RDF/XML
  * XLSX Spreadsheet
  * XLS Spreadsheet
  * JSON
  * XML
  * YAML

Example to convert a SPDX file from tag to rdf format:

    java -jar spdx-tools-jar-with-dependencies.jar Convert Examples/SPDXTagExample.tag TagToRDF.rdf

## Compare utilities
The following  tools can be used to compare one or more SPDX documents:

  * CompareSpdxDocs

    Example to compare two SPDX files provided in rdf format:

        java -jar spdx-tools-jar-with-dependencies.jar CompareSpdxDocs doc1 doc2 [output]

  * CompareMultipleSpdxDocs

    Example to compare multiple SPDX files provided in rdf format and provide a spreadsheet with the results:

        java -jar spdx-tools-jar-with-dependencies.jar CompareMultipleSpdxDocs output.xls doc1 doc2 ... docN

## SPDX Viewer
The following tool can be used to "Pretty Print" an SPDX document.

  * SPDXViewer

Sample usage:

    java -jar spdx-tools-jar-with-dependencies.jar SPDXViewer TestFiles/SPDXRdfExample.rdf

## Verifier
The following tool can be used to verify an SPDX document:

  * Verify

Sample usage:

    java -jar spdx-tools-jar-with-dependencies.jar Verify TestFiles/SPDXRdfExample.rdf

## Generators
The following tool can be used to generate an SPDX verification code from a directory of source files:

  * GenerateVerificationCode sourceDirectory
  
  Sample usage:

        java -jar spdx-tools-jar-with-dependencies.jar GenerateVerificationCode sourceDirectory [ignoredFilesRegex]

## SPDX Validation Tool
The SPDX Workgroup provides an online interface to validate, compare, and convert SPDX documents in addition to the command line options above. The [SPDX Validation Tool](http://13.57.134.254/app/) is an all-in-one portal to upload and parse SPDX documents for validation, comparison and conversion and search the SPDX license list. 

# License
A complete SPDX file is available including dependencies is available in the bintray and Maven repos.

    SPDX-License-Identifier:	Apache-2.0
    PackageLicenseDeclared:	Apache-2.0

# Development

## Build
You need [Apache Maven](http://maven.apache.org/) to build the project:

    mvn clean install


## Update for new properties or classes
To update Spdx-Tools-Library, the following is a very brief checklist:

  1. Update the properties files in the org.spdx.tag package for any new tag values
  2. Update the org.spdx.tag.CommonCode.java for any new or changed tag values.  This will implement both the rdfToTag and the SPDXViewer applications.
  3. Update the org.spdx.tag.BuildDocument to implement changes for the TagToRdf application
  4. Update the HTML template (resources/htmlTemplate/SpdxHTMLTemplate.html) and contexts in org.spdx.html to implement changes for the SpdxToHtml application
  5. Update the related sheets and RdfToSpreadsheet.java file in the package org.spdx.spreadsheet
  6. Update the sheets for SPDX compare utility
