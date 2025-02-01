/**
 * SPDX-FileCopyrightText: Copyright (c) 2011 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.SpdxPackageVerificationCode;
import org.spdx.storage.IModelStore;
import org.spdx.storage.simple.InMemSpdxStore;
import org.spdx.utility.verificationcode.JavaSha1ChecksumGenerator;
import org.spdx.utility.verificationcode.VerificationCodeGenerator;

/**
 * Generates a verification code for a specific directory
 * @author Gary O'Neall
 *
 */
public class GenerateVerificationCode {

	/**
	 * Print an SPDX Verification code for a directory of files
	 * args[0] is the source directory containing the files
	 * args[1] is an optional regular expression of skipped files.  The expression is applied against a file path relative the the source directory supplied
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 1 || args.length > 2) {
			error("Incorrect number of arguments.");
			System.exit(1);
		}
		String directoryPath = args[0];
		String skippedRegex = null;
		if (args.length > 1) {
			skippedRegex = args[1];
		}

		SpdxToolsHelper.initialize();
		try {
			SpdxPackageVerificationCode verificationCode = generateVerificationCode(directoryPath, skippedRegex);
			printVerificationCode(verificationCode);
			System.exit(0);
		} catch (Exception ex) {
			error("Error creating verification code: "+ex.getMessage());
			System.exit(1);
		}
	}
	
	public static SpdxPackageVerificationCode generateVerificationCode(String directoryPath, @Nullable String skippedRegex) throws OnlineToolException {
		Objects.requireNonNull(directoryPath, "Directory path must not be null");
		File sourceDirectory = new File(directoryPath);
		if (!sourceDirectory.exists()) {
			throw new OnlineToolException("Source directory "+directoryPath+" does not exist.");
		}
		if (!sourceDirectory.isDirectory()) {
			throw new OnlineToolException("File "+directoryPath+" is not a directory.");
		}
		File[] skippedFiles = new File[0];
		if (Objects.nonNull(skippedRegex)) {
			skippedFiles = collectSkippedFiles(skippedRegex, sourceDirectory);
		}
		try {
			VerificationCodeGenerator vcg = new VerificationCodeGenerator(new JavaSha1ChecksumGenerator());
			IModelStore ms = new InMemSpdxStore();		
			return vcg.generatePackageVerificationCode(sourceDirectory, skippedFiles, ms, "https://temp/URI");
		} catch (NoSuchAlgorithmException e) {
			throw new OnlineToolException("Error creating checksum algorithm",e);
		} catch (IOException e) {
			throw new OnlineToolException("I/O Error generating verification code",e);
		} catch (InvalidSPDXAnalysisException e) {
			throw new OnlineToolException("SPDX Analysis Error generating verification code",e);
		}
	}

	/**
	 * Collect files to be skipped
	 * @param skippedRegex Regular Expression for file paths to be skipped
	 * @param dir Directory to scan for collecting skipped files
	 * @return
	 */
	private static File[] collectSkippedFiles(String skippedRegex, File dir) {
		Pattern skippedPattern = Pattern.compile(skippedRegex);
		List<File> skippedFiles = new ArrayList<>();
		collectSkippedFiles(skippedPattern, skippedFiles, dir.getPath(), dir);
		File[] retval = new File[skippedFiles.size()];
		retval = skippedFiles.toArray(retval);
		return retval;
	}

	/**
	 * Internal method to recurse through the source directory collecting files to skip
	 * @param skippedPattern
	 * @param skippedFiles
	 * @param rootPath
	 * @param dir
	 * @return
	 */
	private static void collectSkippedFiles(Pattern skippedPattern,
			List<File> skippedFiles, String rootPath, File dir) {
		if (dir.isFile()) {
			String relativePath = dir.getPath().substring(rootPath.length()+1);
			if (skippedPattern.matcher(relativePath).matches()) {
				skippedFiles.add(dir);
			}
		} else if (dir.isDirectory()) {
			File[] children = dir.listFiles();
			if (children != null) {
				for (int i = 0; i < children.length; i++) {
					if (children[i].isFile()) {
						String relativePath = children[i].getPath().substring(rootPath.length()+1);
						if (skippedPattern.matcher(relativePath).matches()) {
							skippedFiles.add(children[i]);
						}
					} else if (children[i].isDirectory()) {
						collectSkippedFiles(skippedPattern, skippedFiles, rootPath, children[i]);
					}
				}
			}
		}
	}

	/**
	 * @param verificationCode
	 * @throws InvalidSPDXAnalysisException 
	 */
	private static void printVerificationCode(
			SpdxPackageVerificationCode verificationCode) throws InvalidSPDXAnalysisException {
		System.out.println("Verification code value: "+verificationCode.getValue());
		String[] excludedFiles = verificationCode.getExcludedFileNames().toArray(new String[verificationCode.getExcludedFileNames().size()]);
		if (excludedFiles != null && excludedFiles.length > 0) {
			System.out.println("Excluded files:");
			for (int i = 0; i < excludedFiles.length; i++) {
				System.out.println("\t"+excludedFiles[i]);
			}
		} else {
			System.out.println("No excluded files");
		}
	}

	/**
	 * @param string
	 */
	private static void error(String string) {
		System.out.println(string);
		usage();
	}

	/**
	 *
	 */
	private static void usage() {
		System.out.println("Usage: GenerateVerificationCode sourceDirectory [skippedFiles]");
		System.out.println("where sourceDirectory is the root of the archive file for which the verification code is generated and [skippedFiles] is an optional regular expression of skipped files.  The expression is applied against a file path relative the the source directory supplied");
	}

}
