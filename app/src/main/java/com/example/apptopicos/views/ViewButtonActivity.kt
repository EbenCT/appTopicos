package com.example.apptopicos.views

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.apptopicos.R

class ViewButtonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)

        setContentView(R.layout.activity_view_button)
    }

}
