/**

 * Copyright (c) 2010 Source Auditor Inc.

 *

 *   Licensed under the Apache License, Version 2.0 (the "License");

 *   you may not use this file except in compliance with the License.

 *   You may obtain a copy of the License at

 *

 *       http://www.apache.org/licenses/LICENSE-2.0

 *

 *   Unless required by applicable law or agreed to in writing, software

 *   distributed under the License is distributed on an "AS IS" BASIS,

 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

 *   See the License for the specific language governing permissions and

 *   limitations under the License.

 *

 */
package org.spdx.tag;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.Annotation;
import org.spdx.library.model.Checksum;
import org.spdx.library.model.ExternalDocumentRef;
import org.spdx.library.model.ExternalRef;
import org.spdx.library.model.Relationship;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxElement;
import org.spdx.library.model.SpdxFile;
import org.spdx.library.model.SpdxModelFactory;
import org.spdx.library.model.SpdxPackage;
import org.spdx.library.model.SpdxSnippet;
import org.spdx.library.model.enumerations.FileType;
import org.spdx.library.model.license.AnyLicenseInfo;
import org.spdx.library.model.license.ExtractedLicenseInfo;
import org.spdx.library.model.license.SimpleLicensingInfo;
import org.spdx.library.model.pointer.ByteOffsetPointer;
import org.spdx.library.model.pointer.LineCharPointer;
import org.spdx.library.model.pointer.StartEndPointer;
import org.spdx.library.referencetype.ListedReferenceTypes;
import org.spdx.tools.SpdxViewer;

/**
 * Define Common methods used by Tag-Value and SPDXViewer to print the SPDX
 * document.
 * 
 * @author Rana Rahal, Protecode Inc.
 */
