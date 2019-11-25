package com.actia.mapsapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.util.Log
import kotlinx.android.synthetic.main.activity_config.*


class ConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        val intent = intent
        val lat = intent.getDoubleExtra("lat",0.0)
        val lng= intent.getDoubleExtra("lng",0.0)
        val inCircle = intent.getBooleanExtra("state",false)

        if(lat != 0.0){
            ed1.setText(lat.toString())
        }
        if(lng != 0.0){
            ed2.setText(lng.toString())
        }
        btnAnnuler.setOnClickListener{
            val  myIntent  = Intent(this,MainActivity::class.java)
            myIntent.putExtra("lat", lat)
            myIntent.putExtra("lng", lng)
            myIntent.putExtra("state", inCircle)
            this.startActivity(myIntent)
        }
        btnAjouter.setOnClickListener{
            val myIntent  = Intent(this,MainActivity::class.java)
            val newLat = ed1.text.toString().toDouble()
            val newLng = ed2.text.toString().toDouble()
            val rayon =  ed3.text.toString().toInt()
            myIntent.putExtra("lat", newLat)
            myIntent.putExtra("lng", newLng)
            myIntent.putExtra("rayon", rayon)
            myIntent.putExtra("state", inCircle)
            this.startActivity(myIntent)
        }
    }

}
