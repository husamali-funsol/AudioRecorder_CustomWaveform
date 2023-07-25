package com.example.audiorecorder

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.example.audiorecorder.databinding.ActivityMainBinding
import com.example.audiorecorder.databinding.BottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), Timer.onTimerTickListener {

    private lateinit var amplitudes: ArrayList<Float>
    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomSheetBinding: BottomSheetBinding

    private var permissions = arrayOf(
        android.Manifest.permission.RECORD_AUDIO
    )
    private var permissionsGranted = false
    private lateinit var recorder: MediaRecorder

    private var dirPath = ""
    private var filename = ""

    private var isRecording = false
    private var isPaused = false

    private lateinit var timer: Timer

    private lateinit var vibrator: Vibrator

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomSheetBinding = BottomSheetBinding.inflate(layoutInflater)


        val bottomSheetView: View = findViewById(R.id.bottomSheet) // Replace 'R.id.bottomSheet' with the correct ID of the bottom sheet view
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView)
//        bottomSheetBehavior = BottomSheetBehavior.from(this.bottomSheetBinding.bottomSheet)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        permissionsGranted = ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED

        if(!permissionsGranted){
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
        }

        timer = Timer(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        binding.btnRecord.setOnClickListener {
            when{
                isPaused -> resumeRecording()
                isRecording -> pauseRecording()
                else -> startRecording()
            }

            vibrator.vibrate(VibrationEffect.createOneShot(50,VibrationEffect.DEFAULT_AMPLITUDE))
        }

        binding.btnList.setOnClickListener {
            //TODO

            Toast.makeText(this, "Record List", Toast.LENGTH_SHORT).show()
        }

        binding.btnStop.setOnClickListener {
            stopRecording()

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.bottomSheetBG.visibility = View.VISIBLE
            bottomSheetBinding.etFilename.setText(filename)

            Toast.makeText(this, "Record Saved", Toast.LENGTH_SHORT).show()

        }

        bottomSheetBinding.btnCancel.setOnClickListener {
            File("$dirPath$filename.mp3").delete()

            dismiss()
        }


        bottomSheetBinding.btnSave.setOnClickListener {
            dismiss()

            save()
        }

        binding.bottomSheetBG.setOnClickListener {
            File("$dirPath$filename.mp3").delete()
            dismiss()
        }

        binding.btnClear.setOnClickListener {
            stopRecording()
            File("$dirPath$filename.mp3").delete()

            Toast.makeText(this, "Record Deleted", Toast.LENGTH_SHORT).show()
        }



        binding.btnClear.isClickable = false


    }

    private fun save() {
        val newFilename = bottomSheetBinding.etFilename.text.toString()
        if(newFilename != filename){
            var newFile = File("$dirPath$newFilename.mp3")
            File("$dirPath$filename.mp3").renameTo(newFile)
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun dismiss(){
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        binding.bottomSheetBG.visibility = View.GONE

        hideKeyboard(bottomSheetBinding.etFilename)
    }

    private fun pauseRecording() {
        recorder.pause()
        isPaused = true
        binding.btnRecord.setImageResource(R.drawable.ic_mic)
        timer.pause()
    }

    private fun resumeRecording() {
        recorder.resume()
        isPaused = false
        binding.btnRecord.setImageResource(R.drawable.ic_pause)
        timer.start()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == REQUEST_CODE){
            permissionsGranted = grantResults[0] ==PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startRecording(){
        if(!permissionsGranted){
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
            return
        }

        // Implement start recording
        recorder = MediaRecorder()

        dirPath = "${externalCacheDir?.absolutePath}/"
        var simpleDateFormat = SimpleDateFormat("DD.MM.yyyy_hh.mm.ss")
        var date = simpleDateFormat.format(Date())
        filename = "audio_record_$date"

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$filename.mp3")

            try {
                prepare()
            }
            catch (e: IOException){}

            start()

        }

        binding.btnRecord.setImageResource(R.drawable.ic_pause)

        isRecording = true
        isPaused = false

        timer.start()

        binding.btnClear.isClickable = true
        binding.btnClear.setImageResource(R.drawable.ic_clear)

        binding.btnList.visibility = View.GONE
        binding.btnStop.visibility = View.VISIBLE
    }

    override fun onTimerTick(duration: String) {
        binding.tvTime.text = duration

//        Toast.makeText(this, recorder.maxAmplitude.toString(), Toast.LENGTH_SHORT).show()

        binding.waveform.addAmplitude(recorder.maxAmplitude.toFloat())
    }

    private fun stopRecording(){
        timer.stop()

        recorder.apply {
            stop()
            release()
        }

        isPaused = false
        isRecording = false

        binding.btnList.visibility = View.VISIBLE
        binding.btnStop.visibility = View.GONE

        binding.btnClear.isClickable = false
        binding.btnClear.setImageResource(R.drawable.ic_clear_grey)

        binding.btnRecord.setImageResource(R.drawable.ic_mic)

        binding.tvTime.text = "00:00:00"

        amplitudes = binding.waveform.clear()

    }


}