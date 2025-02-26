/**
 * SPDX-FileCopyrightText: Copyright (c) 2011 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.spdx.tools.compare;

import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.text.AttributedString;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Abstract class representing a workbook sheet used in storing structured data
 * @author Gary O'Neall
 */
public abstract class AbstractSheet {
	
	static Logger logger = LoggerFactory.getLogger(AbstractSheet.class);
	// Default style for cells
	static final String FONT_NAME = "Arial";
	protected static final short FONT_SIZE = (short)10*20;
	static final String CHECKBOX_FONT_NAME = "Wingdings 2";
	static final String CHECKBOX = "P";
	private static final short MAX_ROW_LINES = 10;
	protected CellStyle checkboxStyle;
	protected CellStyle dateStyle;
	protected CellStyle greenWrapped;
	protected CellStyle redWrapped;
	protected CellStyle yellowWrapped;

	protected Workbook workbook;
	protected Sheet sheet;
	protected int lastRowNum;
	protected int firstCellNum;
	protected int firstRowNum;

	/**
	 * @param workbook
	 * @param sheetName
	 */
	public AbstractSheet(Workbook workbook, String sheetName) {
		this.workbook = workbook;
		sheet = workbook.getSheet(sheetName);
		if (sheet != null) {
			firstRowNum = sheet.getFirstRowNum();
			Row firstRow = sheet.getRow(firstRowNum);
			if (firstRow == null) {
				firstCellNum = 1;
			} else {
				firstCellNum = firstRow.getFirstCellNum();
			}
			findLastRow();
		} else {
			firstRowNum = 0;
			lastRowNum = 0;
			firstCellNum = 0;
		}
		createStyles(workbook);
	}

	/**
	 * create the styles in the workbook
	 */
	private void createStyles(Workbook wb) {
		// create the styles
		this.checkboxStyle = wb.createCellStyle();
		this.checkboxStyle.setAlignment(HorizontalAlignment.CENTER);
		this.checkboxStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		this.checkboxStyle.setBorderBottom(BorderStyle.THIN);
		this.checkboxStyle.setBorderLeft(BorderStyle.THIN);
		this.checkboxStyle.setBorderRight(BorderStyle.THIN);
		this.checkboxStyle.setBorderTop(BorderStyle.THIN);
		Font checkboxFont = wb.createFont();
		checkboxFont.setFontHeight(FONT_SIZE);
		checkboxFont.setFontName(CHECKBOX_FONT_NAME);
		this.checkboxStyle.setFont(checkboxFont);

		this.dateStyle = wb.createCellStyle();
		DataFormat df = wb.createDataFormat();
		this.dateStyle.setDataFormat(df.getFormat("m/d/yy h:mm"));

		this.greenWrapped = createLeftWrapStyle(wb);
		this.greenWrapped.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
		this.greenWrapped.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		this.greenWrapped.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		this.yellowWrapped = createLeftWrapStyle(wb);
		this.yellowWrapped.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
		this.yellowWrapped.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		this.redWrapped = createLeftWrapStyle(wb);
		this.redWrapped.setFillForegroundColor(IndexedColors.RED.getIndex());
		this.redWrapped.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	}

	/**
	 *
	 */
	private void findLastRow() {
		boolean done = false;
		lastRowNum = firstRowNum + 1;
		try {
			while (!done) {
				Row row = sheet.getRow(lastRowNum);
				if (row == null || row.getCell(firstCellNum) == null ||
						row.getCell(firstCellNum).getStringCellValue() == null ||
						row.getCell(firstCellNum).getStringCellValue().isEmpty()) {
					lastRowNum--;
					done = true;
				} else {
					lastRowNum++;
				}
			}
		}
		catch (Exception ex) {
			// we just stop - stop counting rows at the first invalid row
		}
	}

	/**
	 * Add a new row to the end of the sheet
	 * @return new row
	 */
	protected Row addRow() {
		lastRowNum++;
		Row row = sheet.createRow(lastRowNum);
		return row;
	}

