/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 * <p>
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * <p>
 *       https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.spdx.tools;

import org.apache.commons.lang3.ArrayUtils;
import org.spdx.library.SpdxModelFactory;

/**
 * Dispatch individual tools
 * <br/>
 * Each dispatched tool's main() calls System.exit() itself, which
 * terminates the process immediately with that tool's own exit status -
 * so the exit codes below only apply when no tool is actually dispatched:
 * <ul>
 *   <li>0 - success (e.g. the "Version" command, which is handled here directly instead of being dispatched)</li>
 *   <li>2 - the command was invoked incorrectly (missing/unrecognized tool name)</li>
 * </ul>
 *
 * @author Gary O'Neall
 */
public class Main {

    /**
	 * @param args args[0] is the name of the tools with the remaining args being the tool parameters
	 */
	public static void main(String[] args) {
		System.exit(run(args));
	}

	/**
	 * Runs the Main dispatch logic and reports results to standard out.
	 * @param args args[0] is the name of the tools with the remaining args being the tool parameters
	 * @return process exit status, see {@link ExitCode}
	 */
	static int run(String[] args) {
		if (args.length < 1) {
			usage();
			return ExitCode.USAGE_ERROR;
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
					"; Specification Version: " + SpdxVersion.getLatestSpecVersion() +
					"; License List Version: " + SpdxVersion.getLicenseListVersion());
		} else if ("MatchingStandardLicenses".equals(spdxTool)) {
			MatchingStandardLicenses.main(args);
		} else {
			usage();
			return ExitCode.USAGE_ERROR;
		}
		return ExitCode.SUCCESS;
	}

	private static void usage() {
		System.out.println("Usage: java -jar spdx-tools-jar-with-dependencies.jar <function> <parameters> \n"
						+ "function                 parameters\n"
						+ "------------------------------------------------------------------------------- \n"
						+ "SPDXViewer      inputFile\n"
						+ "Verify          inputFile [type]\n"
						+ "Convert         inputFile outputFile [fromType] [toType] [--toVersion version]\n"
						+ "CompareDocs     output.xlsx input1 input2 [input3 ... inputN]\n"
						+ "GenerateVerificationCode sourceDirectory\n"
						+ "MatchingStandardLicenses licenseTextFile\n"
						+ "Version\n"
						+ "------------------------------------------------------------------------------- \n"
						+ "Example:\n"
						+ "java -jar spdx-tools...jar Convert example.spdx.json example.spdx\n"
						+ "java -jar spdx-tools...jar Convert ex-2.2.spdx.json ex-2.3.spdx --toVersion 2.3\n"
						+ "java -jar spdx-tools...jar CompareDocs output.xlsx ex1.spdx ex2.spdx.rdf\n"
					);
	}

}
