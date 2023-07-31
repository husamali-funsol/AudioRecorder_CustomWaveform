package com.example.audiorecorder

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Adapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.example.audiorecorder.databinding.ActivityMainBinding
import com.example.audiorecorder.databinding.BottomSheetLayoutBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.Date

const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), Timer.onTimerTickListener {

    private var duration = ""
    private lateinit var amplitudes: ArrayList<Float>
    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomSheetBinding: BottomSheetLayoutBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>



    private var permissions = arrayOf(
        android.Manifest.permission.RECORD_AUDIO ,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE

    )
    private var permissionsGranted = false
    private lateinit var recorder: MediaRecorder

    private var dirPath = ""
    private var filename = ""

    private var isRecording = false
    private var isPaused = false

    private lateinit var timer: Timer

    private lateinit var vibrator: Vibrator

    private lateinit var db: ARDatabase




    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Find the bottom sheet view using view binding
        bottomSheetBinding = BottomSheetLayoutBinding.bind(binding.root.findViewById(R.id.bottomSheet))

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        db = ARDatabase.getDB(this)

        // Initialize the BottomSheetBehavior
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetBinding.root)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.isHideable = true // Allow the bottom sheet to be hidden by dragging



        permissionsGranted = ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, permissions[1]) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, permissions[2]) == PackageManager.PERMISSION_GRANTED

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
            intent = Intent(this, RecordsListActivity::class.java)
            startActivity(intent)

            Toast.makeText(this, "Record List", Toast.LENGTH_SHORT).show()
        }

        binding.btnStop.setOnClickListener {
            stopRecording()

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.bottomSheetBG.visibility = View.VISIBLE
            bottomSheetBinding.etFilename.setText(filename)
            bottomSheetBinding.etFilename.setSelection(bottomSheetBinding.etFilename.text!!.length)



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

        var filePath = "$dirPath$filename.mp3"
        var timeStamp = Date().time
        var ampsPath = "$dirPath$filename"


        try {
            var fos = FileOutputStream(ampsPath)
            var out = ObjectOutputStream(fos)
            out.writeObject(amplitudes)
            fos.close()
            out.close()
        }
        catch (_: IOException){}

        var record = AudioRecord(newFilename, filePath, timeStamp, duration, ampsPath)

        GlobalScope.launch {
            db.audioRecordDao().Insert(record)
        }
        Toast.makeText(this@MainActivity, "Data Inserted in DB", Toast.LENGTH_SHORT).show()

    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun dismiss(){
        binding.bottomSheetBG.visibility = View.GONE
        hideKeyboard(bottomSheetBinding.etFilename)

        Handler(Looper.getMainLooper()).postDelayed(
            {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        , 100
        )
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
        var simpleDateFormat = SimpleDateFormat("dd-MM-yyyy_hh-mm-ss")
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
        this.duration = duration.dropLast(3)

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