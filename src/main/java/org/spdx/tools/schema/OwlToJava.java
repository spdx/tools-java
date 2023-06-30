/**
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2023 Source Auditor Inc.
 */
package org.spdx.tools.schema;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.Shape;
import org.spdx.library.model.IndividualUriValue;

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
	
	OntModel model = null;
	Shapes shapes = null;
	Map<Node, Shape> shapeMap = null;


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
		model.listClasses().forEach(ontClass -> {
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
			ontClass.listSuperClasses().forEach(oc -> {
				superClasses.add(oc);
			});
			List<OntProperty> properties = ontClass.listDeclaredProperties(true).toList();

			if (isEnumClass(ontClass, superClasses)) {
				try {
					generateJavaEnum(dir, classUri, name, allIndividuals, comment);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} else {
				generateJavaClass(dir, classUri, name, properties, comment);
			}
		});
	}

	/**
	 * @param ontClass
	 * @param superClasses
	 * @return true if the class is an enumeration
	 */
	private boolean isEnumClass(OntClass ontClass,
			List<OntClass> superClasses) {
		if (ontClass.isEnumeratedClass()) {
			return true;
		}
		//TODO: Switch to enums to remove this hack
		return superClasses.isEmpty();
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
			printClassComment(comment, writer);
			writer.println();
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
	 * @param comment
	 */
	private void printClassComment(String comment, PrintWriter writer) {
		writer.println("/**");
		writer.println(" * DO NOT EDIT - this file is generated by the Owl to Java Utility ");
		writer.println(" * See: https://github.com/spdx/tools-java ");
		writer.println(" * ");
		String[] tokens = comment.split("\\s+");
		int i = 0;
		while (i < tokens.length) {
			int len = 4;
			writer.print(" * ");
			while (len < COMMENT_LINE_LEN && i < tokens.length) {
				len += tokens[i].length();
				writer.print(tokens[i++].trim());
				writer.print(" ");
			}
			writer.println();
		}
		writer.println(" **/");
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
	 * @param dir Directory to store the source files in
	 * @param classUri URI for the class
	 * @param name local name for the class
	 * @param properties properties for the class
	 * @param comment Description of the class
	 */
	private void generateJavaClass(File dir, String classUri, String name,
			List<OntProperty> properties, String comment) {
		String pkg = uriToPkg(classUri);
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
