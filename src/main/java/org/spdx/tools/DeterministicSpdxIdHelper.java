/**
 * SPDX-FileCopyrightText: Copyright (c) 2026 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class DeterministicSpdxIdHelper {
	static final String STABLE_IDS_FLAG = "--stable-ids";
	private static final String FALLBACK_PREFIX = "SPDXRef-";
	private static final int HASH_HEX_LEN = 16;
	private static final String V3_ID_PATTERN = "^[A-Za-z0-9._-]+$";

	private DeterministicSpdxIdHelper() {
		// Utility class.
	}

	static boolean isStableIdsFlag(String arg) {
		return STABLE_IDS_FLAG.equalsIgnoreCase(arg);
	}

	static boolean isValidV3Id(String id) {
		if (id == null) {
			return false;
		}
		String trimmed = id.trim();
		return !trimmed.isEmpty() && trimmed.matches(V3_ID_PATTERN);
	}

	static String deterministicFallbackId(String fromObjectUri) {
		String input = fromObjectUri == null ? "" : fromObjectUri;
		String hex = sha256Hex(input);
		if (hex.length() > HASH_HEX_LEN) {
			hex = hex.substring(0, HASH_HEX_LEN);
		}
		return FALLBACK_PREFIX + hex;
	}

	private static String sha256Hex(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder(hashed.length * 2);
			for (byte value : hashed) {
				builder.append(String.format("%02x", value));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 not available", e);
		}
	}
}
