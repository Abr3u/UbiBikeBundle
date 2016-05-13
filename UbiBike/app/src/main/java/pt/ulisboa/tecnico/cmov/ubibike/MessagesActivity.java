package pt.ulisboa.tecnico.cmov.ubibike;

import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public final class MessagesActivity extends AppCompatActivity {
	
	private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        
		username = getIntent().getStringExtra(IntentKey.USERNAME.toString());
		
        ArrayList<String> aux = new ArrayList<>();
        aux.add("Luis");
        aux.add("Carlos");


        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, aux);

        final ListView listView = (ListView) findViewById(R.id.MessagesListView);
        listView.setAdapter(itemsAdapter);
    }

    public void newMessage(View view) {
        startActivity(new Intent(MessagesActivity.this, PeopleNearNewMessageActivity.class));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
