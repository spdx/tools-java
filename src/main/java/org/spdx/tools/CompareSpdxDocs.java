/**
 * Copyright (c) 2013 Source Auditor Inc.
 * Copyright (c) 2013 Black Duck Software Inc.
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
package org.spdx.tools;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.SpdxDocument;
import org.spdx.spreadsheetstore.SpreadsheetException;
import org.spdx.tools.compare.MultiDocumentSpreadsheet;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

/**
 * Compares multiple SPDX documents and stores the results in a spreadsheet
 * Usage: CompareSpdxDoc output.xls doc1 doc2 doc3 ... docN
 * where output.xls is a file name for the output spreadsheet file
 * and docX are SPDX document files to compare.  Document files can be either in RDF/XML  or tag/value format
 *
 * @author Gary O'Neall
 *
 */
public class CompareSpdxDocs {
	static final int MIN_ARGS = 3;
	static final int MAX_ARGS = 14;
	static final int ERROR_STATUS = 1;
	static final Logger logger = LoggerFactory.getLogger(CompareSpdxDocs.class);


	/**
	 * @param args args[0] is the output Excel file name, all other args are SPDX document file names
	 */
	public static void main(String[] args) {
		if (args.length < MIN_ARGS) {
			System.out.println("Insufficient arguments");
			usage();
			System.exit(ERROR_STATUS);
		}
		if (args.length > MAX_ARGS) {
			System.out.println("Too many SPDX documents specified.  Must be less than "+String.valueOf(MAX_ARGS-1)+" document filenames");
			usage();
			System.exit(ERROR_STATUS);
		}
		try {
			onlineFunction(args);
		} catch (OnlineToolException e){
			System.out.println(e.getMessage());
			System.exit(ERROR_STATUS);
		}
	}

	/**
	 *
	 * @param args args[0] is the output Excel file name, all other args are SPDX document file names
	 * @throws OnlineToolException Exception caught by JPype and displayed to the user
	 */
	public static void onlineFunction(String[] args) throws OnlineToolException {
		// Arguments length( 14>=args length>=3 ) will checked in the Python Code
		File outputFile = new File(args[0]);
		// Output File name will be checked in the Python code for no clash, but if still found
		if (outputFile.exists()) {
			throw new OnlineToolException("Output file "+args[0]+" already exists. Change the name of the result file.");
		}
		List<SpdxDocument> compareDocs = new ArrayList<>();
		List<String> docNames = new ArrayList<>();
		List<List<String>> verificationErrors = new ArrayList<>();
		
		for (int i = 1; i < args.length; i++) {
			try {
				SpdxDocument doc = SpdxToolsHelper.deserializeDocument(new File(args[i]));
				compareDocs.add(doc);
				List<String> warnings = doc.verify();
				if (!warnings.isEmpty()) {
					System.out.println("Verification errors were found in "+args[i].trim()+".  See verification errors sheet for details.");
				}
				docNames.add(convertDocName(args[i]));
				verificationErrors.add(warnings);
			} catch (InvalidSPDXAnalysisException | IOException | InvalidFileNameException e) {
				throw new OnlineToolException("Error opening SPDX document "+args[i]+": "+e.getMessage());
			}
		}
		MultiDocumentSpreadsheet outSheet = null;
		try {
			outSheet = new MultiDocumentSpreadsheet(outputFile, true, false);
			outSheet.importVerificationErrors(verificationErrors, docNames);
			SpdxComparer comparer = new SpdxComparer();
			comparer.compare(compareDocs);
			outSheet.importCompareResults(comparer, docNames);
		} catch (SpreadsheetException e) {
			throw new OnlineToolException("Unable to create output spreadsheet: "+e.getMessage());
		} catch (InvalidSPDXAnalysisException e) {
			throw new OnlineToolException("Invalid SPDX analysis: "+e.getMessage());
		} catch (SpdxCompareException e) {
			throw new OnlineToolException("Error comparing SPDX documents: "+e.getMessage());
		} finally {
			if (outSheet != null) {
				try {
					outSheet.close();
				} catch (SpreadsheetException e) {
					logger.warn("Warning - error closing spreadsheet: "+e.getMessage());
				}
			}
		}
	}
	
	/**
	 * Converts a file path or URL to a shorter document name
	 * @param docPath
	 * @return
	 */
	protected static String convertDocName(String docPath) {
		if (docPath.contains(File.separator)) {
			File docFile = new File(docPath);
			return docFile.getName();
		} else {
			try {
				URI uri = new URI(docPath);
				String path = uri.getPath();
				return path;
			} catch (URISyntaxException e) {
				return docPath;
			}
		}
	}

	/**
	 *
	 */
	private static void usage() {
		System.out.println("Usage: CompareMultipleSpdxDoc output.xls doc1 doc2 ... docN");
		System.out.println("where output.xls is a file name for the output spreadsheet file");
		System.out.println("and doc1 through docN are file names of valid SPDX documents ");
		System.out.println("in either tag/value or RDF/XML format");
	}

}
