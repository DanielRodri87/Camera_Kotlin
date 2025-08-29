package com.example.image_server2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.image_server2.databinding.ActivityMainBinding
import com.example.image_server2.databinding.DialogServerSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Atividade principal do aplicativo que gerencia a captura e envio de imagens.
 * Implementa funcionalidades de câmera usando CameraX e permite configurar
 * as informações do servidor para envio das imagens.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var settingsManager: SettingsManager

    /**
     * Inicializa a atividade, configura os listeners e inicia a câmera.
     * Também verifica se é primeira execução para solicitar configurações do servidor.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = SettingsManager(this)
        
        // Verificar se é a primeira execução para mostrar o diálogo de configurações
        if (settingsManager.isFirstRun()) {
            showServerSettingsDialog(true)
        } else {
            updateServerInfoText()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        binding.captureButton.setOnClickListener {
            takePhoto()
        }
        
        binding.settingsButton.setOnClickListener {
            showServerSettingsDialog(false)
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    /**
     * Atualiza o texto de informação do servidor na interface
     * com o IP e porta configurados.
     */
    private fun updateServerInfoText() {
        val serverIp = settingsManager.getServerIp()
        val serverPort = settingsManager.getServerPort()
        binding.serverInfoText.text = getString(R.string.server_info, serverIp, serverPort)
    }
    
    /**
     * Exibe diálogo para configuração do servidor.
     * @param isFirstRun indica se é primeira execução do app
     */
    private fun showServerSettingsDialog(isFirstRun: Boolean) {
        val dialogBinding = DialogServerSettingsBinding.inflate(LayoutInflater.from(this))
        
        // Preencher com valores salvos
        dialogBinding.ipAddressInput.setText(settingsManager.getServerIp())
        dialogBinding.portInput.setText(settingsManager.getServerPort().toString())
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val newIp = dialogBinding.ipAddressInput.text.toString()
                val newPortStr = dialogBinding.portInput.text.toString()
                
                if (newIp.isNotEmpty() && newPortStr.isNotEmpty()) {
                    try {
                        val newPort = newPortStr.toInt()
                        settingsManager.saveServerSettings(newIp, newPort)
                        updateServerInfoText()
                        
                        if (isFirstRun) {
                            settingsManager.setFirstRunCompleted()
                        }
                        
                        Snackbar.make(
                            binding.root,
                            R.string.settings_saved,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } catch (e: NumberFormatException) {
                        Toast.makeText(
                            this,
                            R.string.invalid_port,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            
        if (!isFirstRun) {
            dialog.setNegativeButton(R.string.cancel, null)
        } else {
            dialog.setCancelable(false)
        }
        
        dialog.show()
    }

    /**
     * Captura uma foto usando CameraX, processa e envia para o servidor.
     * O processamento inclui:
     * - Redimensionamento para largura máxima de 1280px
     * - Compressão JPEG com qualidade 80%
     * As imagens são salvas temporariamente e depois removidas.
     */
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Falha ao capturar foto: ${exc.message}", exc)
                    Snackbar.make(
                        binding.root,
                        R.string.error_capture_photo,
                        Snackbar.LENGTH_LONG
                    ).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val serverIp = settingsManager.getServerIp()
                    val serverPort = settingsManager.getServerPort()
                    
                    Snackbar.make(
                        binding.root,
                        R.string.image_captured,
                        Snackbar.LENGTH_SHORT
                    ).show()

                    // Processa a imagem em background
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // Processar imagem
                            val bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
                            val processedBitmap = if (bitmap.width > 1280) {
                                val ratio = 1280.0f / bitmap.width
                                val newHeight = (bitmap.height * ratio).toInt()
                                android.graphics.Bitmap.createScaledBitmap(bitmap, 1280, newHeight, true)
                            } else {
                                bitmap
                            }

                            // Salvar imagem processada
                            val processedFile = File(photoFile.parentFile, "processed_${photoFile.name}")
                            processedFile.outputStream().use { out ->
                                processedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                            }

                            // Enviar imagem processada
                            NetworkClient.sendImage(processedFile, serverIp, serverPort)
                            
                            // Limpar arquivos temporários
                            photoFile.delete()
                            processedFile.delete()

                            runOnUiThread {
                                Snackbar.make(
                                    binding.root,
                                    R.string.image_sent_success,
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Erro ao processar/enviar imagem: ${e.message}", e)
                            runOnUiThread {
                                Snackbar.make(
                                    binding.root,
                                    getString(R.string.error_sending_image, e.message),
                                    Snackbar.LENGTH_LONG
                                ).setAction(R.string.retry) {
                                    takePhoto()
                                }.show()
                            }
                        }
                    }
                }
            })
    }

    /**
     * Inicializa e configura a câmera usando CameraX.
     * Configura preview e captura de imagem.
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Falha na vinculação da câmera", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Verifica se todas as permissões necessárias foram concedidas.
     * @return true se todas as permissões foram concedidas, false caso contrário
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Obtém o diretório para salvar as imagens temporárias.
     * @return File apontando para o diretório de saída
     */
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) startCamera()
            else {
                Toast.makeText(this, "Permissões não concedidas", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        /** Tag para logs */
        private const val TAG = "CameraXApp"
        
        /** Formato do nome do arquivo de imagem */
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        
        /** Código de requisição para permissões */
        private const val REQUEST_CODE_PERMISSIONS = 10
        
        /** Lista de permissões necessárias */
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
