/**
 * SPDX-FileCopyrightText: Copyright (c) 2017 Source Auditor Inc.
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

/**
 * Default Exception thrown to the Online Tool
 *
 * @author Rohit Lodha
 */
public class OnlineToolException extends Exception {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor for OnlineToolException.
	 */
	public OnlineToolException() {
	}

	/**
	 * Constructs an OnlineToolException with the specified message.
	 * @param arg0
	 */
	public OnlineToolException(String arg0) {
		super(arg0);
	}

	/**
	 * Constructs an OnlineToolException with the specified throwable.
	 * @param arg0
	 */
	public OnlineToolException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * Constructs an OnlineToolException with the specified message and throwable.
	 * @param arg0
	 * @param arg1
	 */
	public OnlineToolException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * Constructs an OnlineToolException with the specified message, throwable, and booleans.
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 */
	public OnlineToolException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

}
