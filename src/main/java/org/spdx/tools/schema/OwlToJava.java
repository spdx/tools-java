/**
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2023 Source Auditor Inc.
 */
package org.spdx.tools.schema;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.ext.com.google.common.base.Optional;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.Shape;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.IndividualUriValue;
import org.spdx.library.model.ModelObject;
import org.spdx.library.model.Relationship;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;

/**
 * Generates Java source class files for the SPDX Java Library from the RDF Owl Document generated from the spec model
 * 
 * @author Gary O'Neall
 *
 */
public class OwlToJava {
	
	static final String SPDX_URI_PREFIX = "https://spdx.org/rdf/";
	static final String INDENT = "\t";
	private static final int COMMENT_LINE_LEN = 72;
	private static final String BOOLEAN_TYPE = "http://www.w3.org/2001/XMLSchema#boolean";
	private static final String STRING_TYPE = "http://www.w3.org/2001/XMLSchema#string";
	private static final String ELEMENT_TYPE_URI = "https://spdx.org/rdf/Core/Element";
	private static final String ELEMENT_TYPE_ANY_LICENSE_INFO = "https://spdx.org/rdf/Licensing/AnyLicenseInfo";
	static final String TEMPLATE_CLASS_PATH = "resources" + "/" + "javaTemplates";
	static final String TEMPLATE_ROOT_PATH = "resources" + File.separator + "javaTemplates";
	private static final String JAVA_CLASS_TEMPLATE = "ModelObjectTemplate";
	private static final String DATE_TIME_TYPE = "https://spdx.org/rdf/Core/DateTime";
	private static final String ANY_URI_TYPE = "http://www.w3.org/2001/XMLSchema#anyURI";
	private static Set<String> INTEGER_TYPES = new HashSet<>();
	static {
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#positiveInteger");
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#decimal");
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#nonNegativeInteger");
		//TODO: Add other types - needs research
	}
	
	OntModel model = null;
	Shapes shapes = null;
	Map<Node, Shape> shapeMap = null;
	
	Set<String> enumClassUris = new HashSet<>(); // Set of enum URI's
	
	/**
	 * Property maps mapping a property URI to a map of metadata about the property suitable for passing into Mustache templates
	 */
	Map<String, Map<String, Object>> elementProperties = new HashMap<>();
	Map<String, Map<String, Object>> anyLicenseInfoProperties = new HashMap<>();
	Map<String, Map<String, Object>> objectProperties = new HashMap<>();
	Map<String, Map<String, Object>> enumerationProperties = new HashMap<>();
	Map<String, Map<String, Object>> booleanProperties = new HashMap<>();
	Map<String, Map<String, Object>> integerProperties = new HashMap<>();
	Map<String, Map<String, Object>> stringProperties = new HashMap<>();
	Map<String, Map<String, Object>> objectPropertyValueCollection = new HashMap<>();
	Map<String, Map<String, Object>> stringCollection = new HashMap<>();
	Map<String, Map<String, Object>> objectPropertyValueSet = new HashMap<>();

	/**
	 * @param model model to use to generate the java files
	 */
	public OwlToJava(OntModel model) {
		this.model = model;
		shapes = Shapes.parse(model);
		shapeMap = shapes.getShapeMap();
	}
	
