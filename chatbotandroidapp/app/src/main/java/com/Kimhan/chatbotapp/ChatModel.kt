package com.Kimhan.chatbotapp

data class ChatModel(var chat: String, var isBot: Int = 0, var viewType: Int) {
    constructor() : this("", 0, 0)
}