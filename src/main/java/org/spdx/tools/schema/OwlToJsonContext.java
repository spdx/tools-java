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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.library.SpdxConstants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Convert OWL RDF schema to a JSON Context file
 * 
 * @author Gary O'Neall
 *
 */
public class OwlToJsonContext extends AbstractOwlRdfConverter {

	static final Map<String, String> NAMESPACES;
	
	static {
		Map<String, String> namespaceMap = new HashMap<>();
		namespaceMap.put(SpdxConstants.SPDX_NAMESPACE, "spdx");
		namespaceMap.put(SpdxConstants.RDFS_NAMESPACE, "rdfs");
		namespaceMap.put(SpdxConstants.RDF_NAMESPACE, "rdf");
		namespaceMap.put(SpdxConstants.RDF_POINTER_NAMESPACE, "rdfpointer");
		namespaceMap.put(SpdxConstants.OWL_NAMESPACE, "owl");
		namespaceMap.put(SpdxConstants.DOAP_NAMESPACE, "doap");
		namespaceMap.put(SpdxConstants.XML_SCHEMA_NAMESPACE, "xs");
		NAMESPACES = Collections.unmodifiableMap(namespaceMap);
	}
	
	public static ObjectMapper jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
	
	public OwlToJsonContext(OntModel model) {
		super(model);
	}

	/**
	 * @return Object node containing a JSON context for the model
	 */
	public ObjectNode convertToContext() {
		ObjectNode contexts = jsonMapper.createObjectNode();
		NAMESPACES.forEach((namespace, name) -> {
			contexts.put(name, namespace);
		});
		ExtendedIterator<OntProperty> iter = model.listAllOntProperties();
		while (iter.hasNext()) {
			OntProperty property = iter.next();
			String propName = uriToPropName(property.getURI());
			String propNamespace = uriToNamespace(property.getURI());
			PropertyRestrictions propertyRestrictions = new PropertyRestrictions(property);
			boolean hasListProperty = propertyRestrictions.isListProperty();
			String typeUri = propertyRestrictions.getTypeUri();
			if (hasListProperty) {
				String listPropName = MultiFormatStore.propertyNameToCollectionPropertyName(propName);
				ObjectNode listPropContext = jsonMapper.createObjectNode();
				listPropContext.put("@id", propNamespace + listPropName);
				if (Objects.nonNull(typeUri)) {
					listPropContext.put("@type", uriToNamespace(typeUri) + uriToPropName(typeUri));
				}
				listPropContext.put("@container", "@set");
				contexts.set(listPropName, listPropContext);
			} if (propertyRestrictions.isSingleProperty()) {
		        ObjectNode propContext = jsonMapper.createObjectNode();
		        propContext.put("@id", propNamespace + propName);
                if (Objects.nonNull(typeUri)) {
                    propContext.put("@type", uriToNamespace(typeUri) + uriToPropName(typeUri));
                }
                contexts.set(propName, propContext);
			}
		}
		// Manually added contexts - specific to the JSON format
		ObjectNode documentContext = jsonMapper.createObjectNode();
		documentContext.put("@type", "spdx:SpdxDocument");
		documentContext.put("@id", "spdx:spdxDocument");
		contexts.set("Document", documentContext);
		contexts.put(SpdxConstants.SPDX_IDENTIFIER, "@id");
		contexts.put(SpdxConstants.EXTERNAL_DOCUMENT_REF_IDENTIFIER, "@id");
		ObjectNode context = jsonMapper.createObjectNode();
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
