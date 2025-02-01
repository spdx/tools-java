/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0 *
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
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.jacksonstore.MultiFormatStore.Verbose;
import org.spdx.core.CoreModelObject;
import org.spdx.core.DefaultModelStore;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v2.SpdxConstantsCompatV2;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.library.model.v3_0_1.SpdxConstantsV3;
import org.spdx.spdxRdfStore.OutputFormat;
import org.spdx.spdxRdfStore.RdfStore;
import org.spdx.spreadsheetstore.SpreadsheetStore;
import org.spdx.spreadsheetstore.SpreadsheetStore.SpreadsheetFormatType;
import org.spdx.storage.ISerializableModelStore;
import org.spdx.storage.simple.InMemSpdxStore;
import org.spdx.tagvaluestore.TagValueStore;
import org.spdx.v3jsonldstore.JsonLDStore;

/**
 * Static helper methods for the various tools
 *
 * @author Gary O'Neall
 */
public class SpdxToolsHelper {

	public enum SerFileType {
		JSON, RDFXML, XML, XLS, XLSX, YAML, TAG, RDFTTL, JSONLD
	}

	static final String XML_INPUT_FACTORY_PROPERTY_KEY = "javax.xml.stream.XMLInputFactory";

	static Map<String, SerFileType> EXT_TO_FILETYPE;
	static {
		HashMap<String, SerFileType> temp = new HashMap<>();
		temp.put("jsonld.json", SerFileType.JSONLD);
		temp.put("jsonld", SerFileType.JSONLD);
		temp.put("json", SerFileType.JSON);
		temp.put("rdf.xml", SerFileType.RDFXML);
		temp.put("rdf", SerFileType.RDFXML);
		temp.put("xml", SerFileType.XML);
		temp.put("xls", SerFileType.XLS);
		temp.put("xlsx", SerFileType.XLSX);
		temp.put("yaml", SerFileType.YAML);
		temp.put("tag", SerFileType.TAG);
		temp.put("spdx", SerFileType.TAG);
		temp.put("yml", SerFileType.YAML);
		temp.put("rdf.ttl", SerFileType.RDFTTL);
		EXT_TO_FILETYPE = Collections.unmodifiableMap(temp);
	}

	/**
	 * @param fileType
	 *            file type for the store
	 * @return the appropriate in memory based model store which supports
	 *         serialization for the fileType
	 * @throws InvalidSPDXAnalysisException
	 */
	public static ISerializableModelStore fileTypeToStore(SerFileType fileType)
			throws InvalidSPDXAnalysisException {
		switch (fileType) {
			case JSON :
				return new MultiFormatStore(new InMemSpdxStore(),
						Format.JSON_PRETTY, Verbose.COMPACT);
			case RDFXML : {
				RdfStore rdfStore = new RdfStore();
				rdfStore.setOutputFormat(OutputFormat.XML);
				return rdfStore;
			}
			case RDFTTL : {
				RdfStore rdfStore = new RdfStore();
				rdfStore.setOutputFormat(OutputFormat.TURTLE);
				return rdfStore;
			}
			case TAG :
				return new TagValueStore(new InMemSpdxStore());
			case XLS :
				return new SpreadsheetStore(new InMemSpdxStore(),
						SpreadsheetFormatType.XLS);
			case XLSX :
				return new SpreadsheetStore(new InMemSpdxStore(),
						SpreadsheetFormatType.XLSX);
			case XML :
				return new MultiFormatStore(new InMemSpdxStore(), Format.XML,
						Verbose.COMPACT);
			case YAML :
				return new MultiFormatStore(new InMemSpdxStore(), Format.YAML,
						Verbose.COMPACT);
			case JSONLD :
				return new JsonLDStore(new InMemSpdxStore());
			default :
				throw new InvalidSPDXAnalysisException("Unsupported file type: "
						+ fileType + ".  Check back later.");
		}
	}

	/**
	 * @param file
	 * @return the file type based on the file name and file extension
	 * @throws InvalidFileNameException
	 */
	public static SerFileType fileToFileType(File file)
			throws InvalidFileNameException {
		String fileName = file.getName();
		if (!fileName.contains(".")) {
			throw new InvalidFileNameException(
					"Can not convert file to file type - no file extension for file "+file.getPath());
		}
		String ext = fileName.substring(fileName.lastIndexOf(".") + 1)
				.toLowerCase();
		if ("xml".equals(ext)) {
			if (fileName.endsWith("rdf.xml")) {
				ext = "rdf.xml";
			}
		}
		if ("ttl".equals(ext)) {
			if (fileName.endsWith("rdf.ttl")) {
				ext = "rdf.ttl";
			}
		}if ("json".equals(ext)) {
			if (fileName.endsWith("jsonld.json")) {
				ext = "jsonld.json";
			}
		}
		SerFileType retval = EXT_TO_FILETYPE.get(ext);
		if (SerFileType.JSON.equals(retval)) {
			// we need to check for a JSON-LD file type
			try (Scanner scanner = new Scanner(file)) {
				scanner.useDelimiter("\"");
				boolean foundContext = false;
				boolean foundRdfUri = false;
				while (scanner.hasNext()) {
					String line = scanner.next().toLowerCase();
					if (line.contains("https://spdx.org/rdf/3.")) {
						foundRdfUri = true;
					}
					if (line.contains("@context")) {
						foundContext = true;
					}
					if (foundContext && foundRdfUri) {
						retval = SerFileType.JSONLD;
						break;
					}
				}
			} catch (FileNotFoundException e) {
				// We'll assume it is just a JSON file
			}
		}
		if (Objects.isNull(retval)) {
			throw new InvalidFileNameException(
					"Unrecognized file extension: " + ext + " for file "+file.getPath());
		}
		return retval;
	}

