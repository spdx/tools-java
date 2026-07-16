/**
 * SPDX-FileCopyrightText: 2026 SPDX Contributors
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

/**
 * Shared process exit status codes for the command line tools.
 * <br/>
 * Individual tools are free to only use a subset of these - e.g. a tool
 * with no document-level validity concept may only ever return
 * {@link #SUCCESS} or {@link #ERROR}.
 * <br/>
 * Values match the {@code CommandLine.ExitCode} constants used by
 * <a href="https://picocli.info/">picocli</a>, a common Java CLI framework 
 * ({@code OK=0}, {@code SOFTWARE=1}, {@code USAGE=2}).
 *
 * @author Arthit Suriyawongkul
 */
public class ExitCode {

	private ExitCode() {
		// Static constants only, no instances
	}

	/**
	 * The command completed successfully.
	 */
	public static final int SUCCESS = 0;

	/**
	 * The command failed - e.g. the SPDX document is invalid, or could
	 * not be read/parsed.
	 */
	public static final int ERROR = 1;

	/**
	 * The command was invoked incorrectly - e.g. missing or invalid
	 * arguments.
	 */
	public static final int USAGE_ERROR = 2;
}