public class CommonCode {
	/**
	 * @param doc
	 * @param out
	 * @param constants
	 * @throws InvalidSPDXAnalysisException
	 */
	public static void printDoc(SpdxDocument doc, PrintWriter out,
			Properties constants) throws InvalidSPDXAnalysisException {
		if (doc == null) {
			println(out, "Warning: No document to print");
			return;
		}
		// version
		String spdxVersion = "";
		if (doc.getSpecVersion() != null
				&& doc.getCreationInfo().getCreated() != null) {
			spdxVersion = doc.getSpecVersion();
			println(out, constants.getProperty("PROP_SPDX_VERSION") + spdxVersion);
		}
		// Data license
		AnyLicenseInfo dataLicense = doc.getDataLicense();
		if (dataLicense != null) {
			if (dataLicense instanceof SimpleLicensingInfo) {
				println(out, constants.getProperty("PROP_SPDX_DATA_LICENSE")
						+ ((SimpleLicensingInfo)dataLicense).getLicenseId());
			} else {
				println(out, constants.getProperty("PROP_SPDX_DATA_LICENSE")
						+ dataLicense.toString());
			}
		}
		// Document Uri
		String docNamespace = doc.getDocumentUri();
		if (docNamespace != null && !docNamespace.isEmpty()) {
			out.println(constants.getProperty("PROP_DOCUMENT_NAMESPACE") + docNamespace);
		}
		// element properties
		printElementProperties(doc, out, constants, "PROP_DOCUMENT_NAME", "PROP_SPDX_COMMENT");
		println(out, "");
		// External References
		Collection<ExternalDocumentRef> externalRefs = doc.getExternalDocumentRefs();
		if (externalRefs != null && !externalRefs.isEmpty()) {
			String externalDocRefHedr = constants.getProperty("EXTERNAL_DOC_REFS_HEADER");
			if (externalDocRefHedr != null && !externalDocRefHedr.isEmpty()) {
				println(out, externalDocRefHedr);
			}
			for (ExternalDocumentRef externalRef:externalRefs) {
				printExternalDocumentRef(externalRef, out, constants);
			}
		}
		// Creators
		Collection<String> creators = doc.getCreationInfo().getCreators();
		if (!creators.isEmpty()) {
			println(out, constants.getProperty("CREATION_INFO_HEADER"));
			for (String creator:creators) {
				println(out, constants.getProperty("PROP_CREATION_CREATOR")
						+ creator);
			}
		}
		// Creation Date
		if (doc.getCreationInfo().getCreated() != null
				&& !doc.getCreationInfo().getCreated().isEmpty()) {
			println(out, constants.getProperty("PROP_CREATION_CREATED")
					+ doc.getCreationInfo().getCreated());
		}
		// Creator Comment
		if (doc.getCreationInfo().getComment().isPresent()
				&& !doc.getCreationInfo().getComment().get().isEmpty()) {
			println(out, constants.getProperty("PROP_CREATION_COMMENT")
					+ constants.getProperty("PROP_BEGIN_TEXT") 
					+ doc.getCreationInfo().getComment().get()
					+ constants.getProperty("PROP_END_TEXT"));
		}
		// License list version
		if (doc.getCreationInfo().getLicenseListVersion().isPresent() &&
				!doc.getCreationInfo().getLicenseListVersion().get().isEmpty()) {
			println(out, constants.getProperty("PROP_LICENSE_LIST_VERSION") + 
					doc.getCreationInfo().getLicenseListVersion().get());
		}
		printElementAnnotationsRelationships(doc, out, constants, "PROP_DOCUMENT_NAME", "PROP_SPDX_COMMENT");
		println(out, "");
		// Print the actual files
		@SuppressWarnings("unchecked")
		Stream<SpdxPackage> allPackagesStream = (Stream<SpdxPackage>) SpdxModelFactory.getElements(doc.getModelStore(), doc.getDocumentUri(),
				doc.getCopyManager(), SpdxPackage.class);
		final List<SpdxPackage> allPackages = new ArrayList<>();
		allPackagesStream.forEach((SpdxPackage pkg) -> allPackages.add(pkg));
		@SuppressWarnings("unchecked")
		Stream<SpdxFile> allFilesStream = (Stream<SpdxFile>) SpdxModelFactory.getElements(doc.getModelStore(), doc.getDocumentUri(),
				doc.getCopyManager(), SpdxFile.class);
		final List<SpdxFile> allFiles = new ArrayList<>();
		allFilesStream.forEach((SpdxFile file) -> allFiles.add(file));
		@SuppressWarnings("unchecked")
		Stream<SpdxSnippet> allSnippets = (Stream<SpdxSnippet>) SpdxModelFactory.getElements(doc.getModelStore(), doc.getDocumentUri(),
				doc.getCopyManager(), SpdxSnippet.class);
		// first print out any described files or snippets
		final List<SpdxElement> alreadyPrinted = new ArrayList<>();
		Collection<SpdxElement> items = doc.getDocumentDescribes();
		for (SpdxElement item:items) {
			if (item instanceof SpdxFile) {
				printFile((SpdxFile)item, out, constants);
				alreadyPrinted.add(item);
			} else if (items instanceof SpdxSnippet) {
				printSnippet((SpdxSnippet)items, out, constants);
				alreadyPrinted.add(item);
			}
		}
		// print any described packages
		for (SpdxElement item:items) {
			if (item instanceof SpdxPackage) {
				printPackage((SpdxPackage)item, out, constants, allFiles, doc.getDocumentUri());
				alreadyPrinted.add(item);
			}
		}
		// print remaining packages
		allPackages.forEach((SpdxPackage pkg) -> {
			if (!alreadyPrinted.contains(pkg)) {
				try {
					printPackage(pkg, out, constants, allFiles, doc.getDocumentUri());
				} catch (InvalidSPDXAnalysisException e) {
					out.println("Error printing package: "+e.getMessage());
				}
			}
		});
		allFiles.forEach((SpdxFile file) -> {
			if (!alreadyPrinted.contains(file)) {
				try {
					printFile(file, out, constants);
				} catch (InvalidSPDXAnalysisException e) {
					out.println("Error printing file: "+e.getMessage());
				}
			}
		});
		allSnippets.sorted().forEach((SpdxSnippet snippet) -> {
			if (!alreadyPrinted.contains(snippet)) {
				try {
					printSnippet(snippet, out, constants);
				} catch (InvalidSPDXAnalysisException e) {
					out.println("Error printing package: "+e.getMessage());
				}
			}
		});
		
		// Extracted license infos
		println(out, "");
		Collection<ExtractedLicenseInfo> extractedLicenseInfos = doc.getExtractedLicenseInfos();
		if (!extractedLicenseInfos.isEmpty()) {
			println(out, constants.getProperty("LICENSE_INFO_HEADER"));
			for (ExtractedLicenseInfo extractedLicenseInfo:extractedLicenseInfos) {
				printLicense(extractedLicenseInfo, out, constants);
			}
		}
	}

