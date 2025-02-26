/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
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
package org.spdx.tools.schema;

/**
 * Exception thrown for schema-related errors
 *
 * @author Gary O'Neall
 */
public class SchemaException extends Exception {

	/**
	 * Serial version UID for serialization
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new SchemaException with the specified detail message
	 *
	 * @param message the detail message
	 */
	public SchemaException(String message) {
		super(message);
	}

	/**
	 * Constructs a new SchemaException with the specified detail message and
	 * cause
	 *
	 * @param message the detail message
	 * @param cause   the cause
	 */
	public SchemaException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new SchemaException with the specified detail message,
	 * cause, and configurations
	 * 
	 * @param message            the detail message
	 * @param cause              the cause
	 * @param enableSuppression  whether or not suppression is enabled
	 * @param writableStackTrace whether or not the stack trace is writable
	 */
	public SchemaException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
