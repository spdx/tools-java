/**
 * SPDX-FileCopyrightText: Copyright (c) 2017 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

/**
 * Default Exception thrown to the Online Tool
 *
 * @author Rohit Lodha
 *
 */

public class OnlineToolException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 *
	 */
	public OnlineToolException() {
	}

	/**
	 *
	 * @param arg0
	 */
	public OnlineToolException(String arg0) {
		super(arg0);
	}

	/**
	 *
	 * @param arg0
	 */
	public OnlineToolException(Throwable arg0) {
		super(arg0);
	}

	/**
	 *
	 * @param arg0
	 * @param arg1
	 */
	public OnlineToolException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 *
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 */
	public OnlineToolException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

}
