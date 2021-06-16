package com.Kimhan.chatbotapp

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Patterns
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_sign_up.*

@Suppress("NAME_SHADOWING")
class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    lateinit var userName: String // user name
    var check = false // for checking if user select their photo
    var selectedPhotoURI: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        // user click on already have an account
        alreadyHaveAccount.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // when user click on sign up button
        btn_sign_up.setOnClickListener {
            // start sign up
            signUpUser()
        }

        // user click on select photo button
        selectphoto_button_register.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }


    }

    // this function is called when start startActivityForResult (selectphoto_button_register)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 0 && resultCode == Activity.RESULT_OK && data != null){
            selectedPhotoURI = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoURI)
            selectphoto_imageview_register.setImageBitmap(bitmap)
            selectphoto_button_register.alpha = 0f
            check = true

        }
    }

    private fun signUpUser()
    {
        // check if user is not select their image
        if(check == false){
            Toast.makeText(this,"Please upload your image", Toast.LENGTH_SHORT).show()
            return
        }

        // check if photo uri is null
        if(selectedPhotoURI == null) {
            Toast.makeText(this, "No Photo URI", Toast.LENGTH_SHORT).show()
            return
        }

        // check user name is empty
        if(tv_user_name.text.toString().isEmpty()){
            tv_user_name.error = "Please enter user name"
            tv_user_name.requestFocus()
            return
        }

        // check email is empty
        if(tv_email.text.toString().isEmpty())
        {
            tv_email.error = "Please enter email"
            tv_email.requestFocus()
            return // if any error occurs just stop
        }

        // check email is valid
        if(!Patterns.EMAIL_ADDRESS.matcher(tv_email.text.toString()).matches())
        {
            tv_email.error = "Please enter valid email"
            tv_email.requestFocus()
            return
        }

        // check password is empty
        if(tv_password.text.toString().isEmpty())
        {
            tv_password.error = "Please enter password"
            tv_password.requestFocus()
            return
        }

        // store userName with user name text
        userName = tv_user_name.text.toString()
        // after that if no error occur then register the user
        // we need to use createUserWithEmailAndPassword() to create
        auth.createUserWithEmailAndPassword(tv_email.text.toString(), tv_password.text.toString())
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    // take current user object from authentification
                    val user = auth.currentUser

                    // send email verification
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    baseContext, "Email has been sent",
                                    Toast.LENGTH_SHORT).show()

                                val uid = user.uid // get user id

                                //add user profile image into firebase storage with user uid
                                val imageRef = storage.getReference("/user_image/$uid") // ref in firebase storage
                                val userImageProfileRef = db.getReference("users/$uid/profile").push() // ref in firebase database
                                imageRef.putFile(selectedPhotoURI!!).addOnSuccessListener {
                                    imageRef.downloadUrl.addOnSuccessListener {
                                        userImageProfileRef.setValue(it.toString())
                                    }
                                }

                                // add user name to firebase in usersname folder
                                val userNameRef = db.getReference("/users/$uid/usersname").push()
                                userNameRef.setValue(userName)

                                // add user to database
                                val ref = db.getReference("/users/$uid").push() // push reference
                                ref.setValue(user)

                                // start new activity to login activity
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                        }

                } else {
                    // If sign up fails, display a message to the user.
                    Toast.makeText(baseContext, "Sign up failed.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

}