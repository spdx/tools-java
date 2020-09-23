/**
 * Copyright (c) 2020 Source Auditor Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
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
 */
package org.spdx.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.SpdxConstants;
import org.spdx.storage.ISerializableModelStore;
import org.spdx.tools.SpdxToolsHelper.SerFileType;

/**
 * Converts between various SPDX file types
 * arg[0] from file path
 * arg[1] to file path
 * arg[2] from file type [RDFXML|JSON|XLS|XLSX|YAML|TAG] - if not present, file type of the from file will be used
 * arg[3] to file type [RDFXML|JSON|XLS|XLSX|YAML|TAG] - if not present, file type of the to file will be used
 * 
 * @author Gary O'Neall
 *
 */
public class SpdxConverter {
	
	static final int MIN_ARGS = 2;
	static final int MAX_ARGS = 4;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < MIN_ARGS) {
			
			System.err
					.println("Invalid number of arguments");
			usage();
			return;
		}
		if (args.length > MAX_ARGS) {
			System.out.printf("Warning: Extra arguments will be ignored");
		}
		File fromFile = new File(args[0]);
		if (!fromFile.exists()) {
			System.err
				.println("Input file "+args[0]+" does not exist.");
			usage();
			return;
		}
		File toFile = new File(args[1]);
		if (toFile.exists()) {
			System.err.println("Output file "+args[1]+" already exists.");
			usage();
			return;
		}
		SerFileType fromFileType;
		if (args.length > 2) {
			try {
				fromFileType = SpdxToolsHelper.strToFileType(args[2]);
			} catch(Exception ex) {
				System.err.println("Invalid file type for input file: "+args[2]);
				usage();
				return;
			}
		} else {
			try {
				fromFileType = SpdxToolsHelper.fileToFileType(fromFile);
			} catch(Exception ex) {
				System.err.println("Can not determine file type for input file: "+args[0]);
				usage();
				return;
			}
		}
		SerFileType toFileType;
		if (args.length > 3) {
			try {
				toFileType = SpdxToolsHelper.strToFileType(args[3]);
			} catch(Exception ex) {
				System.err.println("Invalid file type for output file: "+args[3]);
				usage();
				return;
			}
		} else {
			try {
				toFileType = SpdxToolsHelper.fileToFileType(toFile);
			} catch(Exception ex) {
				System.err.println("Can not determine file type for output file: "+args[0]);
				usage();
				return;
			}
		}
		FileInputStream input = null;
		FileOutputStream output = null;
		try {
			ISerializableModelStore fromStore = SpdxToolsHelper.fileTypeToStore(fromFileType);
			ISerializableModelStore toStore = SpdxToolsHelper.fileTypeToStore(toFileType);
			input = new FileInputStream(fromFile);
			output = new FileOutputStream(toFile);
			String documentUri = fromStore.deSerialize(input, false);
			ModelCopyManager copyManager = new ModelCopyManager();
			// Need to copy the external document refs first so that they line up with the references
			fromStore.getAllItems(documentUri, SpdxConstants.CLASS_EXTERNAL_DOC_REF).forEach(tv -> {
				try {
					copyManager.copy(toStore, documentUri, fromStore, documentUri, 
							tv.getId(), tv.getType());
				} catch (InvalidSPDXAnalysisException e) {
					throw new RuntimeException(e);
				}
			});
			fromStore.getAllItems(documentUri, null).forEach(tv -> {
				try {
					if (!SpdxConstants.CLASS_EXTERNAL_DOC_REF.equals(tv.getType())) {
						copyManager.copy(toStore, documentUri, fromStore, documentUri, 
								tv.getId(), tv.getType());
					}
				} catch (InvalidSPDXAnalysisException e) {
					throw new RuntimeException(e);
				}
			});
			toStore.serialize(documentUri, output);
		} catch (Exception ex) {
			String msg = "Error converting SPDX file: "+ex.getClass().toString();
			if (Objects.nonNull(ex.getMessage())) {
				msg = msg + ex.getMessage();
			}
			System.err.println(msg);
		} finally {
			if (Objects.nonNull(input)) {
				try {
					input.close();
				} catch (IOException e) {
					System.out.println("Error closing input file: "+e.getMessage());
				}
			}
			if (Objects.nonNull(output)) {
				try {
					output.close();
				} catch (IOException e) {
					System.out.println("Error closing output file: "+e.getMessage());
				}
			}
		}
	}


	private static void usage() {
		System.out.println("Usage:");
		System.out.println("SpdxConverter fromFilePath toFilePath [fromFileType] [toFileType]");
		System.out.println("\tfromFilePath - File path of the file to convert from");
		System.out.println("\ttoFilePath - output file");
		System.out.println("\t[fromFileType] - optional file type of the input file.  One of JSON, XLS, XLSX, TAG, RDFXML, YAML or XML.  If not provided the file type will be determined by the file extension");
		System.out.println("\t[fromFileType] - optional file type of the output file.  One of JSON, XLS, XLSX, TAG, RDFXML, YAML or XML.  If not provided the file type will be determined by the file extension");
	}

}
