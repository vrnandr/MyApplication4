package com.example.andrey.myapplication;

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
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    final String TAG = "myLogs";
    final String DIR_SD = "OSK";
    final String FILENAME_SD = "ServiceLog.log";
    TextView tv;
    ProgressBar pg;
    HashSet<Integer> tasksID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG,"Активность видна");
        tv = findViewById(R.id.textView);
        pg = findViewById(R.id.progressBar);
        pg.setScaleY(5f);
    }

    public void onclick (View view){
        Log.d(TAG,"Кнопка нажата");
        MyTask mt = new MyTask();
        mt.execute();
    }

    void jsonTest() {
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Log.d(TAG, "jsonTest: " + "SD-карта не доступна: " + Environment.getExternalStorageState());
            return;
        }
        File sdPath = Environment.getExternalStorageDirectory();
        sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
        File sdFile = new File(sdPath, FILENAME_SD);
        tasksID = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(sdFile));
            String jsonString;
            while ((jsonString = br.readLine()) != null)  {
                if (jsonString.contains("Получен массив задач: [{"))
                    jsonString = jsonString.substring(jsonString.indexOf('['), jsonString.length());
                else
                    continue;

                try{
                    Gson gson = new Gson();
                    ZNO[] znos = gson.fromJson(jsonString,ZNO[].class);
                    for(ZNO z: znos) {
                        tasksID.add((Integer.parseInt(z.SDTASKID)));
                     //      Log.d(TAG, "jsonTest: " + z.SDTASKID);
                    }
                } catch (JsonParseException e){
                    Log.d(TAG, "jsonTest: Ошибка JsonParseException "+jsonString);
                    e.printStackTrace();
                }
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //TODO проверить при 0 кол-ве
        tv.append("Запросов: "+String.valueOf(tasksID.size())+"\n");

        Iterator<Integer> iterator = tasksID.iterator();
        while (iterator.hasNext()){
            tv.append(String.valueOf(iterator.next())+"\n");
        }
    }

    public class MyTask extends AsyncTask<Void,Long,HashSet<ZNO>>{
        long size;
        float k;
        File sdPath;
        File sdFile;
        long totalReadBytes=0;

        @Override
        protected void onPreExecute (){
            Log.d(TAG, "onPreExecute: начало потока");
            //TODO тут хуйня насчет открытия файла в преекзекут
            pg.setVisibility(ProgressBar.VISIBLE);
            if (!Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                Log.d(TAG, "jsonTest: " + "SD-карта не доступна: " + Environment.getExternalStorageState());
                return;
            }
            sdPath = Environment.getExternalStorageDirectory();
            sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
            sdFile = new File(sdPath, FILENAME_SD);
            size = sdFile.length();
            k = 1;
            if(size> Integer.MAX_VALUE)
                k = size / Integer.MAX_VALUE;
            pg.setMax((int)(size/k));
            Log.d(TAG, "onPreExecute: k="+Float.toString(k)+" size="+Long.toString(size)+" "+pg.getMax());
        }

        //на выходе должен быть набор запросов, который потом занести в базу
        @Override
        protected HashSet<ZNO> doInBackground(Void... params) {

            HashSet<ZNO> returnZNOs = new HashSet<ZNO>();
            try {
                BufferedReader br = new BufferedReader(new FileReader(sdFile));
                String jsonString;
                while ((jsonString = br.readLine()) != null)  {
                    totalReadBytes+= jsonString.length();
                    publishProgress(totalReadBytes);
                    if (jsonString.contains("Получен массив задач: [{"))
                        jsonString = jsonString.substring(jsonString.indexOf('['), jsonString.length());
                    else
                        continue;
                    try{
                        Gson gson = new Gson();
                        ZNO[] znos = gson.fromJson(jsonString,ZNO[].class);
                        //returnZNOs.addAll(Arrays.asList(znos));
                        for(ZNO z: znos) {
                            returnZNOs.add(z);
                        }
                    } catch (JsonParseException e){
                        Log.d(TAG, "jsonTest: Ошибка JsonParseException "+jsonString);
                        e.printStackTrace();
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return returnZNOs;
        }

        @Override
        protected void onProgressUpdate (Long... values){
            super.onProgressUpdate(values);
            pg.setProgress((int)(totalReadBytes/k));
        }

        @Override
        protected void onPostExecute (HashSet<ZNO> result){
            super.onPostExecute(result);
            pg.setVisibility(ProgressBar.INVISIBLE);
            Log.d(TAG, "onPostExecute: конец потока");
            tv.append("Запросов: "+String.valueOf(result.size())+"\n");
            for (ZNO z: result)
                tv.append(z.SDTASKID+"\n");
        }
    }




}
