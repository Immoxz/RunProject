package com.immoxz.run;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HelloScreen extends AppCompatActivity {
    //useful shit
//    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";

    private Button btnTrain, btnStat, btnHist;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hello_screen);
        final Intent trainIntent = new Intent(this, TrainingActivity.class);
        //setiing buttons
        btnTrain = (Button) findViewById(R.id.btnTrain);
        btnStat = (Button) findViewById(R.id.btnStat);
        btnHist = (Button) findViewById(R.id.btnHist);

        //usage of buttons
        btnTrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //send msg to intent
                //trainIntent.putExtra(EXTRA_MESSAGE, message);
                startActivity(trainIntent);

            }
        });
        btnStat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        btnHist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

    }
}