	/**
	 * @param spdxSnippet
	 * @param out
	 * @param constants
	 * @throws InvalidSPDXAnalysisException 
	 */
	private static void printSnippet(SpdxSnippet spdxSnippet, PrintWriter out,
			Properties constants) throws InvalidSPDXAnalysisException {
		println(out, constants.getProperty("SNIPPET_HEADER"));
		// NOTE: We can't call the print element properties since the order for tag/value is different for snippets
		println(out, constants.getProperty("PROP_SNIPPET_SPDX_ID") + spdxSnippet.getId());
		if (spdxSnippet.getSnippetFromFile() != null) {
			println(out, constants.getProperty("PROP_SNIPPET_FROM_FILE_ID") + 
					spdxSnippet.getSnippetFromFile().getId());
		}
		if (spdxSnippet.getByteRange() != null) {
			println(out, constants.getProperty("PROP_SNIPPET_BYTE_RANGE") + 
					formatPointerRange(spdxSnippet.getByteRange()));
		}
		if (spdxSnippet.getLineRange() != null) {
			println(out, constants.getProperty("PROP_SNIPPET_LINE_RANGE") +
					formatPointerRange(spdxSnippet.getLineRange().get()));
		}
		if (spdxSnippet.getLicenseConcluded() != null) {
			println(out, constants.getProperty("PROP_SNIPPET_CONCLUDED_LICENSE") +
					spdxSnippet.getLicenseConcluded());
		}
		if (spdxSnippet.getLicenseInfoFromFiles() != null) {
			for (AnyLicenseInfo seenLicense:spdxSnippet.getLicenseInfoFromFiles()) {
				println(out, constants.getProperty("PROP_SNIPPET_SEEN_LICENSE") +
						seenLicense);
			}
		}
		if (spdxSnippet.getLicenseComments().isPresent() && !spdxSnippet.getLicenseComments().get().trim().isEmpty()) {
			println(out, constants.getProperty("PROP_SNIPPET_LIC_COMMENTS") +
					spdxSnippet.getLicenseComments());
		}
		if (spdxSnippet.getCopyrightText() != null && !spdxSnippet.getCopyrightText().trim().isEmpty()) {
			println(out, constants.getProperty("PROP_SNIPPET_COPYRIGHT") +
					spdxSnippet.getCopyrightText());
		}
		if (spdxSnippet.getComment().isPresent() && !spdxSnippet.getComment().get().trim().isEmpty()) {
			println(out, constants.getProperty("PROP_SNIPPET_COMMENT") +
					spdxSnippet.getComment());
		}
		if (spdxSnippet.getName().isPresent() && !spdxSnippet.getName().get().trim().isEmpty()) {
			println(out, constants.getProperty("PROP_SNIPPET_NAME") +
					spdxSnippet.getName());	
		}
		println(out, "");
	}

