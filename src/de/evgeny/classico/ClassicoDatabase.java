package de.evgeny.classico;

import java.util.HashMap;

import android.app.SearchManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

public class ClassicoDatabase {

	private final static String TAG = ClassicoDatabase.class.getSimpleName();

	public static final String KEY_COMPOSER = SearchManager.SUGGEST_COLUMN_TEXT_1;
	public static final String KEY_COMPOSITION = SearchManager.SUGGEST_COLUMN_TEXT_2;
	public static final String KEY_COMPOSITION_ID = "comp_id";

	public static final String DATABASE_NAME = "/sdcard/classico.db";
	private static final String FTS_VIRTUAL_TABLE = "FTSclassico";
	public static final String SCORE_TABLE = "scores";

	private static final HashMap<String,String> mColumnMap = buildColumnMap();
	private SQLiteDatabase mClassicoDatabase;

	public ClassicoDatabase() {
		mClassicoDatabase = 
			SQLiteDatabase.openDatabase(DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
	}
	
	public void close() {
		mClassicoDatabase.close();
	}
	
	/**
	 * Builds a map for all columns that may be requested, which will be given to the 
	 * SQLiteQueryBuilder. This is a good way to define aliases for column names, but must include 
	 * all columns, even if the value is the key. This allows the ContentProvider to request
	 * columns w/o the need to know real column names and create the alias itself.
	 */
	private static HashMap<String,String> buildColumnMap() {
		HashMap<String,String> map = new HashMap<String,String>();
		map.put(KEY_COMPOSER, KEY_COMPOSER);
		map.put(KEY_COMPOSITION, KEY_COMPOSITION);
		map.put(KEY_COMPOSITION_ID, KEY_COMPOSITION_ID);
		map.put(BaseColumns._ID, "rowid AS " +
				BaseColumns._ID);
		map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS " +
				SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
		return map;
	}
	

	public Cursor getCursor(String table, String[] columns, String selection, String[] selectionArgs) {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(table);
		Cursor cursor = builder.query(mClassicoDatabase,
				columns, selection, selectionArgs, null, null, null);
				//columns, selection, selectionArgs, null, null, KEY_COMPOSITION_ID + " ASC LIMIT 20");				
		if (cursor == null) {
			return null;
		} else if (!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}
		return cursor;
	}
	
	/**
	 * Returns a Cursor positioned at the word specified by rowId
	 *
	 * @param rowId id of word to retrieve
	 * @param columns The columns to include, if null then all are included
	 * @return Cursor positioned to matching word, or null if not found.
	 */
	public Cursor getComposer(String rowId, String[] columns) {
		String selection = "rowid = ?";
		String[] selectionArgs = new String[] {rowId};

		return query(selection, selectionArgs, columns);
	}
	
	public Cursor getScores(String compId, String[] columns) {
		String selection = "comp_id = ?";
		String[] selectionArgs = new String[] {compId};

		return query(selection, selectionArgs, columns);
	}

	/**
	 * Returns a Cursor over all words that match the given query
	 *
	 * @param query The string to search for
	 * @param columns The columns to include, if null then all are included
	 * @return Cursor over all words that match, or null if none found.
	 */
	public Cursor getComposerMatches(String query, String[] columns) {
		String selection = FTS_VIRTUAL_TABLE + " MATCH ?";
		String[] selectionArgs = new String[] {query+"*"};

		return query(selection, selectionArgs, columns);

		/* This builds a query that looks like:
		 *     SELECT <columns> FROM <table> WHERE <KEY_WORD> MATCH 'query*'
		 * which is an FTS3 search for the query text (plus a wildcard) inside the word column.
		 *
		 * - "rowid" is the unique id for all rows but we need this value for the "_id" column in
		 *    order for the Adapters to work, so the columns need to make "_id" an alias for "rowid"
		 * - "rowid" also needs to be used by the SUGGEST_COLUMN_INTENT_DATA alias in order
		 *   for suggestions to carry the proper intent data.
		 *   These aliases are defined in the DictionaryProvider when queries are made.
		 * - This can be revised to also search the definition text with FTS3 by changing
		 *   the selection clause to use FTS_VIRTUAL_TABLE instead of KEY_WORD (to search across
		 *   the entire table, but sorting the relevance could be difficult.
		 */
	}

	/**
	 * Performs a database query.
	 * @param selection The selection clause
	 * @param selectionArgs Selection arguments for "?" components in the selection
	 * @param columns The columns to return
	 * @return A Cursor over all rows matching the query
	 */
	private Cursor query(String selection, String[] selectionArgs, String[] columns) {
		/* The SQLiteBuilder provides a map for all possible columns requested to
		 * actual columns in the database, creating a simple column alias mechanism
		 * by which the ContentProvider does not need to know the real column names
		 */
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(FTS_VIRTUAL_TABLE);
		builder.setProjectionMap(mColumnMap);
		
		Cursor cursor = builder.query(mClassicoDatabase,
				columns, selection, selectionArgs, null, null, null);
				//columns, selection, selectionArgs, null, null, KEY_COMPOSITION_ID + " ASC LIMIT 20");				
		if (cursor == null) {
			return null;
		} else if (!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}
		return cursor;
	}
}
