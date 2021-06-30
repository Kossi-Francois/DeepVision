package com.domine.mundi.unbwebview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import com.google.android.material.slider.Slider

class OptionsActivity : AppCompatActivity() {


    val paramFN = "myPreferences"
    val paramEndPoint = "apiEndPoint"
    val paramStreamUrl = "streamUrl"
    val paramVocActif = "vocCom"
    val paramLang = "language"
    val paramVocOverLap ="voiceOverlap"


    //var streamUrl = "www.google.com"
    var streamUrl = "http://192.168.25.1:8080/?action=stream"
    var endPointUrl =""
    var langChoosen = "English"
    var vocComIsActif = true
    var overlapFactor = 0.8f


    lateinit var editEndPoint: EditText
    lateinit var editUrl: EditText
    lateinit var saveOptions: Button
    lateinit var switcherLang: Spinner
    lateinit var switchVocComand: Switch
    lateinit var oveLapFactorSlider: Slider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)
        loadParam()

        editEndPoint = findViewById(R.id.editText_endPoint)
        editEndPoint.setText(endPointUrl)

        editUrl = findViewById(R.id.editText_streamUrl)
        editUrl.setText(streamUrl)

        saveOptions = findViewById(R.id.button_save_options)
        saveOptions.setOnClickListener { saveParameters() }

        switcherLang = findViewById(R.id.switch_lang)


        switchVocComand =findViewById(R.id.switch_voc_commznd)
        switchVocComand.isChecked = vocComIsActif


        oveLapFactorSlider = findViewById(R.id.overlapfactor)
        oveLapFactorSlider.value = overlapFactor

    }




    override fun onStop() {
        super.onStop()
        saveParameters()
    }


    private fun saveParameters() {
        val settings = getSharedPreferences(paramFN, 0)
        val editor = settings.edit()
        editor.putString(paramEndPoint, editEndPoint.text.toString())
        editor.putString(paramStreamUrl, editUrl.text.toString())
        editor.putBoolean(paramVocActif, switchVocComand.isChecked)
        editor.putString(paramLang, switcherLang.selectedItem.toString())
        editor.putFloat(paramVocOverLap, oveLapFactorSlider.value)

        editor.commit()



    }


    fun loadParam(){
        val settings = getSharedPreferences(paramFN, 0)

        endPointUrl = settings.getString(paramEndPoint, "api endpoit").toString()
        streamUrl = settings.getString(paramStreamUrl, streamUrl).toString()
        langChoosen = settings.getString(paramLang, "English").toString()
        vocComIsActif = settings.getBoolean(paramVocActif, true)
        overlapFactor = settings.getFloat(paramVocOverLap, 0.8f)



    }


}