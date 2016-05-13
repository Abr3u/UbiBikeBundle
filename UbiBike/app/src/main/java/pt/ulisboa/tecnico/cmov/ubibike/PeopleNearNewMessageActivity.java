package pt.ulisboa.tecnico.cmov.ubibike;

import android.os.Bundle;
import android.view.MenuItem;

public final class PeopleNearNewMessageActivity extends PeopleNearActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myWifiService.setNearMsgActivity(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}