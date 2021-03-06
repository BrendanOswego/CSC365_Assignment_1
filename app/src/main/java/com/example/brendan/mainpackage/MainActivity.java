package com.example.brendan.mainpackage;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.example.brendan.mainpackage.event.DataEvent;
import com.example.brendan.mainpackage.event.StartEvent;
import com.example.brendan.mainpackage.model.DataEntries;
import com.example.brendan.mainpackage.model.DayEntries;
import com.example.brendan.mainpackage.model.JsonModel;
import com.example.brendan.mainpackage.model.LocationModel;
import com.example.brendan.mainpackage.onboarding.StartFragment;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;

/**
 * Main Activity that handles Fragment Navigation and the StartEvent event.
 * - This appilcation takes in weather data from the NOAA API https://www.ncdc.noaa.gov/cdo-web/webservices/v2.
 * - Specific data for the program consists of taking the Mean Average Temperature from the East Coast
 * States via a FIPS ID and stores them into a CustomHashTable<K,V> class.
 * - The MainFragment handles the API calls for getting the Location FIPS ID's as well as the calls
 * for getting the Temperature data.
 * - Once Retrofit successfully receives a response from the web service it makes an EventBus post
 * that the MainFragment listens for.
 * - Once the MainFragment receives a post it adds the response body to the respective CustomHashTable table.
 * - When all calls are made a ListView is populated by TempItems and listens for clicks to elements
 * - When an element is clicked a similarity metric returns the temperature that is closest to the
 * element that was clicked
 */
public class MainActivity extends BaseActivity {
    private static final String TAG = MainActivity.class.getName();
    private static final String masterJson = "master_test_5.json";
    private static final String locationJson = "location_model_1.json";
    private String startTime;
    private JsonModel master;

    enum EntryStatus {
        ADDED,
        EXISTS,
        NEW_FILE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        if (savedInstanceState == null) {
            if (getSupportFragmentManager().findFragmentByTag("startFragment") == null) {
                BaseFragment f = new StartFragment();
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, f, "startFragment")
                        .commit();
            }
        } else {
            System.out.println("Start Fragment is not null");
        }
    }

    /**
     * Sends User to StartFragment
     */
    public void navigateToStartDate() {
        BaseFragment f = new StartFragment();
        FragmentManager m = getSupportFragmentManager();
        if (m.findFragmentByTag("startFragment") == null) {
            m.beginTransaction()
                    .replace(R.id.fragment_container, f, "startFragment")
                    .commit();
        } else {
            m.beginTransaction()
                    .replace(R.id.fragment_container, m.findFragmentByTag("startFragment"))
                    .commit();
        }
    }

    /**
     * Sends User to MainFragment
     */
    public void navigateToMain() {
        BaseFragment f = new MainFragment();
        FragmentManager m = getSupportFragmentManager();

        if (m.findFragmentByTag("mainFragment") == null) {
            m.beginTransaction()
                    .replace(R.id.fragment_container, f, "mainFragment")
                    .commit();
        } else {
            m.beginTransaction()
                    .replace(R.id.fragment_container, m.findFragmentByTag("mainFragment"))
                    .commit();
        }

    }

    /**
     * Receives startTime from StartFragment used for information in MainFragment
     *
     * @param event EventBus CallBack event after post has been made
     */
    @Subscribe
    public void onStartEvent(StartEvent event) {
        startTime = event.getTime();
    }

    public void writeDataInternal(DataEvent event, String name) {
        File dir = getCacheDir();
        File file = new File(dir, name);
        Gson gson = new Gson();
        String content = gson.toJson(event.getDataModel());
        Log.v(TAG, content);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file, true);
            outputStream.write(content.getBytes());
            outputStream.close();
            Log.v(TAG, "Data saved to " + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean writeLocationInternal(LocationModel model) throws IOException {
        File dir = getFilesDir();
        File file = new File(dir, locationJson);
        Gson gson = new Gson();
        Writer writer;
        if (file.exists()) {
            return false;
        } else {
            writer = new FileWriter(file.getAbsolutePath());
            gson.toJson(model, writer);
            writer.flush();
            writer.close();
            return true;
        }

    }

    public EntryStatus writeKeyValueData(DayEntries newEntry, String date) throws IOException {
        //TODO: append to file that already has stored information about days already searched
        File dir = getFilesDir();
        File file = new File(dir, masterJson);
        Gson gson = new Gson();
        String content;
        JsonModel master;
        Writer writer = null;
        BufferedWriter buffer;
        if (!file.exists()) {
            Log.v(TAG, "Creating new json file");
            master = new JsonModel();
            ArrayList<DayEntries> temp_day = new ArrayList<>();
            ArrayList<DataEntries> temp_data;
            temp_day.add(newEntry);
            temp_data = newEntry.getDataEntries();
            master.setDays(temp_day);
            master.getDays().get(0).setDataEntries(temp_data);
            writer = new FileWriter(file.getAbsolutePath());
            buffer = new BufferedWriter(writer);
            gson.toJson(master, buffer);
            buffer.close();
            writer.close();
            return EntryStatus.NEW_FILE;
        } else {
            //TODO:Check if date is already added
            Log.v(TAG, "Attempting to append data");
            if (entryExists(date)) {
                return EntryStatus.EXISTS;
            }
            master = getMaster(masterJson);
            master.getDays().add(newEntry);
            int size = master.getDays().size() - 1;
            master.getDays().get(size).setDate(date);
            writer = new FileWriter(file.getAbsolutePath());
            buffer = new BufferedWriter(writer);
            gson.toJson(master, buffer);
            readData(masterJson);
            buffer.close();
            writer.close();
            return EntryStatus.ADDED;
        }
    }


    /**
     * @return startTime class variable
     */
    public String getStartTime() {
        return startTime;
    }

    public boolean isDevMode() {
        return false;
    }

    public LocationModel readLocationModel(String name) throws IOException {
        FileInputStream iStream = openFileInput(name);
        InputStreamReader isr = new InputStreamReader(iStream);
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(isr);
        return gson.fromJson(reader, LocationModel.class);
    }

    void setMaster(JsonModel master) {
        this.master = master;
    }

    JsonModel getMaster(String name) throws IOException {
        File dir = getFilesDir();
        File file = new File(dir, masterJson);
        if (!file.exists()) {
            return null;
        }
        FileInputStream fis = openFileInput(name);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader bufferedReader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
        }
        String json = sb.toString();
        Log.v(TAG, json);
        Gson gson = new Gson();
        return gson.fromJson(json, JsonModel.class);
    }

    void readData(String name) throws IOException {
        FileInputStream fis = openFileInput(name);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader bufferedReader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
        }
        String json = sb.toString();
        Log.v(TAG, json);
    }

    public boolean entryExists(String date) throws IOException {
        if (getMaster(masterJson) == null) {
            return false;
        }
        JsonModel model = getMaster(masterJson);
        for (int i = 0; i < model.getDays().size(); i++) {
            if (model.getDays().get(i).getDate().equals(date)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<DataEntries> getDataEntries(String date) throws IOException {
        JsonModel model = getMaster(masterJson);
        for (int i = 0; i < model.getDays().size(); i++) {
            if (model.getDays().get(i).getDate().equals(date)) {
                return model.getDays().get(i).getDataEntries();
            }
        }
        return null;
    }
}
