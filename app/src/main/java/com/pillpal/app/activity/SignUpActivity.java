package com.pillpal.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pillpal.app.R;
import com.pillpal.app.databinding.ActivitySignUpBinding;
import com.pillpal.app.model.User;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySignUpBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupRealTimeValidation();

        binding.signupTxtSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                startActivity(intent);
            }
        });

        binding.signupBtnSignup.setOnClickListener(view -> {

            if (validateInputs()) {
                String name = binding.signupInputFullname.getText().toString().trim();
                String email = binding.signupInputEmail.getText().toString().trim();
                String password = binding.signupInputPassword.getText().toString().trim();

                firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {

                                String uid = task.getResult().getUser().getUid();

                                User user = User.builder()
                                        .uid(uid)
                                        .name(name)
                                        .email(email).build();

                                firebaseFirestore.collection("users")
                                        .document(uid)
                                        .set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                Toast.makeText(SignUpActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                                                finish();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {

                                            }
                                        });
                            } else {
                                String error = task.getException() != null ? task.getException().getMessage() : "Registration Failed";
                                Toast.makeText(SignUpActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

    }

    // Method for Key-Type (Real-time) Validation
    private void setupRealTimeValidation() {
        // Full Name Validation
        binding.signupInputFullname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    binding.signupInputFullname.setError("Name cannot be empty");
                } else {
                    binding.signupInputFullname.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Email Validation
        binding.signupInputEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String email = s.toString().trim();
                if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    binding.signupInputEmail.setError("Invalid email format");
                } else {
                    binding.signupInputEmail.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Password Validation
        binding.signupInputPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0 && s.length() < 6) {
                    binding.signupInputPassword.setError("Must be at least 6 characters");
                } else {
                    binding.signupInputPassword.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    // Method to check validation when button is clicked
    private boolean validateInputs() {
        String name = binding.signupInputFullname.getText().toString().trim();
        String email = binding.signupInputEmail.getText().toString().trim();
        String password = binding.signupInputPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            binding.signupInputFullname.setError("Full name is required!");
            binding.signupInputFullname.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            binding.signupInputEmail.setError("Email is required!");
            binding.signupInputEmail.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.signupInputEmail.setError("Invalid email format!");
            binding.signupInputEmail.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            binding.signupInputPassword.setError("Password must be at least 6 characters!");
            binding.signupInputPassword.requestFocus();
            return false;
        }
        return true;
    }
}