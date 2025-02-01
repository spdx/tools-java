/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools.schema;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContentType;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.spdx.library.model.v2.SpdxConstantsCompatV2;
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
public class OwlToXsd extends AbstractOwlRdfConverter {
	
	XmlSchemaCollection schemas = new XmlSchemaCollection();

	public OwlToXsd(OntModel model) {
		super(model);
	}
	
	public XmlSchema convertToXsd() throws XmlSchemaSerializerException, SchemaException {
		if (!ontology.isURIResource()) {
			throw new SchemaException("Ontology is not a URI resource");
		}
		String nameSpace = ontology.getURI();
		XmlSchema schema = new XmlSchema(nameSpace, schemas);
		schema.setSchemaNamespacePrefix(nameSpace);
		schema.setInputEncoding(StandardCharsets.UTF_8.name());
		schema.setElementFormDefault(XmlSchemaForm.QUALIFIED);
		NamespaceMap namespaces = new NamespaceMap();
		namespaces.add("xs", "http://www.w3.org/2001/XMLSchema");
		schema.setNamespaceContext(namespaces);
		addDocumentation(schema, schema, ontology.getComment(null));

		ExtendedIterator<OntClass> classIter = model.listClasses().filterKeep(ontClass -> {
			return ontClass.isURIResource();
		});
		while (classIter.hasNext()) {
			OntClass type = classIter.next();
			addTypeToSchema(schema, type);
		}
		
		// Add the top level element
		XmlSchemaElement documentElement = new XmlSchemaElement(schema, true);
		documentElement.setName("Document");
		documentElement.setSchemaTypeName(new QName(SpdxConstantsCompatV2.SPDX_NAMESPACE.substring(0, SpdxConstantsCompatV2.SPDX_NAMESPACE.length()-1), "SpdxDocument"));
		addDocumentation(schema, documentElement, "Top level element for the SPDX document");
		return schema;
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
	
	private XmlSchemaType addComplexTypeToSchema(XmlSchema schema, OntClass type) throws XmlSchemaSerializerException, SchemaException {
		XmlSchemaComplexType xmlType = new XmlSchemaComplexType(schema, true);
		xmlType.setName(type.getLocalName());
		addDocumentation(schema, xmlType, type.getComment(null));
		XmlSchemaComplexContentExtension schemaExtension = null;
		ExtendedIterator<OntClass> superClassIter = type.listSuperClasses(true).filterKeep(sc -> {
			return sc.isURIResource() && 
					!"http://www.w3.org/2000/01/rdf-schema#Container".equals(sc.getURI());
		});
		
		if (superClassIter.hasNext()) {
			OntClass superClass = superClassIter.next();
			schemaExtension = new XmlSchemaComplexContentExtension();
			schemaExtension.setBaseTypeName(new QName(SpdxConstantsCompatV2.SPDX_NAMESPACE.substring(0, SpdxConstantsCompatV2.SPDX_NAMESPACE.length()-1), superClass.getLocalName()));
		}
		if (superClassIter.hasNext()) {
			throw new SchemaException("Ambiguous superclasses for "+type.getLocalName());
		}
		Collection<OntProperty> ontProperties = propertiesFromClassRestrictions(type, true);
		xmlType.setContentType(XmlSchemaContentType.ELEMENT_ONLY);
		XmlSchemaSequence sequence = new XmlSchemaSequence();
		for (OntProperty property:ontProperties) {
			if (SKIPPED_PROPERTIES.contains(property.getURI())) {
				continue;
			}
			PropertyRestrictions restrictions = getPropertyRestrictions(type, property);
			XmlSchemaElement propertyMember = new XmlSchemaElement(schema, false);
			propertyMember.setName(property.getLocalName());
			Optional<Resource> rdfType = getPropertyType(property);
			if (rdfType.isPresent()) {
				String typeNamespace;
				if (rdfType.get().getNameSpace().equals(SpdxConstantsCompatV2.RDF_POINTER_NAMESPACE) || 
						rdfType.get().getNameSpace().equals(SpdxConstantsCompatV2.DOAP_NAMESPACE)) {
					typeNamespace = SpdxConstantsCompatV2.SPDX_NAMESPACE.substring(0, SpdxConstantsCompatV2.SPDX_NAMESPACE.length()-1);
				} else {
					typeNamespace = rdfType.get().getNameSpace().substring(0, rdfType.get().getNameSpace().length()-1);
				}
				propertyMember.setSchemaTypeName(
						new QName(typeNamespace, rdfType.get().getLocalName()));
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
			sequence.getItems().add(propertyMember);
		}
		if (type.getLocalName().equals(SpdxConstantsCompatV2.CLASS_SPDX_ELEMENT)) {
			// Manually add in SPDXID
			XmlSchemaElement propertyMember = new XmlSchemaElement(schema, false);
			propertyMember.setName("SPDXID");
			propertyMember.setSchemaTypeName(
					new QName("http://www.w3.org/2001/XMLSchema", "string"));
			sequence.getItems().add(propertyMember);
		}
		XmlSchemaComplexContent contentModel = new XmlSchemaComplexContent();
		if (Objects.nonNull(schemaExtension)) {
			if (sequence.getItems().size() > 0) {
				schemaExtension.setParticle(sequence);
			}
			contentModel.setContent(schemaExtension);
			xmlType.setContentModel(contentModel);
		} else if (sequence.getItems().size() > 0) {
			xmlType.setParticle(sequence);
		} else {
			xmlType.setAbstract(true);
		}
		return xmlType;
	}

	private void addEnumTypeToSchema(XmlSchema schema, OntClass type, List<Individual> individuals) throws XmlSchemaSerializerException {
		XmlSchemaSimpleType xmlType = new XmlSchemaSimpleType(schema, true);
		xmlType.setName(type.getLocalName());
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
