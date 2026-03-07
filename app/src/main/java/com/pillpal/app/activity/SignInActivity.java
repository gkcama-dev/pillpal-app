package com.pillpal.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pillpal.app.R;
import com.pillpal.app.databinding.ActivitySignInBinding;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onStart() {
        super.onStart();

        if (firebaseAuth.getCurrentUser() != null) {
            // User is already logged in, skip Login screen
            updateUI(firebaseAuth.getCurrentUser());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySignInBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();

        setupRealTimeValidation();

        binding.signinSignupTxt.setOnClickListener(view -> {
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
            finish();
        });

        binding.signinForgotPassword.setOnClickListener(view -> {
            Intent intent = new Intent(SignInActivity.this, ResetPasswordActivity.class);
            startActivity(intent);
        });

        binding.signinBtnSignin.setOnClickListener(view -> {
            if (validateInputs()) {
                String email = binding.signinInputEmail.getText().toString().trim();
                String password = binding.signinInputPassword.getText().toString().trim();

                firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    updateUI(firebaseAuth.getCurrentUser());
                                } else {
                                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                                    Toast.makeText(SignInActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

    }


    // Method to check validation when button is clicked
    private boolean validateInputs() {
        String email = binding.signinInputEmail.getText().toString().trim();
        String password = binding.signinInputPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.signinInputEmail.setError("Email is required!");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.signinInputEmail.setError("Invalid email format!");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.signinInputPassword.setError("Password is required!");
            return false;
        }
        if (password.length() < 6) {
            binding.signinInputPassword.setError("Password must be at least 6 characters!");
            return false;
        }
        return true;
    }

    // Method for Key-Type Validation
    private void setupRealTimeValidation() {
        binding.signinInputEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int charStart, int before, int count) {
                String email = s.toString().trim();
                if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    binding.signinInputEmail.setError("Invalid email format");
                } else {
                    binding.signinInputEmail.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.signinInputPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0 && s.length() < 6) {
                    binding.signinInputPassword.setError("Must be at least 6 characters");
                } else {
                    binding.signinInputPassword.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void updateUI(FirebaseUser user) {
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}