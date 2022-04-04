package mirea.buryakov.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private EditText editTextEmailAddress;
    private EditText editTextPassword;
    private TextView textViewHaveAnAccount;
    private Button btnRegister;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();
        editTextEmailAddress = findViewById(R.id.editTextEmailAddress);
        textViewHaveAnAccount = findViewById(R.id.textViewRegister);
        editTextPassword = findViewById(R.id.editTextPassword);
        btnRegister = findViewById(R.id.buttonLogin);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickRegister(view);
            }
        });

        textViewHaveAnAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

    }

    public void onClickRegister(View view) {
        String email = editTextEmailAddress.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            return;
        }
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(RegisterActivity.this, "Ошибка " + task.getException(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}