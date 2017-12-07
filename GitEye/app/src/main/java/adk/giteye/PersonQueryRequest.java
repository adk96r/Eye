package adk.giteye;

import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.toolbox.Volley;

/**
 * Created by Adu on 12/6/2017.
 */

public class PersonQueryRequest extends AsyncTask {

    private static final String TAG = "Checks";
    private Person person;

    public PersonQueryRequest(Person person) {
        Log.d(TAG, "Initialising the request.");
        this.person = person;

        
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG, "Querying the person.");
    }

    @Override
    protected Object doInBackground(Object[] params) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Got the details of the person.");
        person.setName("Aditya Kakaraparti");
        person.setRollNo(1210314802);
        person.setSem(8);
        person.setBranch("CSE");
        person.setAttendance(85.0f);
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        Log.d(TAG, "Updated the person.");
    }

}
