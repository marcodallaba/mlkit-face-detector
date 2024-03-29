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

package com.marcodallaba.mlkit.kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.annotation.KeepName
import com.google.mlkit.common.MlKitException
import com.marcodallaba.mlkit.kotlin.facedetector.FaceDetectorProcessor
import com.marcodallaba.mlkit.kotlin.preference.PreferenceUtils
import com.marcodallaba.mlkit.kotlin.preference.SettingsActivity
import com.marcodallaba.mlkit.kotlin.preference.SettingsActivity.LaunchSource
import com.marcodallaba.mlkit.kotlin.viewmodels.CameraXViewModel
import com.marcodallaba.mlkit.kotlin.visionprocessors.VisionImageProcessor
import com.marcodallaba.mlkit.R
import com.marcodallaba.mlkit.databinding.ActivityCameraxLivePreviewBinding
import java.util.*


@KeepName
class CameraXLivePreviewActivity :
        AppCompatActivity(),
        ActivityCompat.OnRequestPermissionsResultCallback,
        CompoundButton.OnCheckedChangeListener {

    private lateinit var binding: ActivityCameraxLivePreviewBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var imageProcessor: VisionImageProcessor? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var cameraSelector: CameraSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        if (savedInstanceState != null) {
            lensFacing =
                    savedInstanceState.getInt(
                            STATE_LENS_FACING,
                            CameraSelector.LENS_FACING_BACK
                    )
        }
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        binding = ActivityCameraxLivePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.facingSwitch.setOnCheckedChangeListener(this)
        ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(application))
                .get(CameraXViewModel::class.java)
                .processCameraProvider
                .observe(
                        this,
                        { provider: ProcessCameraProvider? ->
                            cameraProvider = provider
                            if (allPermissionsGranted()) {
                                bindAllCameraUseCases()
                            }
                        }
                )

        binding.settingsButton.setOnClickListener {
            val intent =
                    Intent(applicationContext, SettingsActivity::class.java)
            intent.putExtra(
                    SettingsActivity.EXTRA_LAUNCH_SOURCE,
                    LaunchSource.CAMERAX_LIVE_PREVIEW
            )
            startActivity(intent)
        }

        if (!allPermissionsGranted()) {
            runtimePermissions
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putInt(STATE_LENS_FACING, lensFacing)
    }


    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        Log.d(TAG, "Set facing")
        if (cameraProvider == null) {
            return
        }
        val newLensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        val newCameraSelector =
                CameraSelector.Builder().requireLensFacing(newLensFacing).build()
        try {
            if (cameraProvider!!.hasCamera(newCameraSelector)) {
                lensFacing = newLensFacing
                cameraSelector = newCameraSelector
                bindAllCameraUseCases()
                return
            }
        } catch (e: CameraInfoUnavailableException) {
            // Falls through
        }
        Toast.makeText(
                applicationContext, "This device does not have lens with facing: $newLensFacing",
                Toast.LENGTH_SHORT
        )
                .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.live_preview_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra(
                    SettingsActivity.EXTRA_LAUNCH_SOURCE,
                    LaunchSource.CAMERAX_LIVE_PREVIEW
            )
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onResume() {
        super.onResume()
        bindAllCameraUseCases()
    }

    override fun onPause() {
        super.onPause()

        imageProcessor?.run {
            this.stop()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        imageProcessor?.run {
            this.stop()
        }
    }

    private fun bindAllCameraUseCases() {
        bindPreviewUseCase()
        bindAnalysisUseCase()
    }

    private fun bindPreviewUseCase() {
        if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
            return
        }
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        previewUseCase = Preview.Builder().build()
        previewUseCase!!.setSurfaceProvider(binding.previewView.surfaceProvider)
        cameraProvider!!.bindToLifecycle(/* lifecycleOwner= */this, cameraSelector!!, previewUseCase)
    }

    @SuppressLint("NewApi")
    private fun bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }
        if (imageProcessor != null) {
            imageProcessor!!.stop()
        }

        imageProcessor = try {
            val faceDetectorOptions =
                    PreferenceUtils.getFaceDetectorOptionsForLivePreview(this)
            FaceDetectorProcessor(this, faceDetectorOptions)
        } catch (e: Exception) {
            Log.e(
                    TAG,
                    "Can not create image processor: $FACE_DETECTION",
                    e
            )
            Toast.makeText(
                    applicationContext,
                    "Can not create image processor: " + e.localizedMessage,
                    Toast.LENGTH_LONG
            )
                    .show()
            return
        }

        val builder = ImageAnalysis.Builder()
        val targetAnalysisSize = PreferenceUtils.getCameraXTargetAnalysisSize(this)
        if (targetAnalysisSize != null) {
            builder.setTargetResolution(targetAnalysisSize)
        }
        analysisUseCase = builder.build()

        needUpdateGraphicOverlayImageSourceInfo = true

        analysisUseCase?.setAnalyzer(
                // imageProcessor.processImageProxy will use another thread to run the detection underneath,
                // thus we can just runs the analyzer itself on main thread.
                ContextCompat.getMainExecutor(this),
                { imageProxy: ImageProxy ->
                    if (needUpdateGraphicOverlayImageSourceInfo) {
                        val isImageFlipped =
                                lensFacing == CameraSelector.LENS_FACING_FRONT
                        val rotationDegrees =
                                imageProxy.imageInfo.rotationDegrees
                        if (rotationDegrees == 0 || rotationDegrees == 180) {
                            binding.graphicOverlay.setImageSourceInfo(
                                    imageProxy.width, imageProxy.height, isImageFlipped
                            )
                        } else {
                            binding.graphicOverlay.setImageSourceInfo(
                                    imageProxy.height, imageProxy.width, isImageFlipped
                            )
                        }
                        needUpdateGraphicOverlayImageSourceInfo = false
                    }
                    try {
                        imageProcessor!!.processImageProxy(imageProxy, binding.graphicOverlay)
                    } catch (e: MlKitException) {
                        Log.e(
                                TAG,
                                "Failed to process image. Error: " + e.localizedMessage
                        )
                        Toast.makeText(
                                applicationContext,
                                e.localizedMessage,
                                Toast.LENGTH_SHORT
                        )
                                .show()
                    }
                }
        )

        cameraProvider!!.bindToLifecycle( /* lifecycleOwner= */this, cameraSelector!!, analysisUseCase)
    }

    private val requiredPermissions: Array<String?>
        get() = try {
            val info = this.packageManager
                    .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: Exception) {
            arrayOfNulls(0)
        }

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(this, permission)) {
                return false
            }
        }
        return true
    }

    private val runtimePermissions: Unit
        get() {
            val allNeededPermissions: MutableList<String?> = ArrayList()
            for (permission in requiredPermissions) {
                if (!isPermissionGranted(this, permission)) {
                    allNeededPermissions.add(permission)
                }
            }
            if (allNeededPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                        this,
                        allNeededPermissions.toTypedArray(),
                        PERMISSION_REQUESTS
                )
            }
        }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        Log.i(TAG, "Permission granted!")
        if (allPermissionsGranted()) {
            bindAllCameraUseCases()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val TAG = "CameraXLivePreview"
        private const val PERMISSION_REQUESTS = 1
        private const val FACE_DETECTION = "Face Detection"
        private const val STATE_LENS_FACING = "lens_facing"

        private fun isPermissionGranted(
                context: Context,
                permission: String?
        ): Boolean {
            if (ContextCompat.checkSelfPermission(context, permission!!)
                    == PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(TAG, "Permission granted: $permission")
                return true
            }
            Log.i(TAG, "Permission NOT granted: $permission")
            return false
        }
    }
}
