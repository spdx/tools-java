/**
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2021 Source Auditor Inc.
 */
package org.spdx.examples;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.LicenseInfoFactory;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.model.v2.Relationship;
import org.spdx.library.model.v2.SpdxConstantsCompatV2;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.library.model.v2.SpdxPackage;
import org.spdx.library.model.v2.enumerations.RelationshipType;
import org.spdx.library.model.v2.license.AnyLicenseInfo;
import org.spdx.storage.IModelStore.IdType;
import org.spdx.storage.ISerializableModelStore;
import org.spdx.storage.simple.InMemSpdxStore;

/**
 * This example demonstrate programmatically creating an SPDX spec version 2.X document, adding document, files
 * and saving the document in a JSON file format
 * 
 * This example depends on the Spdx-Java-Library and the spdx-java-jackson store libraries
 * 
 * @author Gary O'Neall
 *
 */
public class SimpleSpdxDocumentV2Compat {

	/**
	 * @param args args[0] is the file path to store the resultant JSON file
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			usage();
			System.exit(1);
		}
		File outFile = new File(args[0]);
		if (outFile.exists()) {
			System.out.println("Output file already exists: "+args[0]);
			System.exit(1);
		}
		
		/*
		 * First thing we need is a store to store document as build the SPDX document
		 * We'll chose the MultiFormatStore since it supports serializing to JSON files
		 * It takes an underlying model store as the first parameter - the inMemSpdxStore is a simple
		 * built in store included in the Spdx-Java-Library.  The second parameter is the format
		 * to use when serializing or deserializing
		 */
		ISerializableModelStore modelStore = new MultiFormatStore(new InMemSpdxStore(), Format.JSON_PRETTY);
		/*
		 * There are several other choices which can be made for a model store - most of which 
		 * are in separate libraries that need to be included as dependencies.  Below are a few examples:
		 * IModelStore modelStore = new InMemSpdxStore(); - the simplest store, but does not support serializing or deserializing
		 * ISerializableModelStore modelStore = new TagValueStore(new InMemSpdxStore()); Supports serializing and deserializing SPDX tag/value files
		 * ISerializableModelStore modelStore = new RdfStore(); Supports serializing and deserializing various RDF formats (e.g. RDF/XML, RDF/Turtle)
		 * ISerializableModelStore modelStore = new SpreadsheetStore(new InMemSpdxStore()); Supports serializing and deserializing .XLS or .XLSX spreadsheets
		 */
		
		/*
		 * The documentUri is a unique identifier for the SPDX documents
		 */
		String documentUri = "https://org.spdx.examples/spdx/doc/b7490f5a-b6ac-45e7-9971-4c27f1db97f7";
		/*
		 * The ModelCopyManager is used when using more than one Model Store.  The Listed Licenses uses
		 * it's own model store, so we will need a ModelCopyManager to manage the copying of the listed
		 * license information over to the document model store
		 */
		ModelCopyManager copyManager = new ModelCopyManager();
		try {
			// Time to create the document
			SpdxDocument document = new SpdxDocument(modelStore, documentUri, copyManager, false);
			// Let's add a few required fields to the document
			SimpleDateFormat dateFormat = new SimpleDateFormat(SpdxConstantsCompatV2.SPDX_DATE_FORMAT);
			String creationDate = dateFormat.format(new Date());
			document.setCreationInfo(document.createCreationInfo(
					Arrays.asList(new String[] {"Tool: Simple SPDX Document Example"}),
					creationDate));
			/*
			 * Now that we have the initial model object 'document' created, we can use the helper
			 * methods from that cleass to create any other model elements such as the CreationInfo
			 * above.  These helper functions will use the same Document URI, Model Store and Model Copy Manager
			 * as the document element.
			 */
			AnyLicenseInfo dataLicense = LicenseInfoFactory.parseSPDXLicenseStringCompatV2("CC0-1.0");
			/*
			 * Note that by passing in the modelStore and documentUri, the parsed license information is stored
			 * in the same model store we are using for the document
			 */
			document.setDataLicense(dataLicense);
			document.setName("SPDX Example Document");
			document.setSpecVersion("SPDX-2.2");
			
			// Now that we have the basic document information filled in, let's create a package
			AnyLicenseInfo pkgConcludedLicense = LicenseInfoFactory.parseSPDXLicenseStringCompatV2("Apache-2.0 AND MIT");
			AnyLicenseInfo pkgDeclaredLicense = LicenseInfoFactory.parseSPDXLicenseStringCompatV2("Apache-2.0");
			String pkgId = modelStore.getNextId(IdType.SpdxId);
			// The ID's used for SPDX elements must be unique.  Calling the model store getNextId function is a
			// convenient and safe method to make sure you have a correctly formatted and unique ID
			SpdxPackage pkg = document.createPackage(pkgId, "Example Package Name", pkgConcludedLicense,
					"Copyright example.org", pkgDeclaredLicense)
					.setFilesAnalyzed(false)  // Default is true and we don't want to add all the required fields
					.setComment("This package is used as an example in creating an SPDX document from scratch")
					.setDownloadLocation("NOASSERTION")
					.build();
			/*
			 * Note that many of the more complex elements use a builder pattern as in the
			 * example above
			 */
					
			/* Let's add the package relationships to the document
			 * This step
			*/
			// This step will add a relationship between document and pkg as "DESCRIBES".
			document.getDocumentDescribes().add(pkg);	
			// Let's create another package
			pkgId = modelStore.getNextId(IdType.SpdxId);
			SpdxPackage childPkg = document.createPackage(pkgId, "Child Example Package Name", pkgConcludedLicense,
					"Copyright example.org", pkgDeclaredLicense)
					.setFilesAnalyzed(false)  // Default is true and we don't want to add all the required fields
					.setComment("This package is used as an example in creating an SPDX document from scratch")
					.setDownloadLocation("NOASSERTION")
					.build();
			// Then create a DEPEND_ON relation by relationship factory
			Relationship relationship = pkg.createRelationship(childPkg, RelationshipType.DEPENDS_ON, "");
			pkg.addRelationship(relationship);

			// That's for creating the simple document.  Let's verify to make sure nothing is out of spec
			List<String> warnings = document.verify();
			if (warnings.size() > 0) {
				System.out.println("The document has the following warnings:");
				for (String warning:warnings) {
					System.out.print("\t");
					System.out.println(warning);
				}
			}
			// Last step is to serialize
			try (OutputStream outputStream = new FileOutputStream(outFile)) {
				modelStore.serialize(outputStream);
			}
			System.out.println("Example document written to "+args[0]);
			System.exit(0);
		} catch (InvalidSPDXAnalysisException e) {
			System.out.println("Unexpected error creating SPDX document: "+e.getMessage());
			System.exit(1);
		} catch (IOException e) {
			System.out.println("I/O error writing output JSON file");
			System.exit(1);
		}

		
	}
	
	private static void usage() {
		System.out.println("Usage: SimpleSpxDocument outputFilePath");
	}

}
