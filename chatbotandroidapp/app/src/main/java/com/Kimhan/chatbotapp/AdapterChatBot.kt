package com.Kimhan.chatbotapp

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.from_bot_chat.view.*
import kotlinx.android.synthetic.main.to_bot_chat.view.*

class AdapterChatBot(context: android.content.Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val list = ArrayList<ChatModel>()

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase

    private companion object{
        private const val TAG = "AdapterChatBot"
        const val VIEW_TYPE_BOT = 1
        const val VIEW_TYPE_HUMAN = 0
    }

    private val context: android.content.Context = context
    // view holder class
    // to hold each item in recycler view
    // each item has chat and boolean bot or not to represent chat of user or bot
    inner class HumanViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        fun bind(chat: ChatModel) = with(itemView) {
            if(chat.isBot == 0) {
                tv_user_chat.text = chat.chat

                Log.i(TAG, "Not Bot from firebase: " + tv_user_chat.text.toString())

                auth = FirebaseAuth.getInstance()
                db = FirebaseDatabase.getInstance()

                val currentUseruid = auth.currentUser?.uid
                val proRef = db.getReference("users/${currentUseruid}/profile")
                proRef.addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for(ds: DataSnapshot in snapshot.children){
                            Picasso.get().load(ds.getValue().toString()).into(user_imageView)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })
            }
        }
    }

    inner class BotViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        fun bind(chat: ChatModel) = with(itemView) {
            if(chat.isBot == 1) {
                tv_bot_chat.text = chat.chat
                Log.i(TAG, "Bot from firebase: " + tv_bot_chat.text.toString())

            }
        }
    }

    // get view holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder{
        if (viewType == VIEW_TYPE_HUMAN) {
            return HumanViewHolder(
                LayoutInflater.from(context).inflate(R.layout.to_bot_chat, parent, false)
            )
        } else {
            return BotViewHolder(
                LayoutInflater.from(context).inflate(R.layout.from_bot_chat, parent, false)
            )
        }
    }

    // set view holder
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // holder.bind(list[position])
        if (list[position].viewType == 1) {
            (holder as BotViewHolder).bind(list[position])
        } else {
            (holder as HumanViewHolder).bind(list[position])
        }
    }

    override fun getItemCount() = list.size

    override fun getItemViewType(position: Int): Int {
        return list[position].viewType
    }

    // to store message of bot and user into list
    fun addChatToList(chat: ChatModel) {
        list.add(chat)
        notifyDataSetChanged()
    }

}


