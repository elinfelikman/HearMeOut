package com.example.hearmeout

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

/**
 * LoginScreen: Handles user authentication via Firebase Auth.
 * Includes "Remember Me" functionality to persist login state across sessions
 * and a localized UI supporting both Hebrew and English.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    // UI Localization state
    var isHebrew by remember { mutableStateOf(true) }
    val layoutDirection = if (isHebrew) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Input and Error states
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Persistence State: Remember Me preference
    var rememberMe by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // Strings localization logic
    val title = if (isHebrew) "התחברות למערכת" else "System Login"
    val emailLabel = if (isHebrew) "אימייל" else "Email"
    val passLabel = if (isHebrew) "סיסמה (לפחות 6 תווים)" else "Password (min 6 chars)"
    val rememberMeText = if (isHebrew) "זכור אותי בטלפון זה" else "Remember me on this device"
    val loginBtn = if (isHebrew) "התחבר" else "Login"
    val registerBtn = if (isHebrew) "משתמש חדש? לחץ כאן להרשמה" else "New user? Click here to register"
    val emptyError = if (isHebrew) "נא להזין אימייל וסיסמה" else "Please enter email and password"
    val registerPromptError = if (isHebrew) "הזן אימייל וסיסמה ואז לחץ כאן להרשמה" else "Enter email and password, then click here to register"

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF001F3F))
        ) {
            // Language Switcher Button
            Button(
                onClick = { isHebrew = !isHebrew },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40E0D0)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .padding(top = 24.dp)
            ) {
                Icon(Icons.Default.Translate, contentDescription = "Translate", tint = Color(0xFF001F3F), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isHebrew) "EN" else "עב", color = Color(0xFF001F3F), fontWeight = FontWeight.Bold)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Brand Assets
                Image(
                    painter = painterResource(id = R.drawable.hearmeout_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(120.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("HearMeOut", color = Color(0xFF40E0D0), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Text(title, color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp)
                Spacer(modifier = Modifier.height(40.dp))

                // Input Fields
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(emailLabel, color = Color.LightGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF40E0D0),
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(passLabel, color = Color.LightGray) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF40E0D0),
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Persistence Toggle: Utilizing SharedPreferences for "Remember Me"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF40E0D0),
                            uncheckedColor = Color.LightGray,
                            checkmarkColor = Color(0xFF001F3F)
                        )
                    )
                    Text(text = rememberMeText, color = Color.White, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = Color(0xFFD32F2F), fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator(color = Color(0xFF40E0D0))
                } else {
                    // Sign-In Action: Firebase Auth Integration
                    Button(
                        onClick = {
                            if (email.isNotEmpty() && password.isNotEmpty()) {
                                isLoading = true
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            // Persist login preference to local storage
                                            val prefs = context.getSharedPreferences("HearMeOutPrefs", Context.MODE_PRIVATE)
                                            prefs.edit().putBoolean("remember_me", rememberMe).apply()
                                            onLoginSuccess()
                                        } else {
                                            val errorPrefix = if (isHebrew) "שגיאה: " else "Error: "
                                            errorMessage = errorPrefix + task.exception?.localizedMessage
                                            isLoading = false
                                        }
                                    }
                            } else {
                                errorMessage = emptyError
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40E0D0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(loginBtn, color = Color(0xFF001F3F), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sign-Up Action
                    TextButton(onClick = {
                        if (email.isNotEmpty() && password.isNotEmpty()) {
                            isLoading = true
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val prefs = context.getSharedPreferences("HearMeOutPrefs", Context.MODE_PRIVATE)
                                        prefs.edit().putBoolean("remember_me", rememberMe).apply()
                                        onLoginSuccess()
                                    } else {
                                        val errorPrefix = if (isHebrew) "שגיאה: " else "Error: "
                                        errorMessage = errorPrefix + task.exception?.localizedMessage
                                        isLoading = false
                                    }
                                }
                        } else {
                            errorMessage = registerPromptError
                        }
                    }) {
                        Text(registerBtn, color = Color(0xFF40E0D0))
                    }
                }
            }
        }
    }
}