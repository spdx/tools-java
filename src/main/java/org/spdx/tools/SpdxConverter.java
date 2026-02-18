/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.conversion.Spdx2to3Converter;
import org.spdx.library.model.v2.SpdxConstantsCompatV2;
import org.spdx.library.model.v3_0_1.SpdxConstantsV3.SpdxMajorVersion;
import org.spdx.library.model.v3_0_1.core.CreationInfo;
import org.spdx.spdxRdfStore.RdfStore;
import org.spdx.storage.ISerializableModelStore;
import org.spdx.tools.SpdxToolsHelper.SerFileType;
import org.spdx.v3jsonldstore.JsonLDStore;

/**
 * Converts between various SPDX file types
 * arg[0] from file path
 * arg[1] to file path
 * arg[2] from file type [RDFXML|RDFTTL|JSON|XLS|XLSX|YAML|TAG] - if not present, file type of the from file will be used
 * arg[3] to file type [RDFXML|RDFTTL|JSON|XLS|XLSX|YAML|TAG] - if not present, file type of the to file will be used
 * arg[4] excludeLicenseDetails If present, listed license and listed exception properties will not be included in the output file
 * 
 * the <code>covert(...)</code> methods can be called programmatically to convert files
 * @author Gary O'Neall
 */
public class SpdxConverter {
    static final Logger logger = LoggerFactory.getLogger(SpdxConverter.class);
    
	static final int ERROR_STATUS = 1;
	
