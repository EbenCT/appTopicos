package com.example.apptopicos.views

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.apptopicos.R

class ViewButtonActivity : AppCompatActivity() {

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()  // Cierra el Activity
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        setContentView(R.layout.activity_view_button)

        // Registrar el receptor de broadcast para cerrar el Activity
        registerReceiver(closeReceiver, IntentFilter("com.example.apptopicos.CLOSE_ACTIVITY"))
    }

    override fun onDestroy() {
        super.onDestroy()
        // Desregistrar el receptor para evitar fugas de memoria
        unregisterReceiver(closeReceiver)
    }
}
