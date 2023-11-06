package com.evisax.MobileApp.flutter_twi_login

import android.util.Log
import com.google.android.gms.common.util.JsonUtils
import com.google.android.gms.common.util.MapUtils
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.JSONUtil
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterFragmentActivity() {

    private val CHANNEL = "firebase.auth.plugin/twitter"
    private val provider = OAuthProvider.newBuilder("twitter.com")
    private var firebaseAuth: FirebaseAuth? = null
    private var pendingResultTask : Task<AuthResult>? = null

    override fun onStart() {
        FirebaseApp.initializeApp(applicationContext)
        firebaseAuth = FirebaseAuth.getInstance()
        super.onStart()
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
                call, result ->
            if (call.method == "twitterLogin") {
                twitter {
                    //result.success("Success");
                    if (it["success"] as Boolean){
                        result.success(it)
                    }else{
                        result.error(it["success"].toString(), it["message"].toString(), it["error"].toString())
                    }
                }
            } else {
                result.notImplemented()
            }
        }
    }

    private fun twitter(callBack: (Map<String, Any?>)->Unit){

        if (pendingResultTask != null) {
            // There's something already here! Finish the sign-in for your user.
            pendingResultTask
                ?.addOnSuccessListener {
                    buildResult(it){map->
                        callBack(map)
                    }

                }
                ?.addOnFailureListener {
                    val map =  mutableMapOf<String,Any?>()
                    // Handle failure.
                    map["success"] = false
                    map["message"] = it.message
                    map["error"] = it.toString()
                    callBack(map)
                }
        } else {
            pendingResultTask =   firebaseAuth
                ?.startActivityForSignInWithProvider(this, provider.build())
                ?.addOnSuccessListener {
                    buildResult(it){map->
                        callBack(map)
                    }
                }
                ?.addOnFailureListener {
                    val map =  mutableMapOf<String,Any?>()
                    // Handle failure.
                    Log.d("twitter", "${it.message}")
                    map["success"] = false
                    map["message"] = it.message
                    map["error"] = it.toString()
                    callBack(map)
                }
        }

    }

    private fun buildResult(authResult: AuthResult, onTokenReceived:(Map<String, Any?>)->Unit){
        val map =  mutableMapOf<String,Any?>()
        // User is signed in.
        // IdP data available in
        Log.d("twitter", "============================")
        authResult.user?.getIdToken(true)?.addOnCompleteListener {

            if(it.isComplete){
                val idToken : String? = it.result.token
                Log.d("twitter", "Id Token::$idToken")
                Log.d("twitter", "Access Token::${((authResult.credential) as OAuthCredential).accessToken}")
                Log.d("twitter", "============================")
                map["profile"] = authResult.additionalUserInfo?.profile.toString()
                map["idToken"] = idToken
                map["accessToken"] = ((authResult.credential) as OAuthCredential).accessToken.toString()
                map["success"] = true
                map["message"] = "Got the data"
                onTokenReceived(map)
            }
        }

    }
}
