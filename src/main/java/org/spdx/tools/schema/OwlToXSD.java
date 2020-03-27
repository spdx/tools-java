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
package org.spdx.tools.schema;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.namespace.QName;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Converts from OWL RDF/XML to XML Schema (XSD)
 * 
 * 
 * @author Gary O'Neall
 *
 */
public class OwlToXSD extends AbstractOwlRdfConverter {
	
	XmlSchemaCollection schemas = new XmlSchemaCollection();

	public OwlToXSD(OntModel model) {
		super(model);
	}
	
	public XmlSchema convertToXsd() {
		XmlSchema schema = new XmlSchema(ontology.getNameSpace(), schemas);
		schema.setInputEncoding(StandardCharsets.UTF_8.name());
		// TODO: see if there are any namespaces we want to add
		NamespaceMap namespaces = new NamespaceMap();
		schema.setNamespaceContext(namespaces);
		getTypesFromModel().forEachRemaining(type -> {
			try {
				addTypeToSchema(schema, type);
			} catch (XmlSchemaSerializerException e) {
				throw new RuntimeException(e);
			}
		});
		XmlSchemaSimpleType test = new XmlSchemaSimpleType(schema, true);
		XmlSchemaSimpleTypeRestriction content = new XmlSchemaSimpleTypeRestriction();
		content.setBaseTypeName(new QName("testtype"));
		test.setContent(content);
		return schema;
	}

	private void addTypeToSchema(XmlSchema schema, OntClass type) throws XmlSchemaSerializerException {
		XmlSchemaComplexType xmlType = new XmlSchemaComplexType(schema, true);
		xmlType.setName(type.getLocalName());
		String comment = type.getComment(null);
		if (Objects.nonNull(comment)) {
			XmlSchemaAnnotation annotation = new XmlSchemaAnnotation();
			XmlSchemaDocumentation schemaDoc = new XmlSchemaDocumentation();
			xmlType.setAnnotation(annotation);
			//FIXME: This is very kludgy - there has to be an easier way to create the markup
			final Text commentNode = schema.getSchemaDocument().createTextNode(comment);
			NodeList markup = new NodeList() {

				@Override
				public Node item(int index) {
					if (index == 0) {
						return commentNode;
					} else {
						return null;
					}
				}

				@Override
				public int getLength() {
					return 1;
				}
				
			};
			schemaDoc.setMarkup(markup);
			annotation.getItems().add(schemaDoc);
		}
	}

	private ExtendedIterator<OntClass> getTypesFromModel() {
		return model.listClasses().filterKeep(ontClass -> {
			return ontClass.isURIResource();
		});
	}

}