	/**
	 * Format a start end pointer into a numeric range
	 * @param pointer
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	private static String formatPointerRange(StartEndPointer pointer) throws InvalidSPDXAnalysisException {
		String start = "[MISSING]";
		String end = "[MISSING]";
		if (pointer.getStartPointer() != null) {
			if (pointer.getStartPointer() instanceof ByteOffsetPointer) {
				start = String.valueOf(((ByteOffsetPointer)(pointer.getStartPointer())).getOffset());
			} else if (pointer.getStartPointer() instanceof LineCharPointer) {
				start = String.valueOf(((LineCharPointer)(pointer.getStartPointer())).getLineNumber());
			}
		}
		if (pointer.getEndPointer() != null) {
			if (pointer.getEndPointer() instanceof ByteOffsetPointer) {
				end = String.valueOf(((ByteOffsetPointer)(pointer.getEndPointer())).getOffset());
			} else if (pointer.getStartPointer() instanceof LineCharPointer) {
				end = String.valueOf(((LineCharPointer)(pointer.getEndPointer())).getLineNumber());
			}
		}
		return start + ":" + end;
	}

	/**
	 * @param externalDocumentRef
	 * @param out
	 * @param constants
	 * @throws InvalidSPDXAnalysisException 
	 */
	private static void printExternalDocumentRef(
			ExternalDocumentRef externalDocumentRef, PrintWriter out,
			Properties constants) throws InvalidSPDXAnalysisException {
		String uri = externalDocumentRef.getSpdxDocumentNamespace();
		if (uri == null || uri.isEmpty()) {
			uri = "[UNSPECIFIED]";
		}
		String sha1 = "[UNSPECIFIED]";
		Optional<Checksum> checksum = externalDocumentRef.getChecksum();
		if (checksum.isPresent() && checksum.get().getValue() != null && !checksum.get().getValue().isEmpty()) {
			sha1 = checksum.get().getValue();
		}
		String id = externalDocumentRef.getId();
		if (id == null || id.isEmpty()) {
			id = "[UNSPECIFIED]";
		}
		println(out, constants.getProperty("PROP_EXTERNAL_DOC_URI") +
					id + " " + uri + " SHA1: " + sha1);	
	}

	/**
	 * @param doc
	 * @param out
	 * @param constants
	 * @param string
	 * @throws InvalidSPDXAnalysisException 
	 */
	private static void printElementProperties(SpdxElement element,
			PrintWriter out, Properties constants, String nameProperty,
			String commentProperty) throws InvalidSPDXAnalysisException {
		if (element.getName().isPresent() && !element.getName().get().isEmpty()) {
			println(out, constants.getProperty(nameProperty) + element.getName().get());
		}
		if (element.getId() != null && !element.getId().isEmpty()) {
			println(out, constants.getProperty("PROP_ELEMENT_ID") + element.getId());
		}
		if (element.getComment().isPresent() && !element.getComment().get().isEmpty()) {
			println(out, constants.getProperty(commentProperty)
					+ constants.getProperty("PROP_BEGIN_TEXT")
					+ element.getComment().get()
					+ constants.getProperty("PROP_END_TEXT"));
		}
	}

	private static void printElementAnnotationsRelationships(SpdxElement element,
			PrintWriter out, Properties constants, String nameProperty,
			String commentProperty) throws InvalidSPDXAnalysisException {
		Collection<Annotation> annotations = element.getAnnotations();
		if (!annotations.isEmpty()) {
			println(out, constants.getProperty("ANNOTATION_HEADER"));
			for (Annotation annotation:annotations) {
				printAnnotation(annotation, element.getId(), out, constants);
			}
		}
		Collection<Relationship> relationships = element.getRelationships();
		if (!relationships.isEmpty()) {
			println(out, constants.getProperty("RELATIONSHIP_HEADER"));
			for (Relationship relationship:relationships) {
				printRelationship(relationship, element.getId(), out, constants);
			}
		}
	}
	/**
	 * @param relationship
	 * @param out
	 * @param constants
	 * @throws InvalidSPDXAnalysisException 
	 */
	private static void printRelationship(Relationship relationship,
			String elementId, PrintWriter out, Properties constants) throws InvalidSPDXAnalysisException {
		String relatedElementId = "[MISSING]";
		if (relationship.getRelatedSpdxElement().isPresent()) {
			relatedElementId = relationship.getRelatedSpdxElement().get().getId();
		}
		out.println(constants.getProperty("PROP_RELATIONSHIP")+
				elementId+" " +
				relationship.getRelationshipType().toString()+
				" " + relatedElementId);
	}

