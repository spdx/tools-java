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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.spdx.library.SpdxConstants;
import org.spdx.library.model.enumerations.SpdxEnumFactory;

/**
 * Abstract class for implementing classes which convert from RDF/XML OWL format to some other format
 * 
 * @author Gary O'Neall
 *
 */
public class AbstractOwlRdfConverter {
	
	static final Set<String> SKIPPED_PROPERTIES;
	static {
		Set<String> skipped = new HashSet<>();
		skipped.add("http://www.w3.org/2003/06/sw-vocab-status/ns#term_status");
		
		SKIPPED_PROPERTIES = Collections.unmodifiableSet(skipped);
	}
	
	/**
	 * @author Gary O'Neall
	 * 
	 * Holds information on the property restrictions
	 *
	 */
	class PropertyRestrictions {

		private OntProperty property;
		private boolean listProperty = false;
		private boolean singleProperty = false;
		private String typeUri = null;
		private int absoluteCardinality = -1;
		private int minCardinality = -1;
		private int maxCardinality = -1;
		private boolean optional = true;
		private boolean enumProperty = false;
		Set<String> enumValues = new HashSet<>();

		public PropertyRestrictions(OntClass ontClass, OntProperty property) {
			Objects.requireNonNull(ontClass, "Missing required ontology class");
			Objects.requireNonNull(property, "Missing required property");
			this.property = property;
			List<Restriction> restrictions = getRestrictionsFromSuperclasses(ontClass, property);
			interpretRestrictions(restrictions);
		}
		
		/**
		 * Convert a list of restrictions for this property into the field values
		 * @param restrictions
		 */
		private void interpretRestrictions(List<Restriction> restrictions) {
			for (Restriction r:restrictions) {
				RDFNode typePropertyValue = r.getPropertyValue(owlClassProperty);
				if (Objects.nonNull(typePropertyValue) && typePropertyValue.isURIResource()) {
					typeUri = typePropertyValue.asResource().getURI();
					// check to see if this type is an enumeration type
					OntClass typeClass = model.getOntClass(typeUri);
					if (Objects.nonNull(typeClass)) {
						ExtendedIterator<Individual> individualIter = model.listIndividuals(typeClass);
						while (individualIter.hasNext()) {
							Individual individual = individualIter.next();
							if (individual.isURIResource()) {
								Enum<?> e = SpdxEnumFactory.uriToEnum.get(individual.getURI());
								if (Objects.nonNull(e)) {
									this.enumValues.add(e.toString());
									this.enumProperty = true;
								}
							}
						}
					}
					
				} else {
					// Check for enumeration types as a direct restriction
					NodeIterator hasValueIter = r.listPropertyValues(hasValueProperty);
					while (hasValueIter.hasNext()) {
						RDFNode hasValue = hasValueIter.next();
						if (hasValue.isURIResource()) {
							Enum<?> e = SpdxEnumFactory.uriToEnum.get(hasValue.asResource().getURI());
							if (Objects.nonNull(e)) {
								this.enumValues.add(e.toString());
								this.enumProperty = true;
							}
						}
					}
				}
				// cardinality
				RDFNode qualCardPropValue = r.getPropertyValue(qualCardProperty);
				RDFNode cardPropValue = r.getPropertyValue(cardProperty);
				RDFNode maxQualCardPropValue = r.getPropertyValue(maxQualCardProperty);
				RDFNode maxCardPropValue = r.getPropertyValue(maxCardProperty);
				RDFNode minCardPropValue = r.getPropertyValue(minCardProperty);
				RDFNode minQualCardPropValue = r.getPropertyValue(minQualCardProperty);
				if (Objects.nonNull(qualCardPropValue) && qualCardPropValue.isLiteral()) {
					absoluteCardinality = qualCardPropValue.asLiteral().getInt();
					if (absoluteCardinality > 0) {
						optional = false;
					}
					if (absoluteCardinality > 1) {
						listProperty = true;
					} else {
						singleProperty = true;
					}
				} else if (Objects.nonNull(cardPropValue) && cardPropValue.isLiteral()) {
					absoluteCardinality = cardPropValue.asLiteral().getInt();
					if (absoluteCardinality > 0) {
						optional = false;
					}
					if (absoluteCardinality > 1) {
						listProperty = true;
					} else {
						singleProperty = true;
					}
				} else if (Objects.nonNull(maxQualCardPropValue) && maxQualCardPropValue.isLiteral()) {
					maxCardinality = maxQualCardPropValue.asLiteral().getInt();
					if (maxCardinality > 1) {
						listProperty = true;
					} else if (maxCardinality == 1) {
						singleProperty = true;
					}
				} else if (Objects.nonNull(maxCardPropValue) && maxCardPropValue.isLiteral()) {
					maxCardinality = maxCardPropValue.asLiteral().getInt();
					if (maxCardinality > 1) {
						listProperty = true;
					} else if (maxCardinality == 1) {
						singleProperty = true;
					}
				} else if (Objects.nonNull(minCardPropValue) && minCardPropValue.isLiteral()) {
					minCardinality = minCardPropValue.asLiteral().getInt();
					if (minCardinality > 0) {
						optional = false;
					}
					listProperty = true;
				} else if (Objects.nonNull(minQualCardPropValue) && minQualCardPropValue.isLiteral()) {
					minCardinality = minQualCardPropValue.asLiteral().getInt();
					if (minCardinality > 0) {
						optional = false;
					}
					listProperty = true;
				}
			}
			if (Objects.isNull(typeUri)) {
				// get the type from the range of the property
				ExtendedIterator<? extends OntResource> rangeIter = property.listRange();
				while (rangeIter.hasNext()) {
					OntResource range = rangeIter.next();
					if (range.isURIResource()) {
						if (Objects.isNull(typeUri) || typeUri.equals("http://www.w3.org/2000/01/rdf-schema#Literal")) {
							typeUri = range.asResource().getURI();
						}
					}
				}
			}
			if (Objects.isNull(typeUri) && ("comment".equals(property.getLocalName()) || "seeAlso".equals(property.getLocalName()))) {
				// A bit of a hack, the schema file can not store the type of rdfs:comment, so we must override it to xsd:string
				typeUri = SpdxConstants.XML_SCHEMA_NAMESPACE + "string";
			}
		}

