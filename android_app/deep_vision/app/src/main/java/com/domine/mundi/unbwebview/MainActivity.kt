package com.domine.mundi.unbwebview


import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.error.VolleyError
import com.android.volley.request.SimpleMultiPartRequest
import com.android.volley.toolbox.Volley
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {


    val paramFN = "myPreferences"
    val paramEndPoint = "apiEndPoint"
    val paramStreamUrl = "streamUrl"
    val paramVocActif = "vocCom"
    val paramLang = "language"


    //default param values
    var streamUrl = "http://192.168.25.1:8080/?action=stream"
    var endPointUrl =""
    var langChoosen = "English"
    var vocComIsActif = true
    val listLang = arrayListOf<String>("English", "Chinese", "French", "German", "Hindi", "Russian", "Spanish", "Arabic")

    lateinit var myWebView:WebView
    lateinit var editText: EditText
    lateinit var lunchButton: Button
    lateinit var startServiceButton: Button
    lateinit var startOnLiveButton: Button

    lateinit var stopServiceButton: Button
    //val camV1Url = "http://192.168.25.1:8080/?action=stream"
    val camV1Url = "http://192.168.25.1:8080/?action=stream"




    var frameFreq = 10  * 1000

    var tts: TextToSpeech? = null

    var onLiveThread =  Thread()
    var executingCaptionTrhead = Thread()

    var isOnLiveCaption = false




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        tts = TextToSpeech(this, this)
        loadParam()



        myWebView = findViewById(R.id.webview)
        initWebview()





        editText = findViewById(R.id.editUrl)
        editText.setText(camV1Url)

        lunchButton = findViewById(R.id.lanchWeb)
        lunchButton.setOnClickListener {
            loadParam()
            lunchWeb()
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1000)

        }else{
        }



        startServiceButton = findViewById(R.id.start_service)

        startServiceButton.setOnClickListener {

            loadParam()
            takeScreenshotUi()
            //DownloadImageFromInternet().execute(streamUrl)
        }

        startOnLiveButton = findViewById(R.id.start_onLive)

        stopServiceButton = findViewById(R.id.stop_service)



        startOnLiveButton.setOnClickListener {
            loadParam()
            takeMultipleScreen()

            isOnLiveCaption = !isOnLiveCaption

            startOnLiveButton.visibility = View.GONE
            stopServiceButton.visibility = View.VISIBLE

        }



        stopServiceButton.setOnClickListener { //stopService(clService)
            StopOnLiveCaption()

            isOnLiveCaption = !isOnLiveCaption
            stopServiceButton.visibility = View.GONE
            startOnLiveButton.visibility = View.VISIBLE
         }



    }


    fun lunchWeb(){
        //var url = editText.text

        myWebView.loadUrl(streamUrl.toString())
    }


    private fun initWebview(){
        myWebView.settings.javaScriptEnabled = true
        myWebView.settings.domStorageEnabled = true
        myWebView.webViewClient = object : WebViewClient(){

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                //takeMultipleScreen()
            }

            override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)


            }



        }
    }






    fun takeMultipleScreen(){
        var t1 = System.currentTimeMillis()

        onLiveThread =  Thread(Runnable {
            while(true){
                if( (System.currentTimeMillis() - t1 )>= frameFreq){
                    takeScreenshotUi()
                    t1 = System.currentTimeMillis()
                }

            }
        })

        onLiveThread.start()



    }

    fun StopOnLiveCaption(){

        executingCaptionTrhead.interrupt()

        onLiveThread.interrupt()


        tts!!.stop()
    }



    fun takeScreenshotUi(){

        executingCaptionTrhead = Thread(Runnable {
            var bitmap = Bitmap.createBitmap(myWebView.getWidth(), myWebView.getHeight(), Bitmap.Config.ARGB_8888)
            //Bitmap bitmap = Bitmap.createBitmap(500, 700, Bitmap.Config.ARGB_8888);

            val canvas = Canvas(bitmap)
            myWebView.draw(canvas)



            postIMG(bitmap)

        })

        executingCaptionTrhead.start()




    }



    fun saveBitmap(bitmap: Bitmap){
        val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        val PHOTO_EXTENSION = ".jpg"

        val filDir =  this.externalMediaDirs.first().toString()

        val timeStamp:String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())


        val file = File(filDir, timeStamp + ".png")


        val out =  FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)

        out.flush();
        out.close();

    }




    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<out String>,
            grantResults: IntArray
    ) {
        if(requestCode == 1000){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){


            }else{ Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }


    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater:MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)

        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {


       return when(item.itemId){
           R.id.menu_option -> {
               val intent = Intent(this, OptionsActivity::class.java)
               startActivity(intent)
               true

           } R.id.camera_activity -> {
               val intent = Intent(this, CameraActivity::class.java)
               startActivity(intent)
               true

           }else -> super.onOptionsItemSelected(item)
            }


    }



    fun loadParam(){
        val settings = getSharedPreferences(paramFN, 0)

        endPointUrl = settings.getString(paramEndPoint, "api endpoit").toString()
        streamUrl = settings.getString(paramStreamUrl, streamUrl).toString()
        langChoosen = settings.getString(paramLang, "English").toString()
        vocComIsActif = settings.getBoolean(paramVocActif, true)

        //soundClassifier.isPaused = !vocComIsActif

        setTTSLang()
    }




    fun postIMG(bitmap: Bitmap){
        //create a file to write bitmap data



        val filDir =  this.getCacheDir().toString()

        val timeStamp:String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())


        val file = File(filDir, timeStamp + ".png")
        file.createNewFile()


        val out =  FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)

        out.flush();
        out.close();



        Log.i("endpoit", endPointUrl.toString())


        val smr = SimpleMultiPartRequest(
                Request.Method.POST, endPointUrl,
                object : Response.Listener<String?> {
                    override fun onResponse(response: String?) {
                        Log.i("***************Response", response!!)

                        speakOut(response!!)

                        Toast.makeText(applicationContext, response!!, Toast.LENGTH_LONG).show()
                    }
                }, object : Response.ErrorListener {
            override fun onErrorResponse(error: VolleyError) {
                Toast.makeText(applicationContext, error.message, Toast.LENGTH_LONG).show()
            }
        })

        smr.addStringParam("param_lang", langChoosen.toString())
        smr.addStringParam("param_process", "glass1")
        smr.addFile("img_file", file.absolutePath)
        
        val mRequestQueue = Volley.newRequestQueue(applicationContext)
        mRequestQueue.add(smr)
        Log.i("****request", "*envoyé")


    }




    @SuppressLint("StaticFieldLeak")
    @Suppress("DEPRECATION")
    private inner class DownloadImageFromInternet() : AsyncTask<String, Void, Bitmap?>() {
        init {
            Toast.makeText(applicationContext, "Please wait, it may take a few minute...", Toast.LENGTH_SHORT).show()
        }
        override fun doInBackground(vararg urls: String): Bitmap? {
            val imageURL = urls[0]
            var image: Bitmap? = null
            /*try {
                val inp = java.net.URL(imageURL).openConnection().getInputStream()
                Log.i("stream****", "stream")
            val bufered = BufferedInputStream(inp)
                image = BitmapFactory.decodeStream(bufered)
            Log.i("stream****", inp.toString())

            val byteread = inp.read()
            Log.i("stream****", byteread.toString())

            }
            catch (e: Exception) {
                Log.e("Error Message", e.message.toString())
                e.printStackTrace()
            }*/


            val url = URL(imageURL)
            val urlConnection = url.openConnection() as HttpURLConnection
            try {
                val inp: InputStream = BufferedInputStream(urlConnection.inputStream)

                Log.i("stream****", inp.reader().toString())
                image = BitmapFactory.decodeStream(inp)
            } finally {
                urlConnection.disconnect()
            }

            /*val bufHttpEntity = BufferedHttpEntity(entity)

            val instream: InputStream = bufHttpEntity.getContent()

            bmp = BitmapFactory.decodeStream(instream)*/



            return image
        }
        override fun onPostExecute(result: Bitmap?) {
            result?.let { postIMG(it) }
        }
    }


    override fun onResume() {
        super.onResume()
        loadParam()

    }




    override fun onInit(p0: Int) {

        if (p0 == TextToSpeech.SUCCESS){
           //val tts_result = tts!!.setLanguage(Locale.FRANCE)
            tts!!.setSpeechRate(0.65F)


        }

        Log.i("TTS", "tts initied")
    }

    fun setTTSLang() {

        if (langChoosen == listLang[2] ) {

            Log.i("lang", langChoosen)
            val tts_result = tts!!.setLanguage(Locale.FRANCE)
            tts!!.setSpeechRate(0.8F)

        }else if(langChoosen == listLang[0]){
            val tts_result = tts!!.setLanguage(Locale.US)
            tts!!.setSpeechRate(0.65F)


        }else if(langChoosen == listLang[3]) {
            tts!!.setLanguage(Locale.GERMAN)
            tts!!.setSpeechRate(0.65F)

        }



    }


    fun speakOut(outText:String){
        tts!!.speak(outText, TextToSpeech.QUEUE_FLUSH, null, "")
    }


}