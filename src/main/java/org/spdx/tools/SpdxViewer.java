package org.spdx.tools;
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


import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.SpdxDocument;
import org.spdx.storage.ISerializableModelStore;
import org.spdx.tag.CommonCode;
import org.spdx.tools.SpdxToolsHelper.SerFileType;

/**
 * Simple pretty printer for SPDX RDF XML files. Writes output to System.out.
 * Usage: PrettyPrinter SPDXRdfXMLFile > textFile where SPDXRdfXMLFile is a
 * valid SPDX RDF XML file
 *
 * @author Gary O'Neall
 * @version 0.1
 */

public class SpdxViewer {

	static final int MIN_ARGS = 1;
	static final int MAX_ARGS = 2;
	static final int ERROR_STATUS = 1;

	/**
	 * Pretty Printer for an SPDX Document
	 *
     * @param args args[0] SPDX file path; args[1] [RDFXML|JSON|XLS|XLSX|YAML|TAG] an optional file type - if not present, file type of the to file will be used
     * 
	 */

	public static void main(String[] args) {
		if (args.length < MIN_ARGS) {
			System.err
					.println("Usage:\n SPDXViewer file [RDFXML|JSON|XLS|XLSX|YAML|TAG] \n"
							+ "where file is the file path to a valid SPDX file\n"
							+ "and [RDFXML|JSON|XLS|XLSX|YAML|TAG] is an optional file type\n"
							+ "if not present, file type of the to file will be used");
			return;
		}
		if (args.length > MAX_ARGS) {
			System.out.printf("Warning: Extra arguments will be ignored");
		}
		SpdxDocument doc = null;
		ISerializableModelStore store = null;
		PrintWriter writer = null;
		try {
			File file = new File(args[0]);
			if (!file.exists()) {
				throw new SpdxVerificationException("File "+args[0]+" not found.");
			}
			if (!file.isFile()) {
				throw new SpdxVerificationException(args[0]+" is not a file.");
			}
			SerFileType fileType = null;
			if (args.length > 1) {
				try {
					fileType = SpdxToolsHelper.strToFileType(args[1]);
				} catch (Exception ex) {
					System.err.println("Invalid file type: "+args[1]);
					System.exit(ERROR_STATUS);
				}
			} else {
				fileType = SpdxToolsHelper.fileToFileType(file);
			}
			try {
				store = SpdxToolsHelper.fileTypeToStore(fileType);
			} catch (InvalidSPDXAnalysisException e) {
				throw new SpdxVerificationException("Error converting fileType to store",e);
			}
			
			try {
				doc = SpdxToolsHelper.readDocumentFromFile(store, file);
			} catch (Exception ex) {
		        System.out
		                .print("Error creating SPDX Document: " + ex.getMessage());
		        return;
		    }
		    writer = new PrintWriter(System.out);
			List<String> verify = doc.verify();
			if (verify.size() > 0) {
				System.out.println("This SPDX Document is not valid due to:");
				for (int i = 0; i < verify.size(); i++) {
					System.out.print("\t" + verify.get(i)+"\n");
				}
			}
			// read the constants from a file
			Properties constants = CommonCode
					.getTextFromProperties("org/spdx/tag/SpdxViewerConstants.properties");
			// print document to system output using human readable format
			CommonCode.printDoc(doc, writer, constants);
		} catch (InvalidSPDXAnalysisException e) {
			System.out.print("Error pretty printing SPDX Document: "
					+ e.getMessage());
			return;
		} catch (Exception e) {
			System.out.print("Unexpected error displaying SPDX Document: "
					+ e.getMessage());
		} finally {
		    if (Objects.nonNull(writer)) {
		        writer.close();
		    }
		    if (Objects.nonNull(store)) {
    			try {
                    store.close();
                } catch (Exception e) {
                    System.out.println("Warning - unable to close SPDX file: "+e.getMessage());
                }
		    }
		}
	}
}