	/**
	 * Clears all data from the worksheet
	 */
	public void clear() {
		for (int i = lastRowNum; i > firstRowNum; i--) {
			Row row = sheet.getRow(i);
			sheet.removeRow(row);
		}
		lastRowNum = firstRowNum;
	}

	public int getFirstDataRow() {
		return this.firstRowNum + 1;
	}

	public int getNumDataRows() {
		return this.lastRowNum - (this.firstRowNum);
	}

	public Sheet getSheet() {
		return this.sheet;
	}

	public abstract String verify();

	public static CellStyle createHeaderStyle(Workbook wb) {
		CellStyle headerStyle = wb.createCellStyle();
		headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		Font headerFont = wb.createFont();
		headerFont.setFontName("Arial");
		headerFont.setFontHeight(FONT_SIZE);
		headerFont.setBold(true);
		headerStyle.setFont(headerFont);
		headerStyle.setAlignment(HorizontalAlignment.CENTER);
		headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		headerStyle.setWrapText(true);
		return headerStyle;
	}

	public static CellStyle createLeftWrapStyle(Workbook wb) {
		CellStyle wrapStyle = wb.createCellStyle();
		wrapStyle.setWrapText(true);
		wrapStyle.setAlignment(HorizontalAlignment.LEFT);
		wrapStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		return wrapStyle;
	}

	public static CellStyle createCenterStyle(Workbook wb) {
		CellStyle centerStyle = wb.createCellStyle();
		centerStyle.setWrapText(false);
		centerStyle.setAlignment(HorizontalAlignment.CENTER);
		return centerStyle;
	}

	/**
	 * resize the rows for a best fit.  Will not exceed maximum row height.
	 */
	public void resizeRows() {
		// header row
		// data rows
		int lastRow = this.getNumDataRows()+this.getFirstDataRow()-1;
		for (int i = 0; i <= lastRow; i++) {
			Row row = sheet.getRow(i);
			int lastCell = row.getLastCellNum();	// last cell + 1
			int maxNumLines = 1;
			for (int j = 0; j < lastCell; j++) {
				Cell cell = row.getCell(j);
				if (cell != null) {
					int cellLines = getNumWrappedLines(cell);
					if (cellLines > maxNumLines) {
						maxNumLines = cellLines;
					}
				}
			}
			if (maxNumLines > MAX_ROW_LINES) {
				maxNumLines = MAX_ROW_LINES;
			}
			if (maxNumLines > 1) {
				row.setHeight((short) (sheet.getDefaultRowHeight()*maxNumLines));
			}
		}
	}

	/**
	 * @param cell
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private int getNumWrappedLines(Cell cell) {
		if (cell.getCellTypeEnum() == CellType.STRING) {
			String val = cell.getStringCellValue();
			if (val == null || val.isEmpty()) {
				return 1;
			}
			CellStyle style = cell.getCellStyle();
			if (style == null || !style.getWrapText()) {
				return 1;
			}
			Font font = sheet.getWorkbook().getFontAt(style.getFontIndex());
			AttributedString astr = new AttributedString(val);
			java.awt.Font awtFont = new java.awt.Font(font.getFontName(), 0, font.getFontHeightInPoints());
			float cellWidth = sheet.getColumnWidth(cell.getColumnIndex())/ 256F * 5.5F;
			astr.addAttribute(TextAttribute.FONT, awtFont);
			FontRenderContext context = new FontRenderContext(null, true, true);
			java.awt.font.LineBreakMeasurer measurer = new java.awt.font.LineBreakMeasurer(astr.getIterator(), context);
			int pos = 0;
			int numLines = 0;
			while (measurer.getPosition() < val.length()) {
				pos = measurer.nextOffset(cellWidth);
				numLines++;
				measurer.setPosition(pos);
			}
			return numLines;
		} else {	// Not a string type
			return 1;
		}
	}
}
