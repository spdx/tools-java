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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContentType;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.utils.NamespaceMap;
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
	
	public XmlSchema convertToXsd() throws XmlSchemaSerializerException, SchemaException {
		String nameSpace = ontology.getURI();
		XmlSchema schema = new XmlSchema(nameSpace, schemas);
		schema.setInputEncoding(StandardCharsets.UTF_8.name());
		NamespaceMap namespaces = new NamespaceMap();
		schema.setNamespaceContext(namespaces);
		addDocumentation(schema, schema, ontology.getComment(null));
		ExtendedIterator<OntProperty> propertyIter = model.listAllOntProperties().filterKeep(objectProp -> {
			return objectProp.isURIResource();
		});
		while (propertyIter.hasNext()) {
			OntProperty property = propertyIter.next();
			addElementToSchema(schema, property);
		}
		
		ExtendedIterator<OntClass> classIter = model.listClasses().filterKeep(ontClass -> {
			return ontClass.isURIResource();
		});
		while (classIter.hasNext()) {
			OntClass type = classIter.next();
			addTypeToSchema(schema, type);
		}
		return schema;
	}

	private XmlSchemaElement addElementToSchema(XmlSchema schema, OntProperty dataProp) throws SchemaException, XmlSchemaSerializerException {
		XmlSchemaElement retval = new XmlSchemaElement(schema, true);
		retval.setName(dataProp.getLocalName());
		//TODO: Check the namespace for non-SPDX namespaces
//		retval.setId(dataProp.getLocalName());
		Optional<OntResource> rdfType = getPropertyType(dataProp);
		if (rdfType.isPresent()) {
			retval.setSchemaTypeName(new QName(
					rdfType.get().getNameSpace().substring(0, rdfType.get().getNameSpace().length()-1), 
					rdfType.get().getLocalName()));
		}
		addDocumentation(schema, retval, dataProp.getComment(null));
		return retval;
	}
	
	private XmlSchemaType addComplexTypeToSchema(XmlSchema schema, OntClass type) throws XmlSchemaSerializerException, SchemaException {
		XmlSchemaComplexType xmlType = new XmlSchemaComplexType(schema, true);
		xmlType.setName(type.getLocalName());
		//TODO: Handle other namespaces
		addDocumentation(schema, xmlType, type.getComment(null));
		XmlSchemaComplexContentExtension schemaExtension = null;
		ExtendedIterator<OntClass> superClassIter = type.listSuperClasses(true).filterKeep(sc -> {
			return sc.isURIResource();
		});
		
		while (superClassIter.hasNext()) {
			OntClass superClass = superClassIter.next();
			if (!"http://www.w3.org/2000/01/rdf-schema#Container".equals(superClass.getURI())) {
				if (Objects.nonNull(schemaExtension)) {
					throw new SchemaException("Ambiguous superclasses for "+type.getLocalName());
				}
				schemaExtension = new XmlSchemaComplexContentExtension();
				schemaExtension.setBaseTypeName(new QName(superClass.getNameSpace().substring(0, superClass.getNameSpace().length()-1), superClass.getLocalName()));
			}
		}
		Collection<OntProperty> ontProperties = propertiesFromClassRestrictions(type, true);
		xmlType.setContentType(XmlSchemaContentType.ELEMENT_ONLY);
		XmlSchemaAll allRestriction = new XmlSchemaAll();
		for (OntProperty property:ontProperties) {
			if (SKIPPED_PROPERTIES.contains(property.getURI())) {
				continue;
			}
			PropertyRestrictions restrictions = getPropertyRestrictions(type, property);
			XmlSchemaElement propertyMember = new XmlSchemaElement(schema, false);
			propertyMember.setName(property.getLocalName());
			//TODO: Check the namespace for non-SPDX namespaces
			Optional<OntResource> rdfType = getPropertyType(property);
			if (rdfType.isPresent()) {
				propertyMember.setSchemaTypeName(
						new QName(rdfType.get().getNameSpace().substring(0, rdfType.get().getNameSpace().length()-1), 
						rdfType.get().getLocalName()));
			}
			addDocumentation(schema, propertyMember, property.getComment(null));
			if (restrictions.isOptional()) {
				propertyMember.setMinOccurs(0);
			} else {
				propertyMember.setMinOccurs(1);
			}
			if (restrictions.isListProperty()) {
				if (restrictions.getMinCardinality() > -1) {
					propertyMember.setMinOccurs(restrictions.getMinCardinality());
				}
				if (restrictions.getMaxCardinality() > -1) {
					propertyMember.setMaxOccurs(restrictions.getMaxCardinality());
				} else {
					propertyMember.setMaxOccurs(Long.MAX_VALUE);
				}
			} else {
				propertyMember.setMaxOccurs(1);
			}
			//TODO: Change the propertyMember to ref=
			allRestriction.getItems().add(propertyMember);
		}
		XmlSchemaComplexContent contentModel = new XmlSchemaComplexContent();
		if (Objects.nonNull(schemaExtension)) {
			schemaExtension.setParticle(allRestriction);
			contentModel.setContent(schemaExtension);
		} else {
			XmlSchemaComplexContentRestriction complexRestriction = new XmlSchemaComplexContentRestriction();
			complexRestriction.setParticle(allRestriction);
			contentModel.setContent(complexRestriction);
		}
		xmlType.setContentModel(contentModel);
		return xmlType;
	}

	private void addTypeToSchema(XmlSchema schema, OntClass type) throws XmlSchemaSerializerException, SchemaException {
		ExtendedIterator<Individual> individualIter = model.listIndividuals(type);
		if (individualIter.hasNext()) {
			// Enum type
			addEnumTypeToSchema(schema, type, individualIter.toList());
		} else {
			addComplexTypeToSchema(schema, type);
		}
	}

	private void addEnumTypeToSchema(XmlSchema schema, OntClass type, List<Individual> individuals) throws XmlSchemaSerializerException {
		XmlSchemaSimpleType xmlType = new XmlSchemaSimpleType(schema, true);
		xmlType.setName(type.getLocalName());
		//TODO: Handle other namespaces
		addDocumentation(schema, xmlType, type.getComment(null));
		XmlSchemaSimpleTypeRestriction xmlContent = new XmlSchemaSimpleTypeRestriction();
		xmlContent.setBaseTypeName(new QName("http://www.w3.org/2001/XMLSchema","string"));
		for (Individual individual:individuals) {
			XmlSchemaEnumerationFacet xmlEnum = new XmlSchemaEnumerationFacet();
			xmlEnum.setValue(individual.getLocalName());
			addDocumentation(schema, xmlEnum, individual.getComment(null));
			xmlContent.getFacets().add(xmlEnum);
		}
		xmlType.setContent(xmlContent);
	}

	/**
	 * Add documentation annotation to an XML schema node
	 * @param schema XML Schema
	 * @param xmlSchemaObject Schema object to add documentation
	 * @param documentation Documentation to add
	 * @throws XmlSchemaSerializerException 
	 */
	private void addDocumentation(XmlSchema schema, XmlSchemaAnnotated xmlSchemaObject, @Nullable String documentation) throws XmlSchemaSerializerException {
		Objects.requireNonNull(xmlSchemaObject);
		if (Objects.nonNull(documentation)) {
			XmlSchemaAnnotation annotation = new XmlSchemaAnnotation();
			XmlSchemaDocumentation schemaDoc = new XmlSchemaDocumentation();
			xmlSchemaObject.setAnnotation(annotation);
			//FIXME: This is very kludgy - there has to be an easier way to create the markup
			final Text commentNode = schema.getSchemaDocument().createTextNode(documentation);
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
}
