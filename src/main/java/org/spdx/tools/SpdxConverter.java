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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * the <code>covert(...)</code> methods can be called programmatically to convert files
 * @author Gary O'Neall
 *
 */
public class SpdxConverter {
    static final Logger logger = LoggerFactory.getLogger(SpdxConverter.class);
    
	static final int ERROR_STATUS = 1;
	
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
			System.exit(ERROR_STATUS);
		}
		if (args.length > MAX_ARGS) {
			System.out.printf("Warning: Extra arguments will be ignored");
		}
		if (args.length == 3) {
			System.out.printf("Warning: only the input file type specified - it will be ignored");
		}
		if (args.length < 4) {
			try {
				convert(args[0], args[1]);
			} catch (SpdxConverterException e) {
				System.err.println("Error converting: "+e.getMessage());
				System.exit(ERROR_STATUS);
			}
		} else {
			SerFileType fromFileType = null;
			try {
				fromFileType = SpdxToolsHelper.strToFileType(args[2]);
			} catch (IllegalArgumentException e) {
				System.err
				.println("From file type is not a valid SPDX file type: "+args[2]);
				usage();
				System.exit(ERROR_STATUS);
			}
			SerFileType toFileType = null;
			try {
				toFileType = SpdxToolsHelper.strToFileType(args[3]);
			} catch (IllegalArgumentException e) {
				System.err
				.println("To file type is not a valid SPDX file type: "+args[3]);
				usage();
				System.exit(ERROR_STATUS);
			}
			try {
				convert(args[0], args[1], fromFileType, toFileType);
			} catch (SpdxConverterException e) {
				System.err.println("Error converting: "+e.getMessage());
				System.exit(ERROR_STATUS);
			}
		}
	}
	
	/**
	 * Convert an SPDX file from the fromFilePath to a new file at the toFilePath using the file extensions to determine the serialization type
	 * @param fromFilePath Path of the file to convert from
	 * @param toFilePath Path of output file for the conversion
	 * @throws SpdxConverterException
	 */
	public static void convert(String fromFilePath, String toFilePath) throws SpdxConverterException {
		SerFileType fromFileType;
		try {
			fromFileType = SpdxToolsHelper.fileToFileType(new File(fromFilePath));
		} catch (InvalidFileNameException e) {
			throw new SpdxConverterException("From file "+fromFilePath+" does not end with a valid SPDX file extension.");
		}
		SerFileType toFileType;
		try {
			toFileType = SpdxToolsHelper.fileToFileType(new File(toFilePath));
		} catch (InvalidFileNameException e) {
			throw new SpdxConverterException("To file "+toFilePath+" does not end with a valid SPDX file extension.");
		}
		convert(fromFilePath, toFilePath, fromFileType, toFileType);
	}
	
	/**
	 * Convert an SPDX file from the fromFilePath to a new file at the toFilePath
	 * @param fromFilePath Path of the file to convert from
	 * @param toFilePath Path of output file for the conversion
	 * @param fromFileType Serialization type of the file to convert from
	 * @param toFileType Serialization type of the file to convert to
	 * @throws SpdxConverterException 
	 */
	public static void convert(String fromFilePath, String toFilePath, SerFileType fromFileType, SerFileType toFileType) throws SpdxConverterException {
		File fromFile = new File(fromFilePath);
		if (!fromFile.exists()) {
			throw new SpdxConverterException("Input file "+fromFilePath+" does not exist.");
		}
		File toFile = new File(toFilePath);
		if (toFile.exists()) {
			throw new SpdxConverterException("Output file "+toFilePath+" already exists.");
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
			throw new SpdxConverterException(msg, ex);
		} finally {
			if (Objects.nonNull(input)) {
				try {
					input.close();
				} catch (IOException e) {
					logger.warn("Error closing input file: "+e.getMessage());
				}
			}
			if (Objects.nonNull(output)) {
				try {
					output.close();
				} catch (IOException e) {
					logger.warn("Error closing output file: "+e.getMessage());
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
