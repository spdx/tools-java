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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.Annotation;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

/**
 * Sheet for document level annotations
 * @author Gary O'Neall
 *
 */
public class DocumentAnnotationSheet extends AbstractSheet {

	private static class AnnotationComparator implements Comparator<Annotation>, Serializable {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(Annotation o1, Annotation o2) {
			try {
				if (o1 != null) {
					if (o2 != null) {
						Annotation a1 = o1;
						Annotation a2 = o2;
						int retval = a1.getAnnotator().compareTo(a2.getAnnotator());
						if (retval != 0) {
							return retval;
						}
						retval = a1.getAnnotationType().toString().compareTo(a2.getAnnotationType().toString());
						if (retval != 0) {
							return retval;
						}
						return a1.getComment().compareTo(a2.getComment());
					} else {
						return 1;
					}
				} else {
					return -1;
				}
			} catch(InvalidSPDXAnalysisException ex) {
				logger.error("Error comparing annotations", ex);
				throw new RuntimeException(ex);
			}
		}
	}

	AnnotationComparator annotationComparator = new AnnotationComparator();

	static final int ANNOTATOR_COL = 0;
	static final int ANNOTATOR_COL_WIDTH = 40;
	static final String ANNOTATOR_COL_TEXT_TITLE = "Annotator";

	static final int TYPE_COL = 1;
	static final int TYPE_COL_WIDTH = 15;
	static final String TYPE_COL_TEXT_TITLE = "Type";

	static final int COMMENT_COL = 2;
	static final int COMMENT_COL_WIDTH = 70;
	static final String COMMENT_COL_TEXT_TITLE = "Comment";

	static final int FIRST_DATE_COL = 3;
	static final int DATE_COL_WIDTH = 25;

	public DocumentAnnotationSheet(Workbook workbook, String sheetName) {
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
		sheet.setColumnWidth(ANNOTATOR_COL, ANNOTATOR_COL_WIDTH*256);
		sheet.setDefaultColumnStyle(ANNOTATOR_COL, defaultStyle);
		Cell annotatorHeaderCell = row.createCell(ANNOTATOR_COL);
		annotatorHeaderCell.setCellStyle(headerStyle);
		annotatorHeaderCell.setCellValue(ANNOTATOR_COL_TEXT_TITLE);

		sheet.setColumnWidth(TYPE_COL, TYPE_COL_WIDTH*256);
		sheet.setDefaultColumnStyle(TYPE_COL, defaultStyle);
		Cell typeHeaderCell = row.createCell(TYPE_COL);
		typeHeaderCell.setCellStyle(headerStyle);
		typeHeaderCell.setCellValue(TYPE_COL_TEXT_TITLE);

		sheet.setColumnWidth(COMMENT_COL, COMMENT_COL_WIDTH*256);
		sheet.setDefaultColumnStyle(COMMENT_COL, defaultStyle);
		Cell commentHeaderCell = row.createCell(COMMENT_COL);
		commentHeaderCell.setCellStyle(headerStyle);
		commentHeaderCell.setCellValue(COMMENT_COL_TEXT_TITLE);

		for (int i = FIRST_DATE_COL; i < MultiDocumentSpreadsheet.MAX_DOCUMENTS; i++) {
			sheet.setColumnWidth(i, DATE_COL_WIDTH*256);
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
		int[] annotationIndexes = new int[comparer.getNumSpdxDocs()];
		Annotation[][] annotations = new Annotation[comparer.getNumSpdxDocs()][];
		for (int i = 0; i < annotations.length; i++) {
			Cell headerCell = header.getCell(FIRST_DATE_COL+i);
			headerCell.setCellValue(docNames.get(i));
			Annotation[] docAnnotations = comparer.getSpdxDoc(i).getAnnotations().toArray(new Annotation[comparer.getSpdxDoc(i).getAnnotations().size()]);
			Arrays.sort(docAnnotations, annotationComparator);
			annotations[i] = docAnnotations;
			annotationIndexes[i] = 0;
		}
		while (!allAnnotationsExhausted(annotations, annotationIndexes)) {
			Row currentRow = this.addRow();
			Annotation nextAnnotation = getNextAnnotation(annotations, annotationIndexes);
			if (Objects.nonNull(nextAnnotation)) {
			    Cell annotatorCell = currentRow.createCell(ANNOTATOR_COL);
	            annotatorCell.setCellValue(nextAnnotation.getAnnotator());
	            Cell typeCell = currentRow.createCell(TYPE_COL);
	            typeCell.setCellValue(nextAnnotation.getAnnotationType().toString());
	            Cell commentCell = currentRow.createCell(COMMENT_COL);
	            commentCell.setCellValue(nextAnnotation.getComment());
	            for (int i = 0; i < annotations.length; i++) {
	                if (annotations[i].length > annotationIndexes[i]) {
	                    Annotation compareAnnotation = annotations[i][annotationIndexes[i]];
	                    if (annotationComparator.compare(nextAnnotation, compareAnnotation) == 0) {
	                        Cell dateCell = currentRow.createCell(FIRST_DATE_COL+i);
	                        dateCell.setCellValue(annotations[i][annotationIndexes[i]].getAnnotationDate());
	                        annotationIndexes[i]++;
	                    }
	                }
	            }
			}
		}
	}

	/**
	 * @param annotations
	 * @param annotationIndexes
	 * @return
	 */
	private Annotation getNextAnnotation(Annotation[][] annotations,
			int[] annotationIndexes) {
		Annotation retval = null;
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i].length > annotationIndexes[i]) {
				Annotation candidate = annotations[i][annotationIndexes[i]];
				if (retval == null || this.annotationComparator.compare(retval, candidate) > 0) {
					retval = candidate;
				}
			}
		}
		return retval;
	}

	/**
	 * @param annotations
	 * @param annotationIndexes
	 * @return
	 */
	private boolean allAnnotationsExhausted(Annotation[][] annotations,
			int[] annotationIndexes) {
		for (int i = 0; i < annotations.length; i++) {
			if (annotationIndexes[i] < annotations[i].length) {
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
