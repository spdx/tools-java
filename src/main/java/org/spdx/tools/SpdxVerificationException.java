/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

/**
 * Exceptions for the SPDX Verify tools
 *
 * @author Gary O'Neall
 */
public class SpdxVerificationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public SpdxVerificationException() {
	}

	/**
	 * @param arg0
	 */
	public SpdxVerificationException(String arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public SpdxVerificationException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public SpdxVerificationException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 */
	public SpdxVerificationException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

}
