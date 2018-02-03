package com.example.andrey.myapplication;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug;
import android.widget.TextView;

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
import java.util.HashSet;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    final String TAG = "myLogs";
    final String DIR_SD = "OSK";
    final String FILENAME_SD = "part.log";
    TextView tv;
    HashSet<Integer> tasksID;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG,"Активность видна");
        tv = findViewById(R.id.textView);
    }

    public void onclick (View view){
        Log.d(TAG,"Кнопка нажата");
        jsonTest();
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
           ZNO zno = new ZNO();
           tasksID = new HashSet<>();
           try {
               BufferedReader br = new BufferedReader(new FileReader(sdFile));
               String jsonString;
               while ((jsonString = br.readLine()) != null) {
                   /*Log.d(TAG, "jsonTest: " + jsonString);
                   if ((jsonString.length()<44)&(jsonString.substring(23,43)!="Получен массив задач")) continue;
                   jsonString = jsonString.substring(jsonString.indexOf('['), jsonString.length());
                   JSONArray tasks = new JSONArray(new JSONTokener(jsonString));
                   for (int i = 0; i < tasks.length(); i++) {
                       JSONObject task = tasks.getJSONObject(i);
                       tasksID.add(Integer.parseInt(task.getString(zno.SDTASKID)));
                       //tv.append(task.getString(zno.SDTASKID));
                       //tv.append("\n");
                   }*/
                   if (jsonString.length()>43&&jsonString.substring(22,42)=="Получен массив задач")
                   Log.d(TAG, "jsonTest: " + jsonString.substring(22,42));

               }
           } catch (StringIndexOutOfBoundsException e){
               e.printStackTrace();
           //} //catch (JSONException e) {
               //Log.d(TAG, "jsonTest: " + e.getMessage());
              // e.printStackTrace();
           } catch (FileNotFoundException e) {
               e.printStackTrace();
           } catch (IOException e) {
               e.printStackTrace();
           }
            //TODO проверить при 0 кол-ве
           Iterator<Integer> iterator = tasksID.iterator();
           while (iterator.hasNext()){
               tv.append(String.valueOf(iterator.next()));
               tv.append("\n");
           }
       }
}
