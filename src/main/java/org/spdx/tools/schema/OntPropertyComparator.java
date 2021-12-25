/**
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2021 Source Auditor Inc.
 */
package org.spdx.tools.schema;

import java.util.Comparator;
import java.util.Objects;

import org.apache.jena.ontology.OntProperty;

/**
 * Comparator for Ontology properties
 * 
 * @author Gary O'Neall
 *
 */
public class OntPropertyComparator implements Comparator<OntProperty> {

	@Override
	public int compare(OntProperty o1, OntProperty o2) {
		if (Objects.isNull(o1)) {
			return Objects.isNull(o2)? 0:1;
		}
		if (Objects.isNull(o2)) {
			return -1;
		}
		return o1.getLocalName().compareTo(o2.getLocalName());
	}

}
