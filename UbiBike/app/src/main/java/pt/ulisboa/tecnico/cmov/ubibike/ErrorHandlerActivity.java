package pt.ulisboa.tecnico.cmov.ubibike;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;

public abstract class ErrorHandlerActivity extends AppCompatActivity {

    protected void exitApp(Context context, String message) {
        LogEnter(ErrorHandlerActivity.class.getSimpleName(), "exitApp");
        // Setting Dialog Title, Dialog Message and onClick event and show Alert message
        new AlertDialog.Builder(context)
                .setTitle("App error")
                .setMessage(message + " Apologies for inconvenience.")
                .setCancelable(false)
                .setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //Exit to main menu
                                startActivity(
                                        new Intent(getApplicationContext(), LoginActivity.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                .putExtra(IntentKey.LOGIN_MESSAGE.toString(), "Technical difficulties. Please, try again later."));
                            }
                        })
                .show();
        LogExit(ErrorHandlerActivity.class.getSimpleName(), "exitApp");
    }

    protected void LogEnter(String className, String methodName) {
        Log.d(className, "Entering " + methodName + ".");
    }

    protected void LogExit(String className, String methodName) {
        Log.d(className, "Exiting " + methodName + ".");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                returnToParentActivity();
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onBackPressed() {
        returnToParentActivity();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            returnToParentActivity();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    protected void returnToParentActivity() {
        finish();
    }
}
