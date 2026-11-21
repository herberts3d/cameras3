package com.localcam.viewer

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.localcam.viewer.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var configStore: CameraConfigStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configStore = CameraConfigStore(this)

        binding.btnBack.setOnClickListener {
            finish()
        }

        loadCurrentConfigs()

        binding.btnSave.setOnClickListener {
            saveConfigs()
        }
    }

    private fun loadCurrentConfigs() {
        val cam1 = configStore.getCamera(1)
        binding.switchCam1Enabled.isChecked = cam1.enabled
        binding.inputCam1Name.setText(cam1.name)
        binding.inputCam1Ip.setText(cam1.ip)
        binding.inputCam1Port.setText(cam1.port.toString())
        binding.inputCam1Path.setText(cam1.path)
        binding.inputCam1User.setText(cam1.username)
        binding.inputCam1Pass.setText(cam1.password)

        val cam2 = configStore.getCamera(2)
        binding.switchCam2Enabled.isChecked = cam2.enabled
        binding.inputCam2Name.setText(cam2.name)
        binding.inputCam2Ip.setText(cam2.ip)
        binding.inputCam2Port.setText(cam2.port.toString())
        binding.inputCam2Path.setText(cam2.path)
        binding.inputCam2User.setText(cam2.username)
        binding.inputCam2Pass.setText(cam2.password)
    }

    private fun saveConfigs() {
        val cam1Enabled = binding.switchCam1Enabled.isChecked
        val cam1Ip = binding.inputCam1Ip.text?.toString()?.trim().orEmpty()
        val cam1Port = binding.inputCam1Port.text?.toString()?.trim()?.toIntOrNull() ?: 554

        if (cam1Enabled && cam1Ip.isEmpty()) {
            binding.inputCam1Ip.error = getString(R.string.error_ip_required)
            binding.inputCam1Ip.requestFocus()
            return
        }
        if (cam1Port !in 1..65535) {
            binding.inputCam1Port.error = getString(R.string.error_port_invalid)
            binding.inputCam1Port.requestFocus()
            return
        }

        val cam2Enabled = binding.switchCam2Enabled.isChecked
        val cam2Ip = binding.inputCam2Ip.text?.toString()?.trim().orEmpty()
        val cam2Port = binding.inputCam2Port.text?.toString()?.trim()?.toIntOrNull() ?: 554

        if (cam2Port !in 1..65535) {
            binding.inputCam2Port.error = getString(R.string.error_port_invalid)
            binding.inputCam2Port.requestFocus()
            return
        }

        val cam1 = CameraConfig(
            id = 1,
            enabled = cam1Enabled,
            name = binding.inputCam1Name.text?.toString().orEmpty(),
            ip = cam1Ip,
            port = cam1Port,
            path = binding.inputCam1Path.text?.toString().orEmpty(),
            username = binding.inputCam1User.text?.toString().orEmpty(),
            password = binding.inputCam1Pass.text?.toString().orEmpty()
        )

        val cam2 = CameraConfig(
            id = 2,
            enabled = cam2Enabled,
            name = binding.inputCam2Name.text?.toString().orEmpty(),
            ip = cam2Ip,
            port = cam2Port,
            path = binding.inputCam2Path.text?.toString().orEmpty(),
            username = binding.inputCam2User.text?.toString().orEmpty(),
            password = binding.inputCam2Pass.text?.toString().orEmpty()
        )

        configStore.saveCamera(cam1)
        configStore.saveCamera(cam2)

        Toast.makeText(this, R.string.msg_saved_securely, Toast.LENGTH_SHORT).show()
        finish()
    }
}
