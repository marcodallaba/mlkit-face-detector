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

package com.marcodallaba.mlkit.kotlin.facedetector

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.marcodallaba.mlkit.kotlin.utils.GraphicOverlay
import com.marcodallaba.mlkit.kotlin.visionprocessors.VisionProcessorBase
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.marcodallaba.mlkit.vision.demo.R
import java.util.Locale


class FaceDetectorProcessor(private val context: Context, detectorOptions: FaceDetectorOptions?) :
        VisionProcessorBase<List<Face>>(context),
        FaceMovementDetectorBase.FaceMovementDetectorListenerBase {

    private val detector: FaceDetector
    private val faceMovementDetector: FaceMovementDetector

    init {
        val options = detectorOptions
                ?: FaceDetectorOptions.Builder()
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .enableTracking()
                        .build()

        detector = FaceDetection.getClient(options)
        faceMovementDetector = FaceMovementDetectorBase(this)

        Log.v(MANUAL_TESTING_LOG, "Face detector options: $options")
    }

    override fun stop() {
        super.stop()
        detector.close()
    }

    override fun detectInImage(image: InputImage): Task<List<Face>> {
        return detector.process(image)
    }

    override fun onSuccess(faces: List<Face>, graphicOverlay: GraphicOverlay) {
        for (face in faces) {
            faceMovementDetector.detectFaceMovement(face)
            graphicOverlay.add(FaceGraphic(graphicOverlay, face))
            logExtrasForTesting(face)
        }
    }

    override fun onFailure(e: Exception) {
        Log.d(TAG, "Face detection failed $e")
    }

    override fun onLeftEyeWinkDetected() {
        Log.d(TAG, "Left Eye Wink")
        Toast.makeText(context, context.getString(R.string.left_eye_wink_detected), Toast.LENGTH_SHORT).show()
    }

    override fun onRightEyeWinkDetected() {
        Log.d(TAG, "Right Eye Wink")
        Toast.makeText(context, context.getString(R.string.right_eye_wink_detected), Toast.LENGTH_SHORT).show()
    }

    override fun onSmileDetected() {
        Log.d(TAG, "Smile")
        Toast.makeText(context, context.getString(R.string.smile_detected), Toast.LENGTH_SHORT).show()
    }

    override fun onFaceTurnedUp() {
        Log.d(TAG, "Face turned up")
        Toast.makeText(context, context.getString(R.string.face_turned_up_detected), Toast.LENGTH_SHORT).show()
    }

    override fun onFaceTurnedDown() {
        Log.d(TAG, "Face turned down")
        Toast.makeText(context, context.getString(R.string.face_turned_down_detected), Toast.LENGTH_SHORT).show()
    }

    override fun onFaceTurnedLeft() {
        Log.d(TAG, "Face turned left")
        Toast.makeText(context, context.getString(R.string.face_turned_left_detected), Toast.LENGTH_SHORT).show()
    }

    override fun onFaceTurnedRight() {
        Log.d(TAG, "Face turned right")
        Toast.makeText(context, context.getString(R.string.face_turned_right_detected), Toast.LENGTH_SHORT).show()
    }

    override fun onFaceTiltedLeft() {
        Log.d(TAG, "Face tilted left")
        Toast.makeText(context, context.getString(R.string.face_tilted_left_detected), Toast.LENGTH_SHORT).show()
    }

    override fun onFaceTiltedRight() {
        Log.d(TAG, "Face tilted right")
        Toast.makeText(context, context.getString(R.string.face_tilted_right_detected), Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "FaceDetectorProcessor"
        private fun logExtrasForTesting(face: Face?) {
            if (face != null) {
                Log.v(
                        MANUAL_TESTING_LOG,
                        "face bounding box: " + face.boundingBox.flattenToString()
                )
                Log.v(
                        MANUAL_TESTING_LOG,
                        "face Euler Angle X: " + face.headEulerAngleX
                )
                Log.v(
                        MANUAL_TESTING_LOG,
                        "face Euler Angle Y: " + face.headEulerAngleY
                )
                Log.v(
                        MANUAL_TESTING_LOG,
                        "face Euler Angle Z: " + face.headEulerAngleZ
                )
                // All landmarks
                val landMarkTypes = intArrayOf(
                        FaceLandmark.MOUTH_BOTTOM,
                        FaceLandmark.MOUTH_RIGHT,
                        FaceLandmark.MOUTH_LEFT,
                        FaceLandmark.RIGHT_EYE,
                        FaceLandmark.LEFT_EYE,
                        FaceLandmark.RIGHT_EAR,
                        FaceLandmark.LEFT_EAR,
                        FaceLandmark.RIGHT_CHEEK,
                        FaceLandmark.LEFT_CHEEK,
                        FaceLandmark.NOSE_BASE
                )
                val landMarkTypesStrings = arrayOf(
                        "MOUTH_BOTTOM",
                        "MOUTH_RIGHT",
                        "MOUTH_LEFT",
                        "RIGHT_EYE",
                        "LEFT_EYE",
                        "RIGHT_EAR",
                        "LEFT_EAR",
                        "RIGHT_CHEEK",
                        "LEFT_CHEEK",
                        "NOSE_BASE"
                )
                for (i in landMarkTypes.indices) {
                    val landmark = face.getLandmark(landMarkTypes[i])
                    if (landmark == null) {
                        Log.v(
                                MANUAL_TESTING_LOG,
                                "No landmark of type: " + landMarkTypesStrings[i] + " has been detected"
                        )
                    } else {
                        val landmarkPosition = landmark.position
                        val landmarkPositionStr =
                                String.format(Locale.US, "x: %f , y: %f", landmarkPosition.x, landmarkPosition.y)
                        Log.v(
                                MANUAL_TESTING_LOG,
                                "Position for face landmark: " +
                                        landMarkTypesStrings[i] +
                                        " is :" +
                                        landmarkPositionStr
                        )
                    }
                }
                Log.v(
                        MANUAL_TESTING_LOG,
                        "face left eye open probability: " + face.leftEyeOpenProbability
                )
                Log.v(
                        MANUAL_TESTING_LOG,
                        "face right eye open probability: " + face.rightEyeOpenProbability
                )
                Log.v(
                        MANUAL_TESTING_LOG,
                        "face smiling probability: " + face.smilingProbability
                )
                Log.v(
                        MANUAL_TESTING_LOG,
                        "face tracking id: " + face.trackingId
                )
            }
        }
    }
}
