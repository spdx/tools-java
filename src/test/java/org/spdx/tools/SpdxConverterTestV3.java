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
import org.spdx.library.model.v3_0_1.SpdxConstantsV3;
import org.spdx.library.model.v3_0_1.core.Element;
import org.spdx.library.model.v3_0_1.core.Hash;
import org.spdx.library.model.v3_0_1.core.HashAlgorithm;
import org.spdx.library.model.v3_0_1.core.NamespaceMap;
import org.spdx.library.model.v3_0_1.core.SpdxDocument;
import org.spdx.library.model.v3_0_1.software.SpdxFile;
import org.spdx.library.model.v3_0_1.software.SpdxPackage;
import org.spdx.library.model.v3_0_1.software.Snippet;
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

		// Snippet fidelity. Expected values are SPDXRef-Snippet's fields in SPDXJSONExample-v2.3.spdx.json.
		List<Snippet> resultSnippets = SpdxModelFactory.getSpdxObjects(resultDoc.getModelStore(), null,
						SpdxConstantsV3.SOFTWARE_SNIPPET, null, null)
				.map(o -> (Snippet)o).collect(Collectors.toList());
		assertEquals(1, resultSnippets.size());
		Snippet resultSnippet = resultSnippets.get(0);
		assertEquals("from linux kernel", resultSnippet.getName().get());
		assertEquals("Copyright 2008-2010 John Smith", resultSnippet.getCopyrightText().get());
		// byte offset range (ranges[0]) and line number range (ranges[1]) from the source snippet
		assertEquals(Integer.valueOf(310), resultSnippet.getByteRange().get().getBeginIntegerRange());
		assertEquals(Integer.valueOf(420), resultSnippet.getByteRange().get().getEndIntegerRange());
		assertEquals(Integer.valueOf(5), resultSnippet.getLineRange().get().getBeginIntegerRange());
		assertEquals(Integer.valueOf(23), resultSnippet.getLineRange().get().getEndIntegerRange());
		assertNotNull(resultSnippet.getSnippetFromFile());

		// File checksum fidelity.
		// TODO: package checksums don't carry over as Hash verifiedUsings on the v3 Package
		// (only PackageVerificationCode does) - fix in SpdxConverter/Spdx2to3Converter, then assert here too.
		List<Hash> resultFileHashes = resultFile.getVerifiedUsings().stream()
				.filter(im -> im instanceof Hash).map(im -> (Hash)im).collect(Collectors.toList());
		Optional<Hash> resultSha1 = resultFileHashes.stream()
				.filter(h -> {
					try {
						return HashAlgorithm.SHA1.equals(h.getAlgorithm());
					} catch (InvalidSPDXAnalysisException e) {
						throw new RuntimeException(e);
					}
				}).findFirst();
		assertTrue(resultSha1.isPresent());
		assertEquals("d6a770ba38583ed4bb4525bd96e50461655d2758", resultSha1.get().getHashValue());
		assertEquals("Copyright 2008-2010 John Smith", resultPackage.getCopyrightText().get());

		// Annotation count survives conversion (3 doc-level + 1 package + 1 file).
		long resultAnnotationCount = SpdxModelFactory.getSpdxObjects(resultDoc.getModelStore(), null,
						SpdxConstantsV3.CORE_ANNOTATION, null, null).count();
		assertEquals(5, resultAnnotationCount);

		// Relationship count: converter also synthesizes license/distribution relationships, so assert a floor.
		long resultRelationshipCount = SpdxModelFactory.getSpdxObjects(resultDoc.getModelStore(), null,
						SpdxConstantsV3.CORE_RELATIONSHIP, null, null).count();
		assertTrue(resultRelationshipCount >= 7);
	}

}
