/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools.schema;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.library.model.v2.SpdxConstantsCompatV2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Convert OWL RDF schema to a JSON Context file
 * 
 * @author Gary O'Neall
 */
public class OwlToJsonContext extends AbstractOwlRdfConverter {

	static final Map<String, String> NAMESPACES;
	
	static {
		Map<String, String> namespaceMap = new HashMap<>();
		namespaceMap.put(SpdxConstantsCompatV2.SPDX_NAMESPACE, "spdx");
		namespaceMap.put(SpdxConstantsCompatV2.RDFS_NAMESPACE, "rdfs");
		namespaceMap.put(SpdxConstantsCompatV2.RDF_NAMESPACE, "rdf");
		namespaceMap.put(SpdxConstantsCompatV2.RDF_POINTER_NAMESPACE, "rdfpointer");
		namespaceMap.put(SpdxConstantsCompatV2.OWL_NAMESPACE, "owl");
		namespaceMap.put(SpdxConstantsCompatV2.DOAP_NAMESPACE, "doap");
		namespaceMap.put(SpdxConstantsCompatV2.XML_SCHEMA_NAMESPACE, "xs");
		NAMESPACES = Collections.unmodifiableMap(namespaceMap);
	}
	
	public static final ObjectMapper JSON_MAPPER = new ObjectMapper()
			.enable(SerializationFeature.INDENT_OUTPUT);
	
	public OwlToJsonContext(OntModel model) {
		super(model);
	}

	/**
	 * @return Object node containing a JSON context for the model
	 */
	public ObjectNode convertToContext() {
		ObjectNode contexts = JSON_MAPPER.createObjectNode();
		NAMESPACES.forEach((namespace, name) -> {
			contexts.put(name, namespace);
		});
		// Manually added contexts - specific to the JSON format
		ObjectNode documentContext = JSON_MAPPER.createObjectNode();
		documentContext.put("@type", "spdx:SpdxDocument");
		documentContext.put("@id", "spdx:spdxDocument");
		contexts.set("Document", documentContext);
		contexts.put(SpdxConstantsCompatV2.SPDX_IDENTIFIER, "@id");
		contexts.put(SpdxConstantsCompatV2.EXTERNAL_DOCUMENT_REF_IDENTIFIER, "@id");
		TreeMap<String, OntProperty> sortedOntProperties = new TreeMap<>();
		ExtendedIterator<OntProperty> iter = model.listAllOntProperties();
		while (iter.hasNext()) {
			OntProperty property = iter.next();
			if (property.isURIResource()) {
				String propNamespace = uriToNamespace(property.getURI());
				String propName = uriToPropName(property.getURI());
				String id = propNamespace + propName;
				sortedOntProperties.put(id, property);
			}
		}
		for (Entry<String, OntProperty> ontPropEntry:sortedOntProperties.entrySet()) {
			String propNamespace = uriToNamespace(ontPropEntry.getValue().getURI());
			String propName = uriToPropName(ontPropEntry.getValue().getURI());
			
			PropertyRestrictions propertyRestrictions = new PropertyRestrictions(ontPropEntry.getValue());
			boolean hasListProperty = propertyRestrictions.isListProperty();
			String typeUri = propertyRestrictions.getTypeUri();
			if (hasListProperty) {
				String listPropName = MultiFormatStore.propertyNameToCollectionPropertyName(propName);
				ObjectNode listPropContext = JSON_MAPPER.createObjectNode();
				listPropContext.put("@id", propNamespace + listPropName);
				if (Objects.nonNull(typeUri)) {
					listPropContext.put("@type", uriToNamespace(typeUri) + uriToPropName(typeUri));
				}
				listPropContext.put("@container", "@set");
				contexts.set(listPropName, listPropContext);
			} if (propertyRestrictions.isSingleProperty()) {
				ObjectNode propContext = JSON_MAPPER.createObjectNode();
				propContext.put("@id", ontPropEntry.getKey());
				if (Objects.nonNull(typeUri)) {
					propContext.put("@type", uriToNamespace(typeUri) + uriToPropName(typeUri));
				}
				contexts.set(propName, propContext);
			}
		}
		ObjectNode context = JSON_MAPPER.createObjectNode();
		context.set("@context", contexts);
		return context;
	}
	

	/**
	 * @param uri
	 * @return The namespace portion of the URI
	 */
	private String uriToNamespace(String uri) {
		int poundIndex = uri.lastIndexOf('#');
		String propNamespace = uri.substring(0, poundIndex+1);
		if (NAMESPACES.containsKey(propNamespace)) {
			propNamespace = NAMESPACES.get(propNamespace) + ":";
		}
		return propNamespace;
	}
	
	/**
	 * @param uri
	 * @return The name portion of the URI
	 */
	private String uriToPropName(String uri) {
		int poundIndex = uri.lastIndexOf('#');
		return checkConvertRenamedPropertyName(uri.substring(poundIndex+1));
	}

}
