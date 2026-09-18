/**
 * SPDX-FileCopyrightText: Copyright (c) 2014 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.spdx.tools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.utility.compare.LicenseCompareHelper;
import org.spdx.utility.compare.SpdxCompareException;

/**
 * Tool to compare a license text to standard licenses.  Lists all standard
 * license ID's that are equivalent using the SPDX Legal team's license matching
 * guidelines (http://spdx.org/spdx-license-list/matching-guidelines)
 * <br/>
 * Exit codes:
 * <ul>
 *   <li>0 - the comparison completed (with or without a match)</li>
 *   <li>1 - the comparison failed - e.g. the input file could not be read or the standard licenses could not be compared</li>
 *   <li>2 - the command was invoked incorrectly (missing/invalid arguments)</li>
 * </ul>
 * @author Gary O'Neall
 */
public class MatchingStandardLicenses {

	/**
	 * This class should not be instantiated.  Call the main method to invoke.
	 */
	private MatchingStandardLicenses() {

	}

	static int MIN_ARGS = 1;
	static int MAX_ARGS = 1;

	/**
	 * Main entry point for the MatchingStandardLicenses tool.
	 * Delegates to {@link #run(String[])} and terminates the JVM with its exit status.
	 * @param args
	 */
	public static void main(String[] args) {
		System.exit(run(args));
	}

	/**
	 * Runs the MatchingStandardLicenses command logic and reports results to
	 * standard out.
	 * @param args
	 * @return process exit status, see {@link ExitCode}
	 */
	static int run(String[] args) {
		if (args == null || args.length < MIN_ARGS || args.length > MAX_ARGS) {
			System.out.println("Invalid arguments");
			usage();
			return ExitCode.USAGE_ERROR;
		}
		String textFilePath = args[0];
		if (textFilePath == null) {
			System.out.println("Invalid arguments");
			usage();
			return ExitCode.USAGE_ERROR;
		}
		File textFile = new File(textFilePath);

		if (!textFile.exists()) {
			System.out.println("Text file "+textFile.getName()+" does not exist");
			usage();
			return ExitCode.ERROR;
		}

		SpdxToolsHelper.initialize();
		String licenseText = null;
		try {
			licenseText = readAll(textFile);
		} catch (IOException e) {
			System.out.println("Error reading file: "+e.getMessage());
			return ExitCode.ERROR;
		}

		List<String> matchingLicenseIds = null;
		try {
			matchingLicenseIds = LicenseCompareHelper.listAllListedLicenseIdsMatched(licenseText);
		} catch (InvalidSPDXAnalysisException e) {
			System.out.println("Error reading standard licenses: "+e.getMessage());
			return ExitCode.ERROR;
		} catch (SpdxCompareException e) {
			System.out.println("Error comparing licenses: "+e.getMessage());
			return ExitCode.ERROR;
		}

		if (matchingLicenseIds == null || matchingLicenseIds.isEmpty()) {
			System.out.println("No standard licenses matched.");
		} else {
			StringBuilder sb = new StringBuilder("The following license id(s) match: ");
			sb.append(matchingLicenseIds.get(0));
			for (int i = 1; i < matchingLicenseIds.size(); i++) {
				sb.append(", ");
				sb.append(matchingLicenseIds.get(i));
			}
			System.out.println(sb.toString());
		}
		return ExitCode.SUCCESS;
	}

	/**
	 * @param textFile
	 * @return
	 * @throws IOException
	 */
	private static String readAll(File textFile) throws IOException {
		return new String(Files.readAllBytes(textFile.toPath()), Charset.defaultCharset());
	}

	private static void usage() {
		System.out.println("Usage:");
		System.out.println("MatchingStandardLicenses textfile.txt");
		System.out.println("   textfile.txt is a text file containing the license text to compare.");
	}
}