	public void generateJavaSource(File dir) throws IOException {
		List<ObjectProperty> allObjectProperties = model.listObjectProperties().toList();
		List<DatatypeProperty> allDataProperties = model.listDatatypeProperties().toList();
		List<Individual> allIndividuals = model.listIndividuals().toList();
		List<OntClass> allClasses = model.listClasses().toList();
		collectPropertyInformation(allObjectProperties, allDataProperties, allClasses, allIndividuals);
		allClasses.forEach(ontClass -> {
			String comment = ontClass.getComment(null);
			String classUri = ontClass.getURI();
			String name = ontClass.getLocalName();
			Shape classShape = shapeMap.get(ontClass.asNode());
			List<Statement> props = new ArrayList<>();
			boolean enumeration = ontClass.isEnumeratedClass();
			ontClass.listProperties().forEach(stmt -> {
				props.add(stmt);
			});
			List<OntClass> subClasses = new ArrayList<>();
			ontClass.listSubClasses().forEach(oc -> {
				subClasses.add(oc);
			});
			List<OntClass> superClasses = new ArrayList<>();
			addAllSuperClasses(ontClass, superClasses);
			List<OntProperty> properties = ontClass.listDeclaredProperties(true).toList();

			try {
				//TODO: Handle individual classes
				if (isEnumClass(ontClass)) {
					generateJavaEnum(dir, classUri, name, allIndividuals, comment);
				} else {
					generateJavaClass(dir, classUri, name, properties, comment);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * Collect property information into the field maps for properties and initialize the enum classes field
	 * @param allObjectProperties object properties in the schema
	 * @param allDataProperties data properties in the schema
	 * @param allClasses classes in the schema
	 * @param allIndividuals Individuals to determine enum class types
	 */
	private void collectPropertyInformation(
			List<ObjectProperty> allObjectProperties,
			List<DatatypeProperty> allDataProperties, List<OntClass> allClasses, List<Individual> allIndividuals) {
		
		Set<String> enumerationTypes = new HashSet<>();
		Set<String> anyLicenseInfoTypes = new HashSet<>();
		Set<String> elementTypes = new HashSet<>();
		
		for (Individual individual:allIndividuals) {
			this.enumClassUris.add(individual.getOntClass(true).getURI());
		}
		allClasses.forEach(ontClass -> {
			List<OntClass> superClasses = new ArrayList<>();
			addAllSuperClasses(ontClass, superClasses);
			if (isEnumClass(ontClass)) {
				enumerationTypes.add(ontClass.getURI());
			} else if (isAnyLicenseInfoClass(ontClass, superClasses)) {
				anyLicenseInfoTypes.add(ontClass.getURI());
			} else if (isElementClass(ontClass, superClasses)) {
				elementTypes.add(ontClass.getURI());
			}
		});
		
		for (ObjectProperty prop:allObjectProperties) {
			OntResource rangeResource = prop.getRange();
			String rangeUri = rangeResource == null ? "" : rangeResource.getURI() == null ? "" : rangeResource.getURI();
			if (enumerationTypes.contains(rangeUri)) {
				enumerationProperties.put(prop.getURI(), propertyToMustachMap(prop));
			} else if (isSet(prop)) {
				objectPropertyValueSet.put(prop.getURI(), propertyToMustachMap(prop));
			} else if (isCollection(prop)) {
				objectPropertyValueCollection.put(prop.getURI(), propertyToMustachMap(prop));
			} else if (anyLicenseInfoTypes.contains(rangeUri)) {
				anyLicenseInfoProperties.put(prop.getURI(), propertyToMustachMap(prop));
			} else if (elementTypes.contains(rangeUri)) {
				elementProperties.put(prop.getURI(), propertyToMustachMap(prop));
			} else {
				objectProperties.put(prop.getURI(), propertyToMustachMap(prop));
			}
		}
		
		for (DatatypeProperty prop:allDataProperties) {
			OntResource rangeResource = prop.getRange();
			String rangeUri = rangeResource == null ? "" : rangeResource.getURI() == null ? "" : rangeResource.getURI();
			if (enumerationTypes.contains(rangeUri)) {
				enumerationProperties.put(prop.getURI(), propertyToMustachMap(prop));
			} else if (BOOLEAN_TYPE.equals(rangeUri)) {
				booleanProperties.put(prop.getURI(), propertyToMustachMap(prop));
			} else if  (INTEGER_TYPES.contains(rangeUri)) {
				integerProperties.put(prop.getURI(), propertyToMustachMap(prop));
				//TODO: Add in specific types and type checking for DATE_TIME_TYPE and ANY_URI_TYPE
			} else if  (STRING_TYPE.equals(rangeUri) || DATE_TIME_TYPE.equals(rangeUri) || ANY_URI_TYPE.equals(rangeUri)) {
				if (isCollection(prop)) {
					stringCollection.put(prop.getURI(), propertyToMustachMap(prop));
				} else {
					stringProperties.put(prop.getURI(), propertyToMustachMap(prop));
				}
			} else {
//				throw new OwlToJavaException("Unknown data type URI "+rangeUri+" for property URI "+prop.getURI());
				int i = 0;
				i++;
			}
		}
	}

	/**
	 * Add super classes including transitive superclasses to the superClasses list
	 * @param ontClass class to add superClasses for
	 * @param superClasses
	 */
	private void addAllSuperClasses(OntClass ontClass,
			List<OntClass> superClasses) {
		ontClass.listSuperClasses().forEach(superClass -> {
			superClasses.add(superClass);
			addAllSuperClasses(superClass, superClasses);
		});
	}

	/**
	 * @param prop prop to test
	 * @return true if the property is a collection
	 */
	private boolean isCollection(OntProperty prop) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @param prop prop to test
	 * @return true if the property is a set
	 */
	private boolean isSet(OntProperty prop) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @param ontClass class
	 * @param superClasses list of all superclasses for the class
	 * @return true if the class is an Element or a subclass of Element
	 */
	private boolean isElementClass(OntClass ontClass,
			List<OntClass> superClasses) {
		if (ELEMENT_TYPE_URI.equals(ontClass.getURI())) {
			return true;
		}
		for (OntClass superClass:superClasses) {
			if (ELEMENT_TYPE_URI.equals(superClass.getURI())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param ontClass class
	 * @param superClasses list of all superclasses for the class
	 * @return true if the class is an AnyLicenseInfo or a subclass of AnyLicenseInfo
	 */
	private boolean isAnyLicenseInfoClass(OntClass ontClass,
			List<OntClass> superClasses) {
		if (ELEMENT_TYPE_ANY_LICENSE_INFO.equals(ontClass.getURI())) {
			return true;
		}
		for (OntClass superClass:superClasses) {
			if (ELEMENT_TYPE_ANY_LICENSE_INFO.equals(superClass.getURI())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param ontClass class
	 * @return true if the class is an enumeration
	 */
	private boolean isEnumClass(OntClass ontClass) {
		if (ontClass.isEnumeratedClass()) {
			return true;
		}
		//TODO: Switch to enums to remove this hack
		List<OntProperty> properties = ontClass.listDeclaredProperties().toList();
		for (OntProperty prop:properties) {
			OntResource res = prop.getRange();
			String uri = res.getURI();
		}
		return this.enumClassUris.contains(ontClass.getURI());
	}
	
	/**
	 * @param dir Directory to store the source files in
	 * @param classUri URI for the class
	 * @param name local name for the class
	 * @param properties properties for the class
	 * @param comment Description of the class
	 * @throws IOException 
	 */
	private void generateJavaClass(File dir, String classUri, String name,
			List<OntProperty> properties, String comment) throws IOException {
		String pkgName = uriToPkg(classUri);
		File sourceFile = createJavaSourceFile(classUri, dir);
		Map<String, Object> mustacheMap = new HashMap<>();
		mustacheMap.put("elementProperties", findProperties(properties, elementProperties));
		mustacheMap.put("objectProperties", findProperties(properties, objectProperties));
		mustacheMap.put("anyLicenseInfoProperties", findProperties(properties, anyLicenseInfoProperties));
		mustacheMap.put("enumerationProperties", findProperties(properties, enumerationProperties));
		mustacheMap.put("booleanProperties", findProperties(properties, booleanProperties));
		mustacheMap.put("integerProperties", findProperties(properties, integerProperties));
		mustacheMap.put("stringProperties", findProperties(properties, stringProperties));
		mustacheMap.put("objectPropertyValueCollection", findProperties(properties, objectPropertyValueCollection));
		mustacheMap.put("stringCollection", findProperties(properties, stringCollection));
		mustacheMap.put("objectPropertyValueSet", findProperties(properties, objectPropertyValueSet));
		mustacheMap.put("year", "2023"); // TODO - use actual year
		mustacheMap.put("pkgName", pkgName);
		List<String> imports = buildImports();
		mustacheMap.put("imports", imports);
		mustacheMap.put("classComments", toClassComment(comment));
		String superClass = getSuperClass();
		mustacheMap.put("superClass", superClass);
		mustacheMap.put("verifySuperclass", superClass != "ModelObject");
		//TODO: Implement
		mustacheMap.put("compareUsingProperties", false); // use properties to implement compareTo
		mustacheMap.put("compareProperties", new ArrayList<Map<String, Object>>()); // List of property mustache maps to use in compare
		//TODO: Implement
		mustacheMap.put("usePropertiesForToString", false); // use properties to implement toString
		mustacheMap.put("toStringProperties", new ArrayList<Map<String, Object>>()); // List of property mustache maps to use in compare
		//TODO: Figure out how to handle version specific verify
		
		String templateDirName = TEMPLATE_ROOT_PATH;
		File templateDirectoryRoot = new File(templateDirName);
		if (!(templateDirectoryRoot.exists() && templateDirectoryRoot.isDirectory())) {
			templateDirName = TEMPLATE_CLASS_PATH;
		}
		DefaultMustacheFactory builder = new DefaultMustacheFactory(templateDirName);
		Mustache mustache = builder.compile(JAVA_CLASS_TEMPLATE);
		FileOutputStream stream = null;
		OutputStreamWriter writer = null;
		try {
			stream = new FileOutputStream(sourceFile);
			writer = new OutputStreamWriter(stream, "UTF-8");
	        mustache.execute(writer, mustacheMap);
		} finally {
			if (writer != null) {
				writer.close();
			}
			if (stream != null) {
				stream.close();
			}
		}
	}
	

	/**
	 * @param properties direct ontology properties
	 * @param propertyMap map of property URI's to map of mustache strings to properties for any properties returning a type of Element
	 * @return map of mustache strings to properties for any properties returning a type of Element
	 */
	private List<Map<String, Object>> findProperties(
			List<OntProperty> properties,
			Map<String, Map<String, Object>> propertyMap) {
		List<Map<String, Object>> retval = new ArrayList<>();
		for (OntProperty property:properties) {
			if (propertyMap.containsKey(property.getURI())) {
				retval.add(propertyMap.get(property.getURI()));
			}
		}
		return retval;
	}

	/**
	 * @param property
	 * @return map of Mustache strings to values for a give ontology property
	 */
	private Map<String, Object> propertyToMustachMap(OntProperty property) {
		//TODO: Implement
		Map<String, Object> retval = new HashMap<>();
		retval.put("type", "NOT IMPLEMENTED");
		retval.put("name", "NOT IMPLEMENTED");
		retval.put("getter", "NOT IMPLEMENTED");
		retval.put("setter", "NOT IMPLEMENTED");
		retval.put("required", true);
		retval.put("requiredProfiles", "NOT IMPLEMENTED");
		retval.put("pattern", "NOT IMPLEMENTED");
		retval.put("min", "NOT IMPLEMENTED");
		retval.put("max", "NOT IMPLEMENTED");
		retval.put("uri", property.getURI());
		retval.put("propertyConstant", "NOT IMPLEMENTED");
		return retval;
	}

	/**
	 * @param property
	 * @return
	 */
	private boolean isElementProperty(OntProperty property) {
		// TODO Implement
		return true;
	}

	/**
	 * @return superClass for the class
	 */
	private String getSuperClass() {
		// TODO Implement
		return "SUPERCLASS NOT IMPLEMENTED";
	}

	/**
	 * @return a list of import statements appropriate for the class
	 */
	private List<String> buildImports() {
		List<String> retval = new ArrayList<>();
		retval.add("import java.util.ArrayList;");
		retval.add("import java.util.List;");
		retval.add("import java.util.Set;");
		retval.add("");
		retval.add("import org.spdx.library.InvalidSPDXAnalysisException;");
		retval.add("import org.spdx.library.ModelCopyManager;");
		retval.add("import org.spdx.storage.IModelStore;");
		retval.add("IMPORTS NOT COMPLETELY IMPLEMENTED");
		//TODO: Implement
		
		
		return retval;
	}

	/**
	 * @param dir Directory to store the source files in
	 * @param classUri URI for the class
	 * @param name local name for the class
	 * @param properties properties for the class
	 * @param comment Description of the class
	 * @throws IOException 
	 */
	private void generateUnitTest(File dir, String classUri, String name,
			List<OntProperty> properties, String comment) throws IOException {
		//TODO: Implement
	}
	

	/**
	 * @param dir Directory to store the source files in
	 * @param classUri URI for the enum
	 * @param name local name for the enum
	 * @param allIndividuals individual values from the model
	 * @param comment Description of the enum
	 * @throws IOException 
	 */
	private void generateJavaEnum(File dir, String classUri, String name,
			List<Individual> allIndividuals, String comment) throws IOException {
		Map<String, String> enumToNameMap = new HashMap<>();
		for (Individual individual:allIndividuals) {
			if (individual.hasRDFType(classUri)) {
				StringBuilder enumName = new StringBuilder();
				String localName = individual.getLocalName();
				for (int i = 0; i < localName.length(); i++) {
					char ch = localName.charAt(i);
					if (!Character.isLowerCase(ch)) {
						enumName.append('_');
					}
					if (ch == '-') {
						enumName.append('_');
					} else {
						enumName.append(Character.toUpperCase(ch));
					}
				}
				enumToNameMap.put(enumName.toString(), individual.getLocalName());
			}
		}
		String pkgName = uriToPkg(classUri);
		File sourceFile = createJavaSourceFile(classUri, dir);
		try (PrintWriter writer = new PrintWriter(new FileOutputStream(sourceFile))) {
			writeFileHeader(writer);
			writer.print("package ");
			writer.print(pkgName);
			writer.println(";");
			writer.println();
//			printClassComment(comment, writer);
			writer.print("public enum ");
			writer.print(name);
			writer.println(" implements IndividualUriValue {");
			Iterator<Entry<String, String>> iter = enumToNameMap.entrySet().iterator();
			if (iter.hasNext()) {
				writer.print(INDENT);
				writeEnumEntry(iter.next(), writer);
			}
			while (iter.hasNext()) {
				writer.println(",");
				writer.print(INDENT);
				writeEnumEntry(iter.next(), writer);
			}
			writer.println(";");
			writer.println(INDENT);
			writer.print(INDENT);
			writer.println("private String longName;");
			writer.println(INDENT);
			writer.print(INDENT);
			writer.print("private ");
			writer.print(name);
			writer.println("(String longName) {");
			writer.println(INDENT + INDENT + "this.longName = longName;");
			writer.println(INDENT + "}");
			writer.println(INDENT);
			writer.println(INDENT + "@Override");
			writer.println(INDENT + "public String getIndividualURI() {");
			writer.println(INDENT + INDENT + "return getNameSpace() + getLongName();");
			writer.println(INDENT + "}");
			writer.println(INDENT);
			writer.println(INDENT + "public String getLongName() {");
			writer.println(INDENT + INDENT + "return longName;");
			writer.println(INDENT + "}");
			writer.println(INDENT);
			writer.println(INDENT + "public String getNameSpace() {");
			writer.print(INDENT + INDENT + "return \"");
			writer.print(classUri);
			writer.println("\";");
			writer.println(INDENT + "}");
		}
	}

	/**
	 * @param comment from model documentation
	 * @return text formatted for a class comment
	 */
	private String toClassComment(String comment) {
		StringBuilder sb = new StringBuilder("/**\n");
		sb.append(" * DO NOT EDIT - this file is generated by the Owl to Java Utility \n");
		sb.append(" * See: https://github.com/spdx/tools-java \n");
		sb.append(" * \n");
		String[] tokens = comment.split("\\s+");
		int i = 0;
		while (i < tokens.length) {
			int len = 4;
			sb.append(" * ");
			while (len < COMMENT_LINE_LEN && i < tokens.length) {
				len += tokens[i].length();
				sb.append(tokens[i++].trim());
				sb.append(' ');
			}
			sb.append("\n");
		}
		sb.append(" **/\n");
		return sb.toString();
	}

	/**
	 * @param enumToModelName entry mapping an enum value to the model name
	 * @param writer 
	 */
	private void writeEnumEntry(Entry<String, String> enumToModelName, PrintWriter writer) {
		writer.write(enumToModelName.getKey());
		writer.write("(\"");
		writer.write(enumToModelName.getValue());
		writer.write("\")");
	}

	/**
	 * @param writer
	 */
	private void writeFileHeader(PrintWriter writer) {
		writer.println("/**");
		writer.println(" * Copyright (c) 2019 Source Auditor Inc.");
		writer.println("");
		writer.println(" * SPDX-License-Identifier: Apache-2.0");
		writer.println("");
		writer.println(" */");
	}

	/**
	 * @param classUri URI for the class
	 * @param dir directory to hold the file
	 * @return the created file
	 * @throws IOException 
	 */
	private File createJavaSourceFile(String classUri, File dir) throws IOException {		
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model");
		String[] parts = classUri.substring(SPDX_URI_PREFIX.length()).split("/");
		for (int i = 0; i < parts.length-1; i++) {
			path = path.resolve(parts[i]);
		}
		Files.createDirectories(path);
		File retval = path.resolve(parts[parts.length-1] + ".java").toFile();
		retval.createNewFile();
		return retval;
	}

	/**
	 * @param classUri
	 * @return
	 */
	private String uriToPkg(String classUri) {
		String[] parts = classUri.substring(SPDX_URI_PREFIX.length()).split("/");
		StringBuilder sb = new StringBuilder("org.spdx.library");
		for (String part:parts) {
			sb.append(".");
			sb.append(part.toLowerCase());
		}
		return sb.toString();
	}

}
