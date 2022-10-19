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
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.jacksonstore.MultiFormatStore.Verbose;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.SpdxDocument;
import org.spdx.spdxRdfStore.OutputFormat;
import org.spdx.spdxRdfStore.RdfStore;
import org.spdx.spreadsheetstore.SpreadsheetStore;
import org.spdx.spreadsheetstore.SpreadsheetStore.SpreadsheetFormatType;
import org.spdx.storage.ISerializableModelStore;
import org.spdx.storage.simple.InMemSpdxStore;
import org.spdx.tagvaluestore.TagValueStore;

/**
 * Static helper methods for the various tools
 *
 * @author Gary O'Neall
 *
 */
public class SpdxToolsHelper {

	public enum SerFileType {
		JSON, RDFXML, XML, XLS, XLSX, YAML, TAG, RDFTTL
	}

	static Map<String, SerFileType> EXT_TO_FILETYPE;
	static {
		HashMap<String, SerFileType> temp = new HashMap<>();
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
					"Can not convert file to file type - no file extension");
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
		}
		SerFileType retval = EXT_TO_FILETYPE.get(ext);
		if (Objects.isNull(retval)) {
			throw new InvalidFileNameException(
					"Unrecognized file extension: " + ext);
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
	public static SpdxDocument deserializeDocument(File file)
			throws InvalidSPDXAnalysisException, IOException,
			InvalidFileNameException {
		ISerializableModelStore store = fileTypeToStore(fileToFileType(file));
		try (InputStream is = new FileInputStream(file)) {
			String documentUri = store.deSerialize(is, false);
			return new SpdxDocument(store, documentUri, null, false);
		}
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
	public static SpdxDocument deserializeDocument(File file,
			SerFileType fileType)
			throws InvalidSPDXAnalysisException, IOException {
		ISerializableModelStore store = fileTypeToStore(fileType);
		try (InputStream is = new FileInputStream(file)) {
			String documentUri = store.deSerialize(is, false);
			return new SpdxDocument(store, documentUri, null, false);
		}
	}
}
