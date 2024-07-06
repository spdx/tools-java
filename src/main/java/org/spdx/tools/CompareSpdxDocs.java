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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.spreadsheetstore.SpreadsheetException;
import org.spdx.storage.IModelStore;
import org.spdx.storage.simple.InMemSpdxStore;
import org.spdx.tools.compare.MultiDocumentSpreadsheet;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

/**
 * Compares multiple SPDX documents and stores the results in a spreadsheet
 * Usage: CompareSpdxDoc output.xlsx doc1 doc2 doc3 ... docN
 * where output.xls is a file name for the output spreadsheet file
 * and docX are SPDX document files to compare or directories containing SPDX documents.  
 * Document files can be either in RDF/XML  or tag/value format
 *
 * @author Gary O'Neall
 *
 */
public class CompareSpdxDocs {
	static final int MIN_ARGS = 2;
	static final int MAX_ARGS = MultiDocumentSpreadsheet.MAX_DOCUMENTS + 1;
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
		SpdxToolsHelper.initialize();
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
		List<List<String>> verificationErrors = new ArrayList<>();
		List<String> docNames = new ArrayList<>();
		for (int i = 1; i < args.length; i++) {
			try {
				addDocToComparer(compareDocs, args[i], docNames, verificationErrors);
			} catch (InvalidSPDXAnalysisException | IOException | InvalidFileNameException e) {
				throw new OnlineToolException("Error opening SPDX document "+args[i]+": "+e.getMessage());
			}
		}
		List<String> normalizedDocNames = normalizeDocNames(docNames);
		MultiDocumentSpreadsheet outSheet = null;
		try {
			outSheet = new MultiDocumentSpreadsheet(outputFile, true, false);
			outSheet.importVerificationErrors(verificationErrors, normalizedDocNames);
			SpdxComparer comparer = new SpdxComparer();
			comparer.compare(compareDocs);
			outSheet.importCompareResults(comparer, normalizedDocNames);
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
	 * Adds all SPDX documents found in the file or directory to the compareDocs list
	 * @param compareDocs
	 * @param filePath
	 * @param verificationErrors
	 * @throws InvalidFileNameException 
	 * @throws IOException 
	 * @throws InvalidSPDXAnalysisException 
	 */
	private static void addDocToComparer(List<SpdxDocument> compareDocs,
			String filePath, List<String> docNames, List<List<String>> verificationErrors) throws InvalidSPDXAnalysisException, IOException, InvalidFileNameException {
		File spdxDocOrDir = new File(filePath);
		if (!spdxDocOrDir.exists()) {
			throw new FileNotFoundException("File "+filePath+" not found");
		}
		if (spdxDocOrDir.isFile()) {
			SpdxDocument doc = SpdxToolsHelper.deserializeDocument(spdxDocOrDir);
			List<String> warnings = doc.verify();
			boolean dupDocUri = false;
			for (SpdxDocument otherDocs:compareDocs) {
				if (otherDocs.getDocumentUri().equals(doc.getDocumentUri())) {
					dupDocUri = true;
					break;
				}
			}
			if (dupDocUri) {
				// Make a unique URI by appending a UUID
				String newUri = doc.getDocumentUri() + UUID.randomUUID();
				warnings.add("Duplicate Document URI: " + doc.getDocumentUri() + " changed to " + newUri);
				IModelStore newStore = new InMemSpdxStore();
				ModelCopyManager copyManager = new ModelCopyManager();
				SpdxDocument newDoc = new SpdxDocument(newStore, newUri, copyManager, false);
				newDoc.copyFrom(doc);
				doc = newDoc;
			}
			compareDocs.add(doc);
			if (!warnings.isEmpty()) {
				System.out.println("Verification errors were found in "+filePath.trim()+".  See verification errors sheet for details.");
			}
			verificationErrors.add(warnings);
			docNames.add(filePath);
		} else if (spdxDocOrDir.isDirectory()) {
			for (File file:spdxDocOrDir.listFiles()) {
				try {
					addDocToComparer(compareDocs, file.getPath(), docNames, verificationErrors);
				} catch (InvalidSPDXAnalysisException | IOException | InvalidFileNameException e) {
					System.out.println("Error deserializing "+file+".  Skipping.");
					continue;
				}
			}
		}
	}

	/**
	 * Converts the URI's or file paths to a list of document names by
	 * removing the common string prefixes
     * @param uriFilePaths Un-normalized file paths or URIs
     * @return List of normalized doc names
     */
    private static List<String> normalizeDocNames(List<String> uriFilePaths) {
        List<String> docNames = new ArrayList<>();
        if (uriFilePaths.size() < 1) {
        	return docNames;
        }
        int commonPrefixIndex = uriFilePaths.get(0).length();
        // first find the minimum index length
        for (int i = 1; i < uriFilePaths.size(); i++) {
            if (uriFilePaths.get(i).length() < commonPrefixIndex) {
                commonPrefixIndex = uriFilePaths.get(i).length();
            }
        }
        // look for the smallest common substring
        for (int i = 1; i < uriFilePaths.size(); i++) {
            for (int j = 0; j < commonPrefixIndex; j++) {
                if (uriFilePaths.get(i-1).charAt(j) != uriFilePaths.get(i).charAt(j)) {
                    commonPrefixIndex = j;
                    break;
                }
            }
        }
        // Back up looking for the first path separator
        for (int i = commonPrefixIndex; i >= 0; i--) {
        	if (uriFilePaths.get(0).charAt(i) == '/' || uriFilePaths.get(0).charAt(i) == '\\') {
        		commonPrefixIndex = i+1;
        		break;
        	}
        }
        for (String uriFilePath:uriFilePaths) {
            docNames.add(uriFilePath.substring(commonPrefixIndex).replace("\\", "/"));
        }
        return docNames;
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