		private List<Restriction> getRestrictionsFromSuperclasses(OntClass ontClass, OntProperty property) {
			List<Restriction> retval = new ArrayList<>();
			ExtendedIterator<OntClass> superClasses = ontClass.listSuperClasses();
			while (superClasses.hasNext()) {
				OntClass superClass = superClasses.next();
				if (superClass.isRestriction()) {
					if (property.equals(superClass.asRestriction().getOnProperty())) {
						retval.add(superClass.asRestriction());
					}
				} else {
					retval.addAll(getRestrictionsFromSuperclasses(superClass, property));
				}
			}
			return retval;
		}

		public PropertyRestrictions(OntProperty property) {
			Objects.requireNonNull(property, "Missing required property");
			this.property = property;
			List<Restriction> propertyRestrictions = new ArrayList<>();
			ExtendedIterator<Restriction> restrictionIter = property.listReferringRestrictions();
			while (restrictionIter.hasNext()) {
				propertyRestrictions.add(restrictionIter.next());
			}
			interpretRestrictions(propertyRestrictions);
		}
		/**
		 * @return the property
		 */
		public OntProperty getProperty() {
			return property;
		}

		/**
		 * @return the isListProperty
		 */
		public boolean isListProperty() {
			return listProperty;
		}

		/**
		 * @return the typeUri
		 */
		public String getTypeUri() {
			return typeUri;
		}

		/**
		 * @return the absoluteCardinality
		 */
		public int getAbsoluteCardinality() {
			return absoluteCardinality;
		}

		/**
		 * @return the minCardinality
		 */
		public int getMinCardinality() {
			return minCardinality;
		}

		/**
		 * @return the maxCardinality
		 */
		public int getMaxCardinality() {
			return maxCardinality;
		}

		/**
		 * @return the optional
		 */
		public boolean isOptional() {
			return optional;
		}

		/**
		 * @return the enumProperty
		 */
		public boolean isEnumProperty() {
			return enumProperty;
		}

		/**
		 * @return the enumValues
		 */
		public Set<String> getEnumValues() {
			return enumValues;
		}

		/**
		 * @return the singleProperty
		 */
		public boolean isSingleProperty() {
			return singleProperty;
		}
	}

	protected OntModel model;
	Property qualCardProperty;
	Property maxQualCardProperty;
	Property maxCardProperty;
	Property cardProperty;
	Property minCardProperty;
	Property minQualCardProperty;
	Property owlClassProperty;
	Property hasValueProperty;
	Property commentProperty;
	Ontology ontology;

	public AbstractOwlRdfConverter(OntModel model) {
		Objects.requireNonNull(model, "Model must not be null");
		this.model = model;
		qualCardProperty = model.createProperty("http://www.w3.org/2002/07/owl#qualifiedCardinality");
		maxQualCardProperty = model.createProperty("http://www.w3.org/2002/07/owl#maxQualifiedCardinality");
		maxCardProperty = model.createProperty("http://www.w3.org/2002/07/owl#maxCardinality");
		cardProperty = model.createProperty("http://www.w3.org/2002/07/owl#cardinality");
		minCardProperty = model.createProperty("http://www.w3.org/2002/07/owl#minCardinality");
		minQualCardProperty = model.createProperty("http://www.w3.org/2002/07/owl#minQualifiedCardinality");
		owlClassProperty = model.createProperty("http://www.w3.org/2002/07/owl#onClass");
		hasValueProperty = model.createProperty("http://www.w3.org/2002/07/owl#hasValue");
		commentProperty = model.createProperty("http://www.w3.org/2000/01/rdf-schema#comment");
		ontology = null;
		ExtendedIterator<Ontology> ontIter = model.listOntologies();
		if (!ontIter.hasNext()) {
			throw new RuntimeException("No ontologies defined in RDF OWL");
		}
		ontology = ontIter.next();
		if (ontIter.hasNext()) {
			throw new RuntimeException("No ontologies defined in RDF OWL");
		}
	}
	
	public PropertyRestrictions getPropertyRestrictions(OntClass ontClass, OntProperty property) {
		return new PropertyRestrictions(ontClass, property);
	}

}
