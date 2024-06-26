package uk.co.certait.test;
/* ====================================================================
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==================================================================== */

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.examples.ss.html.HtmlHelper; 
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.examples.ss.html.HSSFHtmlHelper; 
import org.apache.poi.examples.ss.html.XSSFHtmlHelper;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
 
import org.apache.poi.ss.format.CellFormat;
import org.apache.poi.ss.format.CellFormatResult;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.IOUtils; 
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * This example shows how to display a spreadsheet in HTML using the classes for
 * spreadsheet display.
 */
public class ExcelToHtmlConverter {
	private final Workbook wb;
	private final Appendable output;
	private boolean completeHTML;
	private Formatter out;
	private boolean gotBounds;
	private int firstColumn;
	private int endColumn; 
	private HtmlHelper helper;

	private static final String DEFAULTS_CLASS = "excelDefaults";
	private static final String COL_HEAD_CLASS = "colHeader";
	private static final String ROW_HEAD_CLASS = "rowHeader";

	private static final Map<HorizontalAlignment, String> HALIGN = mapFor(HorizontalAlignment.LEFT, "left",
			HorizontalAlignment.CENTER, "center", HorizontalAlignment.RIGHT, "right", HorizontalAlignment.FILL, "left",
			HorizontalAlignment.JUSTIFY, "left", HorizontalAlignment.CENTER_SELECTION, "center");

	private static final Map<VerticalAlignment, String> VALIGN = mapFor(VerticalAlignment.BOTTOM, "bottom",
			VerticalAlignment.CENTER, "middle", VerticalAlignment.TOP, "top");

	private static final Map<BorderStyle, String> BORDER = mapFor(BorderStyle.DASH_DOT, "dashed 1pt",
			BorderStyle.DASH_DOT_DOT, "dashed 1pt", BorderStyle.DASHED, "dashed 1pt", BorderStyle.DOTTED, "dotted 1pt",
			BorderStyle.DOUBLE, "double 3pt", BorderStyle.HAIR, "solid 1px", BorderStyle.MEDIUM, "solid 2pt",
			BorderStyle.MEDIUM_DASH_DOT, "dashed 2pt", BorderStyle.MEDIUM_DASH_DOT_DOT, "dashed 2pt",
			BorderStyle.MEDIUM_DASHED, "dashed 2pt", BorderStyle.NONE, "none", BorderStyle.SLANTED_DASH_DOT,
			"dashed 2pt", BorderStyle.THICK, "solid 3pt", BorderStyle.THIN, "dashed 1pt");

	@SuppressWarnings({ "unchecked" })
	private static <K, V> Map<K, V> mapFor(Object... mapping) {
		Map<K, V> map = new HashMap<K, V>();
		for (int i = 0; i < mapping.length; i += 2) {
			map.put((K) mapping[i], (V) mapping[i + 1]);
		}
		return map;
	}

	/**
	 * Creates a new converter to HTML for the given workbook.
	 *
	 * @param wb
	 *            The workbook.
	 * @param output
	 *            Where the HTML output will be written.
	 *
	 * @return An object for converting the workbook to HTML.
	 */
	public static ExcelToHtmlConverter create(Workbook wb, Appendable output) {
		return new ExcelToHtmlConverter(wb, output);
	}

	/**
	 * Creates a new converter to HTML for the given workbook. If the path ends
	 * with "<tt>.xlsx</tt>" an {@link XSSFWorkbook} will be used; otherwise
	 * this will use an {@link HSSFWorkbook}.
	 *
	 * @param path
	 *            The file that has the workbook.
	 * @param output
	 *            Where the HTML output will be written.
	 *
	 * @return An object for converting the workbook to HTML.
	 */
	public static ExcelToHtmlConverter create(String path, Appendable output) throws IOException {
		return create(new FileInputStream(path), output);
	}

	/**
	 * Creates a new converter to HTML for the given workbook. This attempts to
	 * detect whether the input is XML (so it should create an
	 * {@link XSSFWorkbook} or not (so it should create an
	 * {@link HSSFWorkbook}).
	 *
	 * @param in
	 *            The input stream that has the workbook.
	 * @param output
	 *            Where the HTML output will be written.
	 *
	 * @return An object for converting the workbook to HTML.
	 */
	public static ExcelToHtmlConverter create(InputStream in, Appendable output) throws IOException {
		try {
			Workbook wb = WorkbookFactory.create(in);
			return create(wb, output);
		} catch (EncryptedDocumentException e) {
			throw new IllegalArgumentException("Cannot create workbook from stream", e);
		}
	}

	private ExcelToHtmlConverter(Workbook wb, Appendable output) {
		if (wb == null) {
			throw new NullPointerException("wb");
		}
		if (output == null) {
			throw new NullPointerException("output");
		}
		this.wb = wb;
		this.output = output;
		setupColorMap();
	}