	/**
	 * @param str
	 * @return the file type based on the file extension or string
	 */
	public static SerFileType strToFileType(String str) {
		String strFileType = str.toUpperCase().trim();
		return SerFileType.valueOf(strFileType);
	}

	/**
	 * @param file
	 *            file containing an SPDX document with the standard file
	 *            extension for the serialization formats
	 * @return the SPDX document stored in the file
	 * @throws InvalidSPDXAnalysisException
	 * @throws IOException
	 * @throws InvalidFileNameException
	 */
	public static SpdxDocument deserializeDocumentCompatV2(File file)
			throws InvalidSPDXAnalysisException, IOException,
			InvalidFileNameException {
		ISerializableModelStore store = fileTypeToStore(fileToFileType(file));
		if (!supportsV2(store)) {
			throw new RuntimeException("Store does not support SPDX version 2");
		}
		return readDocumentFromFileCompatV2(store, file);
	}
	/**
	 * @param file
	 *            file containing an SPDX document in one of the supported
	 *            SerFileTypes
	 * @param fileType
	 *            serialization file type
	 * @return the SPDX document stored in the file
	 * @throws InvalidSPDXAnalysisException
	 * @throws IOException
	 */
	public static SpdxDocument deserializeDocumentCompatV2(File file,
			SerFileType fileType)
			throws InvalidSPDXAnalysisException, IOException {
		ISerializableModelStore store = fileTypeToStore(fileType);
		if (!supportsV2(store)) {
			throw new RuntimeException("Store does not support SPDX version 2");
		}
		return readDocumentFromFileCompatV2(store, file);
	}
	
	/**
	 * @param file
	 *            file containing an SPDX document with the standard file
	 *            extension for the serialization formats
	 * @return the SPDX document stored in the file
	 * @throws InvalidSPDXAnalysisException
	 * @throws IOException
	 * @throws InvalidFileNameException
	 */
	public static org.spdx.library.model.v3_0_1.core.SpdxDocument deserializeDocumentCompat(File file)
			throws InvalidSPDXAnalysisException, IOException,
			InvalidFileNameException {
		ISerializableModelStore store = fileTypeToStore(fileToFileType(file));
		if (!supportsV3(store)) {
			throw new RuntimeException("Store does not support SPDX version 3");
		}
		return readDocumentFromFileV3(store, file);
	}
	/**
	 * @param file
	 *            file containing an SPDX document in one of the supported
	 *            SerFileTypes
	 * @param fileType
	 *            serialization file type
	 * @return the SPDX document stored in the file
	 * @throws InvalidSPDXAnalysisException
	 * @throws IOException
	 */
	public static org.spdx.library.model.v3_0_1.core.SpdxDocument deserializeDocument(File file,
			SerFileType fileType)
			throws InvalidSPDXAnalysisException, IOException {
		ISerializableModelStore store = fileTypeToStore(fileType);
		if (!supportsV3(store)) {
			throw new RuntimeException("Store does not support SPDX version 3");
		}
		return readDocumentFromFileV3(store, file);
	}
	
	/**
	 * Reads SPDX data from a file and stores it in the store
	 * @param store Store where the SPDX data is to be stored
	 * @param file File to read the store from
	 * @throws FileNotFoundException If the file is not found
	 * @throws IOException If there is an error reading the file
	 * @throws InvalidSPDXAnalysisException If there is a problem in the SPDX document structure
	 */
	public static void deserializeFile(ISerializableModelStore store, File file) throws FileNotFoundException, IOException, InvalidSPDXAnalysisException {
		String oldXmlInputFactory = null;
		boolean propertySet = false;
		try (InputStream is = new FileInputStream(file)) {
			if (store instanceof RdfStore) {
				// Setting the property value will avoid the error message
				// See issue #90 for more information
				try {
					oldXmlInputFactory = System.setProperty(XML_INPUT_FACTORY_PROPERTY_KEY, 
					        "com.sun.xml.internal.stream.XMLInputFactoryImpl");
					propertySet = true;
				} catch (SecurityException e) {
					propertySet = false; // we'll just deal with the extra error message
				}
			}
			store.deSerialize(is, false);
		} finally {
			if (propertySet) {
				if (Objects.isNull(oldXmlInputFactory)) {
					System.clearProperty(XML_INPUT_FACTORY_PROPERTY_KEY);
				} else {
					System.setProperty(XML_INPUT_FACTORY_PROPERTY_KEY, oldXmlInputFactory);
				}
			}
		}
	}
	
