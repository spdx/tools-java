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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;
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
		private String typeUri = null;
		private int absoluteCardinality = -1;
		private int minCardinality = -1;
		private int maxCardinality = -1;
		private boolean optional = true;
		private boolean enumProperty = false;
		Set<String> enumValues = new HashSet<>();

		public PropertyRestrictions(OntProperty property) {
			this.property = property;
			ExtendedIterator<Restriction> restrictionIter = property.listReferringRestrictions();
			while (restrictionIter.hasNext()) {
				Restriction r = restrictionIter.next();
				RDFNode typePropertyValue = r.getPropertyValue(owlClassProperty);
				if (Objects.nonNull(typePropertyValue) && typePropertyValue.isURIResource()) {
					typeUri = typePropertyValue.asResource().getURI();
				}
				// Check for enumeration types
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
					}
				} else if (Objects.nonNull(cardPropValue) && cardPropValue.isLiteral()) {
					absoluteCardinality = cardPropValue.asLiteral().getInt();
					if (absoluteCardinality > 0) {
						optional = false;
					}
					if (absoluteCardinality > 1) {
						listProperty = true;
					}
				} else if (Objects.nonNull(maxQualCardPropValue) && maxQualCardPropValue.isLiteral()) {
					maxCardinality = maxQualCardPropValue.asLiteral().getInt();
					if (maxCardinality > 1) {
						listProperty = true;
					}
				} else if (Objects.nonNull(maxCardPropValue) && maxCardPropValue.isLiteral()) {
					maxCardinality = maxCardPropValue.asLiteral().getInt();
					if (maxCardinality > 1) {
						listProperty = true;
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
	}
	
	public PropertyRestrictions getPropertyRestrictions(OntProperty property) {
		return new PropertyRestrictions(property);
	}

}
