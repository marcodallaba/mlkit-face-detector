package com.marcodallaba.mlkit.kotlin.facedetector

import com.google.mlkit.vision.face.Face

interface FaceMovementDetector {

    interface FaceMovementDetectorListener

    fun detectFaceMovement(face: Face)
}