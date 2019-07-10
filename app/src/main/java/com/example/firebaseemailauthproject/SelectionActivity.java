package com.example.firebaseemailauthproject;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class SelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);
    }

    public void intentMainActivity(View view){
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }

    public void intentPhoneActivity(View view){
        startActivity(new Intent(getApplicationContext(), PhoneActivity.class));
    }
}
