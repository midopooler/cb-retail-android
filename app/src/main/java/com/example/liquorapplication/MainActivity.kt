package com.example.liquorapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.couchbase.lite.CouchbaseLite
import com.example.liquorapplication.ui.theme.LiquorApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Couchbase Lite
        CouchbaseLite.init(this)
        
        // Enable Vector Search
        try {
            CouchbaseLite.enableVectorSearch()
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to enable vector search: ${e.message}")
        }
        
        // Note: Vector Search will be added later once dependency is properly configured
        
        enableEdgeToEdge()
        setContent {
            LiquorApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LandingScreen()
                }
            }
        }
    }
} 