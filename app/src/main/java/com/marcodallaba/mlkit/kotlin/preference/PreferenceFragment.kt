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

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.preference.*
import com.marcodallaba.mlkit.R

/**
 * Configures CameraX live preview demo settings.
 */
class PreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preference_live_preview)
        setUpCameraPreferences()
        setUpFaceDetectionPreferences()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}

    private fun setUpCameraPreferences() {
        val cameraPreference = findPreference(getString(R.string.pref_category_key_camera)) as? PreferenceCategory
        cameraPreference?.removePreference(
                findPreference(getString(R.string.pref_key_rear_camera_preview_size)))
        cameraPreference?.removePreference(
                findPreference(getString(R.string.pref_key_front_camera_preview_size)))
        setUpCameraXTargetAnalysisSizePreference()
    }

    private fun setUpCameraXTargetAnalysisSizePreference() {
        val pref = findPreference(getString(R.string.pref_key_camerax_target_analysis_size)) as? ListPreference
        pref?.let {
            val entries = arrayOf(
                    "2000x2000",
                    "1600x1600",
                    "1200x1200",
                    "1000x1000",
                    "800x800",
                    "600x600",
                    "400x400",
                    "200x200",
                    "100x100")
            it.entries = entries
            it.entryValues = entries
            it.summary = if (it.entry == null) "Default" else pref.entry
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                val newStringValue = newValue as String?
                it.summary = newStringValue

                context?.let { context ->
                    PreferenceUtils.saveString(
                            context,
                            R.string.pref_key_camerax_target_analysis_size,
                            newStringValue)
                }
                true
            }
        } ?: Log.e(TAG, "preference is null")
    }

    private fun setUpFaceDetectionPreferences() {
        setUpListPreference(R.string.pref_key_live_preview_face_detection_landmark_mode)
        setUpListPreference(R.string.pref_key_live_preview_face_detection_contour_mode)
        setUpListPreference(R.string.pref_key_live_preview_face_detection_classification_mode)
        setUpListPreference(R.string.pref_key_live_preview_face_detection_performance_mode)

        val minFaceSizePreference = findPreference(getString(R.string.pref_key_live_preview_face_detection_min_face_size)) as? EditTextPreference
        minFaceSizePreference?.let {
            minFaceSizePreference.summary = minFaceSizePreference.text
            minFaceSizePreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                try {
                    val minFaceSize = (newValue as String).toFloat()
                    if (minFaceSize in 0.0f..1.0f) {
                        minFaceSizePreference.summary = newValue
                        return@OnPreferenceChangeListener true
                    }
                } catch (e: NumberFormatException) {
                    // Fall through intentionally.
                }
                Toast.makeText(
                        activity, R.string.pref_toast_invalid_min_face_size, Toast.LENGTH_LONG)
                        .show()
                false
            }
        }?: Log.e(TAG, "minFaceSizePreference is null")
    }

    private fun setUpListPreference(@StringRes listPreferenceKeyId: Int) {
        val listPreference = findPreference(getString(listPreferenceKeyId)) as? ListPreference
        listPreference?.let {
            it.summary = it.entry
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                val index = it.findIndexOfValue(newValue as String?)
                it.summary = it.entries[index]
                true
            }
        }
    }

    companion object {
        private val TAG = PreferenceFragment::class.java.simpleName
    }
}