	/**
	 * @param store model store
	 * @return true of the model store support SPDX spec version 3
	 */
	public static boolean supportsV3(ISerializableModelStore store) {
		return store instanceof JsonLDStore;
	}
	
	/**
	 * @param store model store
	 * @return true of the model store support SPDX spec version 2
	 */
	public static boolean supportsV2(ISerializableModelStore store) {
		return !(store instanceof JsonLDStore);
	}
	
	/**
	 * Reads an SPDX Document from a file
	 * @param store Store where the document is to be stored
	 * @param file File to read the store from
	 * @return SPDX Document from the store
	 * @throws FileNotFoundException If the file is not found
	 * @throws IOException If there is an error reading the file
	 * @throws InvalidSPDXAnalysisException If there is a problem in the SPDX document structure
	 */
	public static org.spdx.library.model.v3_0_1.core.SpdxDocument readDocumentFromFileV3(ISerializableModelStore store, File file) throws FileNotFoundException, IOException, InvalidSPDXAnalysisException {
		if (!supportsV3(store)) {
			throw new RuntimeException("Store does not support SPDX version 3");
		}
		deserializeFile(store, file);
		return getDocFromStore(store);
	}
	
	/**
	 * Reads an SPDX Document from a file
	 * @param store Store where the document is to be stored
	 * @param file File to read the store from
	 * @return SPDX Document from the store
	 * @throws FileNotFoundException If the file is not found
	 * @throws IOException If there is an error reading the file
	 * @throws InvalidSPDXAnalysisException If there is a problem in the SPDX document structure
	 */
	public static CoreModelObject readDocumentFromFile(ISerializableModelStore store, File file) throws FileNotFoundException, IOException, InvalidSPDXAnalysisException {
		if (store instanceof JsonLDStore) {
			return readDocumentFromFileV3(store, file);
		} else {
			return readDocumentFromFileCompatV2(store, file);
		}
	}
	
	/**
	 * Reads an SPDX Document from a file
	 * @param store Store where the document is to be stored
	 * @param file File to read the store from
	 * @return SPDX Document from the store
	 * @throws FileNotFoundException If the file is not found
	 * @throws IOException If there is an error reading the file
	 * @throws InvalidSPDXAnalysisException If there is a problem in the SPDX document structure
	 */
	public static SpdxDocument readDocumentFromFileCompatV2(ISerializableModelStore store, File file) throws FileNotFoundException, IOException, InvalidSPDXAnalysisException {
		if (!supportsV2(store)) {
			throw new RuntimeException("Store does not support SPDX version 2");
		}
		deserializeFile(store, file);
		return getDocFromStoreCompatV2(store);
	}
	
	/**
	 * @param store model store
	 * @return returns a document if a single document is found in the model store
	 * @throws InvalidSPDXAnalysisException
	 */
	public static org.spdx.library.model.v3_0_1.core.SpdxDocument getDocFromStore(ISerializableModelStore store) throws InvalidSPDXAnalysisException {
		@SuppressWarnings("unchecked")
		List<org.spdx.library.model.v3_0_1.core.SpdxDocument> docs = 
		(List<org.spdx.library.model.v3_0_1.core.SpdxDocument>)SpdxModelFactory.getSpdxObjects(store, null, SpdxConstantsV3.CORE_SPDX_DOCUMENT, null, null)
				.collect(Collectors.toList());
		if (docs.isEmpty()) {
			// TODO: We could construct an SPDX document just from the serialization information
			throw new InvalidSPDXAnalysisException("No SPDX version 3 documents in model store");
		}
		if (docs.size() > 1) {
			throw new InvalidSPDXAnalysisException("Multiple SPDX version 3 documents in modelSTore.  There can only be one SPDX document.");
		}
		return docs.get(0);
	}
	
	/**
	 * @param store model store
	 * @return returns a document if a single document is found in the model store
	 * @throws InvalidSPDXAnalysisException
	 */
	public static SpdxDocument getDocFromStoreCompatV2(ISerializableModelStore store) throws InvalidSPDXAnalysisException {
		@SuppressWarnings("unchecked")
		List<SpdxDocument> docs = (List<SpdxDocument>)SpdxModelFactory.getSpdxObjects(store, null, SpdxConstantsCompatV2.CLASS_SPDX_DOCUMENT, null, null)
				.collect(Collectors.toList());
		if (docs.isEmpty()) {
			throw new InvalidSPDXAnalysisException("No SPDX version 2 documents in model store");
		}
		if (docs.size() > 1) {
			throw new InvalidSPDXAnalysisException("Multiple SPDX version 2 documents in modelSTore.  There can only be one SPDX document.");
		}
		return docs.get(0);
	}

	/**
	 * Initializes the model registry and default model stores
	 */
	public static void initialize() {
		SpdxModelFactory.init();
		DefaultModelStore.initialize(new InMemSpdxStore(), "https://spdx.org/documents/default", new ModelCopyManager());
	}
}
