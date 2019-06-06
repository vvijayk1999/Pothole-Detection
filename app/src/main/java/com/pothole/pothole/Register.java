package com.pothole.pothole;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class Register extends AppCompatActivity {
    EditText etUname,etPassword;
    String name, email, password;
    Button btnRegister;
    TextView request_processing,uname_taken;
    //////////////////////////MQTT Connect options/////////////////////////////
    String URL = "tcp://m16.cloudmqtt.com";
    String portNumber = "13941";
    String userName = "vbqcvuri";
    String mqpassword = "tgt22oGl7EwE";
    ///////////////////////////////////////////////////////////////////////////
    String clientId = MqttClient.generateClientId();
    MqttAndroidClient client;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        etUname = (EditText) findViewById(R.id.uname);
        etPassword = (EditText) findViewById(R.id.password);
        btnRegister = (Button) findViewById(R.id.register);
        request_processing = (TextView) findViewById(R.id.request_processing);
        uname_taken = (TextView) findViewById(R.id.uname_taken);

        ////////////////////////////////  MQTT //////////////////////////////////////////////////////////////
        client = new MqttAndroidClient(this.getApplicationContext(), URL + ":" + portNumber, clientId);
        MQTTHelper mqttHelper = new MQTTHelper();
        mqttHelper.connect(client, userName, mqpassword);
        //mqttHelper.subscribe(client);
        ////////////////////////////////////////////////////////////////////////////////////////////////////

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = etUname.getText().toString();
                password = etPassword.getText().toString();
                mqttHelper.publish(client,"Client","register,"+name+','+password);
                uname_taken.setText("");
                request_processing.setText("Request processing, Please wait . . .");
                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {

                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {

                        String callback = new String(message.getPayload());
                        if(callback.equals("Username_Taken")) {
                            request_processing.setText("");
                            etPassword.setText("");
                            etUname.setText("");
                            uname_taken.setText("Username taken");
                        }
                        else{
                            finish();
                        }
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                });
            }
        });


    }
}