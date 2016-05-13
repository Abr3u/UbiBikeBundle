package pt.ulisboa.tecnico.cmov.ubibike;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BookBikeBroadcastReceiver extends BroadcastReceiver {

    private UbiBikeService mService;

    public BookBikeBroadcastReceiver(UbiBikeService activity) {
        super();
        this.mService = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BookBikeBroadcast.BIKE_BOOKED_ACTION.equals(action)) {
            mService.onBookBikeOutsideStation(
                    intent.getStringExtra(BookBikeBroadcast.EXTRA_STATION_STATE),
                    intent.getStringExtra(BookBikeBroadcast.EXTRA_BEACON_ID_STATE));
        } else if (BookBikeBroadcast.BIKE_UNBOOKED_ACTION.equals(action)) {
            mService.onUnbookBike();
        }
    }
}
