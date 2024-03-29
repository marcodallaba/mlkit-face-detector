/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.marcodallaba.mlkit.kotlin.visionprocessors

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.annotation.GuardedBy
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.common.InputImage
import com.marcodallaba.mlkit.kotlin.preference.PreferenceUtils
import com.marcodallaba.mlkit.kotlin.utils.*
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Abstract base class for ML Kit frame processors. Subclasses need to implement {@link
 * #onSuccess(T, FrameMetadata, GraphicOverlay)} to define what they want to with the detection
 * results and {@link #detectInImage(VisionImage)} to specify the detector object.
 *
 * @param <T> The type of the detected feature.
 */
abstract class VisionProcessorBase<T>(context: Context) : VisionImageProcessor {

    companion object {
        const val MANUAL_TESTING_LOG = "LogTagForTest"
        private const val TAG = "VisionProcessorBase"
    }

    private var activityManager: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val fpsTimer = Timer()
    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)

    // Whether this processor is already shut down
    private var isShutdown = false

    // Used to calculate latency, running in the same thread, no sync needed.
    private var numRuns = 0
    private var totalRunMs: Long = 0
    private var maxRunMs: Long = 0
    private var minRunMs = Long.MAX_VALUE

    // Frame count that have been processed so far in an one second interval to calculate FPS.
    private var frameProcessedInOneSecondInterval = 0
    private var framesPerSecond = 0

    // To keep the latest images and its metadata.
    @GuardedBy("this")
    private var latestImage: ByteBuffer? = null

    @GuardedBy("this")
    private var latestImageMetaData: FrameMetadata? = null

    // To keep the images and metadata in process.
    @GuardedBy("this")
    private var processingImage: ByteBuffer? = null

    @GuardedBy("this")
    private var processingMetaData: FrameMetadata? = null

    init {
        fpsTimer.scheduleAtFixedRate(
                object : TimerTask() {
                    override fun run() {
                        framesPerSecond = frameProcessedInOneSecondInterval
                        frameProcessedInOneSecondInterval = 0
                    }
                },
                0,
                1000
        )
    }

    @ExperimentalGetImage
    override fun processImageProxy(image: ImageProxy, graphicOverlay: GraphicOverlay) {
        if (isShutdown) {
            return
        }
        var bitmap: Bitmap? = null
        if (!PreferenceUtils.isCameraLiveViewportEnabled(graphicOverlay.context)) {
            bitmap = BitmapUtils.getBitmap(image)
        }
        requestDetectInImage(
                InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees),
                graphicOverlay, /* originalCameraImage= */
                bitmap, /* shouldShowFps= */
                true
        )
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
                .addOnCompleteListener { image.close() }
    }

    // -----------------Common processing logic-------------------------------------------------------
    private fun requestDetectInImage(
            image: InputImage,
            graphicOverlay: GraphicOverlay,
            originalCameraImage: Bitmap?,
            shouldShowFps: Boolean
    ): Task<T> {
        val startMs = SystemClock.elapsedRealtime()

        detectInImage(image).addOnSuccessListener(executor, OnSuccessListener {

        })

        return detectInImage(image).addOnSuccessListener(executor, OnSuccessListener { results: T ->
            val currentLatencyMs = SystemClock.elapsedRealtime() - startMs
            numRuns++
            frameProcessedInOneSecondInterval++
            totalRunMs += currentLatencyMs
            maxRunMs = max(currentLatencyMs, maxRunMs)
            minRunMs = min(currentLatencyMs, minRunMs)
            // Only log inference info once per second. When frameProcessedInOneSecondInterval is
            // equal to 1, it means this is the first frame processed during the current second.
            if (frameProcessedInOneSecondInterval == 1) {
                Log.d(TAG, "Max latency is: $maxRunMs")
                Log.d(TAG, "Min latency is: $minRunMs")
                Log.d(
                        TAG,
                        "Num of Runs: " + numRuns + ", Avg latency is: " + totalRunMs / numRuns
                )
                val mi = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(mi)
                val availableMegs = mi.availMem / 0x100000L
                Log.d(
                        TAG,
                        "Memory available in system: $availableMegs MB"
                )
            }
            graphicOverlay.clear()
            if (originalCameraImage != null) {
                graphicOverlay.add(
                        CameraImageGraphic(
                                graphicOverlay,
                                originalCameraImage
                        )
                )
            }
            this@VisionProcessorBase.onSuccess(results, graphicOverlay)
            graphicOverlay.add(
                    InferenceInfoGraphic(
                            graphicOverlay,
                            currentLatencyMs.toDouble(),
                            if (shouldShowFps) framesPerSecond else null
                    )
            )
            graphicOverlay.postInvalidate()
        })
                .addOnFailureListener(executor, OnFailureListener { e: Exception ->
                    graphicOverlay.clear()
                    graphicOverlay.postInvalidate()
                    Toast.makeText(
                            graphicOverlay.context,
                            "Failed to process.\nError: " +
                                    e.localizedMessage +
                                    "\nCause: " +
                                    e.cause,
                            Toast.LENGTH_LONG
                    )
                            .show()
                    e.printStackTrace()
                    this@VisionProcessorBase.onFailure(e)
                })
    }

    override fun stop() {
        executor.shutdown()
        isShutdown = true
        numRuns = 0
        totalRunMs = 0
        fpsTimer.cancel()
    }

    protected abstract fun detectInImage(image: InputImage): Task<T>

    protected abstract fun onSuccess(faces: T, graphicOverlay: GraphicOverlay)

    protected abstract fun onFailure(e: Exception)
}
