package models;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * A dynamically resizing two-dimensional Array whose fields can be adressed by
 * pairs of Strings as well as pairs of numbers.
 * 
 * Supports output to a csv-Table and can be read from a csv-Table.
 */
public class NamedFieldMatrix {

	private static final Logger LOGGER = Logger.getLogger(NamedFieldMatrix.class.getName());

	// the actual values in a 2-dimensional list
	private double[][] values;

	// current maximum of allocated elements
	private int colMax = 100;
	private int rowMax = 100;

	// current amount of columns and rows set, equals the next index to set
	private int colAmount = 0;
	private int rowAmount = 0;

	// maps rows of the table to a row name and vice versa
	private final Map<Integer, String> rowsToRowNames;
	private final Map<String, Integer> rowNamesToRows;

	// maps columns of the table to a column name no and vice versa
	private final Map<Integer, String> colsToColNames;
	private final Map<String, Integer> colNamesToCols;

	private char DELIMITER = ',';

	public NamedFieldMatrix() {
		this.values = new double[rowMax][];

		this.rowNamesToRows = new TreeMap<String, Integer>();
		this.colNamesToCols = new TreeMap<String, Integer>();

		this.rowsToRowNames = new TreeMap<Integer, String>();
		this.colsToColNames = new TreeMap<Integer, String>();
	}

	/**
	 * Adds value to the current value of the field designated by rowName and
	 * columnName. Returns the new value.
	 * 
	 * @param rowName
	 * @param columnName
	 * @param value
	 */
	public double addValue(String rowName, String columnName, double value) {
		int row = getOrAddRow(rowName);
		int col = getOrAddColumn(columnName);

		double previousValue = values[row][col];
		values[row][col] = previousValue + value;

		return values[row][col];
	}

	/**
	 * Set the value of the field designated by rowName and columnName. Add a
	 * new field if none exists for that combination.
	 * 
	 * @param rowName
	 *            The name of the field's row
	 * @param columnName
	 *            The name of the field's column
	 * @param value
	 *            The value to set
	 * @return The previous value of the field
	 */
	public double setValue(String rowName, String columnName, double value) {
		int row = getOrAddRow(rowName);
		int col = getOrAddColumn(columnName);

		double previousValue = values[row][col];
		values[row][col] = value;

		return previousValue;
	}

	/**
	 * Return the value of the field designated by rowName and column name.
	 * 
	 * @param rowName
	 *            The rowName of the field
	 * @param columnName
	 *            The column name of the field
	 * @return The value of the field designated by rowName and columnName or
	 *         null if the field does not exist
	 */
	public Double getValue(String rowName, String columnName) {
		Integer row = rowNamesToRows.get(rowName);
		Integer col = colNamesToCols.get(columnName);

		if (row == null || col == null) {
			return null;
		} else {
			return values[row][col];
		}
	}

	/**
	 * Get a copy of a row by it's name.
	 * 
	 * @param rowName
	 *            The name of the row.
	 * @return A copy of the row fit in size to the amount of columns set.
	 * @throws IllegalArgumentException
	 *             If there is no row with that name.
	 */
	public double[] getRow(String rowName) throws IllegalArgumentException {
		Integer row = rowNamesToRows.get(rowName);
		if (row == null) {
			throw new IllegalArgumentException("No row for name: " + rowName);
		} else {
			return getRow(row);
		}
	}

	/**
	 * Get a copy of a row by it's index.
	 * 
	 * @param row
	 *            The index of the row.
	 * @return A copy of the row fit in size to the amount of columns set.
	 * @throws IllegalArgumentException
	 *             If there is no row with that index.
	 */
	public double[] getRow(int row) throws IllegalArgumentException {
		if (row >= rowAmount) {
			throw new IllegalArgumentException("Row not set: " + row);
		}
		return Arrays.copyOf(values[row], colAmount);
	}

	/**
	 * Gets a column by it's name.
	 * 
	 * @param columnName
	 *            The name of the column.
	 * @return a new array with all values in the specified column.
	 * @throws IllegalArgumentException
	 *             if no column with the specified name exists.
	 */
	public double[] getColumn(String columnName) throws IllegalArgumentException {
		// get col number and fail if none exists
		Integer col = colNamesToCols.get(columnName);
		if (col == null) {
			throw new IllegalArgumentException("No column for name: " + columnName);
		}
		return getColumn(col);
	}

