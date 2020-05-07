package com.example.hackernewsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView newsListView;
    ArrayList<String> titles = new ArrayList<String>();
    ArrayList<String> content = new ArrayList<String>();
    ArrayAdapter arrayAdapter;

    SQLiteDatabase newsDB;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        newsListView = (ListView) findViewById(R.id.newsListView);


        newsDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        newsDB.execSQL("CREATE TABLE IF NOT EXISTS article (id INTEGER PRIMARY KEY, articleID, title VARCHAR, content VARCHAR)");

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        newsListView.setAdapter(arrayAdapter);



        DownloadTask downloadTask = new DownloadTask();

        try {
           // downloadTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
        } catch (
                Exception e) {
            e.printStackTrace();
        }


        newsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), news_webview.class);
                intent.putExtra("content", content.get(i));
                startActivity(intent);
            }
        });

        updateListView();


    }
    public void updateListView(){
        Cursor c = newsDB.rawQuery("SELECT * FROM article", null);
        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        if (c.moveToFirst()){
            titles.clear();
            content.clear();

            do {
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            } while(c.moveToNext());
                arrayAdapter.notifyDataSetChanged();


        }
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();

                while (data != -1) {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }

                JSONArray jsonArray = new JSONArray(result);
                int numberOfStories = 20;
                if(jsonArray.length()< numberOfStories){
                    numberOfStories = jsonArray.length();
                }

                newsDB.execSQL("DELETE FROM article");
                for (int i = 0; i < numberOfStories; i++) {
                    String id = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + id + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);
                    data = reader.read();

                    String articleInfo = "";

                    while (data != -1) {
                        char current = (char) data;
                        articleInfo += current;
                        data = reader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                        String title = jsonObject.getString("title");
                        String urlWebsite = jsonObject.getString("url");

                        url = new URL(urlWebsite);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        in = urlConnection.getInputStream();
                        reader = new InputStreamReader(in);
                        data = reader.read();
                        String articleContent = "";

                        while(data != -1){
                            char current = (char) data;
                            articleContent += current;
                            data = reader.read();
                        }
                        String sql = "INSERT INTO article (articleID, title, content) VALUES (?, ?, ?)";
                        SQLiteStatement statement = newsDB.compileStatement(sql);
                        statement.bindString(1, id);
                        statement.bindString(2, title);
                        statement.bindString(3, articleContent);

                        statement.execute();
                    }

                }
                return result;
            } catch (Exception e) {
                e.printStackTrace();
                return "Failed";
            }


        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }





}

