package pt.ulisboa.tecnico.cmov.ubibike;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

//import pt.ulisboa.tecnico.cmov.ubibike.commons.UserJsonKey;


public final class HistoryActivity extends JsonHandlerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogEnter(HistoryActivity.class.getSimpleName(), "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        try {
            //Check trajectory information list content
            if(!getIntent().hasExtra(IntentKey.MOST_RECENT_TRAJECTORY.toString())) {
                // Setting Dialog Title, Dialog Message and onClick event and show Alert message
                new AlertDialog.Builder(HistoryActivity.this)
                        .setTitle("Empty history")
                        .setMessage("There are no trajectories recorded.")
                        .setCancelable(false)
                        .setPositiveButton("Return to main menu",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();}})
                        .show();
                return;
            }

            //Put trajectory information from json array into trajectory info list
            final ArrayList<String> trajectoryInfoList = new ArrayList<>();

            //Get all trajectories
            final JSONArray trajectories;
            if(getIntent().hasExtra(IntentKey.PAST_TRAJECTORIES.toString())) {
                trajectories = new JSONArray(getIntent().getStringExtra(IntentKey.PAST_TRAJECTORIES.toString()));
            } else {
                trajectories = new JSONArray();
            }
            trajectories.put(new JSONObject(getIntent().getStringExtra(IntentKey.MOST_RECENT_TRAJECTORY.toString())));

            //Add locations into trajectory info list
            JSONArray locations;
            for (int index = 0; index < trajectories.length(); ++index) {
                locations = trajectories.getJSONObject(index).getJSONArray(UserJsonKey.LOCATIONS.toString());
                trajectoryInfoList.add(
                        getStringFromJson(
                                locations.getJSONObject(0),
                                UserJsonKey.LOCATION_DATE) +
                                Constant.ARROW +
                                getStringFromJson(
                                        locations.getJSONObject(locations.length() - 1),
                                        UserJsonKey.LOCATION_DATE).substring(11));
            }

            //Collections.reverse(trajectoryInfoList);
            //Get array adapter
            ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                        trajectoryInfoList);

            final ListView listView = (ListView) findViewById(R.id.historyList);
            listView.setAdapter(itemsAdapter);
            listView.setOnItemClickListener(
                    new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> arg0, View view,
                                                int position, long id) {
                            try {
                                Intent intent = new Intent(HistoryActivity.this, TrajectoryInformationActivity.class);
                                intent.putExtra(
                                        IntentKey.TRAJECTORY.toString(),
                                        trajectories.getJSONObject(position).toString());
                                startActivity(intent);
                            } catch (JSONException exception) {
                                exitApp(HistoryActivity.this, exception.toString());
                            }
                        }
                    }
            );
        } catch (JSONException |
                 InvalidReplyException exception) {
            exitApp(HistoryActivity.this, exception.getMessage() + ".");
        }
        LogEnter(HistoryActivity.class.getSimpleName(), "onCreate");
    }
}