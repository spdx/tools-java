/**
 * SPDX-FileCopyrightText: Copyright (c) 2015 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;

import org.spdx.core.CoreModelObject;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.Version;
import org.spdx.storage.ISerializableModelStore;
import org.spdx.tagvaluestore.TagValueStore;
import org.spdx.tools.SpdxToolsHelper.SerFileType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;

/**
 * Verifies an SPDX document and lists any verification errors
 * @author Gary O'Neall
 */
public class Verify {

	static final int MIN_ARGS = 1;
	static final int MAX_ARGS = 2;
	static final int ERROR_STATUS = 1;
	private static final String JSON_SCHEMA_RESOURCE_V2_3 = "resources/spdx-schema-v2.3.json";
	private static final String JSON_SCHEMA_RESOURCE_V2_2 = "resources/spdx-schema-v2.2.json";
	private static final String JSON_SCHEMA_RESOURCE_V3 = "resources/spdx-schema-v3.0.1.json";
	
	static final ObjectMapper JSON_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
	
	/**
	 * @param args args[0] SPDX file path; args[1] [RDFXML|JSON|XLS|XLSX|YAML|TAG] an optional file type - if not present, file type of the to file will be used
	 */
	public static void main(String[] args) {
		if (args.length < MIN_ARGS) {
			System.err
					.println("Usage:\n Verify file\nwhere file is the file path to an SPDX file");
			System.exit(ERROR_STATUS);
		}
		if (args.length > MAX_ARGS) {
			System.out.printf("Warning: Extra arguments will be ignored");
		}
		SpdxToolsHelper.initialize();
		List<String> verify = null;
		try {
			SerFileType fileType = null;
			if (args.length > 1) {
				try {
					fileType = SpdxToolsHelper.strToFileType(args[1]);
				} catch (Exception ex) {
					System.err.println("Invalid file type: "+args[1]);
					System.exit(ERROR_STATUS);
				}
			} else {
				fileType = SpdxToolsHelper.fileToFileType(new File(args[0]));
			}
			verify = verify(args[0], fileType);
		} catch (SpdxVerificationException e) {
			System.out.println(e.getMessage());
			System.exit(ERROR_STATUS);
		} catch (InvalidFileNameException e) {
			System.err.println("Invalid file name: "+args[0]);
			System.exit(ERROR_STATUS);
		}
		// separate out the warning from errors
		List<String> warnings = new ArrayList<>();
		List<String> errors = new ArrayList<>();
		for (String verifyMsg : verify) {
			if (verifyMsg.contains(" is deprecated.")) {
				warnings.add(verifyMsg);
			} else {
				errors.add(verifyMsg);
			}
		}
		if (errors.size() > 0) {
			System.out.println("This SPDX Document is not valid due to:");
			for (String errorMsg:errors) {
				System.out.print("\t" + errorMsg+"\n");
			}
		}
		if (warnings.size() > 0) {
			System.out.println("Warning: Deprecated license identifiers were found that should no longer be used.\n"
					+ "References to the following deprecated license ID's should be updated:");
			for (String warningMsg:warnings) {
				System.out.print("\t" + warningMsg+"\n");
			}
		}
		if (errors.size() == 0) {
			System.out.println("This SPDX Document is valid.");
		} else {
			System.exit(ERROR_STATUS);
		}
	}

