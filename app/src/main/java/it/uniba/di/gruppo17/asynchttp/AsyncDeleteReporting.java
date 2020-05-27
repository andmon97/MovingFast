package it.uniba.di.gruppo17.asynchttp;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;

import it.uniba.di.gruppo17.util.Scooter;

public class AsyncDeleteReporting extends AsyncTask<URL, Void, Boolean> {


    @Override
    protected Boolean doInBackground(URL... params) {

        Boolean delete = false;
        for (URL param : params) {
            JSONObject result = new JsonFromHttp().getJsonObject(param);
            try {
                if (result.getString("ok").equals("true")){
                    delete = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return delete;

    }
}