	static final int MIN_ARGS = 2;
	static final int MAX_ARGS = 6;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		SpdxToolsHelper.initialize();
		if (args.length < MIN_ARGS) {
			
			System.err
					.println("Invalid number of arguments");
			usage();
			System.exit(ERROR_STATUS);
		}
		if (args.length > MAX_ARGS) {
			System.out.printf("Warning: Extra arguments will be ignored");
		}
		boolean excludeLicenseDetails = false;
		boolean stableIds = false;
		if (args.length == 3 && !isOptionArg(args[2])) {
			System.out.printf("Warning: only the input file type specified - it will be ignored");
		}
		if (args.length == 3 && isOptionArg(args[2])) {
			excludeLicenseDetails = isExcludeLicenseDetails(args[2]);
			stableIds = DeterministicSpdxIdHelper.isStableIdsFlag(args[2]);
		}
		if (args.length >= 5) {
			for (int i = 4; i < args.length; i++) {
				String option = args[i];
				if (Objects.isNull(option)) {
					continue;
				}
				if (isExcludeLicenseDetails(option)) {
					excludeLicenseDetails = true;
				} else if (DeterministicSpdxIdHelper.isStableIdsFlag(option)) {
					stableIds = true;
				}
			}
		}
		// Handle the case where the fourth argument is an option flag rather than a file type.
		// In this situation, treat it like the 3-argument-with-option form and ignore any
		// explicitly specified input file type, for consistency with the 3-argument handling.
		if (args.length == 4 && isOptionArg(args[3])) {
			if (isExcludeLicenseDetails(args[3])) {
				excludeLicenseDetails = true;
			}
			if (DeterministicSpdxIdHelper.isStableIdsFlag(args[3])) {
				stableIds = true;
			}
			try {
				convert(args[0], args[1], excludeLicenseDetails, stableIds);
			} catch (SpdxConverterException e) {
				System.err.println("Error converting: "+e.getMessage());
				System.exit(ERROR_STATUS);
			}
			return;
		}
		if (args.length < 4) {
			try {
				convert(args[0], args[1], excludeLicenseDetails, stableIds);
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
				convert(args[0], args[1], fromFileType, toFileType, excludeLicenseDetails, stableIds);
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
		convert(fromFilePath, toFilePath, false, false);
	}

	/**
	 * Convert an SPDX file from the fromFilePath to a new file at the toFilePath using the file extensions to determine the serialization type
	 * @param fromFilePath Path of the file to convert from
	 * @param toFilePath Path of output file for the conversion
	 * @param excludeLicenseDetails If true, don't copy over properties of the listed licenses
	 * @param stableIds If true, preserve SPDX IDs when possible during SPDX 2 to 3 conversion
	 * @throws SpdxConverterException
	 */
	public static void convert(String fromFilePath, String toFilePath, boolean excludeLicenseDetails,
			boolean stableIds) throws SpdxConverterException {
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
		convert(fromFilePath, toFilePath, fromFileType, toFileType, excludeLicenseDetails, stableIds);
	}
	
	/**
	 * Convert an SPDX file from the fromFilePath to a new file at the toFilePath including listed license property details
	 * @param fromFilePath Path of the file to convert from
	 * @param toFilePath Path of output file for the conversion
	 * @param fromFileType Serialization type of the file to convert from
	 * @param toFileType Serialization type of the file to convert to
	 * @throws SpdxConverterException 
	 */
	public static void convert(String fromFilePath, String toFilePath, SerFileType fromFileType, 
			SerFileType toFileType) throws SpdxConverterException {
		convert(fromFilePath, toFilePath, fromFileType, toFileType, false, false);
		
	}
	
	/**
	 * Convert an SPDX file from the fromFilePath to a new file at the toFilePath
	 * @param fromFilePath Path of the file to convert from
	 * @param toFilePath Path of output file for the conversion
	 * @param fromFileType Serialization type of the file to convert from
	 * @param toFileType Serialization type of the file to convert to
	 * @param excludeLicenseDetails If true, don't copy over properties of the listed licenses
	 * @throws SpdxConverterException 
	 */
	public static void convert(String fromFilePath, String toFilePath, SerFileType fromFileType,
			SerFileType toFileType, boolean excludeLicenseDetails) throws SpdxConverterException {
		convert(fromFilePath, toFilePath, fromFileType, toFileType, excludeLicenseDetails, false);
	}

	/**
	 * Convert an SPDX file from the fromFilePath to a new file at the toFilePath
	 * @param fromFilePath Path of the file to convert from
	 * @param toFilePath Path of output file for the conversion
	 * @param fromFileType Serialization type of the file to convert from
	 * @param toFileType Serialization type of the file to convert to
	 * @param excludeLicenseDetails If true, don't copy over properties of the listed licenses
	 * @param stableIds If true, preserve SPDX IDs when possible during SPDX 2 to 3 conversion
	 * @throws SpdxConverterException
	 */
	public static void convert(String fromFilePath, String toFilePath, SerFileType fromFileType, 
			SerFileType toFileType, boolean excludeLicenseDetails, boolean stableIds) throws SpdxConverterException {
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
		String oldXmlInputFactory = null;
		boolean propertySet = false;
		try {
			ISerializableModelStore fromStore = SpdxToolsHelper.fileTypeToStore(fromFileType);
			ISerializableModelStore toStore = SpdxToolsHelper.fileTypeToStore(toFileType);
			SpdxMajorVersion fromVersion = fromStore instanceof JsonLDStore ? SpdxMajorVersion.VERSION_3 :
					SpdxMajorVersion.VERSION_2;
			SpdxMajorVersion toVersion = toStore instanceof JsonLDStore ? SpdxMajorVersion.VERSION_3 :
				SpdxMajorVersion.VERSION_2;
			if (fromVersion == SpdxMajorVersion.VERSION_3 && toVersion != SpdxMajorVersion.VERSION_3) {
				throw new SpdxConverterException("Can not convert from SPDX spec version 3 to previous versions");
			}
			if (fromStore instanceof RdfStore || toStore instanceof RdfStore) {
				// Setting the property value will avoid the error message
				// See issue #90 for more information
				try {
					oldXmlInputFactory = System.setProperty(SpdxToolsHelper.XML_INPUT_FACTORY_PROPERTY_KEY, 
					        "com.sun.xml.internal.stream.XMLInputFactoryImpl");
					propertySet = true;
				} catch (SecurityException e) {
					propertySet = false; // we'll just deal with the extra error message
				}
			}
			if (toStore instanceof JsonLDStore) {
				((JsonLDStore)toStore).setUseExternalListedElements(true);
			}
			input = new FileInputStream(fromFile);
			output = new FileOutputStream(toFile);
			fromStore.deSerialize(input, false);
			if (fromVersion == SpdxMajorVersion.VERSION_3) {
				copyV3ToV3(fromStore, toStore, excludeLicenseDetails);
			} else if (toVersion  == SpdxMajorVersion.VERSION_3) {
				copyV2ToV3(fromStore, toStore, excludeLicenseDetails, stableIds);
			} else {
				copyV2ToV2(fromStore, toStore, excludeLicenseDetails);
			}
			toStore.serialize(output);
		} catch (Exception ex) {
			String msg = "Error converting SPDX file: "+ex.getClass().toString();
			if (Objects.nonNull(ex.getMessage())) {
				msg = msg + " " + ex.getMessage();
			}
			throw new SpdxConverterException(msg, ex);
		} finally {
			if (propertySet) {
				if (Objects.isNull(oldXmlInputFactory)) {
					System.clearProperty(SpdxToolsHelper.XML_INPUT_FACTORY_PROPERTY_KEY);
				} else {
					System.setProperty(SpdxToolsHelper.XML_INPUT_FACTORY_PROPERTY_KEY, oldXmlInputFactory);
				}
			}
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


	/**
	 * Copies all data from the SPDX spec version 2 fromStore to an SPDX spec version 2 store
	 * @param fromStore store to copy from
	 * @param toStore store to copy to
	 * @param excludeLicenseDetails If true, don't copy over properties of the listed licenses
	 * @throws InvalidSPDXAnalysisException on copy errors
	 */
	private static void copyV2ToV2(ISerializableModelStore fromStore,
			ISerializableModelStore toStore, boolean excludeLicenseDetails) throws InvalidSPDXAnalysisException {
		String documentUri = SpdxToolsHelper.getDocFromStoreCompatV2(fromStore).getDocumentUri();
		if (toStore instanceof RdfStore) {
			((RdfStore) toStore).setDocumentUri(documentUri, false);
			((RdfStore) toStore).setDontStoreLicenseDetails(excludeLicenseDetails);
		}
		ModelCopyManager copyManager = new ModelCopyManager();
		// Need to copy the external document refs first so that they line up with the references
		fromStore.getAllItems(documentUri, SpdxConstantsCompatV2.CLASS_EXTERNAL_DOC_REF).forEach(tv -> {
			try {
				copyManager.copy(toStore, fromStore, tv.getObjectUri(), tv.getSpecVersion(),
						documentUri + "#");
			} catch (InvalidSPDXAnalysisException e) {
				throw new RuntimeException(e);
			}
		});
		fromStore.getAllItems(documentUri, null).forEach(tv -> {
			try {
				if (!SpdxConstantsCompatV2.CLASS_EXTERNAL_DOC_REF.equals(tv.getType()) &&
						!(excludeLicenseDetails && SpdxConstantsCompatV2.CLASS_CROSS_REF.equals(tv.getType()))) {
					copyManager.copy(toStore, fromStore, tv.getObjectUri(), tv.getSpecVersion(), documentUri);
				}
			} catch (InvalidSPDXAnalysisException e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * Copies all data from the SPDX spec version 2 fromStore to an SPDX spec version 3 store
	 * @param fromStore store to copy from
	 * @param toStore store to copy to
	 * @param excludeLicenseDetails If true, don't copy over properties of the listed licenses
	 * @throws InvalidSPDXAnalysisException on copy errors
	 */
	private static void copyV2ToV3(ISerializableModelStore fromStore,
			ISerializableModelStore toStore, boolean excludeLicenseDetails, boolean stableIds) throws InvalidSPDXAnalysisException {
		ModelCopyManager copyManager = new ModelCopyManager();
		org.spdx.library.model.v2.SpdxDocument fromDoc = SpdxToolsHelper.getDocFromStoreCompatV2(fromStore);
		String toUriPrefix = fromDoc.getDocumentUri() + "-specv3/";
		CreationInfo defaultCreationInfo = Spdx2to3Converter.convertCreationInfo(fromDoc.getCreationInfo(),
				toStore, toUriPrefix);
		Spdx2to3Converter converter = new Spdx2to3Converter(toStore, copyManager, defaultCreationInfo, 
				SpdxModelFactory.getLatestSpecVersion(), toUriPrefix, !excludeLicenseDetails);
		converter.convertAndStore(fromDoc);
		// Make sure we get all files, packages and snippets - any relationships and annotations will be copied
		// as properties.  Note that the conversion of the document should already have been copied.
		SpdxModelFactory.getSpdxObjects(fromStore, copyManager, SpdxConstantsCompatV2.CLASS_SPDX_FILE, 
				fromDoc.getDocumentUri(), fromDoc.getDocumentUri()).forEach(f -> {
					if (!converter.alreadyCopied((((org.spdx.library.model.v2.SpdxFile)f).getObjectUri()))) {
						try {
							converter.convertAndStore((org.spdx.library.model.v2.SpdxFile)f);
						} catch (InvalidSPDXAnalysisException e) {
							throw new RuntimeException("Error upgrading file "+f+" from spec version 2 to spec version 3",e);
						}
					}
				});
		SpdxModelFactory.getSpdxObjects(fromStore, copyManager, SpdxConstantsCompatV2.CLASS_SPDX_PACKAGE, 
				fromDoc.getDocumentUri(), fromDoc.getDocumentUri()).forEach(p -> {
					if (!converter.alreadyCopied((((org.spdx.library.model.v2.SpdxPackage)p).getObjectUri()))) {
						try {
							converter.convertAndStore((org.spdx.library.model.v2.SpdxPackage)p);
						} catch (InvalidSPDXAnalysisException e) {
							throw new RuntimeException("Error upgrading package "+p+" from spec version 2 to spec version 3",e);
						}
					}
				});
		SpdxModelFactory.getSpdxObjects(fromStore, copyManager, SpdxConstantsCompatV2.CLASS_SPDX_SNIPPET, 
				fromDoc.getDocumentUri(), fromDoc.getDocumentUri()).forEach(s -> {
					if (!converter.alreadyCopied((((org.spdx.library.model.v2.SpdxSnippet)s).getObjectUri()))) {
						try {
							converter.convertAndStore((org.spdx.library.model.v2.SpdxSnippet)s);
						} catch (InvalidSPDXAnalysisException e) {
							throw new RuntimeException("Error upgrading snippet "+s+" from spec version 2 to spec version 3",e);
						}
					}
				});
		if (stableIds) {
			applyStableIds(fromStore, toStore, copyManager, fromDoc);
		}
	}

	private static void applyStableIds(ISerializableModelStore fromStore,
			ISerializableModelStore toStore,
			ModelCopyManager copyManager,
			org.spdx.library.model.v2.SpdxDocument fromDoc) throws InvalidSPDXAnalysisException {
		Map<String, String> toUriToType = new HashMap<>();
		toStore.getAllItems(null, null).forEach(tv -> toUriToType.put(tv.getObjectUri(), tv.getType()));
		SpdxModelFactory.getSpdxObjects(fromStore, copyManager, SpdxConstantsCompatV2.CLASS_SPDX_ELEMENT,
				fromDoc.getDocumentUri(), fromDoc.getDocumentUri()).forEach(fromElement -> {
			String fromObjectUri = invokeStringGetter(fromElement, "getObjectUri");
			if (Objects.isNull(fromObjectUri)) {
				return;
			}
			String fromId = firstNonEmpty(invokeStringGetter(fromElement, "getId"),
					invokeStringGetter(fromElement, "getSpdxId"));
			if (Objects.isNull(fromId) || fromId.isEmpty()) {
				return;
			}
			String toObjectUri = copyManager.getCopiedObjectUri(fromStore, fromObjectUri, toStore);
			if (Objects.isNull(toObjectUri)) {
				return;
			}
			String toType = toUriToType.get(toObjectUri);
			if (Objects.isNull(toType)) {
				return;
			}
			try {
				Object toObject = SpdxModelFactory.inflateModelObject(toStore, toObjectUri, toType, copyManager, false, null);
				String stableId = DeterministicSpdxIdHelper.isValidV3Id(fromId) ? fromId :
						DeterministicSpdxIdHelper.deterministicFallbackId(fromObjectUri);
				if (!invokeSetId(toObject, stableId)) {
					logger.warn("Unable to apply stable SPDX ID for {}", toObjectUri);
				}
			} catch (InvalidSPDXAnalysisException e) {
				throw new RuntimeException("Error applying stable SPDX ID for " + fromObjectUri, e);
			}
		});
	}

	private static String invokeStringGetter(Object target, String methodName) {
		if (Objects.isNull(target)) {
			return null;
		}
		try {
			Method method = target.getClass().getMethod(methodName);
			Object value = method.invoke(target);
			return value instanceof String ? (String)value : null;
		} catch (ReflectiveOperationException e) {
			return null;
		}
	}

	private static boolean invokeSetId(Object target, String id) {
		if (Objects.isNull(target)) {
			return false;
		}
		try {
			Method method = target.getClass().getMethod("setId", String.class);
			method.invoke(target, id);
			return true;
		} catch (ReflectiveOperationException e) {
			return false;
		}
	}

	private static String firstNonEmpty(String... values) {
		if (Objects.isNull(values)) {
			return null;
		}
		for (String value : values) {
			if (Objects.nonNull(value) && !value.isEmpty()) {
				return value;
			}
		}
		return null;
	}

	/**
	 * Copies all data from the SPDX spec version 3 fromStore to an SPDX spec version 3 store
	 * @param fromStore store to copy from
	 * @param toStore store to copy to
	 * @param excludeLicenseDetails If true, don't copy over properties of the listed licenses
	 * @throws InvalidSPDXAnalysisException on copy errors
	 */
	private static void copyV3ToV3(ISerializableModelStore fromStore,
			ISerializableModelStore toStore, boolean excludeLicenseDetails) throws InvalidSPDXAnalysisException {
		ModelCopyManager copyManager = new ModelCopyManager();
		fromStore.getAllItems(null, null).forEach(tv -> {
			try {
				copyManager.copy(toStore, fromStore, tv.getObjectUri(), tv.getSpecVersion(), null);
			} catch (InvalidSPDXAnalysisException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static void usage() {
		System.out.println("Usage:");
		System.out.println("SpdxConverter fromFilePath toFilePath [fromFileType] [toFileType]");
		System.out.println("\tfromFilePath - File path of the file to convert from");
		System.out.println("\ttoFilePath - output file");
		System.out.println("\t[fromFileType] - optional file type of the input file.  One of JSON, XLS, XLSX, TAG, RDFXML, RDFTTL, YAML, XML or JSONLD.  If not provided the file type will be determined by the file extension");
		System.out.println("\t[toFileType] - optional file type of the output file.  One of JSON, XLS, XLSX, TAG, RDFXML, RDFTTL, YAML, XML or JSONLD.  If not provided the file type will be determined by the file extension");
		System.out.println("\t[excludeLicenseDetails] - If present, listed license and listed exception properties will not be included in the output file");
		System.out.println("\t[--stable-ids] - If present and converting SPDX 2 to 3, preserve SPDX IDs when possible, otherwise use deterministic IDs");
	}

	private static boolean isOptionArg(String arg) {
		return isExcludeLicenseDetails(arg) || DeterministicSpdxIdHelper.isStableIdsFlag(arg);
	}

	private static boolean isExcludeLicenseDetails(String arg) {
		return Objects.nonNull(arg) && "excludelicensedetails".equals(arg.toLowerCase());
	}

}
