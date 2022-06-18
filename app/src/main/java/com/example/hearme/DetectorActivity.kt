package com.example.hearme

import android.graphics.*
import android.media.ImageReader.OnImageAvailableListener
import android.os.SystemClock
import android.util.Size
import android.util.TypedValue
import android.widget.Toast
import com.example.hearme.detection.BorderedText
import com.example.hearme.detection.ImageUtils
import com.example.hearme.detection.MultiBoxTracker
import com.example.hearme.detection.overlay.OverlayView
import com.example.hearme.detection.overlay.OverlayView.DrawCallback
import com.example.hearme.detection.tf.Detector
import com.example.hearme.detection.tf.TFLiteObjectDetectionAPIModel
import java.io.IOException

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
open class DetectorActivity(override val desiredPreviewFrameSize: Size?) : CameraActivity(), OnImageAvailableListener {
    var trackingOverlay: OverlayView? = null
    private var sensorOrientation: Int? = null
    private var detector: Detector? = null
    private var lastProcessingTimeMs: Long = 0
    private var rgbFrameBitmap: Bitmap? = null
    private var croppedBitmap: Bitmap? = null
    private var cropCopyBitmap: Bitmap? = null
    private var computingDetection = false
    private var timestamp: Long = 0
    private var frameToCropTransform: Matrix? = null
    private var cropToFrameTransform: Matrix? = null
    private var tracker: MultiBoxTracker? = null
    private var borderedText: BorderedText? = null
    override fun onPreviewSizeChosen(size: Size?, rotation: Int) {
        val textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics()
        )
        borderedText = BorderedText(textSizePx)
        borderedText!!.setTypeface(Typeface.MONOSPACE)
        tracker = MultiBoxTracker(this)
        var cropSize = TF_OD_API_INPUT_SIZE
        try {
            detector = TFLiteObjectDetectionAPIModel.create(
                this,
                TF_OD_API_MODEL_FILE,
                TF_OD_API_LABELS_FILE,
                TF_OD_API_INPUT_SIZE,
                TF_OD_API_IS_QUANTIZED
            )
            cropSize = TF_OD_API_INPUT_SIZE
        } catch (e: IOException) {
            e.printStackTrace()
            val toast = Toast.makeText(
                getApplicationContext(), "Detector could not be initialized", Toast.LENGTH_SHORT
            )
            toast.show()
            finish()
        }
        previewWidth = size!!.width
        previewHeight = size!!.height
        sensorOrientation = rotation - screenOrientation
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
        frameToCropTransform = ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            sensorOrientation!!, MAINTAIN_ASPECT
        )
        cropToFrameTransform = Matrix()
        frameToCropTransform!!.invert(cropToFrameTransform)
        trackingOverlay = findViewById(R.id.tracking_overlay) as OverlayView?
        trackingOverlay!!.addCallback(
            object : DrawCallback {
                override fun drawCallback(canvas: Canvas?) {
                    if (canvas != null) {
                        tracker!!.draw(canvas)
                    }
                    if (isDebug) {
                        if (canvas != null) {
                            tracker!!.drawDebug(canvas)
                        }
                    }
                }
            })
        tracker!!.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation!!)
    }

    protected override fun processImage() {
        ++timestamp
        val currTimestamp = timestamp
        trackingOverlay!!.postInvalidate()

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage()
            return
        }
        computingDetection = true
        rgbFrameBitmap!!.setPixels(
            getRgbBytes(),
            0,
            previewWidth,
            0,
            0,
            previewWidth,
            previewHeight
        )
        readyForNextImage()
        val canvas = Canvas(croppedBitmap!!)
        canvas.drawBitmap(rgbFrameBitmap!!, frameToCropTransform!!, null)
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap!!)
        }
        runInBackground(
            Runnable {
                val startTime = SystemClock.uptimeMillis()
                val results: List<Detector.Recognition> = detector?.recognizeImage(croppedBitmap) as List<Detector.Recognition>
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime
                cropCopyBitmap = Bitmap.createBitmap(croppedBitmap!!)
                val canvas = Canvas(cropCopyBitmap!!)
                val paint = Paint()
                paint.color = Color.RED
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.0f
                var minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API
                minimumConfidence =
                    when (MODE) {
                        DetectorMode.TF_OD_API -> MINIMUM_CONFIDENCE_TF_OD_API
                    }
                val mappedRecognitions: MutableList<Detector.Recognition> = ArrayList<Detector.Recognition>()
                for (result in results) {
                    val location: RectF = result.getLocation()
                    if (location != null && result.getConfidence()!! >= minimumConfidence) {
                        canvas.drawRect(location, paint)
                        cropToFrameTransform!!.mapRect(location)
                        result.setLocation(location)
                        mappedRecognitions.add(result)
                    }
                }
                tracker!!.trackResults(mappedRecognitions, currTimestamp)
                trackingOverlay!!.postInvalidate()
                computingDetection = false
                runOnUiThread(
                    Runnable {
                        showFrameInfo(previewWidth.toString() + "x" + previewHeight)
                        showCropInfo(
                            cropCopyBitmap!!.getWidth().toString() + "x" + cropCopyBitmap!!.getHeight()
                        )
                        showInference(lastProcessingTimeMs.toString() + "ms")
                    })
            })
    }

    protected override val layoutId: Int
        protected get() = R.layout.tfe_od_camera_connection_fragment_tracking

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum class DetectorMode {
        TF_OD_API
    }

    protected override fun setUseNNAPI(isChecked: Boolean) {
        runInBackground {
            try {
                detector!!.setUseNNAPI(isChecked)
            } catch (e: UnsupportedOperationException) {
                runOnUiThread { Toast.makeText(this, e.message, Toast.LENGTH_LONG).show() }
            }
        }
    }

    protected override fun setNumThreads(numThreads: Int) {
        runInBackground { detector?.setNumThreads(numThreads) }
    }

    companion object {

        // Configuration values for the prepackaged SSD model.
        private const val TF_OD_API_INPUT_SIZE = 320
        private const val TF_OD_API_IS_QUANTIZED = true
        private const val TF_OD_API_MODEL_FILE = "detect.tflite"
        private const val TF_OD_API_LABELS_FILE = "labelmap.txt"
        private val MODE = DetectorMode.TF_OD_API

        // Minimum detection confidence to track a detection.
        private const val MINIMUM_CONFIDENCE_TF_OD_API = 0.5f
        private const val MAINTAIN_ASPECT = false
        val DESIRED_PREVIEW_SIZE = Size(640, 480)
        val desiredPreviewFrameSize = Size(640, 480)
        private const val SAVE_PREVIEW_BITMAP = false
        private const val TEXT_SIZE_DIP = 10f
    }
}