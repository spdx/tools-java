/**
 * SPDX-FileCopyrightText: Copyright (c) 2013 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools.compare;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Optional;

/**
 * Compares to file name strings normalizing them to a common format using the following rules:
 *  - File separator character is "/"
 *  - Must begin with "./"
 * @author Gary O'Neall
 *
 */
public class NormalizedFileNameComparator implements Comparator<Optional<String>>, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	static final char DOS_SEPARATOR = '\\';
	static final char UNIX_SEPARATOR = '/';
	static final String RELATIVE_DIR = "./";
	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Optional<String> fileName1, Optional<String> fileName2) {
		String normalizedFileName1 = normalizeFileName(fileName1);
		String normalizedFileName2 = normalizeFileName(fileName2);
		return normalizedFileName1.compareTo(normalizedFileName2);
	}

	/**
	 * Returns true if fileName2 matches fileName1 except for leading file name directories
	 * @param fileName1
	 * @param fileName2
	 * @return true if fileName2 matches fileName1 except for leading file name directories
	 */
	public static boolean hasLeadingDir(String fileName1, String fileName2) {
		String compareName1 = fileName1;
		String compareName2 = fileName2;
		if (compareName1.startsWith(RELATIVE_DIR)) {
			compareName1 = compareName1.substring(RELATIVE_DIR.length());
		}
		if (compareName2.startsWith(RELATIVE_DIR)) {
			compareName2 = compareName2.substring(RELATIVE_DIR.length());
		}
		if (compareName1.length() <= compareName2.length()) {
			return false;
		}
		if (!compareName1.endsWith(compareName2)) {
			return false;
		}
		char schar = compareName1.charAt(compareName1.length()-compareName2.length()-1);
		return (schar == UNIX_SEPARATOR);
	}

	public static String normalizeFileName(Optional<String> fileName) {
		if (!fileName.isPresent()) {
			return "[NO_NAME]";
		}
		String retval = fileName.get().replace(DOS_SEPARATOR, UNIX_SEPARATOR);
		if (!retval.startsWith(RELATIVE_DIR)) {
			retval = RELATIVE_DIR + retval;
		}
		return retval;
	}

}
