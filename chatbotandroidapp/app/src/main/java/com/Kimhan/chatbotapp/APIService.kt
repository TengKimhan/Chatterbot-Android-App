package com.Kimhan.chatbotapp

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

// use API to send a request message from input string to get response
// using interface Call
interface APIService {
    @FormUrlEncoded
    @POST("chat")
    fun chatWithTheBit(@Field("chatInput") chatText : String ): Call<ChatResponse>
    // ChatResponse is a data type basically is a body type from postman
}

data class ChatResponse(val chatBotReply: String)