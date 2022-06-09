package com.example.hearme

import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.frame.Frame
import com.otaliastudios.cameraview.size.Size
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*




class CameraActivity : AppCompatActivity() {

    var CAT: String = "CAMERA_ACTIVITY"

    private lateinit var cameraView: CameraView
    private lateinit var tvDetectedItem: TextView

    private val itemMap by lazy {
        hashMapOf<String, Int>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        supportActionBar?.title = "Camera Activity"
        supportActionBar?.setDisplayHomeAsUpEnabled(true);

        cameraView = findViewById(R.id.cameraView)
        cameraView.setLifecycleOwner(this);

        tvDetectedItem = findViewById(R.id.tvDetectedItem)

        cameraView.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) {
                // Picture was taken!
                // Access the raw data if needed.
                var data = result.data
            }

            override fun onVideoTaken(result: VideoResult) {

            }

            override fun onVideoRecordingStart() {
                super.onVideoRecordingStart()
            }

            override fun onVideoRecordingEnd() {
                super.onVideoRecordingEnd()
            }

        }
        )

        var imgCount = 0

        cameraView.addFrameProcessor { frame: Frame ->
            val time = frame.time
            val size: Size = frame.size
            val format = frame.format
            val userRotation = frame.rotationToUser
            val viewRotation = frame.rotationToView
            Log.i(CAT, "Frame Processor...")
            if (frame.dataClass === ByteArray::class.java) {
                val data: ByteArray = frame.getData()
                // Process byte array...
                //Log.i(CAT, "Byte Array...")
                if(imgCount % 30 == 0){
                    saveImage(data, size, format, time)
                }
                imgCount+=1
            } else if (frame.dataClass === Image::class.java) {
                val data: Image = frame.getData()
                // Process android.media.Image...
                Log.i(CAT, "Image...")

            }

            runOnUiThread {
                tvDetectedItem.text = "Hello World! Img = "+ imgCount.div(30).toString()
//                itemMap.forEach { map ->
//                    tvDetectedItem.append("Detected ${map.value} ${map.key}\n")
//                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        Log.i(CAT, "going back")
        finish()
        return super.onSupportNavigateUp()
    }

    private fun saveImage(imageByte: ByteArray, size: Size, format: Int, time: Long) {

        val dirPath = Environment.getExternalStorageDirectory().absolutePath + "/"
        var file = File(dirPath, "DCIM/PMR_PHOTOS")
        if (!file.exists()) {
            file.mkdir()
        }

        val dateFormat = SimpleDateFormat("yyyyMMdd_HH_mm_ss")
        val currentTimeStamp: String = dateFormat.format(Date()) + "_" + time.toString() + ".jpg"

        try {
            val image = YuvImage(
                imageByte, format,
                size.width, size.height, null
            )
            val imgFile = File(file.absolutePath, currentTimeStamp)

            val imgFOS = FileOutputStream(imgFile)
            image.compressToJpeg(
                Rect(0, 0, image.width, image.height), 90,
                imgFOS
            )
        } catch (e: FileNotFoundException) {

        }


    }

    override fun onResume() {
        super.onResume()
        cameraView.open()

    }

    override fun onPause() {
        super.onPause()
        cameraView.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraView.destroy()
    }
}