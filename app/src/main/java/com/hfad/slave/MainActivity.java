package com.hfad.slave;


import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.net.telnet.TelnetClient;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity{

    private TextView response;
    private EditText editTextAddress, editTextPort;
    private Button buttonConnect, buttonClear;
    private Socket socket;
   /* private static Matcher matcherString_AIVDM;
    private static Matcher matcherString_AIVDO;*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextAddress = (EditText) findViewById(R.id.addressEditText);
        editTextPort = (EditText) findViewById(R.id.portEditText);
        buttonConnect = (Button) findViewById(R.id.connectButton);
        buttonClear = (Button) findViewById(R.id.clearButton);
        response = (TextView) findViewById(R.id.responseTextView);

        runtime_permissions();

        buttonConnect.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Client myClient = new Client(editTextAddress.getText()
                        .toString(), Integer.parseInt(editTextPort
                        .getText().toString()), response);
                myClient.execute();
                /*
                try {
                    socket = new Socket("192.168.0.2", 3000);
                    readLogic();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/

            }
        });

        buttonClear.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                response.setText("");
            }
        });
    }

    private void runtime_permissions() {
        if(Build.VERSION.SDK_INT >= 23){ /*&& (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)!=
                PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE)!=
                PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE)!=
                PackageManager.PERMISSION_GRANTED)) {*/

            requestPermissions(new String[]{Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.INTERNET}, 100);
            //Toast.makeText(getApplicationContext(),"requestPerm", Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(getApplicationContext(), "sdk<23", Toast.LENGTH_LONG).show();
            //scanWifi();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED && grantResults[3] == PackageManager.PERMISSION_GRANTED
                    && grantResults[4] == PackageManager.PERMISSION_GRANTED && grantResults[5] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(),"permGranted", Toast.LENGTH_LONG).show();
                //enableButton();

            }else{
                //Toast.makeText(getApplicationContext(),"permnotGranted", Toast.LENGTH_LONG).show();
                runtime_permissions();
            }
        }
    }


    class Client extends AsyncTask<Void, Void, String>{

        private String dstAddress;
        private int dstPort;
        private String response = "";
        private TextView textResponse;
        private Socket socket;
        private TelnetClient chkClient;
        private String packet;
        private BufferedReader bufferedReader;
/*        private static Pattern searchPattern_AIVDM;
        private static Pattern searchPattern_AIVDO;*/
        private Pattern searchPattern_AIVDM = Pattern.compile("AIVDM");
        private Pattern searchPattern_AIVDO = Pattern.compile("AIVDO");
        private Matcher matcherString_AIVDM;
        private Matcher matcherString_AIVDO;

        StringBuilder responseString;// = new StringBuilder();

        Client(String addr, int port,TextView textResponse) {
            dstAddress = addr;
            dstPort = port;
            this.textResponse=textResponse;
            //this.context = con;

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //AIS Decoding Service
            Intent intent = new Intent(getApplicationContext(), AISDecodingService.class);
            startService(intent);

        }

        @Override
        protected String doInBackground(Void... voids) {


            try {
                responseString = new StringBuilder();



                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(
                        128);
                byte[] buffer = new byte[128];
                chkClient = new TelnetClient();
                chkClient.connect(dstAddress, dstPort);

                int bytesRead;
                //while(true) {
                    InputStream inputStream = chkClient.getInputStream();

                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    //while ((bytesRead = inputStream.read(buffer)) != -1) {
                    while(bufferedReader.read() != -1){
                        //byteArrayOutputStream.write(buffer, 0, bytesRead);
                        //responseString.append("\n");
                        //responseString.append(byteArrayOutputStream.toString("UTF-8"));
                        responseString.append(bufferedReader.readLine());

                        if(responseString.toString().contains("AIVDM")){
                            packet = responseString.toString();
                            responseString.setLength(0);
                        }else if(responseString.toString().contains("AIVDO")){
                            packet = responseString.toString();
                            responseString.setLength(0);
                        }

                        Intent intent = new Intent("RECEIVED_PACKET");
                        intent.putExtra("AISPACKET", packet);
                        sendBroadcast(intent);
                        /*runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                            }
                        });*/

                    }

            }
            // }
            catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "UnknownHostException: " + e.toString();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.d("Log", String.valueOf(e.getCause()));
                response = "IOException: " + e.toString();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            return responseString.toString();
        }

        @Override
        protected void onPostExecute(String response) {
            //textResponse.setText(response);
            textResponse.append(response);
            // textResponse.append(responseString,0,10);
            //super.onPostExecute(result);
        }

        /*//Interface
        @Override
        public String onPacketReceived() {
            return packet;
        }*/
    }



}



