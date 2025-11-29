package com.health.virtualdoctor.ui.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*

class WebRTCClient(
    private val context: Context,
    private val eglBase: EglBase,
    private val localVideoView: SurfaceViewRenderer,
    private val remoteVideoView: SurfaceViewRenderer,
    private val listener: Listener
) {
    companion object {
        private const val TAG = "WebRTCClient"
    }

    interface Listener {
        fun onLocalVideoReady()
        fun onRemoteVideoReady()
        fun onIceCandidate(candidate: IceCandidate)
        fun onIceConnectionChange(state: PeerConnection.IceConnectionState)
        fun onOfferCreated(sdp: SessionDescription)
        fun onAnswerCreated(sdp: SessionDescription)
    }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null

    init {
        initializePeerConnectionFactory()
        createPeerConnectionFactory()
    }

    private fun initializePeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)
        Log.d(TAG, "✅ PeerConnectionFactory initialized")
    }

    private fun createPeerConnectionFactory() {
        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase.eglBaseContext,
            true,
            true
        )

        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = false
                disableNetworkMonitor = false
            })
            .createPeerConnectionFactory()

        Log.d(TAG, "✅ PeerConnectionFactory created")
    }

    fun startLocalVideo() {
        val videoSource = peerConnectionFactory?.createVideoSource(false)
        this.videoSource = videoSource

        // Initialize camera capturer
        videoCapturer = createCameraCapturer()

        videoCapturer?.initialize(
            SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext),
            context,
            videoSource?.capturerObserver
        )

        // Start capturing
        videoCapturer?.startCapture(1280, 720, 30)

        // Create local video track
        localVideoTrack = peerConnectionFactory?.createVideoTrack("local_video", videoSource)
        localVideoTrack?.addSink(localVideoView)

        // Create local audio track
        val audioConstraints = MediaConstraints()
        audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("local_audio", audioSource)

        listener.onLocalVideoReady()
        Log.d(TAG, "✅ Local video started")
    }

    private fun createCameraCapturer(): CameraVideoCapturer {
        val enumerator = Camera2Enumerator(context)

        // Try front camera first
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    Log.d(TAG, "📷 Using front camera: $deviceName")
                    return capturer
                }
            }
        }

        // Fallback to back camera
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    Log.d(TAG, "📷 Using back camera: $deviceName")
                    return capturer
                }
            }
        }

        throw RuntimeException("No camera found")
    }

    fun createPeerConnection(iceServers: List<PeerConnection.IceServer>) {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {
                    Log.d(TAG, "🧊 ICE candidate generated")
                    listener.onIceCandidate(candidate)
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                    Log.d(TAG, "🧊 ICE candidates removed: ${candidates?.size ?: 0}")
                }

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                    Log.d(TAG, "🔗 ICE connection state: $state")
                    listener.onIceConnectionChange(state)
                }

                override fun onAddStream(stream: MediaStream) {
                    Log.d(TAG, "📺 Remote stream added")
                    stream.videoTracks?.firstOrNull()?.addSink(remoteVideoView)
                    listener.onRemoteVideoReady()
                }

                override fun onSignalingChange(state: PeerConnection.SignalingState) {
                    Log.d(TAG, "📡 Signaling state: $state")
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) {
                    Log.d(TAG, "🔄 ICE receiving: $receiving")
                }

                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                    Log.d(TAG, "🧊 ICE gathering: $state")
                }

                override fun onRemoveStream(stream: MediaStream) {
                    Log.d(TAG, "❌ Remote stream removed")
                }

                override fun onDataChannel(channel: DataChannel) {
                    Log.d(TAG, "📨 Data channel: ${channel.label()}")
                }

                override fun onRenegotiationNeeded() {
                    Log.d(TAG, "🔄 Renegotiation needed")
                }

                override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {
                    Log.d(TAG, "🎵 Track added: ${receiver.track()?.kind()}")
                }
            }
        )

        // Add local tracks
        localVideoTrack?.let { peerConnection?.addTrack(it, listOf("stream_id")) }
        localAudioTrack?.let { peerConnection?.addTrack(it, listOf("stream_id")) }

        Log.d(TAG, "✅ PeerConnection created")
    }

    fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                Log.d(TAG, "📤 Offer created")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        Log.d(TAG, "✅ Local description set")
                        listener.onOfferCreated(sdp)
                    }
                    override fun onSetFailure(error: String) {
                        Log.e(TAG, "❌ Set local description failed: $error")
                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, sdp)
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String) {
                Log.e(TAG, "❌ Create offer failed: $error")
            }
            override fun onSetFailure(error: String) {}
        }, constraints)
    }

    fun handleOffer(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d(TAG, "✅ Remote description set")
                createAnswer()
            }

            override fun onSetFailure(error: String) {
                Log.e(TAG, "❌ Set remote description failed: $error")
            }

            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sdp)
    }

    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                Log.d(TAG, "📥 Answer created")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        Log.d(TAG, "✅ Local answer set")
                        listener.onAnswerCreated(sdp)
                    }
                    override fun onSetFailure(error: String) {
                        Log.e(TAG, "❌ Set local answer failed: $error")
                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, sdp)
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String) {
                Log.e(TAG, "❌ Create answer failed: $error")
            }
            override fun onSetFailure(error: String) {}
        }, constraints)
    }

    fun handleAnswer(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d(TAG, "✅ Remote answer set")
            }

            override fun onSetFailure(error: String) {
                Log.e(TAG, "❌ Set remote answer failed: $error")
            }

            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
        Log.d(TAG, "🧊 ICE candidate added")
    }

    fun setMicEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
        Log.d(TAG, "🎤 Mic ${if (enabled) "enabled" else "muted"}")
    }

    fun setVideoEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
        Log.d(TAG, "📹 Video ${if (enabled) "enabled" else "disabled"}")
    }

    fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
        Log.d(TAG, "🔄 Camera switched")
    }

    fun dispose() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()

            localVideoTrack?.dispose()
            localAudioTrack?.dispose()
            videoSource?.dispose()
            audioSource?.dispose()

            peerConnection?.close()
            peerConnection?.dispose()

            peerConnectionFactory?.dispose()

            Log.d(TAG, "🧹 WebRTC disposed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Disposal error", e)
        }
    }
}