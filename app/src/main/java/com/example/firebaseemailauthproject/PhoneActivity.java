package com.example.firebaseemailauthproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
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

    // 파이어베이스 인증 객체 생성
    private FirebaseAuth firebaseAuth;

    private TextView showPhoneState;
    private String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        // 파이어베이스 인증 객체 선언
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.setLanguageCode("kr");

        init();
    }

    private void init() {
        showPhoneState = findViewById(R.id.notify_phone_state);
    }

    public void readPhoneState(View view) {
        if (checkTargetPermission(PHONE_READ_STATE)) {
            getPhoneState();
        } else {
            requestTargetPermission();
        }
    }

    public void sendPhoneAuth(View view){
        String phoneNumber = showPhoneState.getText().toString();

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        // This callback will be invoked in two situations:
                        // 1 - Instant verification. In some cases the phone number can be instantly
                        //     verified without needing to send or enter a verification code.
                        // 2 - Auto-retrieval. On some devices Google Play services can automatically
                        //     detect the incoming verification SMS and perform verification without
                        //     user action.
                        Log.d(TAG, "onVerificationCompleted:" + credential);

                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        // This callback is invoked in an invalid request for verification is made,
                        // for instance if the the phone number format is not valid.
                        Log.w(TAG, "onVerificationFailed", e);

                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            // Invalid request
                            // ...
                        } else if (e instanceof FirebaseTooManyRequestsException) {
                            // The SMS quota for the project has been exceeded
                            // ...
                        }
                    }

                    @Override
                    public void onCodeSent(String vid,
                                           PhoneAuthProvider.ForceResendingToken token) {
                        // The SMS verification code has been sent to the provided phone number, we
                        // now need to ask the user to enter the code and then construct a credential
                        // by combining the code with a verification ID.
                        Log.d(TAG, "onCodeSent:" + vid);
                        verificationId = vid;
                        // ...
                    }
                });        // OnVerificationStateChangedCallbacks
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");

                            FirebaseUser user = task.getResult().getUser();
                            Toast.makeText(PhoneActivity.this, "인증 성공", Toast.LENGTH_SHORT).show();
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                            }
                        }
                    }
                });
    }


    public void commitPhoneAuth(View view){
        EditText editText = findViewById(R.id.auth_sms_edit);
        String s_auth = editText.getText().toString();
        if(s_auth.isEmpty()){
            return;
        }
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, s_auth);
    }

    @SuppressLint("MissingPermission")
    private void getPhoneState() {
        //already check Permissions to readPhoneState().
        TelephonyManager telManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        String phoneNumber = telManager.getLine1Number();
        if(phoneNumber.startsWith("+82")){
            phoneNumber = phoneNumber.replace("+82", "0");
        }
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
