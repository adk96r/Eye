package adk.giteye;

import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by Adu on 12/6/2017.
 */

public class PersonQueryRequest extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "Checks";
    private static final String SERVER_LINK = "http://192.168.43.166:9000/think/";
    private Person person;
    private YuvImage yuvImage;
    private String b64;
    private byte[] jpeg;
    private boolean status;
    private HttpURLConnection urlConnection;
    private InputStream inputStream;

    public PersonQueryRequest(Person person, YuvImage yuvImage) {
        this.person = person;
        this.yuvImage = yuvImage;
        this.b64 = null;
        this.status = true;
        person.setQueryingStatus(Person.QUERY_IN_PROGRESS);
    }

    public boolean generateBase64FromImage() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(person.getFaceBounds(), 50, outputStream);
            this.jpeg = outputStream.toByteArray();
            this.b64 = Base64.encodeToString(jpeg, Base64.DEFAULT);
        } catch (Exception e) {
            Log.d(TAG, "Exception while compressing YUVImage into JPEG - " + e.getMessage());
        } finally {
            return (b64 != null);
        }
    }

    @Override
    protected Void doInBackground(Void[] params) {

        if (initConnection()) {
            parseJson();
        }

        return null;
    }

    private boolean initConnection() {


        try {

            URL url = new URL(SERVER_LINK);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");

            OutputStream outputStream = new BufferedOutputStream(urlConnection.getOutputStream());
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(outputStream, "UTF-8"));

            writer.write("ImageData" + "=" + URLEncoder.encode(b64, "UTF-8"));
            writer.flush();
            writer.close();
            urlConnection.connect();

            this.inputStream = urlConnection.getInputStream();

        } catch (MalformedURLException e) {
            Log.d(TAG, "Malformed URL Exception while connecting to server - " + e.getMessage());
            e.printStackTrace();
            status = false;
        } catch (IOException e) {
            Log.d(TAG, "IO Exception while connecting to server - " + e.getMessage());
            e.printStackTrace();
            status = false;
        } catch (Exception e) {
            Log.d(TAG, "Exception while connecting to server - " + e.getMessage());
            status = false;
        }

        if (!status) return false;

        return true;
    }

    private boolean parseJson() {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            JSONObject object = new JSONObject(reader.readLine());

            String name = object.getString("Name");
            int sem = Integer.parseInt(object.getString("Sem"));
            String branch = object.getString("Branch");
            Long rollno = Long.valueOf(object.getString("Rollno"));
            Double att = Double.valueOf(object.getString("Att"));

            person.updateData(name, rollno, sem, branch, att);

            reader.close();
        } catch (JSONException e) {
            Log.d(TAG, "JSON Exception while parsing JSON - " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "IO Exception while parseing JSON - " + e.getMessage());
            e.printStackTrace();
            status = false;
        } catch (Exception e) {
            Log.d(TAG, "Exception while parseing JSON - " + e.getMessage());
            e.printStackTrace();
            status = false;
        }

        return status;
    }

    @Override
    protected void onPostExecute(Void o) {
        super.onPostExecute(o);
        if (status) {
            person.setQueryingStatus(Person.QUERY_DONE);
            person.updateInfoView();
        } else {
            person.setQueryingStatus(Person.QUERY_FAILED);
        }
    }

}
