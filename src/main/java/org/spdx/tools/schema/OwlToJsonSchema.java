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

import java.util.Collection;
import java.util.Objects;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.SpdxJsonLDContext;
import org.spdx.library.SpdxConstants;
import org.spdx.library.model.ReferenceType;
import org.spdx.library.model.SpdxElement;
import org.spdx.library.model.SpdxModelFactory;
import org.spdx.library.model.license.AnyLicenseInfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Gary O'Neall
 *
 * Converts from RDF/OWL RDF/XML documents to JSON Schema draft 7
 * 
 */
public class OwlToJsonSchema extends AbstractOwlRdfConverter {
	
	private static final String SCHEMA_VERSION_URI = "http://json-schema.org/draft-07/schema#";
	private static final String RELATIONSHIP_TYPE = SpdxConstants.SPDX_NAMESPACE + SpdxConstants.CLASS_RELATIONSHIP;
	static ObjectMapper jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	public OwlToJsonSchema(OntModel model) {
		super(model);
	}

	public ObjectNode convertToJsonSchema() {
		ObjectNode root = jsonMapper.createObjectNode();
		root.put("$schema", SCHEMA_VERSION_URI);
		ExtendedIterator<Ontology> ontologyIter = model.listOntologies();
		if (ontologyIter.hasNext()) {
			Ontology ont = ontologyIter.next();
			String ontologyUri = ont.getURI();
			if (Objects.nonNull(ontologyUri)) {
				root.put("$id", ontologyUri);
			}
			String title = ont.getLabel(null);
			if (Objects.nonNull(title)) {
				root.put("title", title);
			}
		}
		root.put("type","object");
		ObjectNode properties = jsonMapper.createObjectNode();
		OntClass docClass = model.getOntClass(SpdxConstants.SPDX_NAMESPACE + SpdxConstants.CLASS_SPDX_DOCUMENT);
		Objects.requireNonNull(docClass, "Missing SpdxDocument class in OWL document");
		ObjectNode documentClassSchema = ontClassToJsonSchema(docClass);
		// Add in the extra properties
		ObjectNode docSchemaProperties = (ObjectNode)documentClassSchema.get("properties");
		OntClass packageClass = model.getOntClass(SpdxConstants.SPDX_NAMESPACE + SpdxConstants.CLASS_SPDX_PACKAGE);
		Objects.requireNonNull(packageClass, "Missing SPDX Package class in OWL document");
		docSchemaProperties.set("packages", toArrayPropertySchema(packageClass, 0));
		OntClass fileClass = model.getOntClass(SpdxConstants.SPDX_NAMESPACE + SpdxConstants.CLASS_SPDX_FILE);
		Objects.requireNonNull(fileClass, "Missing SPDX File class in OWL document");
		docSchemaProperties.set("files", toArrayPropertySchema(fileClass, 0));
		OntClass snippetClass = model.getOntClass(SpdxConstants.SPDX_NAMESPACE + SpdxConstants.CLASS_SPDX_SNIPPET);
		Objects.requireNonNull(snippetClass, "Missing SPDX Snippet class in OWL document");
		docSchemaProperties.set("snippets", toArrayPropertySchema(snippetClass, 0));
		OntClass relationshipClass = model.getOntClass(SpdxConstants.SPDX_NAMESPACE + SpdxConstants.CLASS_RELATIONSHIP);
		Objects.requireNonNull(relationshipClass, "Missing SPDX Relationship class in OWL document");
		docSchemaProperties.set("relationships", toArrayPropertySchema(relationshipClass, 0));
		
		properties.set("Document", documentClassSchema);
		root.set("properties", properties);
		return root;
	}
	
	/**
	 * @param ontClass
	 * @param jsonMapper
	 * @param min Minimum number of array items
	 * @return JSON Schema of an array of item types represented by the ontClass
	 */
	private JsonNode toArrayPropertySchema(OntClass ontClass, int min) {
		ObjectNode classSchema = ontClassToJsonSchema(ontClass);
		ObjectNode property = jsonMapper.createObjectNode();
		property.put("description", checkConvertRenamedPropertyName(ontClass.getLocalName()) + "s referenced in the SPDX document");
		property.put("type", "array");
		property.set("items", classSchema);
		if (min > 0) {
			property.put("minItems", min);
		}
		return property;
	}

	/**
	 * @param spdxClass RDF ontology class
	 * @return JSON Schema 
	 */
	private ObjectNode ontClassToJsonSchema(OntClass spdxClass) {
		ObjectNode retval = jsonMapper.createObjectNode();
		retval.put("type", "object");
		ObjectNode properties = jsonMapper.createObjectNode();
		Collection<OntProperty> ontProperties = propertiesFromClassRestrictions(spdxClass);
		for (OntProperty property:ontProperties) {
			if (SKIPPED_PROPERTIES.contains(property.getURI())) {
				continue;
			}
			PropertyRestrictions restrictions = getPropertyRestrictions(spdxClass, property);
			Objects.requireNonNull(restrictions.getTypeUri(), "Missing type for property "+property.getLocalName());
			if (restrictions.getTypeUri().equals(RELATIONSHIP_TYPE)) {
				continue;
			}
			if (restrictions.isListProperty()) {
				properties.set(MultiFormatStore.propertyNameToCollectionPropertyName(
						checkConvertRenamedPropertyName(property.getLocalName())),
						derivePropertySchema(property, restrictions, true));
			} else {
				properties.set(checkConvertRenamedPropertyName(property.getLocalName()),	derivePropertySchema(property, restrictions, false));
			}
			
		}
		retval.set("properties", properties);
		return retval;
	}

