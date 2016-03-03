package xmlpull.sample.jean.xmlpull;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.*;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    TextView val_pm25;
    TextView val_time;
    ProgressDialog waiting_dialog;
    Button btn_update;

    String xml_tag = "";
    String str_pm25;
    String str_publishTime;

    boolean b_inTargetCounty = false;
    boolean b_tagStart = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        val_pm25 = (TextView) findViewById(R.id.tv_val_pm25);
        val_time = (TextView) findViewById(R.id.tv_val_time);

        btn_update = (Button)findViewById(R.id.button_update);
        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new parseXMLTask().execute("http://opendata2.epa.gov.tw/AQX.xml");
            }
        });

        new parseXMLTask().execute("http://opendata2.epa.gov.tw/AQX.xml");

    }

    private class parseXMLTask extends AsyncTask<String, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            waiting_dialog = ProgressDialog.show(MainActivity.this, "請稍後", "資料更新中", true);
        }

        @Override
        protected Integer doInBackground(String... urls) {
            try {

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();

                String result = resultFromURL(urls[0]);

                xpp.setInput(new StringReader(result));

//                xpp.setInput(new StringReader("<foo>Hello World!</foo><bar>Hello World bar!</bar>"));

                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {

                    if (eventType == XmlPullParser.START_DOCUMENT) {
//                        Log.d("getEventType returned", "Start document");
                    } else if (eventType == XmlPullParser.START_TAG) {
//                        Log.d("getEventType returned", "Start tag, named [" + xpp.getName() + "]");

                        b_tagStart = true;

                        xml_tag = xpp.getName();

                    } else if (eventType == XmlPullParser.END_TAG) {
//                        Log.d("getEventType returned", "End tag, named [" + xpp.getName() + "]");
                        b_tagStart = false;

                        if ((b_inTargetCounty) && (xpp.getName().equals("Data"))) {
                            b_inTargetCounty = false;
                        }

                    } else if (eventType == XmlPullParser.TEXT) {
//                        Log.d("getEventType returned", "Text, the content [" + xpp.getText() + "]");

                        if ((xml_tag.equals("County")) && (xpp.getText().equals(getString(R.string.target_county)))) {
                            b_inTargetCounty = true;
                        } else if ((b_inTargetCounty) && (b_tagStart) && (xml_tag.equals("PM2.5"))) {
                            str_pm25 = xpp.getText();
                            Log.d("TAG", "str_pm25 [" + str_pm25 + "]");
                        } else if ((b_inTargetCounty) && (b_tagStart) && (xml_tag.equals("PublishTime"))) {
                            str_publishTime = xpp.getText();
                            Log.d("TAG", "str_publishTime [" + str_publishTime + "]");
                        }
                    }
                    eventType = xpp.next();
                }
//                    Log.d("getEventType returned", "End document");

            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return 1;
        }

        @Override
        protected void onPostExecute(Integer result) {

            val_pm25.setText(str_pm25);
            val_time.setText(str_publishTime);

            waiting_dialog.dismiss();
        }
    }

    private String resultFromURL(String str_url) {
        String result = "";
        URL url;
        HttpURLConnection urlConnection = null;

        try {
            url = new URL(str_url);
            urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in_s = urlConnection.getInputStream();

            result = getStringFromInputStream(in_s);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return result;
        }

    }

    private String getStringFromInputStream(InputStream is) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }
}
