/**
 * SPDX-FileCopyrightText: 2026 SPDX Contributors
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.spdx.core.DefaultModelStore;
import org.spdx.core.ModelRegistry;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.model.v2.SpdxModelInfoV2_X;
import org.spdx.library.model.v3_0_1.SpdxModelInfoV3_0;
import org.spdx.storage.simple.InMemSpdxStore;

import junit.framework.TestCase;

/**
 * Test cases for MatchingStandardLicenses
 *
 * @author Arthit Suriyawongkul
 */
public class MatchingStandardLicensesTest extends TestCase {

	static final String TEST_DIR = "testResources";

	protected void setUp() throws Exception {
		super.setUp();
		// Force local JAR-cached listed licenses,
		// so tests don't access network - avoids slow/flaky runs.
		System.setProperty("org.spdx.useJARLicenseInfoOnly", "true");
		ModelRegistry.getModelRegistry().registerModel(new SpdxModelInfoV3_0());
		ModelRegistry.getModelRegistry().registerModel(new SpdxModelInfoV2_X());
		DefaultModelStore.initialize(new InMemSpdxStore(), "http://default/namespace",
				new ModelCopyManager());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testMatch() throws Exception {
		File licenseFile = File.createTempFile("apache20", ".txt");
		licenseFile.deleteOnExit();
		Files.write(licenseFile.toPath(),
				"Apache License Version 2.0, January 2004".getBytes(StandardCharsets.UTF_8));
		int result = MatchingStandardLicenses.run(new String[] {licenseFile.getAbsolutePath()});
		assertEquals(ExitCode.SUCCESS, result);
	}

	public void testFileNotFound() {
		int result = MatchingStandardLicenses
				.run(new String[] {TEST_DIR + File.separator + "doesNotExist.txt"});
		assertEquals(ExitCode.ERROR, result);
	}

	public void testUsageError() {
		int result = MatchingStandardLicenses.run(new String[] {});
		assertEquals(ExitCode.USAGE_ERROR, result);

		result = MatchingStandardLicenses.run(new String[] {"a.txt", "b.txt"});
		assertEquals(ExitCode.USAGE_ERROR, result);

		result = MatchingStandardLicenses.run(null);
		assertEquals(ExitCode.USAGE_ERROR, result);
	}
}
