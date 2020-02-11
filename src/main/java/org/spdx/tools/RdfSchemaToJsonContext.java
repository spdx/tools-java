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
package org.spdx.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.spdx.jsonstore.MultiFormatStore;
import org.spdx.library.SpdxConstants;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Convert an RDF schema file containing SPDX property to a JSON context file for all properties in the SPDX namspace
 * @author Gary O'Neall
 *
 */
public class RdfSchemaToJsonContext {
	
	static final Map<String, String> NAMESPACES;
	
	static {
		Map<String, String> namespaceMap = new HashMap<>();
		namespaceMap.put(SpdxConstants.SPDX_NAMESPACE, "spdx");
		namespaceMap.put(SpdxConstants.RDFS_NAMESPACE, "rdfs");
		namespaceMap.put(SpdxConstants.RDF_NAMESPACE, "rdf");
		namespaceMap.put(SpdxConstants.RDF_POINTER_NAMESPACE, "rdfpointer");
		namespaceMap.put(SpdxConstants.OWL_NAMESPACE, "owl");
		namespaceMap.put(SpdxConstants.DOAP_NAMESPACE, "doap");
		NAMESPACES = Collections.unmodifiableMap(namespaceMap);
	}

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
			System.err.println("Output file "+args[0]+" already exists.");
			usage();
			return;
		}
		
		ObjectMapper jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream(fromFile);
			OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
			model.read(is, "RDF/XML");
			Property qualCardProperty = model.createProperty("http://www.w3.org/2002/07/owl#qualifiedCardinality");
			Property maxQualCardProperty = model.createProperty("http://www.w3.org/2002/07/owl#maxQualifiedCardinality");
			Property maxCardProperty = model.createProperty("http://www.w3.org/2002/07/owl#maxCardinality");
			Property cardProperty = model.createProperty("http://www.w3.org/2002/07/owl#cardinality");
			Property minCardProperty = model.createProperty("http://www.w3.org/2002/07/owl#minCardinality");
			Property minQualCardProperty = model.createProperty("http://www.w3.org/2002/07/owl#minQualifiedCardinality");
			Property owlClassProperty = model.createProperty("http://www.w3.org/2002/07/owl#onClass");
			ObjectNode contexts = jsonMapper.createObjectNode();
			NAMESPACES.forEach((namespace, name) -> {
				contexts.put(name, namespace);
			});
			ExtendedIterator<OntProperty> iter = model.listAllOntProperties();
			while (iter.hasNext()) {
				OntProperty property = iter.next();
				String propName = uriToPropName(property.getURI());
				String propNamespace = uriToNamespace(property.getURI());
				ObjectNode propContext = jsonMapper.createObjectNode();
				propContext.put("@id", propNamespace + propName);
				String type = null;
				boolean hasListProperty = false;	// If any of the cardinalities is > 1, this will be set to true
				ExtendedIterator<Restriction> restrictionIter = property.listReferringRestrictions();
				while (restrictionIter.hasNext()) {
					Restriction r = restrictionIter.next();
					RDFNode typePropertyValue = r.getPropertyValue(owlClassProperty);
					if (Objects.nonNull(typePropertyValue) && typePropertyValue.isURIResource()) {
						String typeUri = typePropertyValue.asResource().getURI();
						type = uriToNamespace(typeUri) + uriToPropName(typeUri);
					}
					RDFNode qualCardPropValue = r.getPropertyValue(qualCardProperty);
					RDFNode cardPropValue = r.getPropertyValue(cardProperty);
					RDFNode maxQualCardPropValue = r.getPropertyValue(maxQualCardProperty);
					RDFNode maxCardPropValue = r.getPropertyValue(maxCardProperty);
					RDFNode minCardPropValue = r.getPropertyValue(minCardProperty);
					RDFNode minQualCardPropValue = r.getPropertyValue(minQualCardProperty);
					if (Objects.nonNull(qualCardPropValue) && qualCardPropValue.isLiteral()) {
						if (qualCardPropValue.asLiteral().getInt() > 1) {
							hasListProperty = true;
						}
					} else if (Objects.nonNull(cardPropValue) && cardPropValue.isLiteral()) {
						if (cardPropValue.asLiteral().getInt() > 1) {
							hasListProperty = true;
						}
					} else if (Objects.nonNull(maxQualCardPropValue) && maxQualCardPropValue.isLiteral()) {
						if (maxQualCardPropValue.asLiteral().getInt() > 1) {
							hasListProperty = true;
						}
					} else if (Objects.nonNull(maxCardPropValue) && maxCardPropValue.isLiteral()) {
						if (maxCardPropValue.asLiteral().getInt() > 1) {
							hasListProperty = true;
						}
					} else if (Objects.nonNull(minCardPropValue) && minCardPropValue.isLiteral()) {
							hasListProperty = true;
					} else if (Objects.nonNull(minQualCardPropValue) && minQualCardPropValue.isLiteral()) {
						hasListProperty = true;
					}
				}
				if (Objects.nonNull(type)) {
					propContext.put("@type", type);
				}
				contexts.set(propName, propContext);
				if (hasListProperty) {
					String listPropName = MultiFormatStore.propertyNameToCollectionPropertyName(propName);
					ObjectNode listPropContext = jsonMapper.createObjectNode();
					listPropContext.put("@id", propNamespace + listPropName);
					if (Objects.nonNull(type)) {
						listPropContext.put("@type", type);
					}
					listPropContext.put("@container", "@set");
					contexts.set(listPropName, listPropContext);
				}
			}
			// Manually added contexts - specific to the JSON format
			ObjectNode documentContext = jsonMapper.createObjectNode();
			documentContext.put("@type", "spdx:SpdxDocument");
			documentContext.put("@id", "spdx:spdxDocument");
			contexts.set("Document", documentContext);
			contexts.put("SPDXID", "@id");
			ObjectNode context = jsonMapper.createObjectNode();
			context.set("@context", contexts);
			os = new FileOutputStream(toFile);
			jsonMapper.writeTree(jsonMapper.getFactory().createGenerator(os).useDefaultPrettyPrinter(), 
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
			if (Objects.nonNull(is)) {
				try {
					is.close();
				} catch (IOException e) {
					System.err.println("Error closing input file stream: "+e.getMessage());
				}
			}
			if (Objects.nonNull(os)) {
				try {
					os.close();
				} catch (IOException e) {
					System.err.println("Error closing output file stream: "+e.getMessage());
				}
			}
		}

	}

	/**
	 * @param uri
	 * @return The namespace portion of the URI
	 */
	private static String uriToNamespace(String uri) {
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
	private static String uriToPropName(String uri) {
		int poundIndex = uri.lastIndexOf('#');
		return uri.substring(poundIndex+1);
	}

	public static void usage() {
		System.out.println("Usage:");
		System.out.println("RdfSchemaToJsonContext rdfSchemaFile jsonContextFile");
		System.out.println("\trdfSchemaFile RDF schema file in RDF/XML format");
		System.out.println("\trdfSchemaFile output JSON context file");
	}

}
