package com.example.andrey.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeSet;


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
    final String DB = "db.json";
    final String FILENAME_SD = "ServiceLog.log";
    //final String FILENAME_SD = "log10log";
    TextView tv;
    //HashSet<Integer> tasksID;
    //HashSet<ZNO> znos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG,"Активность видна");
        tv = findViewById(R.id.textView);

        if (!isDBPresent())
        {
            Log.d(TAG, "onCreate: Разбор файла");
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Нет данных")
                    .setMessage("Нет сохраненых данных о запросах! Хотите выполнить разбор лога Мобильного сотрудника?")
                    .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                           parseLog();
                        }
                    })
                    .setNegativeButton("Нет", null);
            AlertDialog dialog = builder.create();
            dialog.show();
        }

    }

    boolean isDBPresent (){
        File file = new File(getApplicationContext().getFilesDir(),DB);
        return (file.exists()&&!file.isDirectory());
    }

    void parseLog(){
        new MyTask(this).execute();

    }

    public void onclick (View view){
        Log.d(TAG,"Кнопка нажата");
        new MyTask(this).execute();

    }

    public class MyTask extends AsyncTask<Void,Integer,LinkedHashSet<ZNO>>{

        int k; //коофициент для частоты обновлния прогрессдиалога

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
            k =pg.getMax()/1000; //хардкод
            pg.show();

        }

        @Override
        protected LinkedHashSet<ZNO> doInBackground(Void... params) {
            LinkedHashSet<ZNO> returnZNOs = new LinkedHashSet<>();
            //если есть db.json сначала считать его
            if (isDBPresent()){
                Log.d(TAG, "doInBackground: обработка существующего DB");
                try {
                    
                    BufferedReader readerDBJson = new BufferedReader(new FileReader(new File(getApplicationContext().getFilesDir(), DB)));
                    Gson gson = new Gson();
                    ZNO[] z = gson.fromJson(readerDBJson, ZNO[].class);
                    Log.d(TAG, "doInBackground: запросов в DB: "+z.length);
                    returnZNOs.addAll(Arrays.asList(z));
                    readerDBJson.close();
                } catch (JsonParseException|IOException e) {
                    e.printStackTrace();
                }

            }

            //


            if (!Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                Log.d(TAG, "jsonTest: " + "SD-карта не доступна: " + Environment.getExternalStorageState());
                return null;
            }
            File sdPath = Environment.getExternalStorageDirectory();
            sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
            File sdFile = new File(sdPath, FILENAME_SD);


            try {
                //разбор ServiceLog.log и добавление запросов в набор запросов
                BufferedReader br = new BufferedReader(new FileReader(sdFile));
                String jsonString;
                String dateStamp;
                int countBytes=0;
                while ((jsonString = br.readLine()) != null) {
                    countBytes+=jsonString.getBytes().length;
                    if (countBytes>k) {
                        publishProgress(countBytes);
                        countBytes =0;
                    }

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

                ArrayList<ZNO> znoList = new ArrayList<>(returnZNOs);
                Map<String, Integer> zMap = new HashMap<>();
                for(int i=0;i<znoList.size();i++){
                    zMap.put(znoList.get(i).SDTASKID,i);
                }

                for(ZNO z: znoList){
                    z.dateClose=znoList.get(zMap.get(z.SDTASKID)).datestamp;
                }


                //запись разобраных запросов в формате json в файл db.json
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                        openFileOutput(DB, MODE_WORLD_READABLE)));
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                //bw.write(gson.toJson(returnZNOs));
                bw.write(gson.toJson(znoList));
                bw.close();

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
        protected void onPostExecute (LinkedHashSet<ZNO> result){
            super.onPostExecute(result);
            Log.d(TAG, "onPostExecute: конец потока");
            if (pg.isShowing()) pg.dismiss();

            TreeSet<Integer> taskIDs = new TreeSet<>();
            for(ZNO z:result){
                taskIDs.add(Integer.parseInt(z.SDTASKID));
            }
            tv.append("Всего запросов: "+taskIDs.size()+"\n");
            tv.append("Последний запрос: "+taskIDs.last()+"\n");
            final ArrayList<ZNO> znoList = new ArrayList<>(result);
            ZNO lastZNO = znoList.get(znoList.size()-1);
            tv.append(lastZNO.toString());

            Map<String, Integer> zMap = new HashMap<>();
            for(int i=0;i<znoList.size();i++){
                zMap.put(znoList.get(i).SDTASKID,i);
            }

            for(ZNO z: znoList){
                z.dateClose=znoList.get(zMap.get(z.SDTASKID)).datestamp;
            }


            //tv.append("Запросов: "+String.valueOf(result.size())+"\n");
            //for (ZNO z: result)
              //  tv.append(z.datestamp+" "+z.SDTASKID+"\n");
        }
    }




}
