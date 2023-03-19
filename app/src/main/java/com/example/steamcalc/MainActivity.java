package com.example.steamcalc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    class CursorAdapterMain extends SimpleCursorAdapter {
        private final int layout_;
        String[] from;
        int[] to;

        ListView listViewCursAd;

        public CursorAdapterMain(Context context, int layout, Cursor
                c, String[] from, int[] to) {
            super(context, layout, c, from, to);
            layout_ = layout;
        }

        @Override
        public void bindView(View view, Context _context, Cursor cursor) {

            String dataBuy = cursor.getString(Math.max(cursor.getColumnIndex("Buy"), 0));
            String dataSell = cursor.getString(Math.max(cursor.getColumnIndex("Sell"), 0));
            int id = cursor.getInt(Math.max(cursor.getColumnIndex("_id"), 0));

            TextView textBuy = view.findViewById(R.id.input_buy_price);
            TextView textSell = view.findViewById(R.id.input_sell_price);
            textBuy.setText(dataBuy);
            textSell.setText(dataSell);

            Button butdel = view.findViewById(R.id.button_delete_main);

            listViewCursAd = listView;

            butdel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SQLiteDatabase db = _context.openOrCreateDatabase("DBName", MODE_PRIVATE, null);

                    db.execSQL("DELETE FROM MarketItems WHERE _id=" + id + "");

                    Cursor cursor = db.rawQuery("SELECT * FROM MarketItems", null);

                    from = new String[]{"Buy", "Sell"};
                    to = new int[]{R.id.input_buy_price, R.id.input_sell_price};

                    CursorAdapterMain scAdapter = new
                            CursorAdapterMain(_context, R.layout.list_item_main, cursor, from, to);

                    listViewCursAd.setAdapter(scAdapter);

                    db.close();
                    Toast.makeText(_context, "row deleted from the db id=" + id, Toast.LENGTH_LONG).show();

                    updateResult();
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

    private Document doc;
    private Thread secondThread;
    private Runnable runnable;

    private Elements data = null;
    private boolean isDataReady = false;

    private double currentRate = 1;
    private double currentAllCommissions = 1;

    Integer i;
    ListView listView;

    String[] from;
    int[] to;

    private static String dbName = "DBName";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Кнопка перехода в настройки
        {
            Button button = findViewById(R.id.button_settings);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this,
                            Settings.class);
                    startActivity(intent);
                }
            });
        }

        // Кнопка показа Replenishment calculator
        {
            Button button = findViewById(R.id.button_show_repl_calc);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LinearLayout ll = findViewById(R.id.layout_repl_calc);
                    if (ll.getVisibility() == View.VISIBLE) {
                        ll.setVisibility(View.GONE);
                    } else {
                        ll.setVisibility(View.VISIBLE);
                    }

                    if (button.getText().toString().equals("hide")) {
                        button.setText("show");
                    } else {
                        button.setText("hide");
                    }
                }
            });
        }

        // Кнопка показа Steam calculator
        {
            Button button = findViewById(R.id.button_show_steam_calc);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LinearLayout ll = findViewById(R.id.layout_steam_calc);
                    if (ll.getVisibility() == View.VISIBLE) {
                        ll.setVisibility(View.GONE);
                    } else {
                        ll.setVisibility(View.VISIBLE);
                    }


                    if (button.getText().toString().equals("hide")) {
                        button.setText("show");
                    } else {
                        button.setText("hide");
                    }
                }
            });
        }

        // Replenishment calculator
        {
            EditText myTextBox = (EditText) findViewById(R.id.input_money_from);

            myTextBox.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
//                    Log.d("MyLog", "" + s.toString());
                    try {
                        TextView moneyFromRub = (TextView) findViewById(R.id.textview_money_from_RUB);

                        double money = myRound(Double.parseDouble((String) s.toString()), 2);
                        double moneyRate = myRound(money * currentRate, 2);
                        moneyFromRub.setText(String.valueOf(moneyRate));

                        TextView moneyTo = (TextView) findViewById(R.id.textview_money_to);
                        TextView moneyToRub = (TextView) findViewById(R.id.textview_money_to_RUB);

                        currentAllCommissions = Settings.steamCommissions * Settings.additionalCommissions;

                        moneyTo.setText(String.valueOf(myRound(money * currentAllCommissions, 2)));
                        moneyToRub.setText(String.valueOf(myRound(moneyRate * currentAllCommissions, 2)));

                    } catch (Exception ignored) {

                    }
                }

                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {
                }
            });
        }

        initThread();

        // listView
        {
            listView = findViewById(R.id.listViewMain);

            from = new String[]{"Buy", "Sell"};
            to = new int[]{R.id.input_buy_price, R.id.input_sell_price};

            Button buttonAdd = findViewById(R.id.button_add_main);

            final EditText getBuyPrice = findViewById(R.id.input_source_price);
            final EditText getSellPrice = findViewById(R.id.input_steam_market_price);

            SQLiteDatabase db = openOrCreateDatabase(dbName, MODE_PRIVATE, null);


            db.execSQL("CREATE TABLE IF NOT EXISTS MarketItems (_id INTEGER PRIMARY KEY AUTOINCREMENT, Buy FLOAT, Sell FLOAT);");

            Cursor cursor = db.rawQuery("SELECT * FROM MarketItems", null);

            i = cursor.getCount() + 1;

            if (cursor.getCount() > 0) {
                CursorAdapterMain scAdapter = new CursorAdapterMain(MainActivity.this, R.layout.list_item_main, cursor, from, to);
                listView = findViewById(R.id.listViewMain);
                listView.setAdapter(scAdapter);
            }

            db.close();

            buttonAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (getBuyPrice.getText().toString().length() == 0 || getSellPrice.getText().toString().length() == 0) {
                        Toast.makeText(findViewById(R.id.listViewMain).getContext(), "Empty field!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (Double.parseDouble(getBuyPrice.getText().toString()) <= 0) {
                        Toast.makeText(findViewById(R.id.listViewMain).getContext(), "Incorrect value for Source price!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (Double.parseDouble(getSellPrice.getText().toString()) <= 0) {
                        Toast.makeText(findViewById(R.id.listViewMain).getContext(), "Incorrect value for Steam price!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    listView = findViewById(R.id.listViewMain);


                    if (listView.getCount() >= 3) {
                        Toast.makeText(findViewById(R.id.listViewMain).getContext(), "No more than three entries are available!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    SQLiteDatabase db = openOrCreateDatabase("DBName", MODE_PRIVATE, null);

                    Cursor cursor2 = db.rawQuery("SELECT * FROM MarketItems", null);
                    i = cursor2.getCount() + 1;
                    cursor2.close();

                    //цикл для того, чтобы подбирать значения _id и не допускать повторения одинаковых значений (primary key как-никак)
                    for (int k = 1; k <= i; k++) {
                        Cursor cursor3 = db.rawQuery("SELECT * FROM MarketItems WHERE _id=" + k + "", null);
                        if (cursor3.getCount() == 0) {
                            i = k;
                            break;
                        }
                        cursor3.close();
                    }

                    db.execSQL("INSERT INTO MarketItems VALUES ('" + i + "','" + getBuyPrice.getText().toString() + "','" + getSellPrice.getText().toString() + "');");

                    Cursor cursor = db.rawQuery("SELECT * FROM MarketItems", null);

                    CursorAdapterMain scAdapter = new CursorAdapterMain(MainActivity.this, R.layout.list_item_main, cursor, from, to);

                    listView.setAdapter(scAdapter);
                    db.close();

                    Toast.makeText(findViewById(R.id.listViewMain).getContext(), "A row added to the table", Toast.LENGTH_LONG).show();

                    updateResult();


                }
            });

        }

        while (!isDataReady) {
//            Log.d("MyLog", "Waiting for website data");
        }

        setCurrencyInfo(Settings.specifiedCurrency);

        updateResult();
    }

    public void updateResult() {
        SQLiteDatabase db = openOrCreateDatabase("DBName", MODE_PRIVATE, null);

        Cursor cursor = db.rawQuery("SELECT * FROM MarketItems", null);

        double buySum = 0;
        double sellSum = 0;
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            buySum += cursor.getDouble(1);
            sellSum += cursor.getDouble(2);
            cursor.moveToNext();
        }

        TextView textViewDiffTRY = findViewById(R.id.textView_DiffTRY);
        TextView textViewDiffRUB = findViewById(R.id.textView_DiffRUB);

        TextView textViewTotBuyTRY = findViewById(R.id.textView_TotalBuyTRY);
        TextView textViewTotBuyRUB = findViewById(R.id.textView_TotalBuyRUB);

        TextView textViewTotSellTRY = findViewById(R.id.textView_TotalSellTRY);
        TextView textViewTotSellRUB = findViewById(R.id.textView_TotalSellRUB);

        textViewTotBuyTRY.setText(String.valueOf(buySum));
        textViewTotBuyRUB.setText(String.valueOf(myRound(buySum * currentRate, 1)));

        double totalGain = myRound(sellSum / Settings.steamCommissions / Settings.additionalCommissions, 1);
        textViewTotSellTRY.setText(String.valueOf(totalGain));
        textViewTotSellRUB.setText(String.valueOf(myRound(totalGain * currentRate, 1)));

        double diff = myRound(totalGain - buySum, 1);
        textViewDiffTRY.setText(String.valueOf(diff));
        textViewDiffRUB.setText(String.valueOf(myRound(diff * currentRate, 1)));

        Resources resources = getResources();

        if (diff >= -1 && diff <= 1) {
            int white = resources.getColor(R.color.white, null);
            textViewDiffTRY.setTextColor(white);
            textViewDiffRUB.setTextColor(white);
        } else if (diff < -1) {
            int red = resources.getColor(R.color.red, null);
            textViewDiffTRY.setTextColor(red);
            textViewDiffRUB.setTextColor(red);
        } else {
            int green = resources.getColor(R.color.green, null);
            textViewDiffTRY.setTextColor(green);
            textViewDiffRUB.setTextColor(green);
        }

        db.close();
        cursor.close();
    }

    public static double myRound(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    private void setCurrencyInfo(String code) {
        TextView textViewName1 = findViewById(R.id.textView_NameTRY);
        TextView textViewName2 = findViewById(R.id.textView_CurrencyInfoTRY1);
        TextView textViewName3 = findViewById(R.id.textView_CurrencyInfoTRY2);
        TextView textViewName4 = findViewById(R.id.textView_CurrencyInfoTRY3);
        TextView textViewName5 = findViewById(R.id.textView_CurrencyInfoTRY4);
        TextView textViewName6 = findViewById(R.id.textView_CurrencyInfoTRY5);
//        TextView textViewName7 = findViewById(R.id.textView_CurrencyInfoTRY);
        TextView textViewName8 = findViewById(R.id.textView_CurrencyInfoTRY7);
        TextView textViewName9 = findViewById(R.id.textView_CurrencyInfoTRY8);
        textViewName1.setText(code);
        textViewName2.setText(code);
        textViewName3.setText(code);
        textViewName4.setText(code);
        textViewName5.setText(code);
        textViewName6.setText(code);
//        textViewName7.setText(code);
        textViewName8.setText(code);
        textViewName9.setText(code);

        TextView textViewRate = findViewById(R.id.textView_ConvertedResult);

        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).text().contains(code)) {
                Log.d("MyLog", "Element : " + data.get(i));
                Elements info = data.get(i).children();

                currentRate = Double.parseDouble(info.get(4).text().replace(",", ".")) /
                        Double.parseDouble(info.get(2).text().replace(",", "."));

                String rate = String.valueOf(currentRate);

                Log.d("MyLog", "Rate : " + info.get(4).text() + " / " + info.get(2).text() + " = " + rate);

                textViewRate.setText(String.valueOf(myRound(currentRate, 2)));

                break;
            }
        }

    }

    private void initThread() {
        runnable = new Runnable() {
            @Override
            public void run() {
                getWeb();
            }
        };
        secondThread = new Thread(runnable);
        secondThread.start();
    }

    private void getWeb() {
        try {
            Log.d("MyLog", "Website scraping is started");

            long time = System.currentTimeMillis();

            doc = Jsoup.connect("https://www.cbr.ru/currency_base/daily/").get();
            Element table = doc.getElementsByTag("tbody").get(0);

            data = table.children();

            isDataReady = true;
            Log.d("MyLog", "Website scraping is done after: " + (double) (System.currentTimeMillis() - time));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}