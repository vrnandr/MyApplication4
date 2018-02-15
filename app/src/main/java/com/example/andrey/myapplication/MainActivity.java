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
        //TaskForCountLines tk = new TaskForCountLines();
        //tk.execute();
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

    public class TaskForCountLines extends AsyncTask<Void,Void, Integer>{

        @Override
        protected void onPreExecute (){
            super.onPreExecute();
            Log.d(TAG, "onPreExecute: начало потока");
        }

        @Override
        protected Integer doInBackground (Void... params){
            if (!Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                Log.d(TAG, "jsonTest: " + "SD-карта не доступна: " + Environment.getExternalStorageState());
                return null;
            }
            File sdPath = Environment.getExternalStorageDirectory();
            sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
            File sdFile = new File(sdPath, FILENAME_SD);
            int count =0;
            try {
                BufferedReader br = new BufferedReader(new FileReader(sdFile));
                String line;
                while ((line = br.readLine()) != null)  {
                    count++;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return count;
        }

        @Override
        protected void onPostExecute (Integer count){
            super.onPostExecute(count);
            Log.d(TAG, "onPostExecute: конец потока");
            tv.append("Всего строк "+count);
        }
    }


    public class MyTask extends AsyncTask<Void,Integer,HashSet<ZNO>>{

        @Override
        protected void onPreExecute (){
            super.onPreExecute();
            Log.d(TAG, "onPreExecute: начало потока");
            pg.setVisibility(ProgressBar.VISIBLE);

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

            //счетчик строк
            Integer count =-1;
            try {
                BufferedReader br = new BufferedReader(new FileReader(sdFile));
                while (br.readLine() != null)  {
                    count++;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //конец счетчика строк

            HashSet<ZNO> returnZNOs = new HashSet<ZNO>();
            try {
                BufferedReader br = new BufferedReader(new FileReader(sdFile));
                String jsonString;
                Integer totalReadLines=0;
                while ((jsonString = br.readLine()) != null)  {
                    totalReadLines++;
                    publishProgress(count, totalReadLines);
                    if (jsonString.contains("Получен массив задач: [{"))
                        jsonString = jsonString.substring(jsonString.indexOf('['), jsonString.length());
                    else
                        continue;
                    try{
                        Gson gson = new Gson();
                        ZNO[] znos = gson.fromJson(jsonString,ZNO[].class);
                        returnZNOs.addAll(Arrays.asList(znos));
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
        protected void onProgressUpdate (Integer... values){
            super.onProgressUpdate(values);
            //TODO вот тут как-то странно
            pg.setMax(values[0]);
            pg.setProgress(values[1]);
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
