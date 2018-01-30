package com.example.andrey.myapplication;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity {

    final String TAG = "myLogs";
    final String DIR_SD = "OSK";
    final String FILENAME_SD = "ServiceLog.log";
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG,"Активность видна");
    }

    public void onclick (View view){
        Log.d(TAG,"Кнопка нажата");
        readFileSD();
    }

    void readFileSD() {
        // проверяем доступность SD
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Log.d(TAG, "SD-карта не доступна: " + Environment.getExternalStorageState());
            return;
        }
        // получаем путь к SD
        File sdPath = Environment.getExternalStorageDirectory();
        // добавляем свой каталог к пути
        sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
        // формируем объект File, который содержит путь к файлу
        File sdFile = new File(sdPath, FILENAME_SD);
        Log.d(TAG,"Файл "+sdFile);
        try {
            // открываем поток для чтения
            Log.d(TAG, "readFileSD: вход в try");
            BufferedReader br = new BufferedReader(new FileReader(sdFile));
            Log.d(TAG, "readFileSD: создан bufferreader");
            // читаем содержимое
            int count = 0;
            Log.d(TAG, "readFileSD: "+String.valueOf(count));
            while (br.readLine() != null) {
                count++;
            }
            Log.d(TAG, String.valueOf(count));
            Log.d(TAG, "readFileSD: конец");
        } catch (FileNotFoundException e) {
            Log.d(TAG, "readFileSD: FileNotFoundException "+ e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