	/**
	 * @param annotation
	 * @param out
	 * @param constants
	 * @throws InvalidSPDXAnalysisException 
	 */
	private static void printAnnotation(Annotation annotation, String id,
			PrintWriter out, Properties constants) throws InvalidSPDXAnalysisException {
		out.println(constants.getProperty("PROP_ANNOTATOR")+annotation.getAnnotator());
		out.println(constants.getProperty("PROP_ANNOTATION_DATE")+annotation.getAnnotationDate());
		out.println(constants.getProperty("PROP_ANNOTATION_COMMENT")
				+ constants.getProperty("PROP_BEGIN_TEXT")
				+ annotation.getComment()
				+ constants.getProperty("PROP_END_TEXT"));
		out.println(constants.getProperty("PROP_ANNOTATION_TYPE")+
				(annotation.getAnnotationType().toString()));
		out.println(constants.getProperty("PROP_ANNOTATION_ID")+id);
	}

	/**
	 * @param license
	 * @throws InvalidSPDXAnalysisException 
	 */
	private static void printLicense(ExtractedLicenseInfo license,
			PrintWriter out, Properties constants) throws InvalidSPDXAnalysisException {
		// id
		if (license.getLicenseId() != null && !license.getLicenseId().isEmpty()) {
			println(out,
					constants.getProperty("PROP_LICENSE_ID") + license.getLicenseId());
		}
		if (license.getExtractedText() != null && !license.getExtractedText().isEmpty()) {
			println(out, constants.getProperty("PROP_EXTRACTED_TEXT") 
					+ constants.getProperty("PROP_BEGIN_TEXT")
					+ license.getExtractedText() + constants.getProperty("PROP_END_TEXT"));
		}
		if (license.getName() != null && !license.getName().isEmpty()) {
			println(out, constants.getProperty("PROP_LICENSE_NAME")+license.getName());
		}
		Collection<String> seeAlsos = license.getSeeAlso();
		if (!seeAlsos.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (String seeAlso:seeAlsos) {
				if (first) {
					sb.append(seeAlso);
					first = false;
				} else {
					sb.append(", ");
					sb.append(seeAlso);
				}
			}
			println(out, constants.getProperty("PROP_SOURCE_URLS")+sb.toString());
		}
		if (license.getSeeAlso() != null) {
            if (license.getComment() != null && !license.getComment().isEmpty()) {
            	println(out, constants.getProperty("PROP_LICENSE_COMMENT")
            			+ constants.getProperty("PROP_BEGIN_TEXT")
            			+ license.getComment()
            			+ constants.getProperty("PROP_END_TEXT"));
            }
        }
		println(out, "");
	}

