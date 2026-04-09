package com.example.streamingtext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.streamingtext.ui.chat.ChatScreen
import com.example.streamingtext.ui.chat.ChatViewModel
import com.example.streamingtext.ui.theme.StreamingTextTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreamingTextTheme {
                val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory)
                ChatScreen(viewModel = chatViewModel)
            }
        }
    }
}