	/**
	 * Derive the schema for a property based on the property restrictions
	 * @param property property for the schema
	 * @param restrictions OWL restrictions for the property
	 * @param list true if the schema should be developed for an array (the restrictions isList is not used in this method)
	 * @return
	 */
	private ObjectNode derivePropertySchema(OntProperty property, PropertyRestrictions restrictions,
			boolean list) {
		//TODO: refactor - just a bit too complex
		ObjectNode propertySchema = jsonMapper.createObjectNode();
		Statement commentStatement = property.getProperty(commentProperty);
		if (Objects.nonNull(commentStatement) && Objects.nonNull(commentStatement.getObject())
				&& commentStatement.getObject().isLiteral()) {
			propertySchema.put("description", commentStatement.getObject().asLiteral().getString());
		}
		if (list) {
			propertySchema.put("type", "array");
			propertySchema.set("items", derivePropertySchema(property, restrictions, false));
			if (restrictions.getAbsoluteCardinality() > 0) {
				propertySchema.put("minItems", restrictions.getAbsoluteCardinality());
				propertySchema.put("maxItems", restrictions.getAbsoluteCardinality());
			}
			if (restrictions.getMinCardinality() > 0) {
				propertySchema.put("minItems", restrictions.getMinCardinality());
			}
			if (restrictions.getMaxCardinality() > 0) {
				propertySchema.put("maxItems", restrictions.getMaxCardinality());
			}
		} else if (restrictions.isEnumProperty()) {
				propertySchema.put("type", "string");
				ArrayNode enums = jsonMapper.createArrayNode();
				for (String val:restrictions.getEnumValues()) {
					enums.add(val);
				}
				propertySchema.set("enum", enums);
		} else if (restrictions.getTypeUri().equals("http://www.w3.org/2000/01/rdf-schema#Literal")) {
			propertySchema.put("type", "string");
		} else if (restrictions.getTypeUri().startsWith(SpdxConstants.XML_SCHEMA_NAMESPACE)) {
				// Primitive type
			String primitiveType = restrictions.getTypeUri().substring(SpdxConstants.XML_SCHEMA_NAMESPACE.length());
			Class<? extends Object> primitiveClass = SpdxJsonLDContext.XMLSCHEMA_TYPE_TO_JAVA_CLASS.get(primitiveType);
			Objects.requireNonNull(primitiveClass, "No primitive class found for type "+restrictions.getTypeUri());
			if (Boolean.class.equals(primitiveClass)) {
				propertySchema.put("type", "boolean");
			} else if (String.class.equals(primitiveClass)) {
				propertySchema.put("type", "string");
			} else if (Integer.class.equals(primitiveClass)) {
				propertySchema.put("type", "integer");
			} else {
				throw new RuntimeException("Unknown primitive class "+primitiveType);
			}
		} else if (restrictions.getTypeUri().startsWith(SpdxConstants.SPDX_NAMESPACE)) {
			String spdxType = restrictions.getTypeUri().substring(SpdxConstants.SPDX_NAMESPACE.length());
			Class<? extends Object> clazz = SpdxModelFactory.SPDX_TYPE_TO_CLASS.get(spdxType);
			if (Objects.nonNull(clazz) && (AnyLicenseInfo.class.isAssignableFrom(clazz))
					&& !SpdxConstants.PROP_SPDX_EXTRACTED_LICENSES.equals(checkConvertRenamedPropertyName(property.getLocalName()))) {
				// check for AnyLicenseInfo - these are strings with the exception of the extractedLicensingInfos which are the actual license description
				JsonNode description = propertySchema.get("description");
				if (Objects.isNull(description)) {
					propertySchema.put("description", "License expression");
				} else {
					propertySchema.put("description", "License expression for "+checkConvertRenamedPropertyName(property.getLocalName())+".  "+description.asText());
				}
				propertySchema.put("type", "string");
			} else if (Objects.nonNull(clazz) && SpdxElement.class.isAssignableFrom(clazz)) {
				// check for SPDX Elements - these are strings
				JsonNode description = propertySchema.get("description");
				if (Objects.isNull(description)) {
					propertySchema.put("description", "SPDX ID for "+spdxType);
				} else {
					propertySchema.put("description", "SPDX ID for "+spdxType+".  "+description.asText());
				}
				propertySchema.put("type", "string");
			} else if (Objects.nonNull(clazz) && ReferenceType.class.isAssignableFrom(clazz)) {
				// check for ReferenceType - these are strings URI's and not the full object description
				JsonNode description = propertySchema.get("description");
				if (Objects.nonNull(description)) {
					propertySchema.put("description", description.asText());
				}
				propertySchema.put("type", "string");
			} else {
				OntClass typeClass = model.getOntClass(restrictions.getTypeUri());
				Objects.requireNonNull(typeClass, "No type class found for "+restrictions.getTypeUri());
				propertySchema = ontClassToJsonSchema(typeClass);
				commentStatement = typeClass.getProperty(commentProperty);
				if (Objects.nonNull(commentStatement) && Objects.nonNull(commentStatement.getObject())
						&& commentStatement.getObject().isLiteral()) {
					// replace the property comment with the class comment
					propertySchema.put("description", commentStatement.getObject().asLiteral().getString());
				}
			}
		} else {
			OntClass typeClass = model.getOntClass(restrictions.getTypeUri());
			commentStatement = typeClass.getProperty(commentProperty);
			if (Objects.nonNull(commentStatement) && Objects.nonNull(commentStatement.getObject())
					&& commentStatement.getObject().isLiteral()) {
				// replace the property comment with the class comment
				propertySchema.put("description", commentStatement.getObject().asLiteral().getString());
			}
			Objects.requireNonNull(typeClass, "No type class found for "+restrictions.getTypeUri());
			propertySchema = ontClassToJsonSchema(typeClass);
		}
		return propertySchema;
	}
}