	/**
	 * @param spdxPackage
	 * @throws InvalidSPDXAnalysisException
	 */
	private static void printPackage(SpdxPackage pkg, PrintWriter out,
			Properties constants, List<SpdxFile> allFiles,
			String documentNamespace) throws InvalidSPDXAnalysisException {
		println(out, constants.getProperty("PACKAGE_INFO_HEADER"));
		printElementProperties(pkg, out, constants,"PROP_PACKAGE_DECLARED_NAME",
				"PROP_PACKAGE_COMMENT");
		// Version
		if (pkg.getVersionInfo().isPresent()) {
			println(out,
					constants.getProperty("PROP_PACKAGE_VERSION_INFO")
							+ pkg.getVersionInfo().get());
		}
		// File name
		if (pkg.getPackageFileName().isPresent()) {
			println(out,
					constants.getProperty("PROP_PACKAGE_FILE_NAME")
							+ pkg.getPackageFileName().get());
		}
		// Supplier
		if (pkg.getSupplier().isPresent()) {
			println(out,
					constants.getProperty("PROP_PACKAGE_SUPPLIER")
							+ pkg.getSupplier().get());
		}
		// Originator
		if (pkg.getOriginator().isPresent()) {
			println(out,
					constants.getProperty("PROP_PACKAGE_ORIGINATOR")
							+ pkg.getOriginator().get());
		}
		// Download location
		if (pkg.getDownloadLocation().isPresent()) {
			println(out,
					constants.getProperty("PROP_PACKAGE_DOWNLOAD_URL")
							+ pkg.getDownloadLocation().get());
		}
		// package verification code
        if (pkg.getPackageVerificationCode().isPresent()
                && pkg.getPackageVerificationCode().get().getValue() != null
                && !pkg.getPackageVerificationCode().get().getValue().isEmpty()) {
          String code = constants.getProperty("PROP_PACKAGE_VERIFICATION_CODE") + pkg.getPackageVerificationCode().get().getValue();
          Collection<String> excludedFiles = pkg.getPackageVerificationCode().get().getExcludedFileNames();
          if (!excludedFiles.isEmpty()) {
              StringBuilder excludedFilesBuilder = new StringBuilder("(");
                
              for (String excludedFile : excludedFiles) {
                if(excludedFilesBuilder.length() > 0){
                    excludedFilesBuilder.append(", ");
                }
                
                excludedFilesBuilder.append(excludedFile);
              }
              
              excludedFilesBuilder.append(')');
              code += excludedFilesBuilder.toString();
          }                    
          println(out, code);
        }
		// Checksums
		Collection<Checksum> checksums = pkg.getChecksums();
		if (!checksums.isEmpty()) {
			for (Checksum checksum:checksums) {
				printChecksum(checksum, out, constants, "PROP_PACKAGE_CHECKSUM");
			}
		}
		// Home page
		if (pkg.getHomepage().isPresent()) {
			println(out, constants.getProperty("PROP_PACKAGE_HOMEPAGE_URL") + 
					pkg.getHomepage().get());
		}
		// Source info
		if (pkg.getSourceInfo().isPresent()) {
			println(out, 
					constants.getProperty("PROP_PACKAGE_SOURCE_INFO")
							+ constants.getProperty("PROP_BEGIN_TEXT") 
							+ pkg.getSourceInfo().get()
							+ constants.getProperty("PROP_END_TEXT"));
		}
		// concluded license
		if (pkg.getLicenseConcluded() != null) {
			println(out, constants.getProperty("PROP_PACKAGE_CONCLUDED_LICENSE")
					+ pkg.getLicenseConcluded());
		}
		// License information from files
		Collection<AnyLicenseInfo> licenses = pkg.getLicenseInfoFromFiles();
		if (!licenses.isEmpty()) {
			println(out, constants.getProperty("LICENSE_FROM_FILES_INFO_HEADER"));
			for (AnyLicenseInfo license:licenses) {
				println(out,
						constants
								.getProperty("PROP_PACKAGE_LICENSE_INFO_FROM_FILES")
								+ license.toString());
			}
		}
		// Declared licenses
		if (pkg.getLicenseDeclared() != null) {
			println(out, constants.getProperty("PROP_PACKAGE_DECLARED_LICENSE")
					+ pkg.getLicenseDeclared());
		}
		if (pkg.getLicenseComments().isPresent()) {
			println(out, constants.getProperty("PROP_PACKAGE_LICENSE_COMMENT")
					+ constants.getProperty("PROP_BEGIN_TEXT") 
					+ pkg.getLicenseComments().get() + 
					constants.getProperty("PROP_END_TEXT"));
		}
		// Declared copyright
		if (pkg.getCopyrightText() != null
				&& !pkg.getCopyrightText().isEmpty()) {
			println(out, constants.getProperty("PROP_PACKAGE_DECLARED_COPYRIGHT")
					+ constants.getProperty("PROP_BEGIN_TEXT") 
					+ pkg.getCopyrightText() + constants.getProperty("PROP_END_TEXT"));
		}
		// Short description
		if (pkg.getSummary().isPresent()) {
			println(out, constants.getProperty("PROP_PACKAGE_SHORT_DESC")
					+ constants.getProperty("PROP_BEGIN_TEXT") 
					+ pkg.getSummary().get() + constants.getProperty("PROP_END_TEXT"));
		}
		// Description
		if (pkg.getDescription().isPresent()) {
			println(out, constants.getProperty("PROP_PACKAGE_DESCRIPTION")
					+ constants.getProperty("PROP_BEGIN_TEXT") 
					+ pkg.getDescription().get() + constants.getProperty("PROP_END_TEXT"));
		}
		// Attribution text
		if (!pkg.getAttributionText().isEmpty()) {
			pkg.getAttributionText().forEach(s -> {
				println(out, constants.getProperty("PROP_PACKAGE_ATTRIBUTION_TEXT")
						+ constants.getProperty("PROP_BEGIN_TEXT") 
						+ s + constants.getProperty("PROP_END_TEXT"));
			});
			
		}
		// External Refs
		Collection<ExternalRef> externalRefs = pkg.getExternalRefs();
		if (!externalRefs.isEmpty()) {
			for (ExternalRef externalRef:externalRefs) {
				printExternalRef(out, constants, externalRef, documentNamespace);
			}
		}
		printElementAnnotationsRelationships(pkg, out, constants,"PROP_PACKAGE_DECLARED_NAME",
				"PROP_PACKAGE_COMMENT");
		// Files
		if (!pkg.isFilesAnalyzed()) {
			// Only print if not the default
			println(out, constants.getProperty("PROP_PACKAGE_FILES_ANALYZED") + "false");
		}
		Collection<SpdxFile> files = pkg.getFiles();
		if (!files.isEmpty()) {
                    /* Add files to a List */
                    /* Sort the SPDX files before printout */
                    List<SpdxFile> sortedFileList = new ArrayList<>(pkg.getFiles());
                    Collections.sort(sortedFileList);                    
                    println(out, "");
			println(out, constants.getProperty("FILE_INFO_HEADER"));
                        /* Print out sorted files */
			for (SpdxFile file : sortedFileList) {
				printFile(file, out, constants);
				allFiles.remove(file);
				println(out, "");
			}
		} else {
			println(out, "");
		}
	}

