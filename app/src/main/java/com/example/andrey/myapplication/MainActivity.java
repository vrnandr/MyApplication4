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

public class MainActivity extends AppCompatActivity {

    final String TAG = "myLogs";
    final String DIR_SD = "OSK";
    final String FILENAME_SD = "someB.log";
    TextView tv;
    

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
           try {
               BufferedReader br = new BufferedReader(new FileReader(sdFile));
               String jsonString;
               while ((jsonString = br.readLine()) != null) {
                   Log.d(TAG, "jsonTest: " + jsonString);
                   jsonString = jsonString.substring(jsonString.indexOf('['), jsonString.length());
                   JSONArray tasks = new JSONArray(new JSONTokener(jsonString));
                   for (int i = 0; i < tasks.length(); i++) {
                       JSONObject task = tasks.getJSONObject(i);
                       tv.append(task.getString(zno.SDTASKID));
                       tv.append("\n");

                   }


               }
           } catch (JSONException e) {
               Log.d(TAG, "jsonTest: " + e.getMessage());
               e.printStackTrace();
           } catch (FileNotFoundException e) {
               e.printStackTrace();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
}
