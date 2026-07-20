/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
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
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;

import junit.framework.TestCase;

/**
 * Tests generation of XML Schema Definitions (XSD) from SPDX OWL ontologies.
 */
public class OwlToXSDTest extends TestCase {

	static final String OWL_FILE_PATH = "testResources" + File.separator + "spdx-2-2-revision-8-ontology.owl.xml";

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testConvertToXsd() throws IOException, XmlSchemaSerializerException, SchemaException {
		OwlToXsd otx = null;
		try (InputStream is = new FileInputStream(new File(OWL_FILE_PATH))) {
			OntModel model = OntModelFactory.createModel(OntSpecification.OWL2_DL_MEM);
			model.read(is, "RDF/XML");
			otx = new OwlToXsd(model);
		}
		XmlSchema result = otx.convertToXsd();

		assertNotNull(result);
		assertNotNull(result.getElementByName("Document"));
		String expectedIRI = "http://spdx.org/rdf/terms";
		assertEquals(expectedIRI, result.getTargetNamespace());
	}

}
