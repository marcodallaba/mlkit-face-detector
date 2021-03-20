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

package com.marcodallaba.mlkit.kotlin.preference

import android.content.Context
import android.util.Size
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.marcodallaba.mlkit.R


/**
 * Utility class to retrieve shared preferences.
 */
object PreferenceUtils {
    fun saveString(context: Context, @StringRes prefKeyId: Int, value: String?) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getString(prefKeyId), value)
                .apply()
    }

    fun getCameraXTargetAnalysisSize(context: Context): Size? {
        val prefKey = context.getString(R.string.pref_key_camerax_target_analysis_size)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return try {
            Size.parseSize(sharedPreferences.getString(prefKey, null))
        } catch (e: Exception) {
            null
        }
    }

    fun getFaceDetectorOptionsForLivePreview(context: Context): FaceDetectorOptions {
        val landmarkMode = getModeTypePreferenceValue(
                context,
                R.string.pref_key_live_preview_face_detection_landmark_mode,
                FaceDetectorOptions.LANDMARK_MODE_NONE)
        val contourMode = getModeTypePreferenceValue(
                context,
                R.string.pref_key_live_preview_face_detection_contour_mode,
                FaceDetectorOptions.CONTOUR_MODE_ALL)
        val classificationMode = FaceDetectorOptions.CLASSIFICATION_MODE_ALL
        val performanceMode = getModeTypePreferenceValue(
                context,
                R.string.pref_key_live_preview_face_detection_performance_mode,
                FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val minFaceSize =
                sharedPreferences.getString(
                        context.getString(R.string.pref_key_live_preview_face_detection_min_face_size),
                        "0.1")!!.toFloat()
        val optionsBuilder = FaceDetectorOptions.Builder()
                .setLandmarkMode(landmarkMode)
                .setContourMode(contourMode)
                .setClassificationMode(classificationMode)
                .setPerformanceMode(performanceMode)
                .setMinFaceSize(minFaceSize)
                .enableTracking()

        return optionsBuilder.build()
    }

    /**
     * Mode type preference is backed by [android.preference.ListPreference] which only support
     * storing its entry value as string type, so we need to retrieve as string and then convert to
     * integer.
     */
    private fun getModeTypePreferenceValue(
            context: Context, @StringRes prefKeyResId: Int, defaultValue: Int): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val prefKey = context.getString(prefKeyResId)
        return sharedPreferences.getString(prefKey, defaultValue.toString())!!.toInt()
    }

    fun isCameraLiveViewportEnabled(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val prefKey = context.getString(R.string.pref_key_camera_live_viewport)
        return sharedPreferences.getBoolean(prefKey, false)
    }
}