	/**
	 * Print a package ExternalRef to out
	 * @param out
	 * @param constants
	 * @param externalRef
	 * @param docNamespace
	 * @throws InvalidSPDXAnalysisException 
	 */
	private static void printExternalRef(PrintWriter out, Properties constants,
			ExternalRef externalRef, String docNamespace) throws InvalidSPDXAnalysisException {
		String category = null;
		if (externalRef.getReferenceCategory() == null) {
			category = "OTHER";
		} else {
			category = externalRef.getReferenceCategory().toString();
		}
		String referenceType = null;
		if (externalRef.getReferenceType() == null) {
			referenceType = "[MISSING]";
		} else {
			try {
				try {
					referenceType = ListedReferenceTypes.getListedReferenceTypes().getListedReferenceName(new URI(externalRef.getReferenceType().getIndividualURI()));
					if (referenceType == null) {
						referenceType = externalRef.getReferenceType().getIndividualURI();
						if (referenceType.startsWith(docNamespace + "#")) {
							referenceType = referenceType.substring(docNamespace.length()+1);
						}
					}
				} catch (URISyntaxException e) {
					referenceType = "[Invalid URI Syntax]";
				}
			} catch (InvalidSPDXAnalysisException e) {
				referenceType = "[ERROR: "+e.getMessage()+"]";
			}
		}
		String referenceLocator = externalRef.getReferenceLocator();
		if (referenceLocator == null) {
			referenceLocator = "[MISSING]";
		}
		println(out, constants.getProperty("PROP_EXTERNAL_REFERENCE") + 
				category + " " + referenceType + " " + referenceLocator);
		if (externalRef.getComment() != null) {
			println(out, constants.getProperty("PROP_EXTERNAL_REFERENCE_COMMENT") + externalRef.getComment());
		}
	}

