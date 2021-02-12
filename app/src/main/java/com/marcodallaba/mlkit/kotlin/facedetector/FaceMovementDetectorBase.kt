package com.marcodallaba.mlkit.kotlin.facedetector

import android.util.Log
import com.marcodallaba.mlkit.kotlin.facedetector.model.FaceDescriptor
import com.google.mlkit.vision.face.Face

class FaceMovementDetectorBase(private val faceMovementDetectorListenerBase: FaceMovementDetectorListenerBase) : FaceMovementDetector {

    interface FaceMovementDetectorListenerBase : FaceMovementDetector.FaceMovementDetectorListener {
        fun onLeftEyeWinkDetected()
        fun onRightEyeWinkDetected()
        fun onSmileDetected()
        fun onFaceTurnedUp()
        fun onFaceTurnedDown()
        fun onFaceTurnedRight()
        fun onFaceTurnedLeft()
        fun onFaceTiltedRight()
        fun onFaceTiltedLeft()
    }

    private val detectedFacesDescriptors: HashMap<Int, FaceDescriptor> = HashMap()

    override fun detectFaceMovement(face: Face) {

        if (face.trackingId == null || face.leftEyeOpenProbability == null || face.rightEyeOpenProbability == null || face.smilingProbability == null) return

        if (!detectedFacesDescriptors.containsKey(face.trackingId!!)) {
            detectedFacesDescriptors[face.trackingId!!] = FaceDescriptor(hasBothEyesOpen = true, isSmiling = false, isHeadMoving = false)
        }
        val faceDescriptor = detectedFacesDescriptors[face.trackingId!!] ?: return

        //WINK DETECTION
        if (faceDescriptor.hasBothEyesOpen && face.leftEyeOpenProbability!! > OPENED_EYE_MIN_THRESHOLD && face.rightEyeOpenProbability!! < CLOSED_EYE_MAX_THRESHOLD) {

            faceDescriptor.hasBothEyesOpen = false
            faceMovementDetectorListenerBase.onRightEyeWinkDetected()
        } else if (faceDescriptor.hasBothEyesOpen && face.rightEyeOpenProbability!! > OPENED_EYE_MIN_THRESHOLD && face.leftEyeOpenProbability!! < CLOSED_EYE_MAX_THRESHOLD) {

            faceDescriptor.hasBothEyesOpen = false
            faceMovementDetectorListenerBase.onLeftEyeWinkDetected()

        } else if (!faceDescriptor.hasBothEyesOpen && face.rightEyeOpenProbability!! > OPENED_EYE_MIN_THRESHOLD && face.leftEyeOpenProbability!! > OPENED_EYE_MIN_THRESHOLD) {
            faceDescriptor.hasBothEyesOpen = true
        }

        //SMILE DETECTION
        if (!faceDescriptor.isSmiling && face.smilingProbability!! > SMILING_MIN_THRESHOLD) {
            faceDescriptor.isSmiling = true
            faceMovementDetectorListenerBase.onSmileDetected()
        } else if (face.smilingProbability!! < NOT_SMILING_MAX_THRESHOLD) {
            faceDescriptor.isSmiling = false
        }

        if (!faceDescriptor.isHeadMoving) {
            when {
                face.headEulerAngleX > 30 -> {
                    faceDescriptor.isHeadMoving = true
                    faceMovementDetectorListenerBase.onFaceTurnedUp()
                }
                face.headEulerAngleX < -30 -> {
                    faceDescriptor.isHeadMoving = true
                    faceMovementDetectorListenerBase.onFaceTurnedDown()
                }
                face.headEulerAngleY > 30 -> {
                    faceDescriptor.isHeadMoving = true
                    faceMovementDetectorListenerBase.onFaceTurnedRight()
                }
                face.headEulerAngleY < -30 -> {
                    faceDescriptor.isHeadMoving = true
                    faceMovementDetectorListenerBase.onFaceTurnedLeft()
                }
                face.headEulerAngleZ > 30 -> {
                    faceDescriptor.isHeadMoving = true
                    faceMovementDetectorListenerBase.onFaceTiltedRight()
                }
                face.headEulerAngleZ < -30 -> {
                    faceDescriptor.isHeadMoving = true
                    faceMovementDetectorListenerBase.onFaceTiltedLeft()
                }
            }
        } else if (faceDescriptor.isHeadMoving && face.headEulerAngleZ > -10 && face.headEulerAngleZ < 10 && face.headEulerAngleX > -10
                && face.headEulerAngleX < 10 && face.headEulerAngleY > -10 && face.headEulerAngleY < 10) {
            faceDescriptor.isHeadMoving = false
            Log.d(TAG, "Face stable in centre")
        }
    }

    companion object {
        private val TAG = FaceMovementDetectorBase::class.java.simpleName
        private const val TRACKING_ID = "TRACKING_ID"
        private const val LEFT_EYE_OPEN_PROBABILITY = "LEFT_EYE_OPEN_PROBABILITY"
        private const val RIGHT_EYE_OPEN_PROBABILITY = "RIGHT_EYE_OPEN_PROBABILITY"
        private const val OPENED_EYE_MIN_THRESHOLD = 0.95
        private const val CLOSED_EYE_MAX_THRESHOLD = 0.1
        private const val SMILING_MIN_THRESHOLD = 0.95
        private const val NOT_SMILING_MAX_THRESHOLD = 0.1
    }
}