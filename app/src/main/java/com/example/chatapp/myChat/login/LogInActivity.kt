package com.example.chatapp.myChat.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp.R
import com.example.chatapp.myChat.mainPage.ChatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class LogInActivity : AppCompatActivity() {
    private lateinit var phoneNumberRequest: EditText
    private lateinit var generateCodeRequestButton: Button
    private lateinit var generatedVerificationCode: EditText
    private lateinit var verificationCallBack: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    var mVerificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)
        phoneNumberRequest = phone_number_request
        generateCodeRequestButton = generate_code_button
        generatedVerificationCode = generated_code
        verificationCallBack = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                signInWithFirebaseAuthCredential(phoneAuthCredential)
            }
            override fun onVerificationFailed(firebaseException: FirebaseException) {
                Toast.makeText(applicationContext, firebaseException.message, Toast.LENGTH_SHORT).show()
            }
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationId, token)
                mVerificationId = verificationId
                generatedVerificationCode.visibility = View.VISIBLE
                generateCodeRequestButton.text = getString(R.string.submit_code)
                phoneNumberRequest.visibility= View.GONE
            }
        }
        generateCodeRequestButton.setOnClickListener {
            if (mVerificationId != null) {
                verifyPhoneNumberWithCode()
            } else
                startPhoneNumberVerification()
        }
    }

    private fun startPhoneNumberVerification() {
        if (phoneNumberRequest.text.isNotEmpty()) {
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumberRequest.text.toString(), 60, TimeUnit.SECONDS, this, verificationCallBack)
        }
    }

    private fun signInWithFirebaseAuthCredential(phoneAuthCredential: PhoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(phoneAuthCredential).addOnCompleteListener {
            val currentAuthenticatedUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
            if (currentAuthenticatedUser != null) {
                val userDatabaseReference = FirebaseDatabase.getInstance().reference
                    .child("user").child(currentAuthenticatedUser.uid)
                userDatabaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            val userCredentialMap = HashMap<String, Any>()
                            userCredentialMap["phoneNumber"] = currentAuthenticatedUser.phoneNumber.toString()
                            userCredentialMap["userName"] = currentAuthenticatedUser.phoneNumber.toString()
                            userDatabaseReference.updateChildren(userCredentialMap)
                        }
                        userIsLoggedIn()
                    }
                    override fun onCancelled(databaseError: DatabaseError) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }
                })
            }
        }
    }

    private fun userIsLoggedIn() {
        FirebaseAuth.getInstance().currentUser
        startActivity(Intent(applicationContext, ChatActivity::class.java))
        finish()
    }

    private fun verifyPhoneNumberWithCode() {
        if (mVerificationId != null && generatedVerificationCode.text.isNotEmpty() ) {
            val phoneCredential: PhoneAuthCredential = PhoneAuthProvider
                .getCredential(mVerificationId!!, generatedVerificationCode.text.toString())
            signInWithFirebaseAuthCredential(phoneCredential)
        }
    }
}