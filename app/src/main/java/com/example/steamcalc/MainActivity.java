package com.example.steamcalc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private Document doc;
    private Thread secondThread;
    private Runnable runnable;

    private Elements data = null;
    private boolean isDataReady = false;

    private double currentRate = 1;
    private double currentAllCommission = 1;

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

                        currentAllCommission = Settings.steamCommissions * Settings.additionalCommissions;

                        moneyTo.setText(String.valueOf(myRound(money * currentAllCommission, 2)));
                        moneyToRub.setText(String.valueOf(myRound(moneyRate * currentAllCommission, 2)));

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


        while (!isDataReady) {
//            Log.d("MyLog", "Waiting for website data");
        }

        setCurrencyInfo(Settings.specifiedCurrency);
    }

    public static double myRound(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    private void setCurrencyInfo(String code) {
        TextView textViewName1 = findViewById(R.id.textView_NameTRY);
        TextView textViewName2 = findViewById(R.id.textView_CurrencyInfoTRY1);
        TextView textViewName3 = findViewById(R.id.textView_CurrencyInfoTRY2);
        textViewName1.setText(code);
        textViewName2.setText(code);
        textViewName3.setText(code);

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

//            for (Object item : table.toArray()) {
//                Log.d("MyLog", "Element : " + item);
//            }
            isDataReady = true;
            Log.d("MyLog", "Website scraping is done after: " + (double) (System.currentTimeMillis() - time));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}