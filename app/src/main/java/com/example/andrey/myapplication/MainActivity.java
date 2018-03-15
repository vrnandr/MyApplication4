package com.example.andrey.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
    final String LAST_DATE = "lastDate";
    //final String FILENAME_SD = "log10log";
    TextView tv;
    ArrayList<ZNO> znos;
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
                           parseLog(null);
                        }
                    })
                    .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            on();
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        on();


    }

    boolean isDBPresent (){
        File file = new File(getApplicationContext().getFilesDir(),DB);
        return (file.exists()&&!file.isDirectory());
    }

    void parseLog(String date){
        new MyTask(this).execute(date);

    }


    void on(){
        //Считываение сохраненного файла
        try {
            BufferedReader readerDBJson = new BufferedReader(new FileReader(new File(getApplicationContext().getFilesDir(), DB)));
            Gson gson = new Gson();
            ZNO[] z = gson.fromJson(readerDBJson, ZNO[].class);
            znos = new ArrayList<>();
            znos.addAll(Arrays.asList(z));
            readerDBJson.close();
        } catch (JsonParseException|IOException e) {
            e.printStackTrace();
        }
        //вывод статистики за текущий месяц
        SimpleDateFormat formatCurrentMonth = new SimpleDateFormat("yyyy-MM");
        String currentMonth = formatCurrentMonth.format(new Date());
        //String  currentMonth= "2017-12";

        Map<String,ZNO> mapZnosCurrentMonth = new TreeMap<>();
        for(ZNO z: znos)
            if (z.dateClose.contains(currentMonth))
                mapZnosCurrentMonth.put(z.SDTASKID,z);

        tv.append("\n");
        tv.append("Запросов за текущий месяц: "+Integer.toString(mapZnosCurrentMonth.size())+"\n");
        tv.append("Номер запроса Дата, время закрытия"+"\n");
        for(ZNO z:mapZnosCurrentMonth.values()){
            tv.append(z.SDTASKID +" "+z.dateClose+"\n");
        }

    }

    public void onclick (View view){
        Log.d(TAG,"Кнопка нажата");
        parseLog(null);

    }

    //onclick Обновить
    public void updateLogs(View view){

        SharedPreferences sPref = getPreferences(MODE_PRIVATE);
        String lastdDate = sPref.getString(LAST_DATE, "");
        parseLog(lastdDate);

    }

    public class MyTask extends AsyncTask<String,Integer,ArrayList<ZNO>>{

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
        protected ArrayList<ZNO> doInBackground(String... params) {
            LinkedHashSet<ZNO> returnZNOs = new LinkedHashSet<>();
            Map<String, String> mapForCloseDate = new TreeMap<>();
            String lastDate= null;

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
                ZNO[] lastznos= null;
                while (((jsonString = br.readLine()) != null)
                       //&((params[0]!=null)&(jsonString.contains(params[0])))
                        )
                {
                    if (params[0]!=null&!jsonString.contains(params[0]))
                        continue;
                    countBytes+=jsonString.getBytes().length;
                    if (countBytes>k) {
                        publishProgress(countBytes);
                        countBytes =0;
                    }

                    if (jsonString.contains("Получен массив задач: [{")) {
                        dateStamp = jsonString.substring(0,19); //хардкод
                        lastDate=dateStamp;
                        jsonString = jsonString.substring(jsonString.indexOf('['), jsonString.length());
                    }
                    else
                        continue;
                    try{
                        Gson gson = new Gson();
                        ZNO[] znos = gson.fromJson(jsonString,ZNO[].class);
                        for (ZNO z:znos){
                            z.datestamp = dateStamp;
                            mapForCloseDate.put(z.SDTASKID,dateStamp);
                        }
                        lastznos= znos;
                        returnZNOs.addAll(Arrays.asList(znos));
                    } catch (JsonParseException e){
                        Log.d(TAG, "jsonTest: Ошибка JsonParseException "+jsonString);
                        e.printStackTrace();
                    }
                }
                br.close();

                //для запросов находяшихся в последней json строке лога установить время закрытия пустым, так как они с бОльшей вероятностью не закрыты
                try {
                    for(ZNO z:lastznos)
                        mapForCloseDate.put(z.SDTASKID, "");
                } catch (NullPointerException e){
                    Log.d(TAG, "doInBackground: Ошибка при обновлении времени закрытия не закрытых запросов");
                    e.printStackTrace();
                }
                //загнать данные из mapForCLoseDate в returnZNOs
                //эта фигня для определения даты закрытия, время закрытия будет между последним вхождением запроса в лог
                // и последним НЕ вхождением, имхо проще сделать последние вхождение
                for (ZNO z:returnZNOs)
                    z.dateClose=mapForCloseDate.get(z.SDTASKID);



                ArrayList<ZNO> znoList = new ArrayList<>(returnZNOs);

                //запись разобраных запросов в формате json в файл db.json
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                        openFileOutput(DB, MODE_WORLD_READABLE)));
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                //bw.write(gson.toJson(returnZNOs));
                bw.write(gson.toJson(znoList));
                bw.close();

                //запись в преференсес дату последней строки с json
                SharedPreferences sPref = getPreferences(MODE_PRIVATE);
                Editor ed = sPref.edit();
                ed.putString(LAST_DATE, lastDate);
                ed.apply();

            } catch (IOException e) {
                e.printStackTrace();
            }


            return new ArrayList<>(returnZNOs);

        }

        @Override
        protected void onProgressUpdate (Integer... values){
            super.onProgressUpdate(values);
            pg.incrementProgressBy(values[0]);
        }

        @Override
        protected void onPostExecute (ArrayList<ZNO> znoList){
            super.onPostExecute(znoList);
            Log.d(TAG, "onPostExecute: конец потока");
            if (pg.isShowing()) pg.dismiss();
            on();
            /*
            //znos=znoList;

            /*TreeSet<Integer> taskIDs = new TreeSet<>();
            for(ZNO z:result){
                taskIDs.add(Integer.parseInt(z.SDTASKID));
            }
            tv.append("Всего запросов: "+taskIDs.size()+"\n");
            tv.append("Последний запрос: "+taskIDs.last()+"\n");

            ZNO lastZNO = znoList.get(znoList.size()-1);
            tv.append(lastZNO.toString());

            /*SimpleDateFormat formatCurrentMonth = new SimpleDateFormat("yyyy-MM");
            String currentMonth = formatCurrentMonth.format(new Date());
            String  currentMonth= "2017-12";

            Map<String,ZNO> mapZnosCurrentMonth = new TreeMap<>();
            for(ZNO z: znoList)
                if (z.dateClose.contains(currentMonth))
                    mapZnosCurrentMonth.put(z.SDTASKID,z);

            tv.append("\n");
            tv.append("Запросов за текущий месяц: "+Integer.toString(mapZnosCurrentMonth.size())+"\n");
            tv.append("Номер запроса Дата, время закрытия"+"\n");
            for(ZNO z:mapZnosCurrentMonth.values()){
                tv.append(z.SDTASKID +" "+z.dateClose+"\n");
            }
            */

        }
    }




}
