/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import org.apache.commons.lang3.ArrayUtils;
import org.spdx.library.SpdxModelFactory;

/**
 * Dispatch individual tools
 * 
 * @author Gary O'Neall
 */
public class Main {

    /**
	 * @param args args[0] is the name of the tools with the remaining args being the tool parameters
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			usage();
			return;
		}
		SpdxModelFactory.init();
		String spdxTool = args[0];
		args = ArrayUtils.removeElement(args, args[0]);
		if ("Convert".equals(spdxTool)) {
			SpdxConverter.main(args);
		} else if ("SPDXViewer".equals(spdxTool)) {
			SpdxViewer.main(args);
		} else if ("Verify".equals(spdxTool)) {
			Verify.main(args);
		} else if ("CompareDocs".equals(spdxTool)) {
			CompareSpdxDocs.main(args);
		} else if ("GenerateVerificationCode".equals(spdxTool)) {
			GenerateVerificationCode.main(args);
		} else if ("Version".equals(spdxTool)) {
			System.out.println("SPDX Tool Version: " + SpdxVersion.getCurrentToolVersion() +
					"; Specification Version: " + SpdxVersion.getLibraryVersion() +
					"; License List Version: " + SpdxVersion.getLicenseListVersion());
		} else if ("MatchingStandardLicenses".equals(spdxTool)) {
			MatchingStandardLicenses.main(args);
		} else {
			usage();
		}
	}
	
	private static void usage() {
		System.out.println(""
						+ "Usage: java -jar spdx-tools-jar-with-dependencies.jar <function> <parameters> \n"
						+ "function                 parameter                         example \n"
						+ "------------------------------------------------------------------------------------------------------------------- \n"
						+ "Convert         inputFile outputFile [fromType] [toType]   Examples/SPDXTagExample.tag TagToSpreadsheet.xls \n"
						+ "SPDXViewer      inputFile                                  TestFiles/SPDXRdfExample.rdf \n"
						+ "Verify          inputFile [type]                           TestFiles/SPDXRdfExample.rdf \n"
						+ "CompareDocs     output.xlsx doc1 doc2 ... docN \n"
						+ "GenerateVerificationCode sourceDirectory\n"
						+ "Version\n"
						+ "MatchingStandardLicenses licenseTextFile");
	}

}
