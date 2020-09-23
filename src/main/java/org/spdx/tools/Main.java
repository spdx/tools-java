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
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import org.apache.commons.lang3.ArrayUtils;
import org.spdx.library.Version;
import org.spdx.library.model.license.ListedLicenses;

/**
 * Dispatch individual tools
 * 
 * @author Gary O'Neall
 *
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
			System.out.println("SPDX Tool Version: "+Version.CURRENT_IMPLEMENTATION_VERSION +
					"; Specification Version: "+Version.CURRENT_SPDX_VERSION +
					"; License List Version: "+ListedLicenses.getListedLicenses().getLicenseListVersion());
		} else if ("MatchingStandardLicenses".equals(spdxTool)) {
			MatchingStandardLicenses.main(args);
		} else {
			usage();
		}
	}
	
	private static void usage() {
		System.out
				.println(""
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
