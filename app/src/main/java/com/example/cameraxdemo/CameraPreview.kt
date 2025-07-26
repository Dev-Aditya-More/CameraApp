package com.example.cameraxdemo

import android.content.Context
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner

@Composable
fun CameraPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    imageCapture: MutableState<ImageCapture?>,
    useFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val previewView = remember { PreviewView(context) }

    AndroidView(factory = { previewView }, modifier = modifier)

    LaunchedEffect(useFrontCamera) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val imageCaptureUseCase = ImageCapture.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .build()

        val cameraSelector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCaptureUseCase
            )
            imageCapture.value = imageCaptureUseCase
        } catch (exc: Exception) {
            exc.printStackTrace()
        }
    }
}