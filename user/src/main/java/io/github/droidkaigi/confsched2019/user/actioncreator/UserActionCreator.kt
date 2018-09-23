package io.github.droidkaigi.confsched2019.user.actioncreator

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import confsched2018.droidkaigi.github.io.dispatcher.Dispatcher
import io.github.droidkaigi.confsched2019.ext.android.await
import io.github.droidkaigi.confsched2019.ext.android.toCoroutineScope
import io.github.droidkaigi.confsched2019.model.Action
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.launch
import nl.adaptivity.android.coroutines.Maybe.*
import nl.adaptivity.android.coroutines.activityResult
import javax.inject.Inject
import javax.inject.Named

class UserActionCreator @Inject constructor(
        val activity: AppCompatActivity,
        val dispatcher: Dispatcher,
        @Named("defaultFirebaseWebClientId")
        val defaultFirebaseWebClientId: Int
) : CoroutineScope by activity.toCoroutineScope(Dispatchers.Main) {
    val googleSignInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(activity, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(defaultFirebaseWebClientId))
                .requestEmail()
                .build())
    }


    val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    fun setupUser() {
        if (firebaseAuth.currentUser == null) {
            signIn()
        } else {
            dispatcher.launchAndSend(Action.UserRegistered)
        }
    }

    private fun signIn() {
        launch {
            try {
                val activityResult = activity.activityResult(googleSignInClient.signInIntent)
                when (activityResult) {
                    is Error -> {
                        onError(activityResult.e)
                    }
                    is Cancelled -> {
                        onError()
                    }
                    is Ok -> {
                        val resultIntent = activityResult.data as Intent
                        val account = GoogleSignIn.getSignedInAccountFromIntent(resultIntent).await()
                        onSignIn(account)
                    }
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private suspend fun onSignIn(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
//        val user = result.user
        dispatcher.send(Action.UserRegistered)
    }

    private fun onError(e: Exception? = null) {
        e?.printStackTrace()
    }
}
