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

public class ComposerProvider extends ContentProvider {
	private final static String TAG = ComposerProvider.class.getSimpleName();

	public static String AUTHORITY = "de.evgeny.classico.composerprovider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/classico");

	// MIME types used for searching words or looking up a single definition
	public static final String COMPOSITIONS_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE +
	"/vnd.evgeny.classico";
	public static final String COMPOSITION_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE +
	"/vnd.evgeny.classico";
	public static final String SCORE_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE +
	"/vnd.evgeny.classico";

	private ClassicoDatabase mClassicoDatabase;

	// UriMatcher stuff
	private static final int SEARCH_COMPOSITIONS = 0;
	private static final int GET_COMPOSITION = 1;
	private static final int SEARCH_SUGGEST = 2;
	private static final int REFRESH_SHORTCUT = 3;
	private static final int GET_SCORE = 4;
	private static final UriMatcher sURIMatcher = buildUriMatcher();


	/**
	 * The columns we'll include in our search suggestions.  There are others that could be used
	 * to further customize the suggestions, see the docs in {@link SearchManager} for the details
	 * on additional columns that are supported.
	 */
	private static final String[] COLUMNS = {
		BaseColumns._ID,  // must include this column
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
		matcher.addURI(AUTHORITY, "classico", SEARCH_COMPOSITIONS);
		matcher.addURI(AUTHORITY, "classico/#", GET_COMPOSITION);
		// to get suggestions...
		matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
		matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
		// imslp table
		matcher.addURI(AUTHORITY, "classico/imslp/#", GET_SCORE);
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
            case SEARCH_COMPOSITIONS:
                return COMPOSITIONS_MIME_TYPE;
            case GET_COMPOSITION:
                return COMPOSITION_MIME_TYPE;
            case GET_SCORE:
                return SCORE_MIME_TYPE;
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
		
		mClassicoDatabase = new ClassicoDatabase();
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
		case SEARCH_COMPOSITIONS:
			if (selectionArgs == null) {
				throw new IllegalArgumentException(
						"selectionArgs must be provided for the Uri: " + uri);
			}
			return search(selectionArgs[0]);
		case GET_COMPOSITION:
			return getComposition(uri);
		case GET_SCORE:
			return getScore(uri);
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
		return mClassicoDatabase.getComposerMatches(query, columns);
	}

	private Cursor search(String query) {
		query = query.toLowerCase();
		String[] columns = new String[] {
				BaseColumns._ID,
				ClassicoDatabase.KEY_COMPOSER,
				ClassicoDatabase.KEY_COMPOSITION};

		//return mClassico.getComposerMatches(query, columns);
		return mClassicoDatabase.getComposerMatches(query, columns);
	}

	private Cursor getComposition(Uri uri) {
		String rowId = uri.getLastPathSegment();
		String[] columns = new String[] {
				ClassicoDatabase.KEY_COMPOSER,
				ClassicoDatabase.KEY_COMPOSITION,
				ClassicoDatabase.KEY_COMPOSITION_ID};

		return mClassicoDatabase.getComposer(rowId, columns);
	}
	
	private Cursor getScore(Uri uri) {
		String compId = uri.getLastPathSegment();
		final String[] columns = new String[]{"_id", "imslp", "meta"};
		final String selection = "comp_id=?";
		final String[] selectionArgs = new String[]{compId};
		
		return mClassicoDatabase.getCursor(
				ClassicoDatabase.SCORE_TABLE, columns, selection, selectionArgs);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}
}
