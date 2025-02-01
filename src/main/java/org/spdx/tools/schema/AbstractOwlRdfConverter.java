/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.ontology.UnionClass;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.library.model.v2.SpdxConstantsCompatV2;
import org.spdx.library.model.v2.enumerations.SpdxEnumFactoryCompatV2;

/**
 * Abstract class for implementing classes which convert from RDF/XML OWL format to some other format
 * 
 * @author Gary O'Neall
 *
 */
public class AbstractOwlRdfConverter {
	
	static final Logger logger = LoggerFactory.getLogger(AbstractOwlRdfConverter.class);
	static final Set<String> SKIPPED_PROPERTIES;
	static {
		Set<String> skipped = new HashSet<>();
		skipped.add("http://www.w3.org/2003/06/sw-vocab-status/ns#term_status");
		skipped.add("http://www.w3.org/2002/07/owl#qualifiedCardinality");
		skipped.add("http://www.w3.org/2002/07/owl#deprecatedProperty");
		skipped.add("http://www.w3.org/2002/07/owl#deprecatedClass");
		skipped.add(SpdxConstantsCompatV2.SPDX_NAMESPACE + "describesPackage");   // This is an old deprecated field from 1.0 which should be ignored - it was only used in RDF format
		SKIPPED_PROPERTIES = Collections.unmodifiableSet(skipped);
	}
	
	/**
	 * Map of the properties renamed due to spec inconsistencies between the RDF format and other formats
	 */
	public static final Map<String, String> RENAMED_PROPERTY_TO_OWL_PROPERTY;
	public static final Map<String, String> OWL_PROPERTY_TO_RENAMED_PROPERTY;
	static {
		Map<String, String> renamedToOwl = new HashMap<>();
		Map<String, String> owlToRenamed = new HashMap<>();
		renamedToOwl.put(SpdxConstantsCompatV2.PROP_SPDX_SPEC_VERSION.getName(), SpdxConstantsCompatV2.PROP_SPDX_VERSION.getName());
		owlToRenamed.put(SpdxConstantsCompatV2.PROP_SPDX_VERSION.getName(), SpdxConstantsCompatV2.PROP_SPDX_SPEC_VERSION.getName());
		RENAMED_PROPERTY_TO_OWL_PROPERTY = Collections.unmodifiableMap(renamedToOwl);
		OWL_PROPERTY_TO_RENAMED_PROPERTY = Collections.unmodifiableMap(owlToRenamed);
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
								Enum<?> e = SpdxEnumFactoryCompatV2.uriToEnum.get(individual.getURI());
								if (Objects.nonNull(e)) {
									this.enumValues.add(e.toString());
									this.enumProperty = true;
								} else {
									logger.warn("Missing enum value for " + individual.getLocalName());
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
							Enum<?> e = SpdxEnumFactoryCompatV2.uriToEnum.get(hasValue.asResource().getURI());
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
				typeUri = SpdxConstantsCompatV2.XML_SCHEMA_NAMESPACE + "string";
			}
		}

