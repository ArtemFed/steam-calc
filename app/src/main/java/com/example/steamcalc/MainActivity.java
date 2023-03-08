package com.example.steamcalc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private Document doc;
    private Thread secondThread;
    private Runnable runnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = findViewById(R.id.button_settings);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,
                        Settings.class);
                startActivity(intent);
            }
        });

//        initThread();
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
            doc = Jsoup.connect("https://www.cbr.ru/currency_base/daily/").get();
            Element table = doc.getElementsByTag("tbody").get(0);

            Elements data = table.children();

            for (int i = 0; i < data.size(); i++) {
                if (data.get(i).text().contains("TRY")) {
                    Log.d("MyLog", "Element : " + data.get(i));
                    Elements info = data.get(i).children();

                    Log.d("MyLog", "Rate : " +
                            info.get(4).text() +
                            " / " +
                            info.get(2).text() +
                            " = " +
                            Double.parseDouble(info.get(4).text().replace(",", ".")) /
                                    Double.parseDouble(info.get(2).text().replace(",", ".")));
                }
            }

//            for (Object item : table.toArray()) {
//                Log.d("MyLog", "Element : " + item);
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}