package com.example.firebaseemailauthproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneActivity extends AppCompatActivity {
    private final String TAG = PhoneActivity.class.getSimpleName();
    private final String PHONE_READ_STATE = Manifest.permission.READ_PHONE_STATE;
    private final int SIG_PHONE_READ_STATE = 501;
    private final String KEY_PHONE_STATE = "com.example.firebaseauth.phone_state";

    // 파이어베이스 인증 객체 생성
    private FirebaseAuth firebaseAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallback;
    private PhoneAuthProvider.ForceResendingToken globalToken;

    private TextView showPhoneState;
    private EditText varifyCode;
    private String verificationId;

    // APP + meta > auth key google > block!
    // 010-0000-0000 > +821000000000

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        if(savedInstanceState != null){
            onRestoreInstanceState(savedInstanceState);
        }

        // 파이어베이스 인증 객체 선언
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.setLanguageCode("kr");

        init();
    }

    @Override
    protected void onDestroy() {
        signout();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_PHONE_STATE, showPhoneState.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        showPhoneState.setText(savedInstanceState.getString(KEY_PHONE_STATE));
    }

    private void signout() {
        Log.i(TAG, "Log out!");
        firebaseAuth.signOut();
    }

    private void init() {
        mCallback = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                Log.d(TAG, "Completed Phone Auth");
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.e(TAG, "Failed Phone Auth");
                if(e instanceof FirebaseAuthInvalidCredentialsException){
                    Snackbar.make(findViewById(android.R.id.content), "Failed Credential",
                            Snackbar.LENGTH_SHORT).show();
                }else if(e instanceof FirebaseTooManyRequestsException){
                    Snackbar.make(findViewById(android.R.id.content), "Too much!! Click!!",
                            Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                Log.d(TAG, "onCodeSent");
                verificationId = s;
                globalToken = forceResendingToken;
                Toast.makeText(PhoneActivity.this, "Code Sent!!", Toast.LENGTH_SHORT).show();
            }
        };

        varifyCode = findViewById(R.id.auth_sms_edit);
        showPhoneState = findViewById(R.id.notify_phone_state);
    }

    private void signInWithPhoneAuthCredential(final PhoneAuthCredential phoneAuthCredential) {
        firebaseAuth.signInWithCredential(phoneAuthCredential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Log.d(TAG, "Success!");
                    FirebaseUser user = task.getResult().getUser();

                    if(phoneAuthCredential != null){
                        if(phoneAuthCredential.getSmsCode() != null){
                            varifyCode.setText(phoneAuthCredential.getSmsCode());
                        }
                    }

                    AlertDialog.Builder alert = new AlertDialog.Builder(PhoneActivity.this);
                    alert.setTitle("Phone 인증").setMessage("인증에 성공하셨습니다.\n"+user.getUid());
                    alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
                }else{
                    Log.d(TAG, "Failed");
                    Toast.makeText(PhoneActivity.this, "Auth Failed!!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void readPhoneState(View view) {
        if (checkTargetPermission(PHONE_READ_STATE)) {
            getPhoneState();
        } else {
            requestTargetPermission();
        }
    }

    public void sendPhoneAuth(View view){
        //인증하기 버튼
        String phoneNumber = showPhoneState.getText().toString();

        Log.d(TAG, "phone : " + phoneNumber);

        startPhoneNumberVerification(phoneNumber);
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                mCallback
        );
    }

    @SuppressLint("MissingPermission")
    private void getPhoneState() {
        //already check Permissions to readPhoneState().
        TelephonyManager telManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        String phoneNumber = telManager.getLine1Number();

        if(!phoneNumber.startsWith("+82")){
            String afterNumber = phoneNumber.substring(1);
            phoneNumber = "+82"+afterNumber;
            phoneNumber = phoneNumber.replaceAll("-", "");
        }
//        if(phoneNumber.startsWith("+82")){
//            phoneNumber = phoneNumber.replace("+82", "0");
//        }
        showPhoneState.setText(phoneNumber);
    }

    private void requestTargetPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{PHONE_READ_STATE}, SIG_PHONE_READ_STATE);
        }
    }

    private boolean checkTargetPermission(String phone_read_state) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(phone_read_state) == PackageManager.PERMISSION_GRANTED){
                return true;
            }
            return false;
        }else{
            return true;
        }
    }

    private void notifyPermission() {
        AlertDialog.Builder alert = new AlertDialog.Builder(PhoneActivity.this);
        alert.setTitle("Phone 인증").setMessage("해당 권한이 반드시 필요한 작업입니다.")
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestTargetPermission();
                    }
                }).setNegativeButton("종료", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(PhoneActivity.this,
                        "Phone 인증이 종료되었습니다.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case SIG_PHONE_READ_STATE:
                boolean isALL = true;
                for(int per : grantResults){
                    if(per == PackageManager.PERMISSION_DENIED){
                        isALL = false;
                        break;
                    }
                }
                if(isALL){
                    getPhoneState();
                }else{
                    notifyPermission();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }
}
