/**
 * SPDX-FileCopyrightText: Copyright (c) 2024 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.spdx.core.DefaultModelStore;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v3_0_1.core.Element;
import org.spdx.library.model.v3_0_1.core.NamespaceMap;
import org.spdx.library.model.v3_0_1.core.SpdxDocument;
import org.spdx.library.model.v3_0_1.software.SpdxFile;
import org.spdx.library.model.v3_0_1.software.SpdxPackage;
import org.spdx.storage.simple.InMemSpdxStore;
import org.spdx.tools.SpdxToolsHelper.SerFileType;
import org.spdx.utility.compare.SpdxCompareException;

import junit.framework.TestCase;

/**
 * Test SPDX converter v3
 *
 * @author Gary O'Neall
 */
public class SpdxConverterTestV3 extends TestCase {
	
	static final String TEST_DIR = "testResources";
	static final String TEST_JSON_FILE_PATH = TEST_DIR + File.separator + "SPDXJSONExample-v2.3.spdx.json";

	Path tempDirPath;
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		SpdxModelFactory.init();
		DefaultModelStore.initialize(new InMemSpdxStore(), "http://default/namespace", new ModelCopyManager());
		tempDirPath = Files.createTempDirectory("spdx-tools-test-");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		deleteDirAndFiles(tempDirPath);
	}
	
	public static void deleteDirAndFiles(Path dirOrFile) {
		if (Objects.isNull(dirOrFile)) {
			return;
		}
		if (!Files.exists(dirOrFile, LinkOption.NOFOLLOW_LINKS)) {
			return;
		}
		if (Files.isDirectory(dirOrFile, LinkOption.NOFOLLOW_LINKS)) {
			try (DirectoryStream<Path> files = Files.newDirectoryStream(dirOrFile)) {
				for (Path file : files) {
					deleteDirAndFiles(file);
			      }
			} catch (IOException e) {
				System.err.println("IO error deleting directory or file "+e.getMessage());
			}
		}
		try {
			Files.delete(dirOrFile);
		} catch (IOException e) {
			System.err.println("IO error deleting directory or file "+e.getMessage());
		}
	}

	public void testV2JsonToV3JsonLD() throws SpdxConverterException, InvalidSPDXAnalysisException, IOException, SpdxCompareException {
		String jsonLdFileName = "result.jsonld";
		Path outFilePath = tempDirPath.resolve(jsonLdFileName);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString(), SerFileType.JSON, SerFileType.JSONLD);
		File result = new File(outFilePath.toString());
		File source = new File(TEST_JSON_FILE_PATH);
		assertTrue(result.exists());
		org.spdx.library.model.v2.SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocumentCompatV2(source, SerFileType.JSON);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.JSONLD);
		List<String> verify = resultDoc.verify();
		assertEquals(0, verify.size());
		org.spdx.library.model.v2.SpdxElement[] sourceRoots = sourceDoc.getDocumentDescribes().toArray(
				new org.spdx.library.model.v2.SpdxElement[sourceDoc.getDocumentDescribes().size()]);
		assertEquals(2, sourceRoots.length);
		org.spdx.library.model.v2.SpdxPackage sourcePackage = (org.spdx.library.model.v2.SpdxPackage)(
				sourceRoots[0] instanceof org.spdx.library.model.v2.SpdxPackage ? sourceRoots[0] : sourceRoots[1]);
		org.spdx.library.model.v2.SpdxFile sourceFile = (org.spdx.library.model.v2.SpdxFile)(
				sourceRoots[0] instanceof org.spdx.library.model.v2.SpdxFile ? sourceRoots[0] : sourceRoots[1]);
		Element[] resultRoots = resultDoc.getRootElements().toArray(new Element[resultDoc.getRootElements().size()]);
		assertEquals(2, resultRoots.length);
		SpdxPackage resultPackage = (SpdxPackage)(resultRoots[0] instanceof SpdxPackage ? resultRoots[0] : resultRoots[1]);
		SpdxFile resultFile = (SpdxFile)(resultRoots[0] instanceof SpdxFile ? resultRoots[0] : resultRoots[1]);
		
		assertEquals(sourcePackage.getName().get(), resultPackage.getName().get());
		assertEquals(sourceFile.getName().get(), resultFile.getName().get());

		assertEquals(1, resultDoc.getNamespaceMaps().size());
		Optional<NamespaceMap> map = resultDoc.getNamespaceMaps().stream().findFirst();
		assertTrue(map.isPresent());
		assertEquals("http://spdx.org/spdxdocs/spdx-tools-v1.2-3F2504E0-4F89-41D3-9A0C-0305E82C3301#", map.get().getNamespace());
		assertEquals("DocumentRef-spdx-tool-1.2", map.get().getPrefix());
		// TODO: create a more extensive set of checks
	}

	public void testV2JsonToV3JsonLDStableIds() throws SpdxConverterException, InvalidSPDXAnalysisException, IOException {
		Path outFilePath1 = tempDirPath.resolve("result-stable-1.jsonld");
		Path outFilePath2 = tempDirPath.resolve("result-stable-2.jsonld");
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath1.toString(), SerFileType.JSON, SerFileType.JSONLD, false, true);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath2.toString(), SerFileType.JSON, SerFileType.JSONLD, false, true);
		SpdxDocument resultDoc1 = SpdxToolsHelper.deserializeDocument(outFilePath1.toFile(), SerFileType.JSONLD);
		SpdxDocument resultDoc2 = SpdxToolsHelper.deserializeDocument(outFilePath2.toFile(), SerFileType.JSONLD);
		assertEquals(resultDoc1.getId(), resultDoc2.getId());
		List<String> rootIds1 = resultDoc1.getRootElements().stream()
				.map(Element::getId)
				.sorted()
				.collect(Collectors.toList());
		List<String> rootIds2 = resultDoc2.getRootElements().stream()
				.map(Element::getId)
				.sorted()
				.collect(Collectors.toList());
		assertEquals(rootIds1, rootIds2);
	}

}