	private void setupColorMap() {
		if (wb instanceof HSSFWorkbook) {
			helper = new HSSFHtmlHelper((HSSFWorkbook) wb);
		} else if (wb instanceof XSSFWorkbook) {
			helper = new XSSFHtmlHelper();
		} else {
			throw new IllegalArgumentException("unknown workbook type: " + wb.getClass().getSimpleName());
		}
	}

	/**
	 * Run this class as a program
	 *
	 * @param args
	 *            The command line arguments.
	 *
	 * @throws Exception
	 *             Exception we don't recover from.
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("usage: ToHtml inputWorkbook outputHtmlFile");
			return;
		}

		ExcelToHtmlConverter toHtml = create(args[0], new PrintWriter(new FileWriter(args[1])));
		toHtml.setCompleteHTML(true);
		toHtml.printPage();
	}

	public void setCompleteHTML(boolean completeHTML) {
		this.completeHTML = completeHTML;
	}

	public void printPage() throws IOException {
		try {
			ensureOut();
			if (completeHTML) {
				out.format("<?xml version=\"1.0\" encoding=\"iso-8859-1\" ?>%n");
				out.format("<html>%n");
				out.format("<head>%n");
				printInlineStyle();
				out.format("</head>%n");
				out.format("<body>%n");
			}

			print();

			if (completeHTML) {
				out.format("</body>%n");
				out.format("</html>%n");
			}
		} finally {
			IOUtils.closeQuietly(out);
			if (output instanceof Closeable) {
				IOUtils.closeQuietly((Closeable) output);
			}
		}
	}
	public void printPageFromSheet(Sheet sheet) throws IOException {
		try {
			ensureOut();
			if (completeHTML) {
				out.format("<?xml version=\"1.0\" encoding=\"UTF8\" ?>%n");
				out.format("<html>%n");
				out.format("<head>%n");
				printInlineStyle();
				out.format("</head>%n");
				out.format("<body>%n");
			}

			printSheet(sheet );

			if (completeHTML) {
				out.format("</body>%n");
				out.format("</html>%n");
			}
		} finally {
			IOUtils.closeQuietly(out);
			if (output instanceof Closeable) {
				IOUtils.closeQuietly((Closeable) output);
			}
		}
	}

	public void print() {
		if (!completeHTML) {
			printInlineStyle();
		}
		printSheets();
	}

	private void printInlineStyle() {
		// out.format("<link href=\"excelStyle.css\" rel=\"stylesheet\"
		// type=\"text/css\">%n");
		out.format("<style type=\"text/css\">%n");
		printStyles();
		out.format("</style>%n");
	}

	private void ensureOut() {
		if (out == null) {
			out = new Formatter(output);
		}
	}

	public void printStyles() {
		ensureOut();

		// First, copy the base css
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/excelStyle.css")));
			String line;
			while ((line = in.readLine()) != null) {
				out.format("%s%n", line);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Reading standard css", e);
		} finally {
			IOUtils.closeQuietly(in);
		}

		// now add css for each used style
		Set<CellStyle> seen = new HashSet<CellStyle>();
		for (int i = 0; i < wb.getNumberOfSheets(); i++) {
			Sheet sheet = wb.getSheetAt(i);
			Iterator<Row> rows = sheet.rowIterator();
			while (rows.hasNext()) {
				Row row = rows.next();
				for (Cell cell : row) {
					CellStyle style = cell.getCellStyle();
					if (!seen.contains(style)) {
						printStyle(style);
						seen.add(style);
					}
				}
			}
		}
	}

	private void printStyle(CellStyle style) {
		out.format(".%s .%s {%n", DEFAULTS_CLASS, styleName(style));
		styleContents(style);
		out.format("}%n");
	}

	private void styleContents(CellStyle style) {
		styleOut("text-align", style.getAlignment(), HALIGN);
		styleOut("vertical-align", style.getVerticalAlignment(), VALIGN);
		fontStyle(style);
		borderStyles(style);
		helper.colorStyles(style, out);
	}

	private void borderStyles(CellStyle style) {
		styleOut("border-left", style.getBorderLeft(), BORDER);
		styleOut("border-right", style.getBorderRight(), BORDER);
		styleOut("border-top", style.getBorderTop(), BORDER);
		styleOut("border-bottom", style.getBorderBottom(), BORDER);
	}

	private void fontStyle(CellStyle style) {
		Font font = wb.getFontAt(style.getFontIndex());

		if (font.getBold()) {
			out.format("  font-weight: bold;%n");
		}
		if (font.getItalic()) {
			out.format("  font-style: italic;%n");
		}

		int fontheight = font.getFontHeightInPoints();
		if (fontheight == 9) {
			// fix for stupid ol Windows
			fontheight = 10;
		}
		out.format("  font-size: %dpt;%n", fontheight);

		// Font color is handled with the other colors
	}

	private String styleName(CellStyle style) {
		if (style == null) {
			style = wb.getCellStyleAt((short) 0);
		}
		StringBuilder sb = new StringBuilder();
		Formatter fmt = new Formatter(sb);
		try {
			fmt.format("style_%02x", style.getIndex());
			return fmt.toString();
		} finally {
			fmt.close();
		}
	}

	private <K> void styleOut(String attr, K key, Map<K, String> mapping) {
		String value = mapping.get(key);
		if (value != null) {
			out.format("  %s: %s;%n", attr, value);
		}
	}

	@SuppressWarnings("deprecation")
	private static CellType ultimateCellType(Cell c) {
		CellType type = c.getCellType();
		if (CellType.FORMULA == type) {
			type = c.getCachedFormulaResultType();
		}
		return type;
	}

	private void printSheets() {
		ensureOut();
		Sheet sheet = wb.getSheetAt(0);
		printSheet(sheet);
	}

	public void printSheet(Sheet sheet) {		 
		ensureOut();
		out.format("<table class=%s>%n", "\""+DEFAULTS_CLASS+"\"");
		printCols(sheet);
		printSheetContent(sheet);
		out.format("</table>%n");
	}

	private void printCols(Sheet sheet) {
		out.format("<col/>%n");
		ensureColumnBounds(sheet);
		for (int i = firstColumn; i < endColumn; i++) {
			if(sheet.isColumnHidden(i)) {
				continue;
			}
			out.format("<col/>%n");
		}
	}

	private void ensureColumnBounds(Sheet sheet) {
		if (gotBounds) {
			return;
		}

		Iterator<Row> iter = sheet.rowIterator();
		firstColumn = (iter.hasNext() ? Integer.MAX_VALUE : 0);
		endColumn = 0;
		while (iter.hasNext()) {
			Row row = iter.next();
			if(row.getZeroHeight()) {
				continue ;
			}
			short firstCell = row.getFirstCellNum();
			if (firstCell >= 0) {
				firstColumn = Math.min(firstColumn, firstCell);
				endColumn = Math.max(endColumn, row.getLastCellNum());
			}
		}
		
		gotBounds = true;
	}

	private void printColumnHeads() {
		out.format("<thead>%n");
		out.format("  <tr class=%s>%n", "\""+COL_HEAD_CLASS+"\"");
		out.format("    <th class=%s>&#x25CA;</th>%n", "\""+COL_HEAD_CLASS+"\"");
		// noinspection UnusedDeclaration
		StringBuilder colName = new StringBuilder();
		for (int i = firstColumn; i < endColumn; i++) {
			colName.setLength(0);
			int cnum = i;
			do {
				colName.insert(0, (char) ('A' + cnum % 26));
				cnum /= 26;
			} while (cnum > 0);
			out.format("    <th class=%s>%s</th>%n", "\""+COL_HEAD_CLASS+"\"", colName);
		}
		out.format("  </tr>%n");
		out.format("</thead>%n");
	}

	private void printSheetContent(Sheet sheet) {
		printColumnHeads();
		List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
		System.out.println("-----------------------------");
		List<String> mergeAddress = new ArrayList<>();
		mergedRegions.forEach(unit ->{
			String address = unit.formatAsString();
			System.out.println(address);
			String tmp = address.substring(0, address.indexOf(":"));			
			mergeAddress.add(tmp);
		});
		
		System.out.println("-----------------------------");
		out.format("<tbody>%n");
		Iterator<Row> rows = sheet.rowIterator();
		while (rows.hasNext()) {
			Row row = rows.next();

			out.format("  <tr>%n");
			out.format("    <td class=\"%s\">%d</td>%n", ROW_HEAD_CLASS, row.getRowNum() + 1);
			for (int i = firstColumn; i < endColumn; i++) {
				if(sheet.isColumnHidden(i)) {
					continue; 
				}
				String content = "&nbsp;";
				String attrs = "";
				CellStyle style = null;
				if (i >= row.getFirstCellNum() && i < row.getLastCellNum()) {
					Cell cell = row.getCell(i);
					if (cell != null) {
						 CellAddress address = cell.getAddress() ; 
						 
						if(mergeAddress.contains(address.formatAsString())) {
							System.out.println(address.formatAsString());
							
						}
						
						style = cell.getCellStyle();
						attrs = tagStyle(cell, style);
						// Set the value that is rendered for the cell
						// also applies the format
						CellFormat cf = CellFormat.getInstance(style.getDataFormatString());
						CellFormatResult result = cf.apply(cell);
						content = result.text;
						if (content.equals("")) {
							content = "&nbsp;";
						}
					}
				}
				out.format("    <td class=\"%s\" %s>%s</td>%n", styleName(style), attrs, content);
			}
			out.format("  </tr>%n");
		}
		out.format("</tbody>%n");
	}

	private String tagStyle(Cell cell, CellStyle style) {
		if (HorizontalAlignment.GENERAL == style.getAlignment()) {
			switch (ultimateCellType(cell)) {
			case STRING:
				return "style=\"text-align: left;\"";
			case BOOLEAN:
			case ERROR:
				return "style=\"text-align: center;\"";
			case NUMERIC:
			default:
				// "right" is the default
				break;
			}
		}
		return "";
	}
}