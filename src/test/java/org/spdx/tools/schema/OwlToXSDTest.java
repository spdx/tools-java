package org.spdx.tools.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;

import junit.framework.TestCase;

public class OwlToXSDTest extends TestCase {
	
	static final String OWL_FILE_PATH = "testResources" + File.separator + "spdx-2-2-revision-8-onotology.owl.xml";

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testConvertToXsd() throws IOException, XmlSchemaSerializerException, SchemaException {
		OwlToXSD otx = null;
		try (InputStream is = new FileInputStream(new File(OWL_FILE_PATH))) {
			OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
			model.read(is, "RDF/XML");
			otx = new OwlToXSD(model);
		}
		XmlSchema result = otx.convertToXsd();
		
		try (StringWriter sw = new StringWriter()) {
			result.write(sw);
			String str = sw.toString();
			int i = 0;
		}
	}

}
