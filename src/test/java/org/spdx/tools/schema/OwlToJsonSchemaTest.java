/**
 * SPDX-FileCopyrightText: 2026 SPDX Contributors
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.ontapi.OntModelFactory;
import org.apache.jena.ontapi.OntSpecification;
import org.apache.jena.ontapi.model.OntModel;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import junit.framework.TestCase;

/**
 * Tests generation of JSON-Schema definitions from SPDX OWL ontologies.
 */
public class OwlToJsonSchemaTest extends TestCase {

	static final String OWL_FILE_PATH = "testResources" + File.separator 
			+ "spdx-2-2-revision-8-ontology.owl.xml";

	public void testConvertToJsonSchema() throws IOException {
		OwlToJsonSchema otjs = null;
		try (InputStream is = new FileInputStream(new File(OWL_FILE_PATH))) {
			OntModel model = OntModelFactory
					.createModel(OntSpecification.OWL2_DL_MEM);
			model.read(is, "RDF/XML");
			otjs = new OwlToJsonSchema(model);
		}
		ObjectNode result = otjs.convertToJsonSchema();

		assertNotNull(result);
		assertTrue(result.has("$schema"));
		assertEquals("https://json-schema.org/draft/2019-09/schema#", 
				result.get("$schema").asText());
		assertTrue(result.has("type"));
		assertEquals("object", result.get("type").asText());

		assertTrue(result.has("properties"));
		ObjectNode properties = (ObjectNode) result.get("properties");

		assertTrue(properties.has("packages"));
		assertTrue(properties.has("files"));
		assertTrue(properties.has("snippets"));
		assertTrue(properties.has("relationships"));

		assertTrue(result.has("required"));
		ArrayNode required = (ArrayNode) result.get("required");
		assertTrue(required.size() > 0);
	}
}
