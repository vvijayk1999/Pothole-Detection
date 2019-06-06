package com.pothole.pothole;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.io.File;

import static android.content.ContentValues.TAG;

public class Login extends AppCompatActivity {
    EditText uname , password;
    TextView incorrect_uname , request_processing ;
    Button login, reg_page;
    String stringUname ,stringPassword;
    SharedPreferences preferences;
    boolean loginstatus = false;
    private String filename = "LoginStatus.txt";
    private String filepath = "Pothole";
    File myExternalFile;
    String myData = "";
    MQTTHelper mqttHelper;
    MainActivity mainActivity;
    String callback;

    //////////////////////////MQTT Connect options/////////////////////////////
    String URL = "tcp://m16.cloudmqtt.com";
    String portNumber = "13941";
    String userName = "vbqcvuri";
    String mqpassword = "tgt22oGl7EwE";
    ///////////////////////////////////////////////////////////////////////////
    String clientId = MqttClient.generateClientId();
    MqttAndroidClient client;

    private static final String PREFS_NAME = "PrefsFile";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        ////////////////////////////////  MQTT //////////////////////////////////////////////////////////////
        client = new MqttAndroidClient(this.getApplicationContext(), URL + ":" + portNumber, clientId);
        MQTTHelper mqttHelper = new MQTTHelper();
        mqttHelper.connect(client, userName, mqpassword);

        ////////////////////////////////////////////////////////////////////////////////////////////////////


        uname = (EditText) findViewById(R.id.uname);
        password = (EditText) findViewById(R.id.password);
        login = (Button) findViewById(R.id.login);
        reg_page = (Button) findViewById(R.id.register_page);
        incorrect_uname = (TextView) findViewById(R.id.incorrect_uname);
        request_processing = (TextView) findViewById(R.id.request_processing);

        loginStatus(false,Login.this);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mqttHelper.subscribe(client);
                stringUname = uname.getText().toString();
                stringPassword = password.getText().toString();

                mqttHelper.publish(client,"Client","login,"+stringUname+','+stringPassword);
                incorrect_uname.setText("");
                request_processing.setText("Request processing, Please wait . . .");

                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {

                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {

                        callback = new String(message.getPayload());
                        if(callback.equals("Login_Successful")) {
                            if(loginStatus(true,Login.this))
                                Log.d(TAG, "login status true set");
                            if(uname(stringUname,Login.this))
                                Log.d(TAG, "username saved");
                            Toast.makeText(getApplicationContext(),"Username and password has been saved.",Toast.LENGTH_SHORT);
                            finish();
                        }
                        else{
                            request_processing.setText("");
                            incorrect_uname.setText("Incorrect username or password.");
                            uname.setText("");
                            password.setText("");
                            loginStatus(false,Login.this);
                        }
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                });


            }
        });
        reg_page.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Login.this,Register.class);
                startActivityForResult(intent,0);
            }
        });
    }
    public static boolean uname(String uname, Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString("uname_key",uname);
        prefsEditor.apply();
        return true;
    }
    public static boolean loginStatus(boolean loginstatus, Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putBoolean("login_key",loginstatus);
        prefsEditor.apply();
        return true;
    }
}