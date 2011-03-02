package de.evgeny.classico;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

public class CompositionActivity extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.composition);

		Uri uri = getIntent().getData();
		Cursor cursor = managedQuery(uri, null, null, null, null);

		if (cursor == null) {
			finish();
		} else {
			cursor.moveToFirst();

			TextView word = (TextView) findViewById(R.id.word);
			TextView definition = (TextView) findViewById(R.id.definition);

			int wIndex = cursor.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSITION);
			int dIndex = cursor.getColumnIndexOrThrow(ClassicoDatabase.KEY_COMPOSITION_ID);

			word.setText(cursor.getString(wIndex));
			definition.setText(cursor.getString(dIndex));
		}
	}
}
