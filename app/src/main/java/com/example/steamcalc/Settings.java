package com.example.steamcalc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class Settings extends AppCompatActivity {
    class CursorAdapter extends SimpleCursorAdapter {
        private final int layout_;
        String[] from;
        int[] to;

        ListView listViewCursAd;

        public CursorAdapter(Context context, int layout, Cursor
                c, String[] from, int[] to) {
            super(context, layout, c, from, to);
            layout_ = layout;
        }

        @Override
        public void bindView(View view, Context _context, Cursor
                cursor) {
            String data = cursor.getString(Math.max(cursor.getColumnIndex("Name"), 0));
            int id = cursor.getInt(Math.max(cursor.getColumnIndex("_id"), 0));
            TextView text =
                    view.findViewById(R.id.inputPercent);
            text.setText(data);
            Button butdel = view.findViewById(R.id.buttonDelete);
            listViewCursAd = listView;
            butdel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SQLiteDatabase db =
                            _context.openOrCreateDatabase("DBName", MODE_PRIVATE, null);
                    db.execSQL("DELETE FROM Commissions WHERE _id=" + id + "");
                    Cursor cursor = db.rawQuery("SELECT * FROM Commissions", null);
                    from = new String[]{"Name"};
                    to = new int[]{R.id.inputPercent};
                    CursorAdapter scAdapter = new
                            CursorAdapter(_context, R.layout.list_item_settings, cursor, from, to);
                    listViewCursAd.setAdapter(scAdapter);
                    db.close();
                    Toast.makeText(_context, "row deleted from the db id=" + id, Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public View newView(Context context, Cursor cursor,
                            ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            return inflater.inflate(layout_, parent, false);
        }
    }

    String[] currencies = {"USD", "EUR", "TRY", "KZT"};

    Integer i;
    String[] from;
    int[] to;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        listView = findViewById(R.id.listViewSettings);

        // Кнопка Навигации
        {
            Button button = findViewById(R.id.button_main_activity);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Settings.this,
                            MainActivity.class);
                    startActivity(intent);
                }
            });
        }

        // Выпадающий список
        {
            TextView selection = findViewById(R.id.selection);

            Spinner spinner = findViewById(R.id.spinner);
            // Создаем адаптер ArrayAdapter с помощью массива строк и стандартной разметки элемета spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencies);
            // Определяем разметку для использования при выборе элемента
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Применяем адаптер к элементу spinner
            spinner.setAdapter(adapter);

            AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                    // Получаем выбранный объект
                    String item = (String) parent.getItemAtPosition(position);
                    selection.setText(item);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            };
            spinner.setOnItemSelectedListener(itemSelectedListener);
        }

        // Работа с ListView комиссий
        {
            from = new String[]{"Name"};
            to = new int[]{R.id.inputPercent};
            Button btnadd = findViewById(R.id.buttonAdd);
            final EditText editadd = findViewById(R.id.input_percent);

            SQLiteDatabase db = openOrCreateDatabase("DBName", MODE_PRIVATE, null);

            db.execSQL("CREATE TABLE IF NOT EXISTS Commissions (_id INTEGER PRIMARY KEY AUTOINCREMENT, Name VARCHAR);");

            Cursor cursor = db.rawQuery("SELECT * FROM Commissions", null);

            i = cursor.getCount() + 1;

            if (cursor.getCount() > 0) {
                CursorAdapter scAdapter = new CursorAdapter(Settings.this, R.layout.list_item_settings, cursor, from, to);
                listView = findViewById(R.id.listViewSettings);
                listView.setAdapter(scAdapter);
            }

            db.close();

            btnadd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (editadd.getText().toString().length() == 0) {
                        Toast.makeText(findViewById(R.id.listViewSettings).getContext(), "Empty field!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (editadd.getText().toString().length() >= 3) {
                        Toast.makeText(findViewById(R.id.listViewSettings).getContext(), "Too big value for commission!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    SQLiteDatabase db = openOrCreateDatabase("DBName", MODE_PRIVATE, null);

                    Cursor cursor2 = db.rawQuery("SELECT * FROM Commissions", null);
                    i = cursor2.getCount() + 1;
                    cursor2.close();

                    //цикл для того, чтобы подбирать значения _id и не допускать повторения одинаковых значений (primary key как-никак)
                    for (int k = 1; k <= i; k++) {
                        Cursor cursor3 = db.rawQuery("SELECT * FROM Commissions WHERE _id=" + k + "", null);
                        if (cursor3.getCount() == 0) {
                            i = k;
                            break;
                        }
                        cursor3.close();
                    }


                    db.execSQL("INSERT INTO Commissions VALUES ('" + i + "','" + editadd.getText().toString() + "');");
                    Cursor cursor = db.rawQuery("SELECT * FROM Commissions", null);
                    CursorAdapter scAdapter = new CursorAdapter(Settings.this, R.layout.list_item_settings, cursor, from, to);

                    listView = findViewById(R.id.listViewSettings);
                    listView.setAdapter(scAdapter);
                    db.close();

                    Toast.makeText(findViewById(R.id.listViewSettings).getContext(), "a row added to the table", Toast.LENGTH_LONG).show();
                    getAdditionalCommission();
                }
            });

        }
    }

    public double getAdditionalCommission() {
        SQLiteDatabase db = openOrCreateDatabase("DBName", MODE_PRIVATE, null);

        Cursor cursor = db.rawQuery("SELECT * FROM Commissions", null);

        double currentAdditionCommission = 1;
        StringBuilder str = new StringBuilder();
        if (cursor.moveToFirst()) {
            do {
                currentAdditionCommission *= 1 - Double.parseDouble(cursor.getString(1)) / 100.0;
            } while (cursor.moveToNext());
        }

        Log.d("MyLog", "Current Addition Commission: " + currentAdditionCommission);

        cursor.close();
        return currentAdditionCommission;
    }
}