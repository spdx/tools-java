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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.jacksonstore.MultiFormatStore.Verbose;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.SpdxConstants;
import org.spdx.spdxRdfStore.RdfStore;
import org.spdx.spreadsheetstore.SpreadsheetStore;
import org.spdx.spreadsheetstore.SpreadsheetStore.SpreadsheetFormatType;
import org.spdx.storage.ISerializableModelStore;
import org.spdx.storage.simple.InMemSpdxStore;

/**
 * Converts between various SPDX file types
 * arg[0] from file path
 * arg[1] to file path
 * arg[2] from file type [RDF/XML|JSON] - if not present, file type of the from file will be used
 * arg[3] to file type [RDF/XML|JSON] - if not present, file type of the to file will be used
 * 
 * @author Gary O'Neall
 *
 */
public class SpdxConverter {
	
	static final int MIN_ARGS = 2;
	static final int MAX_ARGS = 4;
	
	enum FileType {
		JSON, RDFXML, XML, XLS, XLSX, YAML, TAG
	}
	
	static Map<String, FileType> EXT_TO_FILETYPE;
	static {
		HashMap<String, FileType> temp = new HashMap<>();
		temp.put("json", FileType.JSON);
		temp.put("rdf.xml", FileType.RDFXML);
		temp.put("xml", FileType.XML);
		temp.put("xls", FileType.XLS);
		temp.put("xlsx", FileType.XLSX);
		temp.put("yaml", FileType.YAML);
		temp.put("tag", FileType.TAG);
		EXT_TO_FILETYPE = Collections.unmodifiableMap(temp);
	}

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
		FileType fromFileType;
		if (args.length > 2) {
			try {
				fromFileType = strToFileType(args[2]);
			} catch(Exception ex) {
				System.err.println("Invalid file type for input file: "+args[2]);
				usage();
				return;
			}
		} else {
			try {
				fromFileType = fileToFileType(fromFile);
			} catch(Exception ex) {
				System.err.println("Can not determine file type for input file: "+args[0]);
				usage();
				return;
			}
		}
		FileType toFileType;
		if (args.length > 3) {
			try {
				toFileType = strToFileType(args[3]);
			} catch(Exception ex) {
				System.err.println("Invalid file type for output file: "+args[3]);
				usage();
				return;
			}
		} else {
			try {
				toFileType = fileToFileType(toFile);
			} catch(Exception ex) {
				System.err.println("Can not determine file type for output file: "+args[0]);
				usage();
				return;
			}
		}
		FileInputStream input = null;
		FileOutputStream output = null;
		try {
			ISerializableModelStore fromStore = fileTypeToStore(fromFileType);
			ISerializableModelStore toStore = fileTypeToStore(toFileType);
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


	private static ISerializableModelStore fileTypeToStore(FileType fromFileType) throws InvalidSPDXAnalysisException {
		switch(fromFileType) {
		case JSON: return new MultiFormatStore(new InMemSpdxStore(), Format.JSON_PRETTY, Verbose.COMPACT);
		case RDFXML: return new RdfStore();
		case TAG: throw new InvalidSPDXAnalysisException("Tag/value is current unsupported.  Check back later.");
		case XLS: return new SpreadsheetStore(new InMemSpdxStore(), SpreadsheetFormatType.XLS);
		case XLSX: return new SpreadsheetStore(new InMemSpdxStore(), SpreadsheetFormatType.XLSX);
		case XML: return new MultiFormatStore(new InMemSpdxStore(), Format.XML, Verbose.COMPACT);
		case YAML: return new MultiFormatStore(new InMemSpdxStore(), Format.YAML, Verbose.COMPACT);
		default: throw new InvalidSPDXAnalysisException("Unsupporte file type: "+fromFileType+".  Check back later.");
		}
	}


	private static FileType fileToFileType(File file) throws InvalidFileNameException {
		String fileName = file.getName();
		if (!fileName.contains(".")) {
			throw new InvalidFileNameException("Can not convert file to file type - no file extension");
		}
		String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
		if ("xml".equals(ext)) {
			if (fileName.endsWith("rdf.xml")) {
				ext = "rdf.xml";
			}
		}
		FileType retval = EXT_TO_FILETYPE.get(ext);
		if (Objects.isNull(retval)) {
			throw new InvalidFileNameException("Unrecognized file extension: "+ext);
		}
		return retval;
	}


	private static FileType strToFileType(String str) {
		String strFileType = str.toUpperCase().trim();
		return FileType.valueOf(strFileType);
	}


	private static void usage() {
		System.out.println("Usage:");
		System.out.println("SpdxConverter fromFilePath toFilePath [fromFileType] [toFileType]");
		System.out.println("\tfromFilePath - File path of the file to convert from");
		System.out.println("\ttoFilePath - output file");
		System.out.println("\t[fromFileType] - optional file type of the input file.  One of JSON, XLS, TAG, RDF/XML, YAML or XML.  If not provided the file type will be determined by the file extension");
		System.out.println("\t[fromFileType] - optional file type of the output file.  One of JSON, XLS, TAG, RDF/XML, YAML or XML.  If not provided the file type will be determined by the file extension");
	}

}
