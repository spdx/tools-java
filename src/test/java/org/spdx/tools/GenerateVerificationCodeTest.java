package org.spdx.tools;

import java.io.File;

import org.spdx.core.DefaultModelStore;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.core.ModelRegistry;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.model.v2.SpdxModelInfoV2_X;
import org.spdx.library.model.v2.SpdxPackageVerificationCode;
import org.spdx.library.model.v3.SpdxModelInfoV3_0;
import org.spdx.storage.simple.InMemSpdxStore;

import junit.framework.TestCase;

public class GenerateVerificationCodeTest extends TestCase {
	
	static final String TEST_DIR = "testResources";
	static final String FILES = TEST_DIR + File.separator + "sourcefiles";

	protected void setUp() throws Exception {
		super.setUp();
		ModelRegistry.getModelRegistry().registerModel(new SpdxModelInfoV3_0());
		ModelRegistry.getModelRegistry().registerModel(new SpdxModelInfoV2_X());
		DefaultModelStore.initialize(new InMemSpdxStore(), "http://default/namespace", new ModelCopyManager());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGenerateVerificationCode() throws OnlineToolException, InvalidSPDXAnalysisException {
		String skippedRegex = "Ver.*";
		SpdxPackageVerificationCode result = GenerateVerificationCode.generateVerificationCode(FILES, skippedRegex);
		assertFalse(result.getValue().isEmpty());
		assertEquals(1, result.getExcludedFileNames().size());
		assertTrue(result.getExcludedFileNames().contains("./VerificationSheet.java"));
		SpdxPackageVerificationCode result2 = GenerateVerificationCode.generateVerificationCode(FILES, null);
		assertFalse(result.equivalent(result2));
		assertEquals(0, result2.getExcludedFileNames().size());
	}

}
