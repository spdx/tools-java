/**
 * SPDX-FileCopyrightText: Copyright (c) 2021 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.examples;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v2.SpdxConstantsCompatV2;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.library.model.v2.SpdxElement;
import org.spdx.storage.ISerializableModelStore;
import org.spdx.storage.simple.InMemSpdxStore;

/**
 * This example demonstrate opening an existing SPDX spec version 2.X document and accessing it.  The format
 * for this example is assumed to be JSON (e.g. the output of the SimpleSpdxDocumentV2Compat example).
 * Different format can be used by using the associated store rather than the spdx-jackson store
 * (e.g. spdx-spreadsheet-store, spdx-tagvalue-store, or the spdx-rdf-store).
 * 
 * This example depends on the Spdx-Java-Library and the spdx-java-jackson store libraries
 * 
 * @author Gary O'Neall
 *
 */
public class ExistingSpdxDocumentV2Compat {

	/**
	 * @param args args[0] is the file path containing the SPDX document
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			usage();
			System.exit(1);
		}
		File inputFile = new File(args[0]);
		if (!inputFile.exists()) {
			System.out.println("Input file does not exist: "+args[0]);
			System.exit(1);
		}
		
		/*
		 * First thing we need is a store deseralize the SPDX document into.
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
		 * The ModelCopyManager is used when using more than one Model Store.  The Listed Licenses uses
		 * it's own model store, so we will need a ModelCopyManager to manage the copying of the listed
		 * license information over to the document model store
		 */
		ModelCopyManager copyManager = new ModelCopyManager();
		// Let's deseralize the document
		try (InputStream stream = new FileInputStream(inputFile)) {
			modelStore.deSerialize(stream, false);
			
		} catch (FileNotFoundException e1) {
			System.out.println("Input file does not exist: "+args[0]);
			System.exit(1);
		} catch (IOException e1) {
			System.out.println("I/O error reading input file: "+args[0]);
			System.exit(1);
		} catch (InvalidSPDXAnalysisException e) {
			System.out.println("The SPDX document is not valid: "+e.getMessage());
			System.exit(1);
		}
		// Now that the document is deserialized, we can access it using the SpdxModelFactory
		try {
			// To find all the SPDX documents in the model store, use the getObjects method from the
			// SpdxModelFactory passing in the SpdxDocument type
			// When using the factory method, we have to type cast the result
			@SuppressWarnings("unchecked")
			List<SpdxDocument> allDocs = (List<SpdxDocument>) SpdxModelFactory.getSpdxObjects(modelStore, copyManager, 
					SpdxConstantsCompatV2.CLASS_SPDX_DOCUMENT, null, null)
					.collect(Collectors.toList());
			SpdxDocument document = allDocs.get(0);
			String documentUri = document.getDocumentUri();
			// If you know the document URI, you can simply create an SPDX document using the followint constructor
			SpdxDocument document2 = new SpdxDocument(modelStore, documentUri, copyManager, false);
			// Note that all class objects in the Spdx Java Library follow the same pattern - 
			// to access any existing object in the store, simply create the object passing in 
			// the document URI, model store and the ID for the object
			// Since the 2 documents are just references to the same object, they will always be equivalent
			if (!document.equivalent(document2)) {
				System.out.println("Oops - these 2 documents should be the same");
				System.exit(1);
			}
			// Let's see find what the document is describing
			Collection<SpdxElement> described = document.getDocumentDescribes();
			for (SpdxElement des:described) {
				System.out.println("The document described ID "+des.getId());
			}
			System.exit(0);
		} catch (InvalidSPDXAnalysisException e) {
			System.out.println("Unexpected error reading SPDX document: "+e.getMessage());
			System.exit(1);
		}
	}
	
	private static void usage() {
		System.out.println("Usage: ExistingSpxDocument inputFilePath");
	}

}
