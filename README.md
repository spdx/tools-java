# Spdx-Tools-Java
SPDX Tools using the Spdx-Java-Library

Note: This tools is in early development and is only partially implemented.  Reviews, suggestions are welcome especially as it relates to the design.  Please enter an issue with any suggestions.

## Update for new properties or classes
To update Spdx-Tools-Library, the following is a very brief checklist:

  1. Update the properties files in the org.spdx.tag package for any new tag values
  2. Update the org.spdx.tag.CommonCode.java for any new or changed tag values.  This will implement both the rdfToTag and the SPDXViewer applications.
  3. Update the org.spdx.tag.BuildDocument to implement changes for the TagToRdf application
  4. Update the HTML template (resources/htmlTemplate/SpdxHTMLTemplate.html) and contexts in org.spdx.html to implement changes for the SpdxToHtml application
  5. Update the related sheets and RdfToSpreadsheet.java file in the package org.spdx.spreadsheet
  6. Update the sheets for SPDX compare utility
