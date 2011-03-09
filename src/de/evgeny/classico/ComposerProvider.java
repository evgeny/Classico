package de.evgeny.classico;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class ComposerProvider extends ContentProvider{
	private final static String TAG = ComposerProvider.class.getSimpleName();

	public static String AUTHORITY = "de.evgeny.classico.ComposerProvider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/classico");

	// MIME types used for searching words or looking up a single definition
	public static final String WORDS_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE +
	"/vnd.evgeny.classico";
	public static final String DEFINITION_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE +
	"/vnd.evgeny.classico";

	//private ClassicoDatabase mClassico;

	// UriMatcher stuff
	private static final int SEARCH_WORDS = 0;
	private static final int GET_WORD = 1;
	private static final int SEARCH_SUGGEST = 2;
	private static final int REFRESH_SHORTCUT = 3;
	private static final UriMatcher sURIMatcher = buildUriMatcher();


	/**
	 * The columns we'll include in our search suggestions.  There are others that could be used
	 * to further customize the suggestions, see the docs in {@link SearchManager} for the details
	 * on additional columns that are supported.
	 */
	private static final String[] COLUMNS = {
		"_id",  // must include this column
		SearchManager.SUGGEST_COLUMN_TEXT_1,
		SearchManager.SUGGEST_COLUMN_TEXT_2,
		SearchManager.SUGGEST_COLUMN_INTENT_DATA,
	};

	/**
	 * Sets up a uri matcher for search suggestion and shortcut refresh queries.
	 */
	private static UriMatcher buildUriMatcher() {
		UriMatcher matcher =  new UriMatcher(UriMatcher.NO_MATCH);
		// to get definitions...
		matcher.addURI(AUTHORITY, "classico", SEARCH_WORDS);
		matcher.addURI(AUTHORITY, "classico/#", GET_WORD);
		// to get suggestions...
		matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
		matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);

		return matcher;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

    @Override
    public String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case SEARCH_WORDS:
                return WORDS_MIME_TYPE;
            case GET_WORD:
                return DEFINITION_MIME_TYPE;
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            case REFRESH_SHORTCUT:
                return SearchManager.SHORTCUT_MIME_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onCreate() {
		Log.w(TAG, "onCreate" );
		//mClassico = new ClassicoDatabase(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// Use the UriMatcher to see what kind of query we have and format the db query accordingly
		switch (sURIMatcher.match(uri)) {
		case SEARCH_SUGGEST:
			if (selectionArgs == null) {
				throw new IllegalArgumentException(
						"selectionArgs must be provided for the Uri: " + uri);
			}
			return getSuggestions(selectionArgs[0]);
		case SEARCH_WORDS:
			if (selectionArgs == null) {
				throw new IllegalArgumentException(
						"selectionArgs must be provided for the Uri: " + uri);
			}
			return search(selectionArgs[0]);
		case GET_WORD:
			return getComposition(uri);
		default:
			throw new IllegalArgumentException("Unknown Uri: " + uri);
		}
	}

	private Cursor getSuggestions(String query) {
		query = query.toLowerCase();
		String[] columns = new String[] {
				BaseColumns._ID,
				ClassicoDatabase.KEY_COMPOSER,
				ClassicoDatabase.KEY_COMPOSITION,
				SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID};

		//return mClassico.getComposerMatches(query, columns);
		return ClassicoDatabase.getComposerMatches(query, columns);
	}

	private Cursor search(String query) {
		query = query.toLowerCase();
		String[] columns = new String[] {
				BaseColumns._ID,
				ClassicoDatabase.KEY_COMPOSER,
				ClassicoDatabase.KEY_COMPOSITION};

		//return mClassico.getComposerMatches(query, columns);
		return ClassicoDatabase.getComposerMatches(query, columns);
	}

	private Cursor getComposition(Uri uri) {
		String rowId = uri.getLastPathSegment();
		String[] columns = new String[] {
				ClassicoDatabase.KEY_COMPOSER,
				ClassicoDatabase.KEY_COMPOSITION,
				ClassicoDatabase.KEY_COMPOSITION_ID};

		//return mClassico.getComposer(rowId, columns);
		return ClassicoDatabase.getComposer(rowId, columns);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

}
