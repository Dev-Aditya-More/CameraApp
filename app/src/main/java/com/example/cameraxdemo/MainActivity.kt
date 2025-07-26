package com.example.cameraxdemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request camera permission
        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }
            val useFrontCamera = remember { mutableStateOf(false) }
            val lastPhoto = remember { mutableStateOf<File?>(null) }
            val flashVisible = remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreview(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    imageCapture = imageCapture,
                    useFrontCamera = useFrontCamera.value
                )

                // Flash Overlay
                if (flashVisible.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.8f))
                    )
                }

                // Capture Button
                Button(
                    onClick = {
                        val photoFile = File(getOutputDirectory(context), "${System.currentTimeMillis()}.jpg")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        // Flash effect
                        flashVisible.value = true

                        scope.launch {
                            delay(100)
                            flashVisible.value = false
                        }

                        // Shutter sound
                        MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)?.start()

                        imageCapture.value?.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    Toast.makeText(context, "Saved to ${photoFile.absolutePath}", Toast.LENGTH_SHORT).show()
                                    lastPhoto.value = photoFile
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp)

                ) {
                    Image(
                        painter = rememberAsyncImagePainter(R.drawable.capture),
                        contentDescription = "Capture",
                        modifier = Modifier.size(45.dp)
                    )
                }

                // Flip Camera Button
                Button(
                    onClick = { useFrontCamera.value = !useFrontCamera.value },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.Transparent
                    )

                ) {
                    Image(
                        painter = rememberAsyncImagePainter(R.drawable.camera),
                        contentDescription = "Flip Camera",
                        modifier = Modifier.size(45.dp)
                    )
                }

                ThumbnailWithFullScreenPreview(lastPhoto = lastPhoto)
            }
        }
    }

    private fun getOutputDirectory(context: Context): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, "CameraXApp").apply { mkdirs() }
        }
        return mediaDir ?: context.filesDir
    }
}

@Composable
fun ThumbnailWithFullScreenPreview(lastPhoto: MutableState<File?>) {
    var isImageOpen by remember { mutableStateOf(false) }

    lastPhoto.value?.let { file ->
        Box {
            // Thumbnail Image
            Image(
                painter = rememberAsyncImagePainter(file),
                contentDescription = "Last Captured",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .size(60.dp)
                    .background(Color.LightGray, shape = CircleShape)
                    .clip(CircleShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        isImageOpen = true
                    }
            )

            // Fullscreen Dialog
            if (isImageOpen) {
                Dialog(onDismissRequest = { isImageOpen = false }) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.9f))
                            .clickable { isImageOpen = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(file),
                            contentDescription = "Full Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}
