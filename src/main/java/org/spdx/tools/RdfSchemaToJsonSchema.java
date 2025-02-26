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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.spdx.tools.schema.OwlToJsonSchema;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Convert an RDF schema file containing SPDX property to a JSON schema file for all properties in the SPDX namespace
 * @author Gary O'Neall
 */
public class RdfSchemaToJsonSchema {

	/**
	 * @param args arg[0] RDF Schema file path; arg[1] output file path
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err
					.println("Invalid number of arguments");
			usage();
			return;
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
		InputStream is = null;
		OntModel model = null;
		try {
			is = new FileInputStream(fromFile);
			model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
			model.read(is, "RDF/XML");
		} catch (FileNotFoundException e) {
			System.err.println("File not found for "+fromFile.getName());
			return;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					System.err.println("Error closing input file stream: "+e.getMessage());
				}
			}
		}
		OwlToJsonSchema owlToJson = new OwlToJsonSchema(model);
		ObjectNode root = owlToJson.convertToJsonSchema();
		ObjectMapper jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

		JsonGenerator jsonGenerator = null;
		try {
		    jsonGenerator = jsonMapper.getFactory().createGenerator(new FileOutputStream(toFile));
			jsonMapper.writeTree(jsonGenerator.useDefaultPrettyPrinter(), 
					root);
		} catch (JsonProcessingException e) {
			System.err.println("JSON error "+e.getMessage());
			return;
		} catch (IOException e) {
			System.err.println("I/O error: "+e.getMessage());
			return;
		} finally {
			if (Objects.nonNull(is)) {
				try {
					is.close();
				} catch (IOException e) {
					System.err.println("Error closing input file stream: "+e.getMessage());
				}
			}
			if (Objects.nonNull(jsonGenerator)) {
				try {
				    jsonGenerator.close();
				} catch (IOException e) {
					System.err.println("Error closing output file stream: "+e.getMessage());
				}
			}
		}

	}

	public static void usage() {
		System.out.println("Usage:");
		System.out.println("RdfSchemaToJsonSchema rdfSchemaFile jsonSchemaFile");
		System.out.println("\trdfSchemaFile RDF schema file in RDF/XML format");
		System.out.println("\tjsonSchemaFile output JSON Schema file");
	}
}
