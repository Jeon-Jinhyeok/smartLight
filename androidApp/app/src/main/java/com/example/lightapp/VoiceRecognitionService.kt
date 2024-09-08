package com.example.lightapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.os.Bundle
import java.io.File

class VoiceRecognitionService : Service() {

    private lateinit var porcupineManager: PorcupineManager
    private val CHANNEL_ID = "VoiceRecognitionServiceChannel"
    private lateinit var speechRecognizer: SpeechRecognizer

    private var mqttService: MqttService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MqttService.LocalBinder
            mqttService = binder?.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mqttService = null
            isBound = false
        }
    }

    private val lightCount = 3

    override fun onCreate() {
        super.onCreate()

        // 알림 채널 생성 및 포그라운드 서비스 시작
        createNotificationChannel()
        startForegroundService()

        // MQTT 서비스에 바인딩
        bindService(Intent(this, MqttService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)

        // Porcupine 키워드 감지 시작
        startKeywordSpotting()
    }

    private fun startKeywordSpotting() {
        val accessKey = "OBG7Ow41WVM0FKV8Clg+g89rgSTdWbfVDG27hZFXoDJSit0TdD9/tA=="

        // Porcupine 모델 및 키워드 파일을 assets 폴더에서 가져와 경로 설정
        val keywordFilePath = copyAssetToCache("Hey-Rux.ppn")
        val modelPath = copyAssetToCache("porcupine_params.pv")

        try {
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setModelPath(modelPath)
                .setKeywordPath(keywordFilePath)
                .setSensitivity(0.7f)
                .build(applicationContext, object : PorcupineManagerCallback {
                    override fun invoke(keywordIndex: Int) {
                        Log.d("VoiceRecognition", "Keyword detected! Starting speech recognition.")
                        startListening()  // 음성 인식 시작
                    }
                })

            porcupineManager.start()

        } catch (e: Exception) {
            Log.e("VoiceRecognitionService", "Porcupine initialization failed: ${e.message}")
        }
    }

    // assets 폴더에서 파일을 복사하여 캐시 디렉토리에 저장
    private fun copyAssetToCache(fileName: String): String {
        val cacheFile = File(cacheDir, fileName)
        if (!cacheFile.exists()) {
            assets.open(fileName).use { inputStream ->
                cacheFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        return cacheFile.path
    }

    private fun startListening() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    stopListening()
                }

                override fun onError(error: Int) {
                    startListening() // 오류 발생 시 다시 시작
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.let {
                        val recognizedText = it[0]
                        handleCommand(recognizedText)
                    }
                    stopListening()
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")  // 한국어 설정
        }

        speechRecognizer.startListening(intent)  // 음성 인식 시작
    }

    private fun handleCommand(command: String) {
        Log.d("VoiceRecognition", "Command : $command")
        val normalizedCommand = command.replace(" ", "")

        when {
            normalizedCommand.contains("음악모드켜줘") -> activateMusicMode()
            normalizedCommand.contains("음악모드꺼줘") -> deactivateMusicMode()
            normalizedCommand.contains("전체켜줘") -> toggleAllLights("ON")
            normalizedCommand.contains("전체꺼줘") -> toggleAllLights("OFF")
            else -> {
                val lightNumber = when {
                    normalizedCommand.contains("1번") -> 1
                    normalizedCommand.contains("2번") -> 2
                    normalizedCommand.contains("3번") -> 3
                    else -> null
                }

                lightNumber?.let {
                    when {
                        normalizedCommand.contains("켜줘") -> mqttService?.publishLightToggle(it, "ON")
                        normalizedCommand.contains("꺼줘") -> mqttService?.publishLightToggle(it, "OFF")
                        else -> Log.e("VoiceRecognitionService", "Invalid command: $command")
                    }
                }
            }
        }
    }

    private fun activateMusicMode() {
        mqttService?.publishMusicMode(true)
    }

    private fun deactivateMusicMode() {
        mqttService?.publishMusicMode(false)
    }

    private fun toggleAllLights(status: String) {
        for (i in 1..lightCount) {
            mqttService?.publishLightToggle(i, status)
        }
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Recognition Service")
            .setContentText("Listening for voice commands...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Recognition Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Service for voice recognition"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        porcupineManager.delete() // Porcupine 해제
        speechRecognizer.destroy() // SpeechRecognizer 해제
        if (isBound) {
            unbindService(serviceConnection)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}