	/**
	 * Gets a column by it's index.
	 * 
	 * @param col
	 *            The index of the column.
	 * @return a new array with all values in the specified column.
	 * @throws IllegalArgumentException
	 *             if the column is not set
	 */
	public double[] getColumn(int col) throws IllegalArgumentException {
		if (col >= colAmount) {
			throw new IllegalArgumentException("Col not set: " + col);
		}
		// simply copy the column
		double[] result = new double[rowAmount];
		for (int i = 0; i < rowAmount; i++) {
			result[i] = values[i][col];
		}
		return result;
	}

	/**
	 * Gets the number of rows currently set.
	 */
	public int getRowAmount() {
		return rowAmount;
	}

	/**
	 * Gets the number of columns currently set.
	 */
	public int getColumnsAmount() {
		return colAmount;
	}

	/**
	 * Format the table's header as csv.
	 * 
	 * @return The comma separated values in the first header row.
	 */
	public String csvHeader() {
		StringBuilder sb = new StringBuilder();
		// the first header field is empty
		sb.append(DELIMITER);
		for (int col = 0; col < colAmount; col++) {
			sb.append(colsToColNames.get(col));
			sb.append(DELIMITER);
		}
		sb.setLength(sb.length() - 1);
		sb.append('\n');
		return sb.toString();
	}

	/**
	 * Returns the row as a csv representation.
	 * 
	 * @param row
	 *            The row to format as csv.
	 * @return A csv representation of the row
	 * @throws IllegalArgumentException
	 *             If row doesn't mach a row in the table.
	 */
	public String csvLine(int row) throws IllegalArgumentException {
		if (row >= rowAmount) {
			throw new IllegalArgumentException("No row for index: " + row);
		}
		StringBuilder sb = new StringBuilder();
		// write the row header
		sb.append(rowsToRowNames.get(row));
		sb.append(DELIMITER);
		// write values
		for (int col = 0; col < colAmount; col++) {
			// Write only non-zero values
			if (values[row][col] != 0) {
				sb.append(values[row][col]);
			}
			sb.append(DELIMITER);
		}
		sb.setLength(sb.length() - 1);
		sb.append('\n');
		return sb.toString();
	}

	// get the current row if it exists or add a new one: i.e. note it's name in
	// the mappings and make sure that it is backed by an appropriately sized
	// array in values[newRowNo]
	private int getOrAddRow(String rowName) {
		Integer row = rowNamesToRows.get(rowName);
		// If the row is new, add it
		if (row == null) {
			if (rowAmount >= rowMax - 1) {
				yResize();
			}
			row = rowAmount;
			values[row] = new double[colMax];
			rowNamesToRows.put(rowName, row);
			rowsToRowNames.put(row, rowName);
			rowAmount += 1;
		}
		return row;
	}

	// just add the column and make sure that it is accessible via the column
	// name, resize columns if neccessary
	private int getOrAddColumn(String columnName) {
		Integer col = colNamesToCols.get(columnName);
		// If the column is new, add it
		if (col == null) {
			if (colAmount >= colMax) {
				xResize();
			}
			col = colAmount;
			colNamesToCols.put(columnName, col);
			colsToColNames.put(col, columnName);
			colAmount += 1;
		}
		return col;
	}

	// Increase the amount of allocated rows
	private void yResize() {
		if (rowMax < 100000) {
			rowMax *= 2;
		} else {
			rowMax += 50000;
		}
		rowMax *= 2;
		values = Arrays.copyOf(values, rowMax);
		LOGGER.info("REISZE Y: " + rowMax);
	}

	// Increase the amount of allocated columns
	private void xResize() {
		if (colMax < 100000) {
			colMax *= 2;
		} else {
			colMax += 50000;
		}
		for (int i = 0; i < rowAmount; i++) {
			double[] row = values[i];
			if (row != null) {
				values[i] = Arrays.copyOf(row, colMax);
			}
		}
		LOGGER.info("REISZE X: " + colMax);
	}

}
