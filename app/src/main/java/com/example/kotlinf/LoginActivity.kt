package com.example.kotlinf

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var request: GetCredentialRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already logged in
        if (isUserLoggedIn()) {
            navigateToMain()
            return
        }

        setContentView(R.layout.activity_login)

        auth = Firebase.auth
        credentialManager = CredentialManager.create(this)

        // Check if Firebase user is already signed in
        if (auth.currentUser != null) {
            // User is signed in Firebase but not in SharedPrefs, save the state
            saveLoginState(auth.currentUser?.displayName, auth.currentUser?.email)
            navigateToMain()
            return
        }

        // Instantiate a Google sign-in request
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()

        // Create the Credential Manager request
        request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    fun handleSignIn(view: View) {
        startSignIn()
    }

    private fun startSignIn() {
        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@LoginActivity, request)
                val credential = result.credential
                handleSignIn(credential)  // Process credential
                Log.d("fire89", "Google Sign In request successful")
            } catch (e: Exception) {
                Log.e("fire89", "Sign-in failed", e)
                showErrorMessage("Sign-in failed: ${e.message}")
            }
        }
    }

    private fun handleSignIn(credential: Credential?) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                firebaseAuthWithGoogle(idToken)
                Log.d("fire89", "Google Sign In token received")
            } catch (e: Exception) {
                Log.e("fire89", "Google Sign In failed", e)
                showErrorMessage("Google Sign In failed: ${e.message}")
            }
        } else {
            Log.w("fire89", "Credential is not of type Google ID!")
            showErrorMessage("Invalid credential type")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(firebaseCredential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d("fire89", "Firebase authentication successful")

                    // Save login state to SharedPreferences
                    saveLoginState(user?.displayName, user?.email)

                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Welcome ${user?.displayName}",
                        Snackbar.LENGTH_SHORT
                    ).show()

                    // Navigate to MainActivity after successful login
                    navigateToMain()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    showErrorMessage("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPrefs = getSharedPreferences("MyFamilyPrefs", Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean("isLoggedIn", false)
    }

    private fun saveLoginState(userName: String?, userEmail: String?) {
        val sharedPrefs = getSharedPreferences("MyFamilyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putBoolean("isLoggedIn", true)
        editor.putLong("loginTime", System.currentTimeMillis())

        // Save user information if available
        userName?.let { editor.putString("userName", it) }
        userEmail?.let { editor.putString("userEmail", it) }

        editor.apply()
        Log.d("fire89", "Login state saved to SharedPreferences")
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }
}