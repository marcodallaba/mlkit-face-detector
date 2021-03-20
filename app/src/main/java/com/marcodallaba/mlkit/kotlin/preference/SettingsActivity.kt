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
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.marcodallaba.mlkit.kotlin.preference.SettingsActivity.LaunchSource
import com.marcodallaba.mlkit.R

/**
 * Hosts the preference fragment to configure settings for a demo activity that specified by the
 * [LaunchSource].
 */
class SettingsActivity : AppCompatActivity() {
    /**
     * Specifies where this activity is launched from.
     */
    enum class LaunchSource(val titleResId: Int, val prefFragmentClass: Class<out PreferenceFragmentCompat?>) {
        CAMERAX_LIVE_PREVIEW(
                R.string.pref_screen_title_camerax_live_preview,
                PreferenceFragment::class.java);

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val launchSource = intent.getSerializableExtra(EXTRA_LAUNCH_SOURCE) as LaunchSource
        val actionBar = supportActionBar
        actionBar?.setTitle(launchSource.titleResId)
        try {
            supportFragmentManager
                    .beginTransaction()
                    .replace(
                            R.id.settings_container,
                            launchSource.prefFragmentClass.getDeclaredConstructor().newInstance()!!)
                    .commit()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    companion object {
        const val EXTRA_LAUNCH_SOURCE = "extra_launch_source"
    }
}