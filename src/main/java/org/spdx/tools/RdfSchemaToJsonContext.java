/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
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
import org.spdx.tools.schema.OwlToJsonContext;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Convert an RDF schema file containing SPDX property to a JSON context file for all properties in the SPDX namespace
 * @author Gary O'Neall
 */
public class RdfSchemaToJsonContext {
	
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
		OwlToJsonContext owlToJsonContext = null;
		try {
			is = new FileInputStream(fromFile);
			OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
			model.read(is, "RDF/XML");
			owlToJsonContext = new OwlToJsonContext(model);
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
		if (Objects.isNull(owlToJsonContext)) {
		    System.err.println("Unable to load ontology from file "+fromFile.getName());
		    return;
		}
		ObjectNode context = owlToJsonContext.convertToContext();
		JsonGenerator jsonGenerator = null;
		try {
			jsonGenerator = OwlToJsonContext.JSON_MAPPER.getFactory().createGenerator(new FileOutputStream(toFile));
			OwlToJsonContext.JSON_MAPPER.writeTree(jsonGenerator.useDefaultPrettyPrinter(), 
					context);
		} catch (FileNotFoundException e) {
			System.err.println("File not found for "+fromFile.getName());
			return;
		} catch (JsonProcessingException e) {
			System.err.println("JSON error "+e.getMessage());
			return;
		} catch (IOException e) {
			System.err.println("I/O error: "+e.getMessage());
			return;
		} finally {
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
		System.out.println("RdfSchemaToJsonContext rdfSchemaFile jsonContextFile");
		System.out.println("\trdfSchemaFile RDF schema file in RDF/XML format");
		System.out.println("\trdfSchemaFile output JSON context file");
	}

}
