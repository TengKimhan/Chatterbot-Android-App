package com.Kimhan.chatbotapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {
    val adapterChatBot = AdapterChatBot(this)
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase

    private companion object{
        private const val TAG = "MainActivity"
    }
    // add override method for menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main, menu);

        return true;
    }

    // add override method for select item for on click listener
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //var showMessage = ""
        // like switch case
        when(item.itemId)
        {
            R.id.profile-> {
                Log.i(TAG, "Profile")
                startActivity(Intent(this,ProfileActivity::class.java))
                finish()
                return true
            }
            R.id.logout-> {
                Log.i(TAG, "Logout")
                auth.signOut() // when logout we need to tell firebase that user is logout
                startActivity(Intent(this , LoginActivity::class.java))
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.title = "Chatterbot Dialog"

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()
        rvChatList.adapter = adapterChatBot

        Log.i(TAG, "onCreate: " + auth.currentUser?.uid.toString())

        // access to message reference of current user by uid
        val dbRef = db.getReference("/messages/${auth.currentUser?.uid}")
        // fetch message from that reference
        dbRef.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for(ds:DataSnapshot in snapshot.children){
                    val chatmessage = ds.getValue(ChatModel::class.java)
                    Log.i(TAG, "onDataChange: " + chatmessage?.isBot.toString() + " Message: " + chatmessage?.chat.toString() )
                    if (chatmessage != null) {
                        adapterChatBot.addChatToList(chatmessage)
                    }
                }
                //rvChatList.adapter = adapterChatBot
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

        // access with retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.4:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // retrofit create api service from APIService class
        val apiService = retrofit.create(APIService::class.java)

        // chat list is a linear layout
        rvChatList.layoutManager = LinearLayoutManager(this)

        // when user click on send btn
        btnSend.setOnClickListener {
            // check if user send chat with nothing
            if(etChat.text.isNullOrEmpty()){
                Toast.makeText(this@MainActivity, "Please enter a text", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // etChat is a chat input from user
            // store chat to list
            adapterChatBot.addChatToList(ChatModel(etChat.text.toString(), 0, 0))
            // save to firebase
            val msg = ChatModel(etChat.text.toString(), 0, 0)
            val user = auth.currentUser // current user
            val uid = user?.uid
            val ref = db.getReference("/messages/$uid").push()
            ref.setValue(msg)
            Log.i(TAG, "onCreate: " + uid.toString())

            // in APIService has method chatWithTheBot
            // and this method take input string from user to get response
            apiService.chatWithTheBit(etChat.text.toString()).enqueue(callBack) // use enque to get response
            Log.i(TAG, "Arrived")
            etChat.text.clear() // remove spans the message that in on the tab with button
        }

        rvChatList.adapter = adapterChatBot
    }

    // call back function to get the response
    private val callBack = object  : Callback<ChatResponse>{

        override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {

            Log.i(TAG, "Outside success")
            if(response.isSuccessful &&  response.body()!= null){
                Log.i(TAG, "Inside success")
                adapterChatBot.addChatToList(ChatModel(response.body()!!.chatBotReply, 1, 1))
                val msg = ChatModel(response.body()!!.chatBotReply, 1, 1)
                val user = auth.currentUser // current user
                val uid = user?.uid
                val ref = db.getReference("/messages/$uid").push()
                ref.setValue(msg)
            }else{
                Toast.makeText(this@MainActivity, "Something went wrong", Toast.LENGTH_LONG).show()
            }
        }

        override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
            Log.i(TAG, "On Failure")
            Toast.makeText(this@MainActivity, "Network Errors", Toast.LENGTH_LONG).show()
        }

    }
}