		private List<Restriction> getRestrictionsFromSuperclasses(OntClass ontClass, OntProperty property) {
			List<Restriction> retval = new ArrayList<>();
			ExtendedIterator<OntClass> superClasses = ontClass.listSuperClasses();
			while (superClasses.hasNext()) {
				OntClass superClass = superClasses.next();
				if (superClass.isUnionClass()) {
		            UnionClass uClass = superClass.asUnionClass();
		            ExtendedIterator<? extends OntClass> unionClassiter = uClass.listOperands();
		            while (unionClassiter.hasNext()) {
		                OntClass operand = unionClassiter.next();
		                if (operand.isRestriction() && property.equals(operand.asRestriction().getOnProperty())) {
		                    retval.add(operand.asRestriction());
		                }
		            }
		        } else if (superClass.isRestriction()) {
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
	Property unionOfProperty;
	Property firstResource;
	Property restResource;
	Property intersectionOfProperty;
	
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
		unionOfProperty = model.createProperty("http://www.w3.org/2002/07/owl#unionOf");
		intersectionOfProperty = model.createProperty("http://www.w3.org/2002/07/owl#intersectionOf");
		firstResource = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#first");
		restResource = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest");
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

	/**
	 * Checks to see if an OWL property name has been renamed for the JSON/YAML formats
	 * @param owlPropertyName
	 * @return renamed property if it was renamed, owlPropertyName if not renamed
	 */
	public static String checkConvertRenamedPropertyName(String owlPropertyName) {
		if (OWL_PROPERTY_TO_RENAMED_PROPERTY.containsKey(owlPropertyName)) {
			return OWL_PROPERTY_TO_RENAMED_PROPERTY.get(owlPropertyName);
		} else {
			return owlPropertyName;
		}
	}
	
	/**
	 * @param oClass
	 * @param excludeSuperClassProperties if true, only return properties for the class but not any superclasses
	 * @return collection of all properties which have a restriction on the class or superclasses if not direct
	 */
	protected Collection<OntProperty> propertiesFromClassRestrictions(OntClass oClass, boolean excludeSuperClassProperties) {
		Collection<OntProperty> properties = new HashSet<>();
		Collection<OntClass> reviewedClasses = new HashSet<>();
		collectPropertiesFromRestrictions(oClass, properties, reviewedClasses, excludeSuperClassProperties);
		removeSuperProperties(properties);
		ArrayList<OntProperty> sorted = new ArrayList<>(properties);
		Collections.sort(sorted, new Comparator<OntProperty>() {

			@Override
			public int compare(OntProperty arg0, OntProperty arg1) {
				return arg0.getLocalName().compareToIgnoreCase(arg1.getLocalName());
			}
		    
		});
		return sorted;
	}
	
	/**
	 * Removes and properties which have a sub-property present in the properties list
     * @param properties
     */
    private void removeSuperProperties(Collection<OntProperty> properties) {
        List<OntProperty> superProperties = new ArrayList<>();
        for (OntProperty property:properties) {
            if (property.isProperty()) {
                OntProperty op = property.asProperty();
                ExtendedIterator<? extends OntProperty> superIter = op.listSuperProperties();
                while (superIter.hasNext()) {
                    superProperties.add(superIter.next());
                }
            }
        }
        for (OntProperty superProp:superProperties) {
            if (properties.contains(superProp)) {
                properties.remove(superProp);
            }
        }
    }

    /**
	 * @param oClass
	 * @return collection of all properties which have a restriction on the class or superclasses
	 */
	protected Collection<OntProperty> propertiesFromClassRestrictions(OntClass oClass) {
		return propertiesFromClassRestrictions(oClass, false);
	}
	
	/**
	 * @param property
	 * @return The class or type for the property
	 * @throws SchemaException
	 */
	protected Optional<Resource> getPropertyType(OntProperty property) throws SchemaException {
		ExtendedIterator<? extends OntResource> rangeIter = property.listRange();
		while (rangeIter.hasNext()) {
			OntResource range = rangeIter.next();
			if (range.isURIResource()) {
				return Optional.of(range);
			} else if (range.hasProperty(unionOfProperty)) {
				// Search for a type
				Resource unionOf = range.getPropertyResourceValue(unionOfProperty);
				Optional<Resource> retval = findTypeInCollection(unionOf.getPropertyResourceValue(firstResource),
						unionOf.getPropertyResourceValue(restResource));
				if (retval.isPresent()) {
					return retval;
				}
			} else if (range.hasProperty(intersectionOfProperty)) {
				// Search for a type
				Resource intersectionOf = range.getPropertyResourceValue(intersectionOfProperty);
				Optional<Resource> retval = findTypeInCollection(intersectionOf.getPropertyResourceValue(firstResource),
						intersectionOf.getPropertyResourceValue(restResource));
				if (retval.isPresent()) {
					return retval;
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * Search a collection for a URI value assumed to be the type
	 * @param first first element in the collection
	 * @param rest rest of the collection
	 * @return
	 */
	private Optional<Resource> findTypeInCollection(Resource first, Resource rest) {
		if (Objects.nonNull(first) && first.isURIResource()) {
			return Optional.of(first);
		} else if (Objects.isNull(rest) || "http://www.w3.org/1999/02/22-rdf-syntax-ns#nil".equals(rest.getURI())) {
			return Optional.empty();
		} else {
			return findTypeInCollection(rest.getPropertyResourceValue(firstResource),
					rest.getPropertyResourceValue(restResource));
		}
	}

	/**
	 * Collects any properties used in restrictions and adds them to the properties collection - includes all subclasses
	 * @param oClass Class to collect properties from
	 * @param properties collection of any properties found
	 * @param reviewedClasses collection of classes already reviewed - used to prevent infinite recursion
	 * @param excludeSuperClassProperties if true, only collect properties for the class and not the superclasses
	 */
	private void collectPropertiesFromRestrictions(OntClass oClass, 
			Collection<OntProperty> properties, Collection<OntClass> reviewedClasses, boolean excludeSuperClassProperties) {
		if (reviewedClasses.contains(oClass)) {
			return;
		}
		reviewedClasses.add(oClass);
		if (oClass.isUnionClass()) {
		    UnionClass uClass = oClass.asUnionClass();
		    ExtendedIterator<? extends OntClass> unionClassiter = uClass.listOperands();
		    while (unionClassiter.hasNext()) {
		        collectPropertiesFromRestrictions(unionClassiter.next(), properties, 
		                reviewedClasses, excludeSuperClassProperties);
		    }
		} else if (oClass.isRestriction()) {
			Restriction r = oClass.asRestriction();
			OntProperty property = r.getOnProperty();
			if (Objects.nonNull(property)) {
				properties.add(property);
			}
		} else {
			ExtendedIterator<OntClass> subClassIter = oClass.listSuperClasses(excludeSuperClassProperties);
			if (excludeSuperClassProperties) {
				subClassIter = subClassIter.filterDrop(sc -> {
					return sc.isURIResource() && 
							!"http://www.w3.org/2000/01/rdf-schema#Container".equals(sc.getURI());
				});
			}			
			while (subClassIter.hasNext()) {
				collectPropertiesFromRestrictions(subClassIter.next(), properties, reviewedClasses, excludeSuperClassProperties);
			}
		}
	}
}