	/**
	 * @param checksum
	 * @param out
	 * @param constants
	 * @param checksumProperty
	 * @throws InvalidSPDXAnalysisException 
	 */
	private static void printChecksum(Checksum checksum, PrintWriter out,
			Properties constants, String checksumProperty) throws InvalidSPDXAnalysisException {
		out.println(constants.getProperty(checksumProperty)
				+ checksum.getAlgorithm().toString()
				+ " " + checksum.getValue());
	}

	/**
	 * @param file
	 * @throws InvalidSPDXAnalysisException 
	 */
	private static void printFile(SpdxFile file, PrintWriter out,
			Properties constants) throws InvalidSPDXAnalysisException {
		printElementProperties(file, out, constants, "PROP_FILE_NAME", 
				"PROP_FILE_COMMENT");
		// type
		Collection<FileType> fileTypes = file.getFileTypes();
		if (!fileTypes.isEmpty()) {
			for (FileType fileType:fileTypes) {
				println(out, constants.getProperty("PROP_FILE_TYPE") + fileType.toString());
			}
		}
		Collection<Checksum> checksums = file.getChecksums();
		if (!checksums.isEmpty()) {
			for (Checksum checksum:checksums) {
				printChecksum(checksum, out, constants, "PROP_FILE_CHECKSUM");
			}
		}
		// concluded license
		if (file.getLicenseConcluded() != null) {
			println(out, constants.getProperty("PROP_FILE_LICENSE")
					+ file.getLicenseConcluded().toString());
		}
		// License info in file
		Collection<AnyLicenseInfo> anyLicenseInfosFromFiles = file.getLicenseInfoFromFiles();
		if (!anyLicenseInfosFromFiles.isEmpty()) {
			// print(out, "\tLicense information from file: ");
			// print(out, file.getSeenLicenses()[0].toString());
			for (AnyLicenseInfo license:anyLicenseInfosFromFiles) {
				println(out, constants.getProperty("PROP_FILE_SEEN_LICENSE")
						+ license.toString());
			}
		}
		// license comments
		if (file.getLicenseComments().isPresent()) {
			println(out,
					constants.getProperty("PROP_FILE_LIC_COMMENTS")
							+ file.getLicenseComments().get());
		}
		// file copyright
		if (file.getCopyrightText() != null && !file.getCopyrightText().isEmpty()) {
			println(out, constants.getProperty("PROP_FILE_COPYRIGHT") 
					+ constants.getProperty("PROP_BEGIN_TEXT")
					+ file.getCopyrightText() + constants.getProperty("PROP_END_TEXT"));
		}
		// File notice
		if (file.getNoticeText().isPresent()) {
			println(out, constants.getProperty("PROP_FILE_NOTICE_TEXT") + 
					constants.getProperty("PROP_BEGIN_TEXT") +
					file.getNoticeText().get() + 
					constants.getProperty("PROP_END_TEXT"));
		}
		// file attribution text
		if (!file.getAttributionText().isEmpty()) {
			file.getAttributionText().forEach(s -> {
				println(out, constants.getProperty("PROP_FILE_ATTRIBUTION_TEXT")
						+ constants.getProperty("PROP_BEGIN_TEXT") 
						+ s + constants.getProperty("PROP_END_TEXT"));
			});
		}
		// file contributors
		Collection<String> fileContributors = file.getFileContributors();
		if (!fileContributors.isEmpty()) {
			for (String fileContributor:fileContributors) {
				println(out, constants.getProperty("PROP_FILE_CONTRIBUTOR")+
						fileContributor);
			}
		}
		printElementAnnotationsRelationships(file, out, constants, "PROP_FILE_NAME", 
				"PROP_FILE_COMMENT");
	}

	private static void println(PrintWriter out, String output) {
		if (out != null) {
			out.println(output);
		} else {
			System.out.println(output);
		}
	}

	public static Properties getTextFromProperties(final String path)
			throws IOException {
		InputStream is = null;
		Properties prop = new Properties();
		try {
			is = SpdxViewer.class.getClassLoader().getResourceAsStream(path);
			prop.load(is);
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (Throwable e) {
//				logger.warn("Unable to close properties file.");
			}
		}
		return prop;
	}

}