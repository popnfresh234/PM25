package com.dmtaiwan.alexander.pm25;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.listener.ColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.view.ColumnChartView;


public class MainActivity extends ActionBarActivity {

    private static final String prefAQLabel = "prefAQLabel";
    private static final String prefPM25 = "prefPM25";
    private static final String prefTime = "prefTime";
    private static final String prefAQLabelColor = "prefAQLabelColor";
    private static final String prefBackgroundColor = "prefBackgroundColor";
    private static final String prefFloatArray = "prefFloatArray";

    private static final int CLEAR_CHART_CODE = 1;

    private static int green;
    private static int green100;
    private static int yellow;
    private static int yellow100;
    private static int orange;
    private static int orange100;
    private static int red;
    private static int red100;
    private static int purple;
    private static int purple100;

    private Toolbar mToolbar;
    private TextView mPM25TextView;
    private TextView mSiteTextView;
    private TextView mTimeTextView;
    private TextView mQualityTextView;
    private Button button;
    private Button settings;
    private ArrayList<String> stationNames;
    private Context context = this;
    private String mPreferredStationName = null;
    private DecimalFormat mDecimalFormat = new DecimalFormat("0.#");
    private Exception mException = null;
    private LinearLayout mLinearLayout;
    private GradientDrawable mGradientDrawable;
    private SharedPreferences mSharedPreferences;
    private ProgressBar mProgressBar;

    //Loading Flags
    private boolean mIsHttpLoading;
    private boolean mIsParseLoading;

    //Parse Data Array
    private ArrayList<JSONArray> mArrayOfJSONArray;

    //Chart Variables


    private ColumnChartView mChart;
    private ColumnChartData mChartData;
    private boolean hasAxes = true;
    private boolean hasAxesNames = true;
    private boolean hasLabels = false;
    private boolean hasLabelForSelected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Set loading flags
        mIsHttpLoading = false;
        mIsParseLoading = false;
        //get color references
        green = getResources().getColor(R.color.green);
        green100 = getResources().getColor(R.color.green100);
        yellow = getResources().getColor(R.color.yellow);
        yellow100 = getResources().getColor(R.color.yellow100);
        orange = getResources().getColor(R.color.orange);
        orange100 = getResources().getColor(R.color.orange100);
        red = getResources().getColor(R.color.red);
        red100 = getResources().getColor(R.color.red100);
        purple = getResources().getColor(R.color.purple);
        purple100 = getResources().getColor(R.color.purple100);


        mArrayOfJSONArray = new ArrayList<JSONArray>();
        for (int i = 0; i < 24; i++) {
            mArrayOfJSONArray.add(null);
        }

        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(mToolbar);
        mProgressBar = (ProgressBar) findViewById(R.id.toolbar_progress_indicator);
        mLinearLayout = (LinearLayout) findViewById(R.id.linearBackground);
        mGradientDrawable = (GradientDrawable) mLinearLayout.getBackground();
        mPM25TextView = (TextView) findViewById(R.id.text_view_pm25);
        mSiteTextView = (TextView) findViewById(R.id.text_view_site);
        mTimeTextView = (TextView) findViewById(R.id.text_view_time);
        mQualityTextView = (TextView) findViewById(R.id.text_view_quality);
        //CHART
        mChart = (ColumnChartView) findViewById(R.id.columnChart);
        mChart.setOnValueTouchListener(new ValueTouchListener());

        //Progress Indicator

