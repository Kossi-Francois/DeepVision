package com.domine.mundi.unbwebview

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.error.VolleyError
import com.android.volley.request.SimpleMultiPartRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    var appMode:String = "caption"

    var tts: TextToSpeech? = null

    var onLiveThread =  Thread()
    var executingCaptionTrhead = Thread()

    var isOnLiveCaption = false



    //param names
    val paramFN = "myPreferences"
    val paramEndPoint = "apiEndPoint"
    val paramStreamUrl = "streamUrl"
    val paramVocActif = "vocCom"
    val paramLang = "language"
    val paramVocOverLap ="voiceOverlap"


    //default param values
    var streamUrl = "http://192.168.25.1:8080/?action=stream"
    var endPointUrl =""
    var langChoosen = "English"
    var vocComIsActif = true
    var overlapFactor = 0.8f

    val probTresh = 0.55

    private lateinit var soundClassifier: SoundClassifier

    //var probResult = mapOf<String, Float>()

    lateinit var buttonRead:Button

    val listLang = arrayListOf<String>("English", "Chinese", "French", "German", "Hindi", "Russian", "Spanish", "Arabic")

    var imgTotation = 0




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)







        tts = TextToSpeech(this, this)

        soundClassifier = SoundClassifier(this, SoundClassifier.Options()).also {
            it.lifecycleOwner = this
        }




        // Set up the listener for take photo button
        camera_capture_button.setOnClickListener { callApi(0) }
        buttonRead = findViewById(R.id.buttonOcr)
        buttonRead.setOnClickListener { callApi(1) }


        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()




        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestMicrophonePermission()
        } else {
            soundClassifier.start()
        }


        loadParam()



        soundClassifier.probabilities.observe(this) { resultMap ->
            if (resultMap.isEmpty() || resultMap.size > soundClassifier.labelList.size) {
                Log.w(TAG, "Invalid size of probability output! (size: ${resultMap.size})")
                return@observe
            }


            var resultVal = resultMap.values
            val argmax =resultVal.max()

            if(argmax!! >= probTresh) {

                if (resultMap["Describe"] == argmax) {
                    vocCommand(0)
                } else if (resultMap["Read"] == argmax) {
                    vocCommand(1)
                } else if (resultMap["ToEnglish"] == argmax) {
                    langChoosen = "English"
                } else if (resultMap["ToFrench"] == argmax) {
                    langChoosen = "French"
                }

                setTTSLang()
            }



            Log.i(TAG, resultMap.toString())


        }


       // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }








    }


    fun vocCommand(commandeIdx: Int){
        if (commandeIdx == 0){
            callApi(0)
            camera_capture_button.alpha = 0f

            camera_capture_button.animate().alpha(1f).setDuration(1500)
        }else if (commandeIdx ==1){
            callApi(1)
            buttonRead.alpha = 0f
            buttonRead.animate().alpha(1f).setDuration(1500)
        }
    }


    fun callApi(mode: Int){
        if(mode == 0){
            appMode = "caption"
        }else if(mode ==1 ){
            appMode = "ocr"
        }

        takePhoto()


    }


    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        // Handles "top" resumed event on multi-window environment
        if (isTopResumedActivity) {
            soundClassifier.start()
        } else {
            soundClassifier.stop()
        }
    }




    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
                outputDirectory,
                SimpleDateFormat(FILENAME_FORMAT, Locale.US
                ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
       /* imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }

            })*/
        //ContextCompat.getMainExecutor(this)

        imageCapture.takePicture(
                Executors.newSingleThreadExecutor(), object : ImageCapture.OnImageCapturedCallback() {

            override fun onCaptureSuccess(image: ImageProxy) {
                imgTotation = image.imageInfo.rotationDegrees
                val bitmap = image.convertImageProxyToBitmap()
                Log.i("*******captured", "captured" + imgTotation.toString())
                super.onCaptureSuccess(image)

                executingCaptionTrhead = Thread(Runnable {
                    postIMG(bitmap)
                })

                executingCaptionTrhead.start()


            }

            override fun onError(exc: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
            }


        })



    }



    fun ImageProxy.convertImageProxyToBitmap(): Bitmap {
        val buffer = planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }


    fun loadParam(){
        val settings = getSharedPreferences(paramFN, 0)

        endPointUrl = settings.getString(paramEndPoint, "api endpoit").toString()
        streamUrl = settings.getString(paramStreamUrl, streamUrl).toString()
        langChoosen = settings.getString(paramLang, "English").toString()
        vocComIsActif = settings.getBoolean(paramVocActif, true)
        overlapFactor = settings.getFloat(paramVocOverLap, 0.8f)

        soundClassifier.isPaused = !vocComIsActif

        soundClassifier.overlapFactor = overlapFactor

        setTTSLang()
    }


    override fun onInit(p0: Int) {

        if (p0 == TextToSpeech.SUCCESS){
            //val tts_result = tts!!.setLanguage(Locale.FRANCE)
            tts!!.setSpeechRate(0.65F)

            tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                var speakingstate = false
                var onSaeted = true
                override fun onStart(utteranceId: String) {
                    Log.i("TextToSpeech", "On Start")
                    if(!onSaeted){
                        speakingstate = soundClassifier.isPaused
                        onSaeted = true
                    }

                    soundClassifier.isPaused = true
                }

                override fun onDone(utteranceId: String) {
                    Log.i("TextToSpeech", "On Done")
                    soundClassifier.isPaused = speakingstate

                    onSaeted = false



                }

                override fun onError(utteranceId: String) {
                    Log.i("TextToSpeech", "On Error")
                    soundClassifier.isPaused = speakingstate
                }
            })

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


    fun speakOut(outText: String){

        tts!!.speak(outText, TextToSpeech.QUEUE_FLUSH, null, "")

    }




    fun postIMG(bitmap: Bitmap){
        //create a file to write bitmap data


        val filDir =  this.getCacheDir().toString()

        val timeStamp:String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())


        val file = File(filDir, timeStamp + ".png")
        file.createNewFile()


        val out =  FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)

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

        smr.addStringParam("SensorAngle", imgTotation.toString())
        smr.addStringParam("param_lang", langChoosen.toString())
        smr.addStringParam("param_process", "phoneCam")
        smr.addStringParam("mode", appMode)
        smr.addFile("img_file", file.absolutePath)

        val mRequestQueue = Volley.newRequestQueue(applicationContext)
        mRequestQueue.add(smr)
        Log.i("****request", "*envoyé")


    }






    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.createSurfaceProvider())
                    }

            imageCapture = ImageCapture.Builder()
                    .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }






    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                baseContext, it) == PackageManager.PERMISSION_GRANTED
    }



    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }


    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults:
            IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show()
                finish()
            }
        }


        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Audio permission granted :)")
                soundClassifier.start()
            } else {
                Log.e(TAG, "Audio permission not granted :(")
            }
        }



    }






    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
        ) {
            soundClassifier.start()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
        }
    }






    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        const val REQUEST_RECORD_AUDIO = 1337
    }






}