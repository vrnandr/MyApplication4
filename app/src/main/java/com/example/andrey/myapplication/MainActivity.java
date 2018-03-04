package com.example.andrey.myapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

/*
 кнопка обновить данные считывает лог файл и зпаисывает запросы в свой файл, фалй наверно лучше хрнаить как текстовый с json
  кнопка поиск - ищет вхождение данныехъ везде и выводит списко зарпосов удовлентворяющих услвию, при клике на запросе выводится подробная информация
  подтянуцть базу трудозатрут и считать трудозатраты
  при запуске выводтся статистика запросов
  кнопку настройки для выбора файла для парсинга
  */

public class MainActivity extends AppCompatActivity {

    final String TAG = "myLogs";
    final String DIR_SD = "OSKMobile";
    final String FILENAME_SD = "ServiceLog.log";
    final String DB = "db.json";
    //final String FILENAME_SD = "part.log";
    TextView tv;
    HashSet<Integer> tasksID;
    HashSet<ZNO> znos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG,"Активность видна");
        tv = findViewById(R.id.textView);

        if (!isDBPresent()) parseLog();

    }

    boolean isDBPresent (){
        File file = new File(DB);
        return (file.exists()&&!file.isDirectory());
    }

    void parseLog(){
        new MyTask(this).execute();

    }

    public void onclick (View view){
        Log.d(TAG,"Кнопка нажата");
        new MyTask(this).execute();

    }

    public class MyTask extends AsyncTask<Void,Integer,HashSet<ZNO>>{

        ProgressDialog pg;
        public  MyTask (Activity activity){
            pg = new ProgressDialog(activity);
        }

        @Override
        protected void onPreExecute (){
            super.onPreExecute();
            Log.d(TAG, "onPreExecute: начало потока");
            pg.setMessage("Разбор файла");
            pg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);


            if (!Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                Log.d(TAG, "jsonTest: " + "SD-карта не доступна: " + Environment.getExternalStorageState());
            }
            File sdPath = Environment.getExternalStorageDirectory();
            sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
            File sdFile = new File(sdPath, FILENAME_SD);

            if(sdFile.length()<Integer.MAX_VALUE) pg.setMax((int)sdFile.length()); //хзхз когда файл больше ~2,147Гб
            pg.show();

        }

        @Override
        protected HashSet<ZNO> doInBackground(Void... params) {

            if (!Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                Log.d(TAG, "jsonTest: " + "SD-карта не доступна: " + Environment.getExternalStorageState());
                return null;
            }
            File sdPath = Environment.getExternalStorageDirectory();
            sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
            File sdFile = new File(sdPath, FILENAME_SD);

            HashSet<ZNO> returnZNOs = new HashSet<>();
            try {
                //разбор ServiceLog.log и добавление запросов в набор запросов
                BufferedReader br = new BufferedReader(new FileReader(sdFile));
                String jsonString;
                String dateStamp;
                while ((jsonString = br.readLine()) != null) {
                    publishProgress(jsonString.getBytes().length);
                    if (jsonString.contains("Получен массив задач: [{")) {
                        dateStamp = jsonString.substring(0,20); //хардкод
                        jsonString = jsonString.substring(jsonString.indexOf('['), jsonString.length());
                    }
                    else
                        continue;
                    try{
                        Gson gson = new Gson();
                        ZNO[] znos = gson.fromJson(jsonString,ZNO[].class);
                        for (ZNO z:znos) z.datestamp = dateStamp;
                        returnZNOs.addAll(Arrays.asList(znos));
                    } catch (JsonParseException e){
                        Log.d(TAG, "jsonTest: Ошибка JsonParseException "+jsonString);
                        e.printStackTrace();
                    }
                }
                br.close();

                //запись разобраных запросов в формате json в файл db.json
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                        openFileOutput(DB, MODE_APPEND|MODE_PRIVATE)));
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                bw.write(gson.toJson(returnZNOs));
                bw.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return returnZNOs;

        }

        @Override
        protected void onProgressUpdate (Integer... values){
            super.onProgressUpdate(values);
            pg.incrementProgressBy(values[0]);
        }

        @Override
        protected void onPostExecute (HashSet<ZNO> result){
            super.onPostExecute(result);
            Log.d(TAG, "onPostExecute: конец потока");
            if (pg.isShowing()) pg.dismiss();



            //tv.append("Запросов: "+String.valueOf(result.size())+"\n");
            //for (ZNO z: result)
              //  tv.append(z.datestamp+" "+z.SDTASKID+"\n");
        }
    }




}
