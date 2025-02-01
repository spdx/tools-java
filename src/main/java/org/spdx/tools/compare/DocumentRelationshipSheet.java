/**
 * SPDX-FileCopyrightText: Copyright (c) 2015 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools.compare;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.Relationship;
import org.spdx.library.model.v2.SpdxElement;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

/**
 * Sheet for document level relationships
 * @author Gary O'Neall
 *
 */
public class DocumentRelationshipSheet extends AbstractSheet {

	private static class RelationshipComparator implements Comparator<Relationship>, Serializable {

		/**
		 * Default
		 */
		private static final long serialVersionUID = 1L;

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(Relationship o1, Relationship o2) {
			try {
				if (o1 != null) {
					if (o2 != null) {
						Relationship r1 = o1;
						Relationship r2 = o2;
						int retval = r1.getRelationshipType().toString().compareTo(r2.getRelationshipType().toString());
						if (retval != 0) {
							return retval;
						}
						Optional<SpdxElement> relatedElement1 = r1.getRelatedSpdxElement();
						Optional<SpdxElement> relatedElement2 = r2.getRelatedSpdxElement();
						if (relatedElement1.isPresent() && !relatedElement2.isPresent()) {
							return 1;
						} else if (!relatedElement1.isPresent() && relatedElement2.isPresent()) {
							return -1;
						} else if (relatedElement1.get().equivalent(relatedElement2.get())) {
							return 0;
						}
						Optional<String> name1 = relatedElement1.get().getName();
						Optional<String> name2 = relatedElement2.get().getName();
						if (name1.isPresent() &&
								name2.isPresent()) {
							return name1.get().compareTo(
									name2.get());
						} else {
							return relatedElement1.get().getId().compareTo(relatedElement2.get().getId());
						}
					} else {
						return 1;
					}
				} else {
					return -1;
				}
			} catch(InvalidSPDXAnalysisException ex) {
				logger.error("Error comparing relationships", ex);
				throw new RuntimeException(ex);
			}
			
		}
	}

	RelationshipComparator relationshipComparator = new RelationshipComparator();

	static final int TYPE_COL = 0;
	static final int TYPE_COL_WIDTH = 25;
	static final String TYPE_COL_TEXT_TITLE = "Type";
	static final int FIRST_RELATIONSHIP_COL = 1;
	static final int FIRST_RELATIONSHIP_COL_WIDTH = 60;

	public DocumentRelationshipSheet(Workbook workbook, String sheetName) {
		super(workbook, sheetName);
	}

	/**
	 * @param wb
	 * @param sheetName
	 */
	public static void create(Workbook wb, String sheetName) {
		int sheetNum = wb.getSheetIndex(sheetName);
		if (sheetNum >= 0) {
			wb.removeSheetAt(sheetNum);
		}
		Sheet sheet = wb.createSheet(sheetName);
		CellStyle headerStyle = AbstractSheet.createHeaderStyle(wb);
		CellStyle defaultStyle = AbstractSheet.createLeftWrapStyle(wb);
		Row row = sheet.createRow(0);

		sheet.setColumnWidth(TYPE_COL, TYPE_COL_WIDTH*256);
		sheet.setDefaultColumnStyle(TYPE_COL, defaultStyle);
		Cell typeHeaderCell = row.createCell(TYPE_COL);
		typeHeaderCell.setCellStyle(headerStyle);
		typeHeaderCell.setCellValue(TYPE_COL_TEXT_TITLE);

		for (int i = FIRST_RELATIONSHIP_COL; i < MultiDocumentSpreadsheet.MAX_DOCUMENTS; i++) {
			sheet.setColumnWidth(i, FIRST_RELATIONSHIP_COL_WIDTH*256);
			sheet.setDefaultColumnStyle(i, defaultStyle);
			Cell cell = row.createCell(i);
			cell.setCellStyle(headerStyle);
		}
	}

	/**
	 * @param comparer
	 * @param docNames
	 * @throws InvalidSPDXAnalysisException
	 */
	public void importCompareResults(SpdxComparer comparer, List<String> docNames) throws SpdxCompareException, InvalidSPDXAnalysisException {
		if (comparer.getNumSpdxDocs() != docNames.size()) {
			throw(new SpdxCompareException("Number of document names does not match the number of SPDX documents"));
		}
		this.clear();
		Row header = sheet.getRow(0);
		int[] relationshipsIndexes = new int[comparer.getNumSpdxDocs()];
		Relationship[][] relationships = new Relationship[comparer.getNumSpdxDocs()][];
		for (int i = 0; i < relationships.length; i++) {
			Cell headerCell = header.getCell(FIRST_RELATIONSHIP_COL+i);
			headerCell.setCellValue(docNames.get(i));
			Relationship[] docRelationships = comparer.getSpdxDoc(i).getRelationships().toArray(new Relationship[comparer.getSpdxDoc(i).getRelationships().size()]);
			Arrays.sort(docRelationships, relationshipComparator);
			relationships[i] = docRelationships;
			relationshipsIndexes[i] = 0;
		}
		while (!allRelationshipsExhausted(relationships, relationshipsIndexes)) {
			Row currentRow = this.addRow();
			Relationship nextRelationship = getNexRelationship(relationships, relationshipsIndexes);
			if (Objects.nonNull(nextRelationship)) {
			    Cell typeCell = currentRow.createCell(TYPE_COL);
	            typeCell.setCellValue(nextRelationship.getRelationshipType().toString());
	            for (int i = 0; i < relationships.length; i++) {
	                if (relationships[i].length > relationshipsIndexes[i]) {
	                    Relationship compareRelationship = relationships[i][relationshipsIndexes[i]];
	                    if (relationshipComparator.compare(nextRelationship, compareRelationship) == 0) {
	                        Cell relationshipCell = currentRow.createCell(FIRST_RELATIONSHIP_COL+i);
	                        relationshipCell.setCellValue(CompareHelper.relationshipToString(relationships[i][relationshipsIndexes[i]]));
	                        relationshipsIndexes[i]++;
	                    }
	                }
	            }
			}
		}
	}

	
	/**
	 * @param relationships
	 * @param relationshipsIndexes
	 * @return
	 */
	private Relationship getNexRelationship(Relationship[][] relationships,
			int[] relationshipsIndexes) {
		Relationship retval = null;
		for (int i = 0; i < relationships.length; i++) {
			if (relationships[i].length > relationshipsIndexes[i]) {
				Relationship candidate = relationships[i][relationshipsIndexes[i]];
				if (retval == null || this.relationshipComparator.compare(retval, candidate) > 0) {
					retval = candidate;
				}
			}
		}
		return retval;
	}

	/**
	 * @param relationships
	 * @param relationshipsIndexes
	 * @return
	 */
	private boolean allRelationshipsExhausted(Relationship[][] relationships,
			int[] relationshipsIndexes) {
		for (int i = 0; i < relationships.length; i++) {
			if (relationshipsIndexes[i] < relationships[i].length) {
				return false;
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.spdx.spdxspreadsheet.AbstractSheet#verify()
	 */
	@Override
	public String verify() {
		return null;	// Nothing to verify
	}

}