	/**
	 * Verify a an SPDX file
	 * @param filePath File path to the SPDX file to be verified
	 * @param fileType 
	 * @return A list of verification errors - if empty, the SPDX file is valid
	 * @throws InvalidFileNameException 
	 * @throws IOException 
	 * @throws SpdxVerificationException 
	 * @throws Errors where the SPDX file can not be parsed or the filename is invalid
	 */
	public static List<String> verify(String filePath, SerFileType fileType) throws SpdxVerificationException {
		Objects.requireNonNull(filePath);
		Objects.requireNonNull(fileType);
		File file = new File(filePath);
		if (!file.exists()) {
			throw new SpdxVerificationException("File "+filePath+" not found.");
		}
		if (!file.isFile()) {
			throw new SpdxVerificationException(filePath+" is not a file.");
		}
		ISerializableModelStore store = null;
		try {
			store = SpdxToolsHelper.fileTypeToStore(fileType);
		} catch (InvalidSPDXAnalysisException e) {
			throw new SpdxVerificationException("Error converting fileType to store",e);
		}
		CoreModelObject doc = null;
		try {
			doc = SpdxToolsHelper.readDocumentFromFile(store, file);
		} catch (FileNotFoundException e) {
			throw new SpdxVerificationException("File "+filePath+ " not found.",e);
		} catch (JsonParseException e) {
			throw new SpdxVerificationException("Invalid JSON file: "+e.getMessage(), e);
		} catch (IOException e) {
			throw new SpdxVerificationException("IO Error reading SPDX file",e);
		} catch (InvalidSPDXAnalysisException e) {
			throw new SpdxVerificationException("Analysis exception processing SPDX file: "+e.getMessage(),e);
		}
		List<String> retval = new ArrayList<String>();
		if (store instanceof TagValueStore) {
			// add in any parser warnings
			retval.addAll(((TagValueStore)store).getWarnings());
		}
		if (SerFileType.JSON.equals(fileType) || SerFileType.JSONLD.equals(fileType)) {
			try {
				String jsonSchemaResource;
				if (SerFileType.JSON.equals(fileType)) {
					jsonSchemaResource = Version.versionLessThan(Version.TWO_POINT_THREE_VERSION, doc.getSpecVersion()) ? 
						JSON_SCHEMA_RESOURCE_V2_2 : JSON_SCHEMA_RESOURCE_V2_3;
				} else {
					jsonSchemaResource = JSON_SCHEMA_RESOURCE_V3;
				}
				JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
				JsonSchema schema;
				try (InputStream is = Verify.class.getResourceAsStream("/" + jsonSchemaResource)) {
					schema = jsonSchemaFactory.getSchema(is);
				}
				JsonNode root;
				try (InputStream is = new FileInputStream(file)) {
					root = JSON_MAPPER.readTree(is);
				}
				Set<ValidationMessage> messages = schema.validate(root);
				for (ValidationMessage msg:messages) {
					retval.add(msg.toString());
				}
			} catch (IOException e) {
				retval.add("Unable to validate JSON file against schema due to I/O Error");
			} catch (InvalidSPDXAnalysisException e) {
				retval.add("Unable to validate JSON file against schema due to error in SPDX file");
			}
		}
		if (SerFileType.JSONLD.equals(fileType)) {
			//TODO: Implement verification against the OWL schema
		}
		List<String> verify;
		try {
			verify = doc.verify(doc.getSpecVersion());
		} catch (InvalidSPDXAnalysisException e) {
			retval.add("Error processing verify for the specific version");
			verify = doc.verify();
		}
		
		if (!verify.isEmpty()) {
			for (String verifyMsg:verify) {
				if (!retval.contains(verifyMsg)) {
					// Check for deprecated licenses - should be warnings, not errors
					if (verifyMsg.contains(" is deprecated.")) {
						verifyMsg = verifyMsg.replaceAll("error:", "warning:");
					}
					retval.add(verifyMsg);
				}
			}
		}
		return retval;
	}

	/**
	 * Verify a tag/value file
	 * @param filePath File path to the SPDX Tag Value file to be verified
	 * @return A list of verification errors - if empty, the SPDX file is valid
	 * @throws SpdxVerificationException Errors where the SPDX Tag Value file can not be parsed or the filename is invalid
	 */
	public static List<String> verifyTagFile(String filePath) throws SpdxVerificationException {
		return verify(filePath, SerFileType.TAG);
	}

	/**
	 * Verify an RDF/XML file
	 * @param filePath File path to the SPDX RDF/XML file to be verified
	 * @return SpdxDocument
	 * @throws SpdxVerificationException Errors where the SPDX RDF/XML file can not be parsed or the filename is invalid
	 */
	public static List<String> verifyRDFFile(String filePath) throws SpdxVerificationException {
		return verify(filePath, SerFileType.RDFXML);
	}
	
	public void usage() {
		System.out.println("Verify filepath [RDFXML|JSON|XLS|XLSX|YAML|TAG|JSONLD]");
		System.out.println("    where filepath is a path to the SPDX file and [RDFXML|JSON|XLS|XLSX|YAML|TAG] is an optional file type - if not present, file type of the to file will be used");
	}
}
