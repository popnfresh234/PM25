package com.dmtaiwan.alexander.pm25;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Created by Alexander on 5/12/2015.
 */
public class SettingsActivity extends ListActivity{
    public static final String CODE = "code";
    public static final String COUNTY = "county";
    public static final String STATION = "station";
    public static final String PREFERRED_STATION = "preferred_station";

    String mIntentCode;
    ArrayAdapter<String> mArrayAdapter;
    String[] mList = null;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mIntentCode = intent.getStringExtra(CODE);
        if (mIntentCode.equals(STATION)) {

            int county = intent.getIntExtra(STATION, -1);
            String countyName = "_"+String.valueOf(county);
            int id = getResources().getIdentifier(countyName, "array", "com.dmtaiwan.alexander.pm25");
            mList = getResources().getStringArray(id);
            mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mList);
            setListAdapter(mArrayAdapter);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_activity);
        mIntentCode = getIntent().getStringExtra(CODE);
        if(mIntentCode.equals(COUNTY)) {
            mList = getResources().getStringArray(R.array.counties);
            mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mList);
            setListAdapter(mArrayAdapter);
        }

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (mIntentCode.equals(COUNTY)) {
            String county = (String) l.getItemAtPosition(position);
            Intent i = new Intent(this, SettingsActivity.class);
            i.putExtra(CODE, STATION);
            i.putExtra(STATION, position);
            startActivity(i);
        }else if (mIntentCode.equals(STATION)) {
            String stationName = (String) l.getItemAtPosition(position);
            SharedPreferences prefs = this.getSharedPreferences(this.getPackageName(), Context.MODE_PRIVATE);
            prefs.edit().putString(PREFERRED_STATION, stationName).apply();
            setResult(RESULT_OK);
            finish();
        }
    }
}
