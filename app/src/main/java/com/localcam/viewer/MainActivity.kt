package com.localcam.viewer

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import com.localcam.viewer.databinding.ActivityMainBinding

@OptIn(UnstableApi::class)
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var configStore: CameraConfigStore

    private data class CameraSlotUi(
        val id: Int,
        val container: View,
        val playerView: PlayerView,
        val titleText: TextView,
        val addressText: TextView,
        val statusText: TextView,
        val overlayContainer: View,
        val overlayMessageText: TextView,
        val progressBar: ProgressBar,
        val audioButton: ImageButton,
        val refreshButton: ImageButton,
        val expandButton: ImageButton
    )

    private var player1: ExoPlayer? = null
    private var player2: ExoPlayer? = null

    private var muted1: Boolean = true
    private var muted2: Boolean = true
    private var expandedSlotId: Int? = null

    private lateinit var slot1Ui: CameraSlotUi
    private lateinit var slot2Ui: CameraSlotUi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configStore = CameraConfigStore(this)

        slot1Ui = CameraSlotUi(
            id = 1,
            container = binding.camera1Container,
            playerView = binding.playerView1,
            titleText = binding.cam1Title,
            addressText = binding.cam1Address,
            statusText = binding.cam1Status,
            overlayContainer = binding.cam1Overlay,
            overlayMessageText = binding.cam1OverlayMessage,
            progressBar = binding.cam1Progress,
            audioButton = binding.btnCam1Audio,
            refreshButton = binding.btnCam1Refresh,
            expandButton = binding.btnCam1Expand
        )

        slot2Ui = CameraSlotUi(
            id = 2,
            container = binding.camera2Container,
            playerView = binding.playerView2,
            titleText = binding.cam2Title,
            addressText = binding.cam2Address,
            statusText = binding.cam2Status,
            overlayContainer = binding.cam2Overlay,
            overlayMessageText = binding.cam2OverlayMessage,
            progressBar = binding.cam2Progress,
            audioButton = binding.btnCam2Audio,
            refreshButton = binding.btnCam2Refresh,
            expandButton = binding.btnCam2Expand
        )

        binding.btnOpenSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        setupSlotControls(slot1Ui)
        setupSlotControls(slot2Ui)
        applyLayoutOrientation(resources.configuration.orientation)
    }

    override fun onStart() {
        super.onStart()
        startAllCameras()
    }

    override fun onStop() {
        super.onStop()
        releaseAllPlayers()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyLayoutOrientation(newConfig.orientation)
    }

    private fun applyLayoutOrientation(orientation: Int) {
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
        binding.camerasContainer.orientation =
            if (isLandscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL

        val marginPx = resources.getDimensionPixelSize(R.dimen.camera_gap)
        val params1 = (binding.camera1Container.layoutParams as LinearLayout.LayoutParams).apply {
            if (isLandscape) {
                width = 0
                height = LinearLayout.LayoutParams.MATCH_PARENT
                weight = 1f
                setMargins(0, 0, if (expandedSlotId == null) marginPx / 2 else 0, 0)
            } else {
                width = LinearLayout.LayoutParams.MATCH_PARENT
                height = 0
                weight = 1f
                setMargins(0, 0, 0, if (expandedSlotId == null) marginPx / 2 else 0)
            }
        }
        val params2 = (binding.camera2Container.layoutParams as LinearLayout.LayoutParams).apply {
            if (isLandscape) {
                width = 0
                height = LinearLayout.LayoutParams.MATCH_PARENT
                weight = 1f
                setMargins(if (expandedSlotId == null) marginPx / 2 else 0, 0, 0, 0)
            } else {
                width = LinearLayout.LayoutParams.MATCH_PARENT
                height = 0
                weight = 1f
                setMargins(0, if (expandedSlotId == null) marginPx / 2 else 0, 0, 0)
            }
        }

        binding.camera1Container.layoutParams = params1
        binding.camera2Container.layoutParams = params2
    }

    private fun setupSlotControls(ui: CameraSlotUi) {
        ui.audioButton.setOnClickListener {
            if (ui.id == 1) {
                muted1 = !muted1
                player1?.volume = if (muted1) 0f else 1f
                updateAudioIcon(ui, muted1)
            } else {
                muted2 = !muted2
                player2?.volume = if (muted2) 0f else 1f
                updateAudioIcon(ui, muted2)
            }
        }

        ui.refreshButton.setOnClickListener {
            restartCamera(ui.id)
        }

        ui.expandButton.setOnClickListener {
            expandedSlotId = if (expandedSlotId == ui.id) null else ui.id
            updateExpandedState()
        }

        ui.overlayContainer.setOnClickListener {
            val config = configStore.getCamera(ui.id)
            if (!config.isConfigured()) {
                startActivity(Intent(this, SettingsActivity::class.java))
            } else {
                restartCamera(ui.id)
            }
        }
    }

    private fun updateExpandedState() {
        when (expandedSlotId) {
            1 -> {
                binding.camera1Container.visibility = View.VISIBLE
                binding.camera2Container.visibility = View.GONE
                slot1Ui.expandButton.setImageResource(R.drawable.ic_fullscreen_exit)
                slot2Ui.expandButton.setImageResource(R.drawable.ic_fullscreen)
            }
            2 -> {
                binding.camera1Container.visibility = View.GONE
                binding.camera2Container.visibility = View.VISIBLE
                slot1Ui.expandButton.setImageResource(R.drawable.ic_fullscreen)
                slot2Ui.expandButton.setImageResource(R.drawable.ic_fullscreen_exit)
            }
            else -> {
                binding.camera1Container.visibility = View.VISIBLE
                binding.camera2Container.visibility = View.VISIBLE
                slot1Ui.expandButton.setImageResource(R.drawable.ic_fullscreen)
                slot2Ui.expandButton.setImageResource(R.drawable.ic_fullscreen)
            }
        }
        applyLayoutOrientation(resources.configuration.orientation)
    }

    private fun updateAudioIcon(ui: CameraSlotUi, isMuted: Boolean) {
        if (isMuted) {
            ui.audioButton.setImageResource(R.drawable.ic_volume_off)
            ui.audioButton.contentDescription = getString(R.string.cd_unmute)
            ui.audioButton.alpha = 0.75f
        } else {
            ui.audioButton.setImageResource(R.drawable.ic_volume_on)
            ui.audioButton.contentDescription = getString(R.string.cd_mute)
            ui.audioButton.alpha = 1.0f
        }
    }

    private fun startAllCameras() {
        startCamera(1)
        startCamera(2)
    }

    private fun restartCamera(slotId: Int) {
        releasePlayer(slotId)
        startCamera(slotId)
    }

    private fun startCamera(slotId: Int) {
        val ui = if (slotId == 1) slot1Ui else slot2Ui
        val isMuted = if (slotId == 1) muted1 else muted2
        val config = configStore.getCamera(slotId)

        ui.titleText.text = config.name
        ui.addressText.text = config.buildSafeDisplayAddress()
        updateAudioIcon(ui, isMuted)

        if (!config.enabled) {
            setStatus(ui, getString(R.string.status_disabled), R.color.status_muted)
            ui.overlayContainer.visibility = View.VISIBLE
            ui.progressBar.visibility = View.GONE
            ui.overlayMessageText.text = getString(R.string.msg_camera_disabled)
            return
        }

        if (!config.isConfigured()) {
            setStatus(ui, getString(R.string.status_unconfigured), R.color.status_warning)
            ui.overlayContainer.visibility = View.VISIBLE
            ui.progressBar.visibility = View.GONE
            ui.overlayMessageText.text = getString(R.string.msg_tap_to_configure)
            return
        }

        setStatus(ui, getString(R.string.status_connecting), R.color.status_warning)
        ui.overlayContainer.visibility = View.VISIBLE
        ui.progressBar.visibility = View.VISIBLE
        ui.overlayMessageText.text = getString(R.string.msg_connecting_udp)

        // Baixo buffer para manter o vídeo ao vivo com atraso mínimo
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(500, 2000, 250, 500)
            .build()

        val player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .build()

        // IMPORTANTE: setForceUseRtpTcp(false) mantém o transporte padrão UDP.
        // Forçar TCP nessas câmeras gera erro "Nonmatching transport in server reply".
        val rtspFactory = RtspMediaSource.Factory()
            .setForceUseRtpTcp(false)
            .setTimeoutMs(10000L)

        val mediaSource = rtspFactory.createMediaSource(MediaItem.fromUri(config.buildRtspUri()))

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        setStatus(ui, getString(R.string.status_connecting), R.color.status_warning)
                        ui.overlayContainer.visibility = View.VISIBLE
                        ui.progressBar.visibility = View.VISIBLE
                        ui.overlayMessageText.text = getString(R.string.msg_connecting_udp)
                    }
                    Player.STATE_READY -> {
                        setStatus(ui, getString(R.string.status_live), R.color.status_live)
                        ui.overlayContainer.visibility = View.GONE
                        ui.progressBar.visibility = View.GONE
                    }
                    Player.STATE_ENDED -> {
                        setStatus(ui, getString(R.string.status_disconnected), R.color.status_error)
                        ui.overlayContainer.visibility = View.VISIBLE
                        ui.progressBar.visibility = View.GONE
                        ui.overlayMessageText.text = getString(R.string.msg_stream_ended)
                    }
                    Player.STATE_IDLE -> {
                        // Mantém estado atual até preparação ou erro
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                setStatus(ui, getString(R.string.status_error), R.color.status_error)
                ui.overlayContainer.visibility = View.VISIBLE
                ui.progressBar.visibility = View.GONE
                ui.overlayMessageText.text = getString(
                    R.string.msg_connection_error,
                    error.errorCodeName
                )
            }
        })

        ui.playerView.player = player
        player.setMediaSource(mediaSource)
        player.volume = if (isMuted) 0f else 1f
        player.playWhenReady = true
        player.prepare()

        if (slotId == 1) {
            player1 = player
        } else {
            player2 = player
        }
    }

    private fun setStatus(ui: CameraSlotUi, label: String, colorRes: Int) {
        ui.statusText.text = label
        ui.statusText.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    private fun releasePlayer(slotId: Int) {
        if (slotId == 1) {
            slot1Ui.playerView.player = null
            player1?.release()
            player1 = null
        } else {
            slot2Ui.playerView.player = null
            player2?.release()
            player2 = null
        }
    }

    private fun releaseAllPlayers() {
        releasePlayer(1)
        releasePlayer(2)
    }
}
