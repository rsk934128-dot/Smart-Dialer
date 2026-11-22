package com.example.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType {
    CREATE,
    UPDATE,
    DELETE,
    LIST,
    GET,
    WRITE
}

object FirestoreErrorHandler {
    private const val TAG = "FirestoreError"

    fun handleFirestoreError(
        error: Throwable,
        operationType: OperationType,
        path: String?,
        auth: FirebaseAuth = FirebaseAuth.getInstance()
    ) {
        val user: FirebaseUser? = auth.currentUser
        val providerArray = JSONArray().apply {
            user?.providerData?.forEach { info ->
                put(
                    JSONObject().apply {
                        put("providerId", info.providerId)
                        put("displayName", info.displayName ?: JSONObject.NULL)
                        put("email", info.email ?: JSONObject.NULL)
                        put("photoUrl", info.photoUrl?.toString() ?: JSONObject.NULL)
                    }
                )
            }
        }

        val authInfo = JSONObject().apply {
            put("userId", user?.uid ?: JSONObject.NULL)
            put("email", user?.email ?: JSONObject.NULL)
            put("emailVerified", user?.isEmailVerified ?: false)
            put("isAnonymous", user?.isAnonymous ?: false)
            put("providerInfo", providerArray)
        }

        val payload = JSONObject().apply {
            put("error", error.message ?: error.toString())
            put("operationType", operationType.name.lowercase())
            put("path", path ?: JSONObject.NULL)
            put("authInfo", authInfo)
        }

        Log.e(TAG, "Firestore Error [${operationType.name}] at '$path': $payload", error)
    }
}