        //Setup Listeners
        button = (Button) findViewById(R.id.button_get_data);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgressBar.setVisibility(View.VISIBLE);
                mIsHttpLoading = true;
                if (!mPreferredStationName.equals("NONE")) {
                    PMClient task = new PMClient();
                    task.execute("http://opendata.epa.gov.tw/ws/Data/AQX/?$orderby=County&$skip=0&$top=1000&format=json");
                    queryParse();
                } else {
                    Toast.makeText(getApplicationContext(), "Please Select a Station", Toast.LENGTH_SHORT).show();
                }
            }
        });

        settings = (Button) findViewById(R.id.button_settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, SettingsActivity.class);
                i.putExtra(SettingsActivity.CODE, SettingsActivity.COUNTY);
                startActivityForResult(i, CLEAR_CHART_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            //Clear out saved chart data from prefs if new station is selected
            mSharedPreferences.edit().putString(prefFloatArray, "NONE").apply();
            mSharedPreferences.edit().putString(prefAQLabel, "NONE").apply();
            mSharedPreferences.edit().putString(prefPM25, "NONE").apply();
            mSharedPreferences.edit().putString(prefTime, "NONE").apply();
            mSharedPreferences.edit().putInt(prefAQLabelColor, 0).apply();
            mSharedPreferences.edit().putInt(prefBackgroundColor, 0).apply();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Get preffered station from Shared Prefs
        mSharedPreferences = this.getSharedPreferences(this.getPackageName(), Context.MODE_PRIVATE);
        String preferredStation = mSharedPreferences.getString(SettingsActivity.PREFERRED_STATION, "NONE");
        mPreferredStationName = preferredStation;

        //Get AQlabel from prefs, set default if not found
        if (!mSharedPreferences.getString(prefAQLabel, "NONE").equals("NONE")) {
            mQualityTextView.setText(mSharedPreferences.getString(prefAQLabel, "NONE"));
        } else {
            mQualityTextView.setText(getResources().getString(R.string.text_view_air_quality_label));
        }

        int AQLabelBGColor = mSharedPreferences.getInt(prefAQLabelColor, 0);
        if (AQLabelBGColor != 0) {
            mQualityTextView.setBackgroundColor(AQLabelBGColor);
        } else {
            mQualityTextView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }

        //Set the preferred station text view
        mSiteTextView.setText(mPreferredStationName);

        //Get PM25label from prefs
        String PM25LabelText = mSharedPreferences.getString(prefPM25, "NONE");
        if (!PM25LabelText.equals("NONE")) {
            mPM25TextView.setText(PM25LabelText);
        } else {
            mPM25TextView.setText("");
        }

        String timeText = mSharedPreferences.getString(prefTime, "NONE");
        if (!timeText.equals("NONE")) {
            mTimeTextView.setText(timeText);
        } else {
            mTimeTextView.setText("");
        }

        //set linear layout background color to transparent
        int backgroundColor = mSharedPreferences.getInt(prefBackgroundColor, 0);
        if (backgroundColor != 0) {
            mGradientDrawable.setColor(backgroundColor);
        } else {
            mGradientDrawable.setColor(getResources().getColor(R.color.transparentWhite));
        }


        //Load chart data from shared prefs
        String jsonString = mSharedPreferences.getString(prefFloatArray, "NONE");
        ArrayList<Float> floatArray = new ArrayList<Float>();
        if (!jsonString.equals("NONE")) {
            try {
                JSONArray jsonFloatArray = new JSONArray(jsonString);
                for (int i = 0; i < jsonFloatArray.length(); i++) {
                    int jsonInt = jsonFloatArray.getInt(i);
                    float jsonFloat = Float.valueOf(jsonInt);
                    floatArray.add(i, jsonFloat);
                }
                Log.i("prefFloat", String.valueOf(floatArray.size()));
                Log.i("prefFloat", floatArray.toString());
                generateChartData(floatArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {
            mChart.setColumnChartData(null);
        }
    }

    public class PMClient extends AsyncTask<String, String, JSONArray> {

        @Override
        protected JSONArray doInBackground(String... params) {
            URL url;
            HttpURLConnection urlConnection = null;
            JSONArray response = new JSONArray();

            try {
                url = new URL(params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String responseString = readStream(urlConnection.getInputStream());
                    response = new JSONArray((responseString));
                } else {
                    Log.v("PMClient", String.valueOf(responseCode));
                }
            } catch (Exception e) {
                Log.e("error", e.toString());
                mException = e;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            if(!mIsParseLoading) {
                mProgressBar.setVisibility(View.INVISIBLE);
            }
            mIsHttpLoading = false;
            if (mException != null) {
                Toast.makeText(getApplicationContext(), mException.toString(), Toast.LENGTH_SHORT).show();
            }
            stationNames = new ArrayList<String>();
            try {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject station = jsonArray.getJSONObject(i);
                    if (station.getString("SiteName").equals(mPreferredStationName)) {
                        //get PM25 concentration as string from JSON data
                        String pm25String = station.getString("PM2.5");
                        //convert to double
                        if (pm25String.equals("")) {
                            pm25String = "0";
                        }
                        Double pm25 = Double.valueOf(pm25String);
                        //calculate AQI
                        Double aqi = aqiCalc(pm25);
                        //set quality text view
                        setQualityText(aqi);
                        //format double and get String
                        String text = mDecimalFormat.format(aqi);
                        mPM25TextView.setText(text);
                        mSharedPreferences.edit().putString(prefPM25, text).apply();
                        String time = station.getString("PublishTime");
                        String newTime = "";
                        for (int j = (time.length() - 5); j < time.length(); j++) {
                            newTime = newTime + time.charAt(j);
                        }
                        mTimeTextView.setText(newTime);
                        mSharedPreferences.edit().putString(prefTime, newTime).apply();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        private String readStream(InputStream in) {
            BufferedReader reader = null;
            StringBuffer response = new StringBuffer();
            try {
                reader = new BufferedReader(new InputStreamReader(in));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return response.toString();
        }
    }

    private Double aqiCalc(double pm25) {
        Double c = Math.floor((10 * pm25) / 10);
        Double AQI = null;
        if (c >= 0 && c < 12.1) {
            AQI = linear(50.0, 0.0, 12.0, 0.0, c);
        } else if (c >= 12.1 && c < 35.5) {
            AQI = linear(100.0, 51.0, 35.4, 12.1, c);
        } else if (c >= 35.5 && c < 55.5) {
            AQI = linear(150.0, 101.0, 55.4, 35.5, c);
        } else if (c >= 55.5 && c < 150.5) {
            AQI = linear(200.0, 151.0, 150.4, 55.5, c);
        } else if (c >= 150.5 && c < 250.5) {
            AQI = linear(300.0, 201.0, 250.4, 150.5, c);
        } else if (c >= 250.5 && c < 350.5) {
            AQI = linear(400.0, 301.0, 350.4, 250.5, c);
        } else if (c >= 350.5 && c < 500.5) {
            AQI = linear(500.0, 401.0, 500.4, 350.5, c);
        } else {
            AQI = -1.0;
        }
        return AQI;


    }

    private Double linear(Double AQIHigh, Double AQILow, Double concHigh, Double concLow, Double Concentration) {
        Double linear;
        Double conc = Concentration;
        Double a = ((conc - concLow) / (concHigh - concLow)) * (AQIHigh - AQILow) + AQILow;
        linear = Double.valueOf(Math.round(a));
        return linear;
    }

    private void setQualityText(Double aqi) {
        if (aqi <= 50) {
            mQualityTextView.setText("GOOD");
            mSharedPreferences.edit().putString(prefAQLabel, "GOOD").apply();

            mQualityTextView.setBackgroundColor(green);
            mSharedPreferences.edit().putInt(prefAQLabelColor, green).apply();

            mGradientDrawable.setColor(green100);
            mSharedPreferences.edit().putInt(prefBackgroundColor, green100).apply();
        }
        if (aqi > 51 && aqi <= 100) {
            mQualityTextView.setBackgroundColor(yellow);
            mSharedPreferences.edit().putInt(prefAQLabelColor, yellow).apply();

            mGradientDrawable.setColor(yellow100);
            mSharedPreferences.edit().putInt(prefBackgroundColor, yellow100).apply();

            mQualityTextView.setText("MODERATE");
            mSharedPreferences.edit().putString(prefAQLabel, "MODERATE").apply();
        }
        if (aqi > 101 && aqi <= 150) {
            mQualityTextView.setBackgroundColor(orange);
            mSharedPreferences.edit().putInt(prefAQLabelColor, orange).apply();

            mGradientDrawable.setColor(orange100);
            mSharedPreferences.edit().putInt(prefBackgroundColor, orange100).apply();

            mQualityTextView.setText("UNHEALTHY");
            mSharedPreferences.edit().putString(prefAQLabel, "UNHEALTHY").apply();
        }
        if (aqi > 151 && aqi <= 200) {
            mQualityTextView.setBackgroundColor(red);
            mSharedPreferences.edit().putInt(prefAQLabelColor, red).apply();

            mGradientDrawable.setColor(red100);
            mSharedPreferences.edit().putInt(prefBackgroundColor, red100).apply();

            mQualityTextView.setText("DANGEROUS");
            mSharedPreferences.edit().putString(prefAQLabel, "DANGEROUS").apply();
        }
        if (aqi > 201) {
            mQualityTextView.setBackgroundColor(purple);
            mSharedPreferences.edit().putInt(prefAQLabelColor, purple).apply();

            mGradientDrawable.setColor(purple100);
            mSharedPreferences.edit().putInt(prefBackgroundColor, purple100).apply();

            mQualityTextView.setText("DEADLY");
            mSharedPreferences.edit().putString(prefAQLabel, "DEADLY").apply();
        }
    }

    private void queryParse() {
        mProgressBar.setVisibility(View.VISIBLE);
        mIsParseLoading = true;
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Data");
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                if (!mIsHttpLoading) {
                    mProgressBar.setVisibility(View.INVISIBLE);
                }
                mIsParseLoading = false;
                if (e == null) {
                    for (int i = 0; i < 24; i++) {
                        String data = parseObject.getString("data" + String.valueOf(i));
                        if (!data.equals("0")) {
                            try {
                                JSONArray array = new JSONArray(data);
                                mArrayOfJSONArray.set(i, array);
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        } else {
                            mArrayOfJSONArray.set(i, null);
                        }

                    }
                    Log.i("mArrayOfJSONArray", String.valueOf(mArrayOfJSONArray.size()));
                    extractDataFromDoubleArray();


                } else {
                    Log.i("Parse Error", e.toString());
                }
            }
        });
    }


    private void extractDataFromDoubleArray() {

        ArrayList<Float> floatArray = new ArrayList<Float>();
        for (int k = 0; k < 24; k++) {
            floatArray.add(Float.valueOf("0"));
        }
        Log.i("floatArray", String.valueOf(floatArray.size()));
        Log.i("mArrayOfJSONArray", String.valueOf(mArrayOfJSONArray.size()));
        for (int j = 0; j < mArrayOfJSONArray.size(); j++) {
            JSONArray currentArray = mArrayOfJSONArray.get(j);
            if (currentArray != null) {
                try {

                    for (int i = 0; i < currentArray.length(); i++) {
                        JSONObject station = currentArray.getJSONObject(i);
                        if (station.getString("SiteName").equals(mPreferredStationName)) {
                            //get PM25 concentration as string from JSON data
                            String pm25String = station.getString("PM2.5");
                            if(pm25String.equals("")) {
                                pm25String = "0";
                            }
                            //convert to double
                            Double pm25 = Double.valueOf(pm25String);
                            //calculate AQI
                            Double aqi = aqiCalc(pm25);
                            String doubleValue = String.valueOf(aqi);
                            floatArray.set(j, Float.valueOf(doubleValue));
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (currentArray == null) {
                floatArray.set(j, Float.valueOf("0"));
            }
            if (currentArray != null) {
                Log.i("currentArray", String.valueOf(currentArray.length()));
            }
            Log.i("floatArray", String.valueOf(floatArray.size()));

        }

        //Convert ArrayList float to JSON array and then JSON string to be saved to Shared Prefs
        JSONArray jsonFloatArray = new JSONArray();
        for (int l = 0; l < floatArray.size(); l++) {
            try {
                jsonFloatArray.put(l, floatArray.get(l));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        String jsonString = jsonFloatArray.toString();
        mSharedPreferences.edit().putString(prefFloatArray, jsonString).apply();

        //Generate chart data
        generateChartData(floatArray);

    }

    private void generateChartData(ArrayList<Float> dataArray) {
        int numColumns = 24;
        List<Column> columns = new ArrayList<Column>();
        List<SubcolumnValue> values;
        for (int i = 0; i < numColumns; i++) {
            values = new ArrayList<SubcolumnValue>();
            values.add(new SubcolumnValue(dataArray.get(i),purple100));
            Column column = new Column(values);
            column.setHasLabels(hasLabels);
            column.setHasLabelsOnlyForSelected(hasLabelForSelected);
            columns.add(column);
        }
        mChartData = new ColumnChartData(columns);


        if (hasAxes) {
            Axis axisX = new Axis();
            axisX.setTextColor(getResources().getColor(android.R.color.black));
            List<AxisValue> axisValues = new ArrayList<AxisValue>();
            for (int i = 0; i <= 24; i++) {
                AxisValue axisValue = new AxisValue(Float.valueOf(i));
                axisValues.add(axisValue);
            }
            axisX.setValues(axisValues);
            axisX.setAutoGenerated(false);
            axisX.setMaxLabelChars(1);
            axisX.setTextSize(6);
            Axis axisY = new Axis().setHasLines(true);
            axisY.setTextColor(getResources().getColor(android.R.color.black));
            if (hasAxesNames) {
                axisX.setName("Time");
                axisY.setName("PM25");
            }
            mChartData.setAxisXBottom(axisX);
            mChartData.setAxisYLeft(axisY);
        } else {
            mChartData.setAxisXBottom(null);
            mChartData.setAxisYLeft(null);
        }

        mChart.setColumnChartData(mChartData);
        mChart.setVisibility(View.VISIBLE);
    }

    private class ValueTouchListener implements ColumnChartOnValueSelectListener {

        @Override
        public void onValueSelected(int i, int i1, SubcolumnValue subcolumnValue) {
            String value = String.valueOf(subcolumnValue.getValue());
            Toast.makeText(getApplicationContext(), value, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onValueDeselected() {

        }
    }
}
