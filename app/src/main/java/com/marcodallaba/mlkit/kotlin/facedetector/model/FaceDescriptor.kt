package com.marcodallaba.mlkit.kotlin.facedetector.model

data class FaceDescriptor(
        var hasBothEyesOpen: Boolean,
        var isSmiling: Boolean,
        var isHeadMoving: Boolean
)