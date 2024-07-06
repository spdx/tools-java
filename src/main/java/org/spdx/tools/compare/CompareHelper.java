/**
 * Copyright (c) 2015 Source Auditor Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
*/
package org.spdx.tools.compare;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.Annotation;
import org.spdx.library.model.v2.Checksum;
import org.spdx.library.model.v2.ExternalRef;
import org.spdx.library.model.v2.ModelObjectV2;
import org.spdx.library.model.v2.Relationship;
import org.spdx.library.model.v2.SpdxElement;
import org.spdx.library.model.v2.enumerations.FileType;
import org.spdx.library.model.v2.license.AnyLicenseInfo;
import org.spdx.library.referencetype.ListedReferenceTypes;

/**
 * Helper class for comparisons
 * @author Gary O'Neall
 *
 */
public class CompareHelper {

	static final int MAX_CHARACTERS_PER_CELL = 32000;

	/**
	 *
	 */
	private CompareHelper() {
		// Static helper, should not be instantiated
	}

	/**
	 * @param annotation
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	public static String annotationToString(Annotation annotation) throws InvalidSPDXAnalysisException {
		if (annotation == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(annotation.getAnnotationDate());
		sb.append(" ");
		sb.append(annotation.getAnnotator());
		sb.append(": ");
		sb.append(annotation.getComment());
		sb.append("[");
		sb.append(annotation.getAnnotationType().toString());
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Create a string from an array of checksums
	 * @param checksums
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	public static String checksumsToString(Collection<Checksum> checksums) throws InvalidSPDXAnalysisException {
		if (checksums == null || checksums.size() == 0) {
			return "";
		}
		List<String> cksumString = new ArrayList<>();
		for (Checksum checksum:checksums) {
			cksumString.add(checksumToString(checksum));
		}
		Collections.sort(cksumString);
		StringBuilder sb = new StringBuilder(cksumString.get(0));
		for (int i = 1; i < cksumString.size(); i++) {
			sb.append("\n");
			if (sb.length() + cksumString.get(i).length() > MAX_CHARACTERS_PER_CELL) {
				int numRemaing = cksumString.size() - i;
				sb.append('[');
				sb.append(numRemaing);
				sb.append(" more...]");
				break;
			}
			sb.append(cksumString.get(i));
		}
		return sb.toString();
	}

	/**
	 * @param checksum
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	public static String checksumToString(Checksum checksum) throws InvalidSPDXAnalysisException {
		if (checksum == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(checksum.getAlgorithm().toString());
		sb.append(' ');
		sb.append(checksum.getValue());
		return sb.toString();
	}

	/**
	 * @param licenseInfos
	 * @return
	 */
	public static String licenseInfosToString(Collection<AnyLicenseInfo> licenseInfos) {
		if (licenseInfos == null || licenseInfos.size() == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		Iterator<AnyLicenseInfo> iter = licenseInfos.iterator();
		sb.append(iter.next().toString());
		while (iter.hasNext()) {
			sb.append(", ");
			sb.append(iter.next().toString());
		}
		return sb.toString();
	}

	/**
	 * @param annotations
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	public static String annotationsToString(Collection<Annotation> annotations) throws InvalidSPDXAnalysisException {
		
		if (annotations == null || annotations.size() == 0) {
			return "";
		}
		Iterator<Annotation> iter = annotations.iterator();
		StringBuilder sb = new StringBuilder(annotationToString(iter.next()));
		int numRemaining = annotations.size() - 1;
		while (iter.hasNext()) {
			sb.append("\n");
			String annotation = annotationToString(iter.next());
			numRemaining--;
			if (sb.length() + annotation.length() > MAX_CHARACTERS_PER_CELL) {
				sb.append('[');
				sb.append(numRemaining);
				sb.append(" more...]");
				break;
			}
			sb.append(annotation);
		}
		return sb.toString();
	}

	public static String attributionsToString(Collection<String> attributions) {
		if (attributions == null || attributions.size() == 0) {
			return "";
		}
		Iterator<String> iter = attributions.iterator();
		StringBuilder sb = new StringBuilder(iter.next());
		while (iter.hasNext()) {
			sb.append("\n");
			sb.append(iter.next());
		}
		return sb.toString();
	}

	public static String relationshipToString(Relationship relationship) throws InvalidSPDXAnalysisException {
		if (relationship == null) {
			return "";
		}
		if (relationship.getRelationshipType() == null) {
			return "Unknown relationship type";
		}
		StringBuilder sb = new StringBuilder(relationship.getRelationshipType().toString());
		sb.append(":");
		Optional<SpdxElement> relatedElement = relationship.getRelatedSpdxElement();
		if (!relatedElement.isPresent()) {
			sb.append("?NULL");
		} else {
		    Optional<String> relatedElementName = relatedElement.get().getName();
			if (relatedElementName.isPresent()) {
				sb.append('[');
				sb.append(relatedElementName.get());
				sb.append(']');
			}
			sb.append(relatedElement.get().getId());
		}
		Optional<String> comment = relationship.getComment();
		if (comment.isPresent() && !comment.get().isEmpty()) {
			sb.append('(');
			sb.append(comment.get());
			sb.append(')');
		}
		return sb.toString();
	}

	/**
	 * @param relationships
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	public static String relationshipsToString(Collection<Relationship> relationships) throws InvalidSPDXAnalysisException {
		if (relationships == null || relationships.size() == 0) {
			return "";
		}
		Iterator<Relationship> iter = relationships.iterator();
		StringBuilder sb = new StringBuilder(relationshipToString(iter.next()));
		int numRemaining = relationships.size() - 1;
		while (iter.hasNext()) {
			sb.append("\n");
			String nextRelationship = relationshipToString(iter.next());
			numRemaining--;
			if (sb.length() + nextRelationship.length() > MAX_CHARACTERS_PER_CELL) {
				sb.append('[');
				sb.append(numRemaining);
				sb.append(" more...]");
				break;
			}
			sb.append(nextRelationship);
		}
		return sb.toString();
	}

	public static String formatSpdxElementList(Collection<SpdxElement> elements) throws InvalidSPDXAnalysisException {
		if (elements == null || elements.size() == 0) {
			return "";
		}
		
		Iterator<SpdxElement> iter = elements.iterator();
		StringBuilder sb = new StringBuilder(formatElement(iter.next()));
		int numRemaining = elements.size() - 1;
		while (iter.hasNext()) {
			sb.append(", ");
			String nextElement = formatElement(iter.next());
			numRemaining--;
			if (sb.length() + nextElement.length() > MAX_CHARACTERS_PER_CELL) {
				sb.append('[');
				sb.append(numRemaining);
				sb.append(" more...]");
				break;
			}
			sb.append(nextElement);
		}
		return sb.toString();
	}

	private static String formatElement(SpdxElement element) throws InvalidSPDXAnalysisException {
		if (Objects.isNull(element) || element.getId() == null || element.getId().isEmpty()) {
			return "[UNKNOWNID]";
		} else {
			StringBuilder sb = new StringBuilder(element.getId());
			Optional<String> name = element.getName();
			if (name.isPresent()) {
				sb.append('(');
				sb.append(name.get());
				sb.append(')');
			}
			return sb.toString();
		}
	}

	/**
	 * @param fileTypes
	 * @return
	 */
	public static String fileTypesToString(FileType[] fileTypes) {
		if (fileTypes == null || fileTypes.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder(fileTypes[0].toString());
		for (int i = 1;i < fileTypes.length; i++) {
			sb.append(", ");
			String fileType = fileTypes[i].toString();
			if (sb.length() + fileType.length() > MAX_CHARACTERS_PER_CELL) {
				int numRemaing = fileTypes.length - i;
				sb.append('[');
				sb.append(numRemaing);
				sb.append(" more...]");
				break;
			}
			sb.append(fileType);
		}
		return sb.toString();
	}

	/**
	 * Convert external refs to a friendly string
	 * @param externalRefs
	 * @return
	 * @throws InvalidSPDXAnalysisException
	 */
	public static String externalRefsToString(Collection<ExternalRef> externalRefs, String docNamespace) throws InvalidSPDXAnalysisException {
		if (externalRefs == null || externalRefs.size() == 0) {
			return "";
		}
		Iterator<ExternalRef> iter = externalRefs.iterator();
		StringBuilder sb = new StringBuilder(externalRefToString(iter.next(), docNamespace));
		while (iter.hasNext()) {
			sb.append("; ");
			sb.append(externalRefToString(iter.next(), docNamespace));
		}
		return sb.toString();
	}

	/**
	 * Convert a single external ref to a friendly string
	 * @param externalRef
	 * @param docNamespace
	 * @return
	 * @throws InvalidSPDXAnalysisException
	 */
	public static String externalRefToString(ExternalRef externalRef, String docNamespace) throws InvalidSPDXAnalysisException {
		String category = null;
		if (externalRef.getReferenceCategory() == null) {
			category = "OTHER";
		} else {
			category = externalRef.getReferenceCategory().toString();
		}
		String referenceType = null;
		if (externalRef.getReferenceType() == null) {
			referenceType = "[MISSING]";
		} else {
			try {
				referenceType = ListedReferenceTypes.getListedReferenceTypes().getListedReferenceName(new URI(externalRef.getReferenceType().getIndividualURI()));
			} catch (InvalidSPDXAnalysisException e) {
				referenceType = null;
			} catch (URISyntaxException e) {
				referenceType = null;
			}
			if (referenceType == null) {
				referenceType = externalRef.getReferenceType().getIndividualURI();
				if (docNamespace != null && !docNamespace.isEmpty() && referenceType.startsWith(docNamespace)) {
					referenceType = referenceType.substring(docNamespace.length());
				}
			}
		}
		String referenceLocator = externalRef.getReferenceLocator();
		if (referenceLocator == null) {
			referenceLocator = "[MISSING]";
		}
		String retval = category + " " + referenceType + " " + referenceLocator;
		Optional<String> comment = externalRef.getComment();
		if (comment.isPresent() && !comment.get().isEmpty()) {
			retval = retval + "(" + comment.get() + ")";
		}
		return retval;
	}

	public static String checksumToString(Optional<Checksum> checksum) throws InvalidSPDXAnalysisException {
		if (checksum.isPresent()) {
			return checksumToString(checksum.get());
		} else {
			return "[NONE]";
		}
	}

	public static boolean equivalent(Optional<? extends ModelObjectV2> c1, Optional<? extends ModelObjectV2> c2) throws InvalidSPDXAnalysisException {
		if (!c1.isPresent()) {
			return !c2.isPresent();
		}
		if (c2.isPresent()) {
			return (c1.get().equivalent(c2.get()));
		} else {
			return false;
		}
	}

	public static boolean equivalent(Collection<? extends ModelObjectV2> collection1, Collection<? extends ModelObjectV2> collection2) throws InvalidSPDXAnalysisException {
		if (Objects.isNull(collection1)) {
			return Objects.isNull(collection2);
		}
		if (Objects.isNull(collection2)) {
			return false;
		}
		if (collection1.size() != collection2.size()) {
			return false;
		}
		for (ModelObjectV2 o1:collection1) {
			boolean found = false;
			for (ModelObjectV2 o2:collection2) {
				if (o1.equivalent(o2)) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}
}
