package com.example.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "Auth"

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

/**
 * Silent / returning-user auto sign-in via Credential Manager GetGoogleIdOption.
 */
suspend fun attemptAutoSignIn(context: Context): Result<FirebaseUser> {
    return try {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context = context, request = request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Firebase user missing after credential exchange"))
            Result.success(user)
        } else {
            Result.failure(IllegalStateException("Unexpected credential type returned"))
        }
    } catch (e: NoCredentialException) {
        Log.d(AUTH_TAG, "No previously authorized Google credential found for silent sign-in")
        Result.failure(e)
    } catch (e: GetCredentialCancellationException) {
        Log.w(AUTH_TAG, "Auto sign-in cancelled by user", e)
        Result.failure(e)
    } catch (e: GetCredentialException) {
        Log.w(AUTH_TAG, "Auto sign-in failed", e)
        Result.failure(e)
    } catch (e: Exception) {
        Log.e(AUTH_TAG, "Unexpected error during auto sign-in", e)
        Result.failure(e)
    }
}

/**
 * Interactive "Sign in with Google" bottom-sheet flow using GetSignInWithGoogleOption.
 */
suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
    return try {
        val credentialManager = CredentialManager.create(context)
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
            context.getString(R.string.default_web_client_id)
        ).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        val result = credentialManager.getCredential(context = context, request = request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Firebase sign-in completed with null user"))
            Result.success(user)
        } else {
            Result.failure(IllegalStateException("Unsupported credential type: ${credential.type}"))
        }
    } catch (e: GetCredentialCancellationException) {
        Log.w(AUTH_TAG, "User cancelled Google Sign-In", e)
        Result.failure(e)
    } catch (e: GetCredentialException) {
        Log.e(AUTH_TAG, "Google Sign-In failed: ${e.errorMessage}", e)
        Result.failure(e)
    } catch (e: Exception) {
        Log.e(AUTH_TAG, "Unexpected Google Sign-In failure", e)
        Result.failure(e)
    }
}

/**
 * Clears active Credential Manager session and signs out from Firebase Auth.
 */
suspend fun signOutGoogle(context: Context) {
    withContext(NonCancellable) {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: ClearCredentialException) {
            Log.w(AUTH_TAG, "Failed to clear credential state cleanly", e)
        } catch (e: Exception) {
            Log.w(AUTH_TAG, "Credential state clear exception", e)
        }
        Firebase.auth.signOut()
    }
}
