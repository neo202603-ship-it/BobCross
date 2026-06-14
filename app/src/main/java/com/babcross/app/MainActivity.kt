package com.babcross.app

import android.Manifest
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.babcross.app.data.MenuCalorieCatalog
import com.babcross.app.data.MenuCatalog
import com.babcross.app.data.NearVoteStore
import com.babcross.app.data.NearbyPoll
import com.babcross.app.data.PollCompletionPolicy
import com.babcross.app.data.PollParticipationPolicy
import com.babcross.app.data.PollDefaults
import com.babcross.app.data.PollTemplate
import com.babcross.app.data.SharedResult
import com.babcross.app.data.SharedResultInsights
import com.babcross.app.data.VoteReceipt
import com.babcross.app.nearby.NearbyVoteConnectionManager
import com.babcross.app.protocol.NearVoteMessage
import com.babcross.app.protocol.NearVoteMessageReplayGuard
import com.babcross.app.protocol.NearVoteMessageType
import com.babcross.app.protocol.NearVoteMessageValidator
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.babcross.app.simulation.LocalVoteSimulator
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : ComponentActivity(), NearbyVoteConnectionManager.Listener {
    private lateinit var page: LinearLayout
    private var pageScrollView: ScrollView? = null
    private var keyboardLiftTarget: View? = null
    private var keyboardLiftToBottom = false
    private lateinit var logView: TextView
    private lateinit var connectionStatusView: TextView
    private var topConnectionBadgeContainerView: FrameLayout? = null
    private var topConnectionBadgePulseView: View? = null
    private var topConnectionSignalView: ConnectionSignalView? = null
    private var compactTitleBarView: LinearLayout? = null
    private var compactTitleTextView: TextView? = null
    private var expandedTitleView: View? = null
    private var stickyHomeTopCardView: View? = null
    private var pageFrameRoot: FrameLayout? = null
    private var stickyBottomActionView: View? = null
    private var stickyBottomPaddingExtra = 0
    private var onboardingCoachOverlay: View? = null
    private var onboardingHomeAvatarTarget: View? = null
    private var onboardingHomeCreateTarget: View? = null
    private var onboardingHomeDemoTarget: View? = null
    private var onboardingSettingsTarget: View? = null
    private var simulationCoachTarget: View? = null
    private var tutorialCategoryTarget: View? = null
    private var tutorialQuestionTarget: View? = null
    private var tutorialOptionsTarget: View? = null
    private var tutorialPublishTarget: View? = null
    private var tutorialResponseTarget: View? = null
    private var tutorialEndTarget: View? = null
    private var tutorialResult: SharedResult? = null
    private var tutorialPoll: NearbyPoll? = null
    private var pendingJoinStatusText: TextView? = null
    private var pendingJoinDotStep = 0
    private val pendingJoinDots = object : Runnable {
        override fun run() {
            val hint = pendingJoinHint
            val status = pendingJoinStatusText
            if (hint == null || status == null) return
            pendingJoinDotStep = (pendingJoinDotStep + 1) % 4
            status.text = "밥판을 연결 중 입니다" + ".".repeat(pendingJoinDotStep + 1)
            handler.postDelayed(this, JOIN_PENDING_DOT_MS)
        }
    }
    private lateinit var nearby: NearbyVoteConnectionManager
    private lateinit var simulator: LocalVoteSimulator
    private lateinit var store: NearVoteStore
    private val messageReplayGuard = NearVoteMessageReplayGuard()
    private val avatarSheet: Bitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.avatar_sheet) }
    private val transparentAvatarCache = mutableMapOf<Int, Bitmap>()
    private val handler = Handler(Looper.getMainLooper())
    private val nearbyHeartbeat = object : Runnable {
        override fun run() {
            applyNearbyRoleMode()
            updateConnectionStatus()
            handler.postDelayed(this, NEARBY_HEARTBEAT_MS)
        }
    }
    private val nearbyPulse = object : Runnable {
        override fun run() {
            animateConnectionSearchPulse()
            retryNearbySearchIfWaiting()
            handler.postDelayed(this, NEARBY_PULSE_MS)
        }
    }
    private var selfName = ""
    private var selfAvatarId = 0
    private var userId = ""
    private var autoConnectEnabled = true
    private var connectedCount = 0
    private val activePolls = linkedMapOf<String, NearbyPoll>()
    private val incomingPolls = linkedMapOf<String, NearbyPoll>()
    private var latestReceipt: VoteReceipt? = null
    private var sharedResult: SharedResult? = null
    private val sharedResultsByPoll = linkedMapOf<String, SharedResult>()
    private val receivedVotesByPoll = linkedMapOf<String, LinkedHashMap<String, String>>()
    private val receivedVoteNamesByPoll = linkedMapOf<String, LinkedHashMap<String, String>>()
    private val receivedVoteAvatarsByPoll = linkedMapOf<String, LinkedHashMap<String, Int>>()
    private val voteEndpointIdsByPoll = linkedMapOf<String, LinkedHashMap<String, String>>()
    private val invitedPeersByPoll = linkedMapOf<String, LinkedHashMap<String, String>>()
    private val pollResponsesByPoll = linkedMapOf<String, LinkedHashMap<String, String>>()
    private val hostConnectedCountsByPoll = linkedMapOf<String, Int>()
    private val hostReadyCountsByPoll = linkedMapOf<String, Int>()
    private val completionPromptShownKeys = linkedSetOf<String>()
    private val submittedVotes = linkedMapOf<String, String>()
    private val acceptedPollIds = linkedSetOf<String>()
    private val declinedPollIds = linkedSetOf<String>()
    private var declinedPollNotice: String? = null
    private var voteChoiceGuardUntilMillis = 0L
    private val sharedResultPollIds = linkedSetOf<String>()
    private val seenIncomingPollIds = linkedSetOf<String>()
    private val seenResultPollIds = linkedSetOf<String>()
    private val screenBackStack = ArrayDeque<() -> Unit>()
    private var currentScreen: (() -> Unit)? = null
    private var restoringScreen = false
    private var composeSuggestionToken = 0
    private var permissionIntroShown = false
    private var pendingJoinHint: JoinHint? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val granted = hasNearbyPermissions()
        appendLog(if (granted) "권한 준비 완료" else "일부 권한이 꺼져 있음")
        if (granted) {
            applyAutoConnectSetting()
        } else {
            updateConnectionStatus()
            showPermissionFallbackDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = NearVoteStore(this)
        selfName = store.loadIdentity { suggestIdentity() }
        userId = store.loadUserId { UUID.randomUUID().toString() }
        selfAvatarId = store.loadAvatarId { avatarIdForUser(userId) }
        autoConnectEnabled = store.isAutoConnectEnabled()
        nearby = NearbyVoteConnectionManager(this, selfName, this)
        simulator = LocalVoteSimulator(selfName)
        restoreSessionState()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBackInApp()
            }
        })
        showHome()
        if (store.hasCompletedOnboarding()) {
            applyAutoConnectSetting()
        } else {
            showFirstRunOnboarding()
        }
        handleJoinIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleJoinIntent(intent)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        nearby.stop()
        super.onDestroy()
    }

    override fun onLog(message: String) = appendLog(message)

    override fun onMessage(endpointId: String, message: String) {
        appendLog("$endpointId 에서 메시지 수신: $message")
        handleNearbyMessage(endpointId, message)
    }

    override fun onEndpointFound(endpointId: String, endpointName: String) {
        animateConnectionSearchPulse()
        appendLog("발견: $endpointName ($endpointId)")
    }

    override fun onEndpointConnected(endpointId: String) {
        appendLog("연결됨: $endpointId")
        sendProfile(endpointId)
        handler.postDelayed({
            sendProfile(endpointId)
            resendAcceptedPollResponses(endpointId)
        }, CONNECTION_SYNC_DELAY_MS)
    }

    override fun onEndpointDisconnected(endpointId: String) {
        appendLog("연결 해제: $endpointId")
    }

    override fun onConnectionCountChanged(count: Int) {
        val countChanged = connectedCount != count
        connectedCount = count
        updateConnectionStatus(animateBadge = countChanged)
        sendHostConnectionSync()
    }

    private fun showHome() {
        setPage("홈", compactTitle = "밥크로스")
        rememberScreen { showHome() }
        page.addView(homeDashboardCard())
        if (!store.hasCompletedDemo()) {
            page.addView(experienceCard())
        }
        if (!hasNearbyPermissions() || disabledConnectionRequirements().isNotEmpty()) {
            page.addView(connectionFixCard())
        }
        addCurrentSessionCards()
    }

    private fun addCurrentSessionCards() {
        var hasActiveSession = false
        page.addView(sectionTitle("열린 밥판"))
        declinedPollNotice?.let { notice ->
            page.addView(statusCard("밥판 초대 거절", notice))
            declinedPollNotice = null
        }
        visibleActivePolls().forEach { poll ->
            hasActiveSession = true
            val status = if (poll.hasEnded()) {
                "밥판 종료"
            } else {
                "밥신호 발신 중"
            }
            page.addView(pollActionCard(poll.question, status, poll) { showPublishedPoll(poll) })
        }
        visibleIncomingPolls().forEach { poll ->
            hasActiveSession = true
            val submitted = submittedVotes[poll.id]
            val accepted = acceptedPollIds.contains(poll.id)
            val status = when {
                submitted != null -> "내 선택: $submitted"
                accepted -> "참여 중"
                else -> "${poll.proposerName}님의 밥판 초대"
            }
            page.addView(pollActionCard(poll.question, status, poll) {
                if (accepted || submitted != null) {
                    showVotePoll(poll)
                } else {
                    showPollInvitation(poll)
                }
            })
        }
        if (!hasActiveSession) {
            val hint = if (connectedCount == 0) {
                "밥판을 열거나, 가까운 밥친구가 보내는 신호를 기다릴 수 있습니다."
            } else {
                "연결된 밥친구와 새 밥판을 열어 메뉴를 정해보세요."
            }
            page.addView(emptyCard("열린 밥판 없음", hint))
        }
        latestHomeResult()?.let { result ->
            page.addView(sectionTitle("최근 결정"))
            page.addView(resultActionCard(result.question, friendlyTime(result.createdAtMillis), result) {
                showSharedResult(result)
            })
        }
    }

    private fun latestHomeResult(): SharedResult? {
        val resultsByPollId = linkedMapOf<String, SharedResult>()
        store.loadResultHistory().forEach { result -> resultsByPollId[result.pollId] = result }
        sharedResultsByPoll.values.forEach { result -> resultsByPollId[result.pollId] = result }
        sharedResult?.let { result -> resultsByPollId[result.pollId] = result }
        return resultsByPollId.values.maxByOrNull { result -> result.createdAtMillis }
    }

    private fun showHistory() {
        val results = store.loadResultHistory()
        setPage("결과")
        rememberScreen { showHistory() }
        page.addView(breadcrumb("홈", "결과"))
        page.addView(topBar("지난 메뉴 결정"))
        if (results.isEmpty()) {
            page.addView(emptyCard("저장된 메뉴 없음", "밥판 결과를 공유받거나 직접 결정하면 여기에 남습니다."))
            page.addView(primaryButton("밥판 열기") { showCompose() })
        } else {
            page.addView(historyReuseShelf(results))
            page.addView(actionCard("이번 주 밥상 결산", "왕좌에 오른 메뉴와 아깝게 놓친 메뉴를 봅니다.") {
                showWeeklyMenuReport()
            })
            results.forEach { result ->
                page.addView(resultActionCard(result.question, friendlyTime(result.createdAtMillis), result) {
                    showSharedResult(result)
                })
            }
        }
    }

    private fun showWeeklyMenuReport() {
        val report = weeklyMenuReport()
        setPage("결과")
        rememberScreen { showWeeklyMenuReport() }
        page.addView(breadcrumb("홈", "결과", "이번 주 밥상 결산"))
        page.addView(topBar("이번 주 밥상 결산"))
        page.addView(bodyText("최근 1주일 동안 밥판에서 왕좌를 차지한 메뉴와, 박수 직전에서 멈춘 메뉴를 모았습니다."))
        if (report.resultCount == 0) {
            page.addView(emptyCard("최근 결과 없음", "밥판 결과가 쌓이면 자주 선택된 메뉴와 아쉬운 2등 메뉴가 여기에 표시됩니다."))
            page.addView(primaryButton("밥판 열기") { showCompose() })
            return
        }
        page.addView(statusCard("분석한 밥판", "${report.resultCount}건 · ${report.totalVotes}표"))
        page.addView(weeklyReportEntryCard(
            label = "가장 많이 선택된 메뉴",
            menu = report.topSelectedMenu ?: "아직 없음",
            detail = report.topSelectedDetail ?: "선택된 표가 없습니다.",
            accentColor = 0xFFD73B24.toInt()
        ))
        page.addView(weeklyReportEntryCard(
            label = "아쉽게 2등한 메뉴",
            menu = report.topRunnerUpMenu ?: "아직 없음",
            detail = report.topRunnerUpDetail ?: "2등 메뉴가 나올 만큼 표가 갈린 밥판이 아직 없습니다.",
            accentColor = 0xFF3D8B67.toInt()
        ))
        page.addView(outlineButton("지난 메뉴 결정으로") { showHistory() })
    }

    private fun showMyPage() {
        setPage("설정")
        rememberScreen { showMyPage() }
        page.addView(breadcrumb("홈", "설정", "내 밥닉"))
        page.addView(topBar("내 밥닉"))
        page.addView(bodyText("밥닉은 결과와 밥친구 목록에 표시됩니다. 따로 만들지 않아도 제안 밥닉을 바로 사용할 수 있습니다."))
        val identityInput = inputBox("내 밥닉", selfName)
        var selectedAvatarId = selfAvatarId
        var selectSuggestedAvatar: ((Int) -> Unit)? = null
        page.addView(label("아바타"))
        page.addView(avatarPicker(
            initialAvatarId = selfAvatarId,
            onSelected = { selectedAvatarId = it },
            bindSelector = { selector -> selectSuggestedAvatar = selector }
        ))
        page.addView(labelActionRow(
            "현재 밥닉",
            compactButton("밥닉 제안", BUTTON_CHOICE) {
                val suggestion = suggestIdentityWithAvatar()
                identityInput.setText(suggestion.name)
                selectedAvatarId = suggestion.avatarId
                selectSuggestedAvatar?.invoke(suggestion.avatarId)
            }
        ))
        page.addView(identityInput)
        page.addView(primaryButton("저장하기") {
                val nextIdentity = identityInput.text.toString().trim()
                if (nextIdentity.length < 2) {
                    Toast.makeText(this, "밥닉은 2글자 이상 입력해 주세요.", Toast.LENGTH_SHORT).show()
                    return@primaryButton
                }
                saveIdentity(nextIdentity, selectedAvatarId)
                Toast.makeText(this, "밥닉 저장 완료", Toast.LENGTH_SHORT).show()
                showHome()
        })
    }

    private fun showSettings() {
        setPage("설정")
        rememberScreen { showSettings() }
        page.addView(breadcrumb("홈", "설정"))
        page.addView(topBar("설정"))
        page.addView(connectionReadinessCard())
        page.addView(trustSummaryCard())
        page.addView(autoConnectSettingCard())
        page.addView(pollDefaultsSettingCard())
        page.addView(outlineButton("내 로컬 데이터 관리") { showLocalDataManagement() })
        page.addView(outlineButton("밥친구를 못 찾을 때") { showDiagnostics() })
        page.addView(outlineButton("개인정보처리방침") { showPrivacyPolicy() })
        page.addView(versionFooter())
    }

    private fun showLocalDataManagement() {
        setPage("설정")
        rememberScreen { showLocalDataManagement() }
        page.addView(breadcrumb("홈", "설정", "로컬 데이터"))
        page.addView(topBar("내 로컬 데이터 관리"))
        page.addView(statusCard(
            "내 기기 기준 삭제",
            "여기서 지우는 데이터는 이 기기에 저장된 밥판 기록과 설정입니다. 이미 가까운 참여자 기기에 전달된 밥판 데이터는 회수되지 않습니다."
        ))
        page.addView(localDataDeleteCard(
            title = "지난 결정",
            detail = "결과 기록과 결과 카드 희귀도 기록을 지웁니다.",
            buttonText = "지난 결정 삭제",
            onConfirm = {
                store.clearResultHistory()
                clearLocalSessionMemory(clearReceipts = false)
            }
        ))
        page.addView(localDataDeleteCard(
            title = "영수증/해시",
            detail = "내 선택 확인용 영수증과 결과 카드 해시 표시 기록을 지웁니다.",
            buttonText = "영수증 삭제",
            onConfirm = {
                store.clearReceipts()
                latestReceipt = null
                persistSessionState()
            }
        ))
        page.addView(localDataDeleteCard(
            title = "사용자 템플릿",
            detail = "직접 저장한 밥판 템플릿만 지웁니다. 기본 템플릿은 유지됩니다.",
            buttonText = "템플릿 삭제",
            onConfirm = { store.clearUserTemplates() }
        ))
        page.addView(localDataDeleteCard(
            title = "프로필/기본값",
            detail = "밥닉, 아바타, 최근 종류, 작성 기본값을 초기화합니다.",
            buttonText = "프로필 초기화",
            onConfirm = {
                store.clearProfileAndDefaults()
                userId = store.loadUserId { UUID.randomUUID().toString() }
                selfName = store.loadIdentity { suggestIdentity() }
                selfAvatarId = store.loadAvatarId { avatarIdForUser(userId) }
                autoConnectEnabled = store.isAutoConnectEnabled()
            }
        ))
        page.addView(localDataDeleteCard(
            title = "전체 초기화",
            detail = "프로필, 기본값, 진행 중인 밥판, 지난 결정, 영수증, 사용자 템플릿을 모두 지웁니다.",
            buttonText = "전체 초기화",
            destructive = true,
            onConfirm = {
                store.clearAllLocalData()
                clearLocalSessionMemory(clearReceipts = true)
                userId = store.loadUserId { UUID.randomUUID().toString() }
                selfName = store.loadIdentity { suggestIdentity() }
                selfAvatarId = store.loadAvatarId { avatarIdForUser(userId) }
                autoConnectEnabled = store.isAutoConnectEnabled()
            }
        ))
    }

    private fun localDataDeleteCard(
        title: String,
        detail: String,
        buttonText: String,
        destructive: Boolean = false,
        onConfirm: () -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(0xFFFFFFFF.toInt(), 16, if (destructive) 0xFFD76A6A.toInt() else 0xFFE0E7DD.toInt(), 1)
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = title
                textSize = 17f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = detail
                textSize = 13f
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(5), 0, dp(10))
            })
            addView(compactButton(buttonText, if (destructive) BUTTON_PRIMARY else BUTTON_OUTLINE) {
                confirmLocalDataDelete(title, detail, onConfirm)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44))
            })
        }
    }

    private fun confirmLocalDataDelete(title: String, detail: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("$detail\n\n이 기기에서만 삭제되며, 이미 상대 기기에 전달된 밥판 데이터는 회수되지 않습니다.")
            .setPositiveButton("삭제") { _, _ ->
                onConfirm()
                Toast.makeText(this, "$title 삭제 완료", Toast.LENGTH_SHORT).show()
                showLocalDataManagement()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showPrivacyPolicy() {
        setPage("설정")
        rememberScreen { showPrivacyPolicy() }
        page.addView(breadcrumb("홈", "설정", "개인정보처리방침"))
        page.addView(topBar("개인정보처리방침"))
        page.addView(statusCard("시행일", "2026-06-02"))
        page.addView(statusCard("한 줄 요약", "밥크로스는 개발자 서버로 개인정보를 수집하지 않습니다. 밥판에 필요한 정보는 내 기기 안에 저장되거나 사용자가 참여한 가까운 밥판 기기끼리만 전달됩니다."))
        page.addView(privacyPolicySection(
            "개발자 서버로 수집하지 않는 것",
            "회원가입, 전화번호, 이메일, 위치 기록, 연락처, 광고 ID, 사용 분석 데이터를 개발자 서버나 광고/분석 SDK로 수집하지 않습니다."
        ))
        page.addView(privacyPolicySection(
            "밥판 참여자에게 전달되는 것",
            "사용자가 밥판을 열거나 참여하면 Nearby Connections로 가까운 밥판 참여자에게 밥닉과 아바타, 메뉴 후보, 선택 결과 또는 득표 요약, 결과 확인용 밥해시/영수증 정보가 전달될 수 있습니다. 이는 앱 기능 수행을 위한 사용자 주도 근거리 공유이며, 밥크로스 서버 수집이나 공개 인터넷 전송이 아닙니다."
        ))
        page.addView(privacyPolicySection(
            "1. 수집 항목 및 방법",
            "본 앱은 서비스 제공을 위해 최소한의 정보만을 사용하며, 개발자 서버에 사용자의 개인정보를 저장하지 않습니다.\n- 기기 안에 저장될 수 있는 정보: 밥닉네임, 아바타 설정, 밥판 템플릿, 선택 영수증, 지난 결과\n- 근거리 참여자 기기로 전달될 수 있는 정보: 밥닉네임, 아바타, 메뉴 후보, 선택 내용, 결과\n- 기기 권한: Bluetooth, Wi-Fi/Nearby 기기, 위치 관련 권한"
        ))
        page.addView(privacyPolicySection(
            "2. 수집 및 이용 목적",
            "Nearby Connections 기술을 사용하여 주변 사용자들과 메뉴 투표를 진행하고 참여자들을 서로 식별하기 위한 목적으로만 사용됩니다. 위치 관련 권한은 위치 추적이 아니라 근처 참여 기기 검색과 연결을 위해 사용됩니다."
        ))
        page.addView(privacyPolicySection(
            "3. 보유 및 이용 기간",
            "별도의 서버를 운영하지 않으며 모든 정보는 사용자의 기기 내부에만 저장됩니다. 설정의 내 로컬 데이터 관리 또는 앱 삭제를 통해 기기 내 데이터를 지울 수 있습니다."
        ))
        page.addView(privacyPolicySection(
            "4. 제3자 제공",
            "밥크로스 서버나 광고/분석 SDK로 개인정보를 제공하지 않습니다. 다만, 밥판 기능을 위해 Nearby Connections로 가까운 참여자 기기에 후보와 선택 정보가 전달될 수 있으며, Nearby Connections 기술 구현을 위해 Google 서비스를 이용합니다."
        ))
        page.addView(privacyPolicySection(
            "5. 권리 및 행사 방법",
            "사용자는 언제든지 앱 내 설정을 통해 자신의 정보를 수정하거나 지난 결정, 영수증/해시, 사용자 템플릿, 프로필/기본값을 삭제할 수 있습니다. 다만 이미 상대 기기에 전달된 밥판 데이터는 내 기기에서 삭제해도 회수되지 않습니다."
        ))
        page.addView(privacyPolicySection(
            "6. 보호책임자 및 문의처",
            "이메일: neo202603@gmail.com"
        ))
        page.addView(bodyText("시행 일자: 2026년 6월 2일"))
    }

    private fun showCompose(template: PollTemplate? = null, initialFoodCategory: FoodCategory? = null) {
        val recentFoodCategory = initialFoodCategory ?: template?.let { templateFoodCategory(it) } ?: recentComposeFoodCategory()
        if (recentFoodCategory == null) {
            showComposeCategoryPicker()
            return
        }
        val selectedTemplate = template ?: emptyComposeDraft()
        val selectedFoodCategory = recentFoodCategory
        store.saveRecentFoodCategoryKey(selectedFoodCategory.name)
        setPage("밥판")
        rememberScreen { showCompose(template, selectedFoodCategory) }
        page.addView(breadcrumb("홈", "밥판", "밥판 열기"))
        page.addView(topBar("밥판 열기"))
        val composeMode = ComposeMode.TOGETHER
        var publishButton: Button? = null
        lateinit var rulesCard: LinearLayout
        var refreshComposePreview: (() -> Unit)? = null
        var refreshAdvancedSummary: (() -> Unit)? = null

        val questionInput = inputBox("오늘의 질문", selectedTemplate.question).apply {
            tutorialQuestionTarget = this
        }
        val optionEditor = OptionTagEditor(selectedTemplate.options) {
            refreshComposePreview?.invoke()
        }
        val requireInviteCodeInput = CheckBox(this).apply {
            text = "비공개로 보내기"
            textSize = 15f
            setTextColor(0xFF23362D.toInt())
            isChecked = false
            buttonTintList = android.content.res.ColorStateList.valueOf(0xFFD73B24.toInt())
            layoutParams = blockParams()
        }
        val allowParticipantOptionsInput = CheckBox(this).apply {
            text = "밥친구가 메뉴 후보 추가 가능"
            textSize = 15f
            setTextColor(0xFF23362D.toInt())
            isChecked = selectedTemplate.allowParticipantOptions
            buttonTintList = android.content.res.ColorStateList.valueOf(0xFFD73B24.toInt())
            layoutParams = blockParams()
        }
        val revealSelectionsInput = CheckBox(this).apply {
            text = "결과에서 밥친구별 선택 공개"
            textSize = 15f
            setTextColor(0xFF23362D.toInt())
            isChecked = selectedTemplate.revealSelections
            buttonTintList = android.content.res.ColorStateList.valueOf(0xFFD73B24.toInt())
            layoutParams = blockParams()
        }
        val durationInput = inputBox("제한시간(초)", selectedTemplate.durationSeconds.toString(), numberOnly = true)
        val hasCustomDuration = !isPresetDuration(selectedTemplate.durationSeconds)
        val extendedDurationChoices = extendedDurationChoices()
        val initialExtendedDurationIndex = closestDurationChoiceIndex(extendedDurationChoices, selectedTemplate.durationSeconds)
        val durationWheel = NumberPicker(this).apply {
            minValue = 0
            maxValue = extendedDurationChoices.lastIndex
            displayedValues = extendedDurationChoices.map { it.second }.toTypedArray()
            value = initialExtendedDurationIndex
            wrapSelectorWheel = false
            setOnValueChangedListener { _, _, index ->
                durationInput.setText(extendedDurationChoices[index].first.toString())
            }
        }
        if (hasCustomDuration) {
            durationInput.setText(extendedDurationChoices[initialExtendedDurationIndex].first.toString())
        }
        val customDurationPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = if (hasCustomDuration) View.VISIBLE else View.GONE
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = rounded(0xFFFFFFFF.toInt(), 14, 0xFFE7B59D.toInt())
            layoutParams = blockParams()
            addView(durationWheel, LinearLayout.LayoutParams(dp(128), dp(128)))
        }

        val pastRunnerUpSuggestionSlot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val saveTemplateLink = templateActionLink("${selectedFoodCategory.label} 템플릿으로 저장") {
            val template = buildTemplateFromInputs(
                questionInput,
                optionEditor.values(),
                durationInput,
                allowParticipantOptionsInput.isChecked,
                revealSelectionsInput.isChecked,
                selectedFoodCategory
            ) ?: return@templateActionLink
            store.saveTemplate(template)
            Toast.makeText(this, "밥판 템플릿 저장 완료", Toast.LENGTH_SHORT).show()
        }
        val advancedContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            layoutParams = blockParams()
        }
        val advancedSummaryText = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFF526158.toInt())
            setPadding(0, dp(6), 0, dp(8))
        }
        advancedContent.addView(label("제한시간"))
        advancedContent.addView(durationChoiceGrid(
            durationInput = durationInput,
            onPresetSelected = {
                customDurationPanel.visibility = View.GONE
                refreshAdvancedSummary?.invoke()
            },
            onCustomSelected = {
                durationInput.setText(extendedDurationChoices[durationWheel.value].first.toString())
                customDurationPanel.visibility = View.VISIBLE
                refreshAdvancedSummary?.invoke()
            }
        ))
        advancedContent.addView(customDurationPanel)
        advancedContent.addView(requireInviteCodeInput)
        advancedContent.addView(allowParticipantOptionsInput)
        advancedContent.addView(revealSelectionsInput)
        lateinit var advancedToggle: TextView
        advancedToggle = linkActionText("규칙 변경") {
            val showAdvanced = advancedContent.visibility != View.VISIBLE
            advancedContent.visibility = if (showAdvanced) View.VISIBLE else View.GONE
            advancedToggle.text = if (showAdvanced) "규칙 닫기" else "규칙 변경"
        }
        rulesCard = composeRulesCard(advancedSummaryText, advancedToggle, advancedContent)
        page.addView(composeSetupCard(
            category = selectedFoodCategory,
            onTemplateClick = {
                showTemplatePicker(
                    questionInput.text.toString(),
                    optionEditor.values().joinToString("\n"),
                    durationInput.text.toString(),
                    allowParticipantOptionsInput.isChecked,
                    revealSelectionsInput.isChecked,
                    selectedFoodCategory
                )
            },
            onChangeCategoryClick = { showComposeCategoryPicker() },
            questionInput = questionInput,
            optionsView = optionEditor.view.apply { tutorialOptionsTarget = this },
            rouletteAction = menuRouletteLink(optionEditor) { selectedFoodCategory },
            suggestionSlot = pastRunnerUpSuggestionSlot,
            rulesSection = rulesCard,
            saveTemplateLink = saveTemplateLink
        ))
        val composePreviewSlot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = blockParams()
        }
        page.addView(composePreviewSlot)

        refreshAdvancedSummary = {
            val durationSeconds = (durationInput.text.toString().toIntOrNull() ?: 300)
                .coerceIn(CUSTOM_DURATION_MIN_SECONDS, CUSTOM_DURATION_MAX_SECONDS)
            advancedSummaryText.text = composeRulesSummary(
                composeMode = composeMode,
                durationSeconds = durationSeconds,
                requireInviteCode = requireInviteCodeInput.isChecked,
                allowParticipantOptions = allowParticipantOptionsInput.isChecked,
                revealSelections = revealSelectionsInput.isChecked
            )
        }

        refreshComposePreview = {
            val durationSeconds = (durationInput.text.toString().toIntOrNull() ?: 300)
                .coerceIn(CUSTOM_DURATION_MIN_SECONDS, CUSTOM_DURATION_MAX_SECONDS)
            composePreviewSlot.removeAllViews()
            composePreviewSlot.addView(composeAudiencePreviewCard(
                composeMode = composeMode,
                question = questionInput.text.toString(),
                optionCount = optionEditor.values().size,
                durationSeconds = durationSeconds,
                requireInviteCode = requireInviteCodeInput.isChecked,
                allowParticipantOptions = allowParticipantOptionsInput.isChecked,
                revealSelections = revealSelectionsInput.isChecked
            ))
        }
        questionInput.afterTextChanged { refreshComposePreview?.invoke() }
        durationInput.afterTextChanged {
            refreshComposePreview?.invoke()
            refreshAdvancedSummary?.invoke()
        }
        allowParticipantOptionsInput.setOnCheckedChangeListener { _, _ ->
            refreshComposePreview?.invoke()
            refreshAdvancedSummary?.invoke()
        }
        requireInviteCodeInput.setOnCheckedChangeListener { _, _ ->
            refreshComposePreview?.invoke()
            refreshAdvancedSummary?.invoke()
        }
        revealSelectionsInput.setOnCheckedChangeListener { _, _ ->
            refreshComposePreview?.invoke()
            refreshAdvancedSummary?.invoke()
        }
        refreshAdvancedSummary?.invoke()
        refreshComposePreview?.invoke()

        val publishAction = publish@{
            val question = questionInput.text.toString().trim()
            val options = optionEditor.values()
            val durationSeconds = durationInput.text.toString().toIntOrNull() ?: 300
            if (question.isBlank()) {
                Toast.makeText(this, "오늘의 질문을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@publish
            }
            if (options.size < 2) {
                Toast.makeText(this, "메뉴 후보는 2개 이상 필요합니다.", Toast.LENGTH_SHORT).show()
                return@publish
            }
            if (!autoConnectEnabled) {
                setAutoConnectEnabled(true, "자동 연결을 켜고 밥신호를 보냅니다.")
            }
            publishPoll(
                question,
                options,
                durationSeconds.coerceIn(CUSTOM_DURATION_MIN_SECONDS, CUSTOM_DURATION_MAX_SECONDS),
                requireInviteCodeInput.isChecked,
                allowParticipantOptionsInput.isChecked,
                revealSelectionsInput.isChecked
            )
        }
        publishButton = stickyPrimaryActionButton("밥신호 보내기", publishAction).apply {
            tutorialPublishTarget = this
        }
        setStickyBottomAction(publishButton!!)

        val suggestionToken = ++composeSuggestionToken
        handler.postDelayed({
            if (composeSuggestionToken == suggestionToken && pastRunnerUpSuggestionSlot.parent != null) {
                val suggestion = runnerUpMenuSuggestion(questionInput.text.toString(), optionEditor.values())
                if (suggestion != null) {
                    pastRunnerUpSuggestionSlot.removeAllViews()
                    pastRunnerUpSuggestionSlot.addView(pastRunnerUpSuggestionCard(suggestion) {
                        if (optionEditor.addOption(suggestion)) {
                            pastRunnerUpSuggestionSlot.visibility = View.GONE
                            Toast.makeText(this, "지난 아쉬운 메뉴를 후보에 추가했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    })
                    pastRunnerUpSuggestionSlot.visibility = View.VISIBLE
                }
            }
        }, COMPOSE_MENU_SUGGESTION_DELAY_MS)
    }

    private fun showComposeCategoryPicker() {
        setPage("밥판")
        rememberScreen { showComposeCategoryPicker() }
        page.addView(breadcrumb("홈", "밥판", "밥판 종류"))
        page.addView(topBar("밥판 종류 선택"))
        page.addView(bodyText("밥판을 열 종류를 먼저 고르면, 작성 중에는 룰렛과 템플릿이 그 종류로 고정됩니다."))
        recentComposeFoodCategory()?.let { category ->
            page.addView(statusCard("최근 종류", "${category.label}로 바로 시작하려면 홈이나 하단의 밥판 열기를 누르세요."))
        }
        selectableFoodCategories().forEach { category ->
            page.addView(foodCategoryChoiceCard(category) {
                store.saveRecentFoodCategoryKey(category.name)
                showCompose(initialFoodCategory = category)
            }.apply {
                if (category == (tutorialResult?.let { inferredFoodCategory(it.options) } ?: FoodCategory.MEAL)) {
                    tutorialCategoryTarget = this
                }
            })
        }
    }

    private fun showTemplatePicker(
        currentQuestion: String = "",
        currentOptions: String = "",
        currentDuration: String = "300",
        currentAllowParticipantOptions: Boolean = false,
        currentRevealSelections: Boolean = true,
        currentFoodCategory: FoodCategory = inferredFoodCategory(currentOptions.lines().map { it.trim() }.filter { it.isNotBlank() })
    ) {
        setPage("밥판")
        rememberScreen {
            showTemplatePicker(
                currentQuestion,
                currentOptions,
                currentDuration,
                currentAllowParticipantOptions,
                currentRevealSelections,
                currentFoodCategory
            )
        }
        page.addView(breadcrumb("홈", "밥판", "밥판 템플릿"))
        page.addView(topBar("밥판 템플릿"))
        val selectedCategory = currentFoodCategory
        page.addView(lockedFoodCategoryCard(selectedCategory))
        page.addView(bodyText("선택한 종류의 템플릿만 보여줍니다."))
        val templates = store.loadTemplates().filter { template -> templateFoodCategory(template) == selectedCategory }
        if (templates.isEmpty()) {
            page.addView(emptyCard("${selectedCategory.label} 템플릿 없음", "이 종류로 템플릿을 저장하면 여기에 모입니다."))
        } else {
            templates.forEach { template ->
                page.addView(templatePickerRow(template, currentQuestion, currentOptions, currentDuration, currentAllowParticipantOptions, currentRevealSelections, selectedCategory))
            }
        }
        page.addView(outlineButton("작성 화면으로") {
            showCompose(
                PollTemplate(
                    id = "draft",
                    title = currentQuestion.ifBlank { "새 밥판" },
                    question = currentQuestion,
                    options = currentOptions.lines().map { it.trim() }.filter { it.isNotBlank() },
                    durationMinutes = ((currentDuration.toIntOrNull() ?: 300) + 59) / 60,
                    durationSeconds = currentDuration.toIntOrNull() ?: 300,
                    allowParticipantOptions = currentAllowParticipantOptions,
                    revealSelections = currentRevealSelections,
                    categoryKey = selectedCategory.name
                ),
                selectedCategory
            )
        })
    }

    private fun showTemplateCategoryPicker() {
        setPage("밥판")
        rememberScreen { showTemplateCategoryPicker() }
        page.addView(breadcrumb("홈", "밥판", "밥판 템플릿"))
        page.addView(topBar("템플릿 종류 선택"))
        page.addView(bodyText("템플릿을 볼 종류를 먼저 고릅니다. 선택 후에는 그 종류의 템플릿만 확인합니다."))
        selectableFoodCategories().forEach { category ->
            page.addView(foodCategoryChoiceCard(category) {
                val draft = emptyComposeDraft()
                showTemplatePicker(
                    draft.question,
                    draft.options.joinToString("\n"),
                    draft.durationSeconds.toString(),
                    draft.allowParticipantOptions,
                    draft.revealSelections,
                    category
                )
            })
        }
    }

    private fun emptyComposeDraft(): PollTemplate {
        val defaults = store.loadPollDefaults()
        return PollTemplate(
            id = "draft",
            title = "새 밥판",
            question = "",
            options = emptyList(),
            durationMinutes = ((defaults.durationSeconds + 59) / 60).coerceAtLeast(1),
            durationSeconds = defaults.durationSeconds,
            allowParticipantOptions = defaults.allowParticipantOptions,
            revealSelections = defaults.revealSelections
        )
    }

    private fun foodCategorySelectorRow(
        segments: MutableMap<FoodCategory, TextView>,
        onSelected: (FoodCategory) -> Unit
    ): LinearLayout {
        val categories = selectableFoodCategories()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(3), dp(3), dp(3), dp(3))
            background = rounded(0xFFFFF1E8.toInt(), 14, 0xFFE0B49E.toInt(), 1)
            layoutParams = blockParams()
            categories.forEachIndexed { index, category ->
                val segment = foodCategorySegment(category) { onSelected(category) }
                segments[category] = segment
                addView(segment, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                    if (index < categories.lastIndex) {
                        rightMargin = dp(2)
                    }
                })
            }
        }
    }

    private fun selectableFoodCategories(): List<FoodCategory> {
        return listOf(FoodCategory.MEAL, FoodCategory.DISH, FoodCategory.DRINK, FoodCategory.DESSERT, FoodCategory.SNACK)
    }

    private fun foodCategoryChoiceCard(category: FoodCategory, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(14), dp(14))
            background = rounded(0xFFFFFFFF.toInt(), 16, category.strokeColor, 1)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            layoutParams = blockParams()
            addView(foodCategoryBadge(category).apply {
                layoutParams = LinearLayout.LayoutParams(dp(74), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(context).apply {
                text = foodCategoryDetail(category)
                textSize = 14f
                setTextColor(0xFF526158.toInt())
                setPadding(dp(12), 0, dp(8), 0)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(context).apply {
                text = "›"
                textSize = 26f
                setTextColor(0xFF8AA093.toInt())
            })
        }
    }

    private fun lockedFoodCategoryCard(
        category: FoodCategory,
        onTemplateClick: (() -> Unit)? = null,
        onChangeCategoryClick: (() -> Unit)? = null,
        embedded: Boolean = false
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = if (embedded) {
                roundedCorners(
                    color = category.backgroundColor,
                    topLeft = 14,
                    topRight = 14
                )
            } else {
                rounded(category.backgroundColor, 14, category.strokeColor, 1)
            }
            layoutParams = if (embedded) {
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            } else {
                blockParams()
            }
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                if (onChangeCategoryClick != null) {
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { onChangeCategoryClick() }
                }
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply {
                    text = "종류"
                    textSize = 13f
                    setTextColor(0xFF526158.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, 0, dp(8), 0)
                })
                addView(foodCategoryBadge(category, prominent = true))
                if (onChangeCategoryClick != null) {
                    addView(TextView(context).apply {
                        text = "›"
                        textSize = 22f
                        setTextColor(0xFF8AA093.toInt())
                        setPadding(dp(6), 0, 0, 0)
                    })
                }
            })
            if (onTemplateClick != null) {
                addView(templateActionLink("${category.label} 템플릿 선택", onTemplateClick))
            }
        }
    }

    private fun foodCategoryBadge(category: FoodCategory, prominent: Boolean = false): TextView {
        return TextView(this).apply {
            text = category.label
            textSize = if (prominent) 15f else 12f
            setTextColor(category.textColor)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(
                dp(if (prominent) 14 else 10),
                dp(if (prominent) 6 else 4),
                dp(if (prominent) 14 else 10),
                dp(if (prominent) 6 else 4)
            )
            background = rounded(category.backgroundColor, if (prominent) 14 else 12, category.strokeColor, 1)
            maxLines = 1
        }
    }

    private fun foodCategoryDetail(category: FoodCategory): String {
        return when (category) {
            FoodCategory.MEAL -> "식사 메뉴 중심으로 후보를 모읍니다."
            FoodCategory.DISH -> "요리와 회식 메뉴 중심으로 후보를 모읍니다."
            FoodCategory.DRINK -> "음료 후보만 모아 고릅니다."
            FoodCategory.DESSERT -> "후식 후보만 모아 고릅니다."
            FoodCategory.SNACK -> "간식 후보만 모아 고릅니다."
            FoodCategory.OTHER -> "직접 입력한 후보를 모읍니다."
        }
    }

    private fun foodCategorySegment(category: FoodCategory, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = category.label
            textSize = 12f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            isClickable = true
            isFocusable = true
            maxLines = 1
            setOnClickListener { onClick() }
        }
    }

    private fun menuRouletteLink(optionEditor: OptionTagEditor, categoryProvider: () -> FoodCategory): TextView {
        fun addSuggestions(label: String, candidates: List<String>) {
            val addedOptions = optionEditor.addOptions(candidates, markAsRoulette = true)
            val message = if (addedOptions.isNotEmpty()) {
                "$label 후보 ${addedOptions.size}개 추가: ${addedOptions.joinToString(", ")}"
            } else {
                "추가할 새 후보가 없습니다."
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        fun showRoulettePicker() {
            val category = categoryProvider()
            val rouletteChoices = rouletteChoicesFor(category, optionEditor)
            val labels = rouletteChoices.map { it.first }
            val wheel = MenuRouletteWheelView(labels).apply {
                layoutParams = LinearLayout.LayoutParams(dp(244), dp(244)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            val resultText = TextView(this).apply {
                text = "룰렛을 돌려 후보 묶음을 골라보세요."
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(10), 0, dp(2))
            }
            val dialogContent = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(12), dp(18), dp(4))
                addView(wheel)
                addView(resultText)
            }
            val dialog = AlertDialog.Builder(this)
                .setTitle("${category.label} 룰렛")
                .setView(dialogContent)
                .setNegativeButton("닫기", null)
                .create()
            dialog.setOnShowListener {
                val spinButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                spinButton.setOnClickListener {
                    spinButton.isEnabled = false
                    resultText.text = "밥판이 돌아가는 중..."
                    val selectedIndex = rouletteChoices.indices.random()
                    wheel.spinTo(selectedIndex) {
                        val (label, candidates) = rouletteChoices[selectedIndex]
                        resultText.text = "$label 후보를 추가합니다."
                        addSuggestions(label, candidates())
                        handler.postDelayed({ dialog.dismiss() }, ROULETTE_RESULT_HOLD_MS)
                    }
                }
            }
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "룰렛 돌리기") { _, _ -> }
            dialog.show()
        }

        return linkActionText("룰렛으로 후보 추가") {
            showRoulettePicker()
        }
    }

    private fun rouletteChoicesFor(
        category: FoodCategory,
        optionEditor: OptionTagEditor
    ): List<Pair<String, () -> List<String>>> {
        return when (category) {
            FoodCategory.MEAL -> {
                val quickLunch = listOf("김밥", "덮밥", "쌀국수")
                val lightMeal = listOf("샐러드", "포케", "월남쌈")
                listOf(
                    "아무거나 3개" to { randomMenuSuggestions(FoodCategory.MEAL, optionEditor.values(), listOf(quickLunch, lightMeal)) },
                    "빠른 점심" to { quickLunch },
                    "가볍게" to { lightMeal }
                )
            }
            FoodCategory.DISH -> {
                val heartyDish = listOf("국밥", "제육", "돈까스")
                val rainyDish = listOf("칼국수", "부대찌개", "순댓국")
                val chineseDish = listOf("짜장면", "짬뽕", "탕수육")
                listOf(
                    "아무거나 3개" to { randomMenuSuggestions(FoodCategory.DISH, optionEditor.values(), listOf(heartyDish, rainyDish, chineseDish)) },
                    "든든하게" to { heartyDish },
                    "비 오는 날" to { rainyDish },
                    "중식" to { chineseDish }
                )
            }
            FoodCategory.DRINK -> {
                val cafeDrink = listOf("아메리카노", "라떼", "아이스티")
                val coolDrink = listOf("콜라", "사이다", "주스")
                listOf(
                    "아무거나 3개" to { randomMenuSuggestions(FoodCategory.DRINK, optionEditor.values(), listOf(cafeDrink, coolDrink)) },
                    "카페" to { cafeDrink },
                    "시원하게" to { coolDrink }
                )
            }
            FoodCategory.DESSERT -> {
                val sweetDessert = listOf("아이스크림", "케이크", "빙수")
                val lightDessert = listOf("푸딩", "와플", "아이스크림")
                listOf(
                    "아무거나 3개" to { randomMenuSuggestions(FoodCategory.DESSERT, optionEditor.values(), listOf(sweetDessert, lightDessert)) },
                    "달달하게" to { sweetDessert },
                    "가볍게" to { lightDessert }
                )
            }
            FoodCategory.SNACK -> {
                val streetSnack = listOf("떡볶이", "순대", "튀김")
                val lightSnack = listOf("핫도그", "만두", "튀김")
                listOf(
                    "아무거나 3개" to { randomMenuSuggestions(FoodCategory.SNACK, optionEditor.values(), listOf(streetSnack, lightSnack)) },
                    "분식" to { streetSnack },
                    "가볍게" to { lightSnack }
                )
            }
            FoodCategory.OTHER -> listOf(
                "아무거나 3개" to { randomMenuSuggestions(FoodCategory.MEAL, optionEditor.values()) }
            )
        }
    }

    private fun randomMenuSuggestions(
        category: FoodCategory,
        currentOptions: List<String>,
        excludedOptionGroups: List<List<String>> = emptyList()
    ): List<String> {
        val currentOptionSet = currentOptions.map { normalizedOption(it) }.toSet()
        val availableOptions = menuRouletteOptionsByCategory[category].orEmpty()
            .filter { normalizedOption(it) !in currentOptionSet }
        if (availableOptions.size <= 3) return availableOptions

        val excludedSets = excludedOptionGroups.map { group ->
            group.map { normalizedOption(it) }.toSet()
        }.toSet()
        val random = Random(System.currentTimeMillis())
        val candidates = mutableListOf<List<String>>()
        for (first in 0 until availableOptions.size - 2) {
            for (second in first + 1 until availableOptions.size - 1) {
                for (third in second + 1 until availableOptions.size) {
                    candidates += listOf(availableOptions[first], availableOptions[second], availableOptions[third])
                }
            }
        }
        return candidates
            .shuffled(random)
            .firstOrNull { candidate -> candidate.map { normalizedOption(it) }.toSet() !in excludedSets }
            ?: availableOptions.shuffled(random).take(3)
    }

    private fun buildTemplateFromInputs(
        questionInput: EditText,
        options: List<String>,
        durationInput: EditText,
        allowParticipantOptions: Boolean,
        revealSelections: Boolean,
        foodCategory: FoodCategory
    ): PollTemplate? {
        val question = questionInput.text.toString().trim()
        if (question.isBlank()) {
            Toast.makeText(this, "오늘의 질문을 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return null
        }
        if (options.size < 2) {
            Toast.makeText(this, "메뉴 후보는 2개 이상 필요합니다.", Toast.LENGTH_SHORT).show()
            return null
        }
        val durationSeconds = (durationInput.text.toString().toIntOrNull() ?: 300)
            .coerceIn(CUSTOM_DURATION_MIN_SECONDS, CUSTOM_DURATION_MAX_SECONDS)
        return PollTemplate(
            id = "template-${System.currentTimeMillis()}",
            title = question,
            question = question,
            options = options,
            durationMinutes = ((durationSeconds + 59) / 60).coerceIn(1, CUSTOM_DURATION_MAX_SECONDS / 60),
            durationSeconds = durationSeconds,
            allowParticipantOptions = allowParticipantOptions,
            revealSelections = revealSelections,
            categoryKey = foodCategory.name
        )
    }

    private fun templatePickerRow(
        template: PollTemplate,
        currentQuestion: String,
        currentOptions: String,
        currentDuration: String,
        currentAllowParticipantOptions: Boolean,
        currentRevealSelections: Boolean,
        currentFoodCategory: FoodCategory
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = blockParams()
        }
        val card = templateCard(template) {
            showCompose(template, templateFoodCategory(template))
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                if (template.builtIn) ViewGroup.LayoutParams.MATCH_PARENT else 0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                if (template.builtIn) 0f else 1f
            )
        }
        row.addView(card)
        if (!template.builtIn) {
            val deleteButton = Button(this).apply {
                text = "삭제"
                isAllCaps = false
                textSize = 14f
                setTextColor(0xFFFFFFFF.toInt())
                background = rounded(0xFFB3261E.toInt(), 14)
                visibility = View.GONE
                layoutParams = LinearLayout.LayoutParams(dp(76), dp(72)).apply {
                    leftMargin = dp(8)
                }
                setOnClickListener {
                    store.deleteTemplate(template.id)
                    Toast.makeText(this@MainActivity, "템플릿 삭제 완료", Toast.LENGTH_SHORT).show()
                    showTemplatePicker(
                        currentQuestion,
                        currentOptions,
                        currentDuration,
                        currentAllowParticipantOptions,
                        currentRevealSelections,
                        currentFoodCategory
                    )
                }
            }
            var downX = 0f
            var swiped = false
            card.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.x
                        swiped = false
                        false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (downX - event.x > dp(44)) {
                            deleteButton.visibility = View.VISIBLE
                            swiped = true
                            true
                        } else if (event.x - downX > dp(28)) {
                            deleteButton.visibility = View.GONE
                            false
                        } else {
                            false
                        }
                    }
                    MotionEvent.ACTION_UP -> swiped
                    else -> false
                }
            }
            row.addView(deleteButton)
        }
        return row
    }

    private fun templateCard(template: PollTemplate, onClick: () -> Unit): LinearLayout {
        val icon = if (template.builtIn) "🍚" else "👤"
        val category = templateFoodCategory(template)
        fun View.applyTemplateClick() {
            isClickable = true
            setOnClickListener { onClick() }
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, dp(16), 0)
            background = groupedCardBackground()
            applyTemplateClick()
            layoutParams = blockParams()
            addView(View(context).apply {
                background = rounded(0xFFD73B24.toInt(), 16)
                layoutParams = LinearLayout.LayoutParams(dp(6), ViewGroup.LayoutParams.MATCH_PARENT)
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(12), dp(16))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(TextView(context).apply {
                        text = "$icon ${template.title}"
                        textSize = 18f
                        setTextColor(0xFF10251D.toInt())
                        setTypeface(typeface, Typeface.BOLD)
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        applyTemplateClick()
                    })
                    addView(TextView(context).apply {
                        text = "제한시간 ${formatDurationText(template.durationSeconds)}"
                        textSize = 13f
                        setTextColor(0xFF66776E.toInt())
                        applyTemplateClick()
                    })
                })
                addView(TextView(context).apply {
                    text = category.label
                    textSize = 11f
                    setTextColor(category.textColor)
                    setTypeface(typeface, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setPadding(dp(8), dp(3), dp(8), dp(3))
                    background = rounded(category.backgroundColor, 12, category.strokeColor, 1)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(8)
                    }
                    applyTemplateClick()
                })
                addView(templateTagBar(template).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(8)
                    }
                    applyTemplateClick()
                })
                if (template.allowParticipantOptions) {
                    addView(TextView(context).apply {
                        text = "밥친구 메뉴 후보 추가 허용"
                        textSize = 13f
                        setTextColor(0xFFD73B24.toInt())
                        setPadding(0, dp(8), 0, 0)
                        applyTemplateClick()
                    })
                }
                addView(TextView(context).apply {
                    text = if (template.revealSelections) "밥친구 선택 공개" else "득표수만 공개"
                    textSize = 13f
                    setTextColor(0xFF66776E.toInt())
                    setPadding(0, dp(6), 0, 0)
                    applyTemplateClick()
                })
                applyTemplateClick()
            })
            addView(TextView(context).apply {
                text = "›"
                textSize = 28f
                setTextColor(0xFF8AA093.toInt())
                applyTemplateClick()
            })
        }
    }

    private fun showDiscover() {
        setPage("밥판")
        rememberScreen { showDiscover() }
        val polls = visibleIncomingPolls()
        page.addView(breadcrumb("홈", "밥판", "주변 밥신호"))
        page.addView(topBar("주변 밥신호(${polls.size}건)"))
        if (polls.isEmpty()) {
            page.addView(statusCard(if (autoConnectEnabled) "자동 연결 대기 중" else "자동 연결 꺼짐", connectionStatusText()))
            page.addView(emptyCard("아직 받은 밥신호 없음", "근처 밥친구가 밥판을 열면 초대로 먼저 표시됩니다."))
        } else {
            polls.forEach { poll ->
                val submitted = submittedVotes[poll.id]
                val accepted = acceptedPollIds.contains(poll.id)
                val subtitle = when {
                    submitted != null -> "내 선택: $submitted"
                    accepted -> "${poll.proposerName}의 밥신호 · ${poll.remainingText()}"
                    poll.inviteCode.isBlank() -> "${poll.proposerName}님의 밥판 초대"
                    else -> "${poll.proposerName}님의 비공개 밥판 초대"
                }
                page.addView(actionCard(poll.question, subtitle) {
                    if (accepted || submitted != null) {
                        showVotePoll(poll)
                    } else {
                        showPollInvitation(poll)
                    }
                })
            }
        }
        page.addView(buttonRow(
            compactButton("밥판 열기", BUTTON_PRIMARY) { showCompose() },
            compactButton("밥친구 찾기", BUTTON_OUTLINE) { showDiagnostics() }
        ))
    }

    private fun showPollInvitation(poll: NearbyPoll) {
        setPage("밥판")
        rememberScreen { showPollInvitation(poll) }
        page.addView(breadcrumb("홈", "밥판", "밥판 초대"))
        page.addView(topBar("밥판 초대"))
        val inviteDetail = if (poll.inviteCode.isBlank()) "밥판 참여를 누르면 메뉴 선택으로 이동합니다." else "밥판 코드를 입력하면 바로 참여합니다."
        page.addView(avatarInfoCard("새 밥신호 도착", poll.question, "보낸 밥친구: ${poll.proposerName} · $inviteDetail", resolvedAvatarId(poll.proposerId, poll.proposerAvatarId), poll))
        page.addView(inviteCodeEntryCard(poll))
    }

    private fun inviteCodeEntryCard(poll: NearbyPoll): LinearLayout {
        if (poll.inviteCode.isBlank()) {
            return unlockedInvitationCard(poll)
        }
        lateinit var slotRow: LinearLayout
        val codeInput = inputBox("4자리 코드", "", numberOnly = true).apply {
            gravity = Gravity.CENTER
            textSize = 24f
            letterSpacing = 0.12f
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
            alpha = 0.01f
            background = null
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                bottomMargin = dp(2)
            }
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    keyboardLiftTarget = slotRow
                    keyboardLiftToBottom = false
                    slotRow.postDelayed({ liftParticipantOptionInput() }, KEYBOARD_SCROLL_DELAY_MS)
                } else if (keyboardLiftTarget === slotRow) {
                    keyboardLiftTarget = null
                    keyboardLiftToBottom = false
                }
            }
        }
        lateinit var codeSlots: List<TextView>
        slotRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(10))
            isClickable = true
            isFocusable = true
            contentDescription = "4자리 밥판 코드 입력"
            setOnClickListener {
                codeInput.requestFocus()
                (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.showSoftInput(codeInput, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        codeSlots = (0 until 4).map { index ->
            TextView(this).apply {
                text = ""
                textSize = 24f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = rounded(0xFFFFFFFF.toInt(), 14, 0xFF8B5C45.toInt(), 2)
                layoutParams = LinearLayout.LayoutParams(0, dp(54), 1f).apply {
                    if (index > 0) leftMargin = dp(6)
                    if (index < 3) rightMargin = dp(6)
                }
            }.also { slotRow.addView(it) }
        }
        val status = TextView(this).apply {
            text = "밥판장에게 들은 4자리 코드를 입력하세요."
            textSize = 12f
            setTextColor(0xFF526158.toInt())
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(10))
        }
        var acceptedByCode = false
        var previousCodeLength = 0
        fun tickCodeInput() {
            slotRow.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            vibrateCodeInput()
        }
        fun shakeCodeSlots() {
            slotRow.performHapticFeedback(HapticFeedbackConstants.REJECT)
            vibrateCodeError()
            ValueAnimator.ofFloat(
                0f,
                -dp(14).toFloat(),
                dp(14).toFloat(),
                -dp(11).toFloat(),
                dp(11).toFloat(),
                -dp(7).toFloat(),
                dp(7).toFloat(),
                0f
            ).apply {
                duration = 360L
                addUpdateListener { animator ->
                    slotRow.translationX = animator.animatedValue as Float
                }
                start()
            }
        }
        fun refreshCodeSlots() {
            val enteredCode = codeInput.text.toString()
            val visibleDigitIndex = enteredCode.lastIndex
            codeSlots.forEachIndexed { index, slot ->
                val digit = enteredCode.getOrNull(index)?.toString().orEmpty()
                slot.text = when {
                    digit.isBlank() -> ""
                    index < visibleDigitIndex -> "*"
                    else -> digit
                }
                val filled = digit.isNotBlank()
                val active = index == enteredCode.length.coerceAtMost(3) && !acceptedByCode
                slot.background = rounded(
                    if (filled) 0xFFFFF1E8.toInt() else 0xFFFFFFFF.toInt(),
                    14,
                    if (active || filled) 0xFFD73B24.toInt() else 0xFF8B5C45.toInt(),
                    if (active || filled) 3 else 2
                )
            }
        }
        fun acceptIfCodeMatches() {
            refreshCodeSlots()
            if (acceptedByCode) return
            val enteredCode = codeInput.text.toString()
            if (enteredCode.length > previousCodeLength) {
                tickCodeInput()
            }
            previousCodeLength = enteredCode.length
            if (poll.inviteCode.isBlank()) {
                status.text = "이 밥판은 코드가 없어 참여할 수 없습니다."
                status.setTextColor(0xFFD73B24.toInt())
                return
            }
            if (enteredCode.length < 4) {
                status.text = "밥판장에게 들은 4자리 코드를 입력하세요."
                status.setTextColor(0xFF526158.toInt())
                return
            }
            if (enteredCode != poll.inviteCode) {
                status.text = "코드가 맞지 않아요."
                status.setTextColor(0xFFD73B24.toInt())
                shakeCodeSlots()
                codeInput.postDelayed({
                    if (!acceptedByCode && codeInput.text.toString() == enteredCode) {
                        codeInput.text?.clear()
                    }
                }, CODE_ERROR_CLEAR_DELAY_MS)
                return
            }
            acceptedByCode = true
            acceptedPollIds += poll.id
            sendPollResponse(poll, POLL_RESPONSE_ACCEPTED)
            persistSessionState()
            Toast.makeText(this, "밥판에 참여합니다.", Toast.LENGTH_SHORT).show()
            openVotePollAfterJoin(poll)
        }
        codeInput.afterTextChanged { acceptIfCodeMatches() }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(0xFFFFFFFF.toInt(), 16, 0xFFE7B59D.toInt(), 1)
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = "밥판 코드 입력"
                textSize = 17f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = "코드가 맞으면 바로 메뉴 선택으로 이동합니다."
                textSize = 13f
                setTextColor(0xFF526158.toInt())
                gravity = Gravity.CENTER
                setPadding(0, dp(5), 0, dp(10))
            })
            addView(slotRow)
            addView(codeInput)
            addView(status)
            addView(compactButton("거절", BUTTON_OUTLINE) {
                declinedPollIds += poll.id
                incomingPolls.remove(poll.id)
                sendPollResponse(poll, POLL_RESPONSE_DECLINED)
                persistSessionState()
                declinedPollNotice = "${poll.proposerName}님의 밥판 초대를 거절했습니다. 필요하면 새 밥판을 열거나 주변 밥신호를 다시 확인할 수 있어요."
                Toast.makeText(this@MainActivity, "초대를 거절하고 홈으로 돌아갑니다.", Toast.LENGTH_SHORT).show()
                showHome()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(42)
                )
            })
        }
    }

    private fun unlockedInvitationCard(poll: NearbyPoll): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(0xFFFFFFFF.toInt(), 16, 0xFFB7DCC9.toInt(), 1)
            layoutParams = blockParams()
            addView(primaryButton("밥판 참여") {
                acceptedPollIds += poll.id
                sendPollResponse(poll, POLL_RESPONSE_ACCEPTED)
                persistSessionState()
                Toast.makeText(this@MainActivity, "밥판에 참여합니다.", Toast.LENGTH_SHORT).show()
                openVotePollAfterJoin(poll)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(46)
                )
            })
            addView(compactButton("거절", BUTTON_OUTLINE) {
                declinedPollIds += poll.id
                incomingPolls.remove(poll.id)
                sendPollResponse(poll, POLL_RESPONSE_DECLINED)
                persistSessionState()
                declinedPollNotice = "${poll.proposerName}님의 밥판 초대를 거절했습니다. 필요하면 새 밥판을 열거나 주변 밥신호를 다시 확인할 수 있어요."
                Toast.makeText(this@MainActivity, "초대를 거절하고 홈으로 돌아갑니다.", Toast.LENGTH_SHORT).show()
                showHome()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(42)
                ).apply {
                    topMargin = dp(8)
                }
            })
        }
    }

    private fun pollRulesCard(poll: NearbyPoll): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = groupedCardBackground()
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = "밥판 규칙"
                textSize = 14f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = listOf(
                    if (poll.revealSelections) "내 선택 공개" else "득표수만 공개",
                    "후보 추가 ${if (poll.allowParticipantOptions) "가능" else "불가"}",
                    "한 번 선택하면 변경 불가"
                ).joinToString(" · ")
                textSize = 12f
                setTextColor(0xFF526158.toInt())
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(5), 0, 0)
            })
        }
    }

    private fun composeContentCard(
        questionInput: View,
        optionsView: View,
        rouletteAction: TextView,
        suggestionSlot: LinearLayout,
        rulesSection: LinearLayout,
        embedded: Boolean = false
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            if (!embedded) {
                background = groupedCardBackground()
            }
            layoutParams = if (embedded) {
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            } else {
                blockParams()
            }
            addView(TextView(context).apply {
                text = "밥판 내용"
                textSize = 17f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = "질문과 후보를 한 번에 정리합니다."
                textSize = 13f
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(4), 0, dp(10))
            })
            addView(label("오늘의 질문"))
            addView(questionInput)
            addView(labelActionRow("메뉴 후보", rouletteAction, bottomPaddingDp = 2))
            addView(optionsView)
            addView(suggestionSlot)
            addView(View(context).apply {
                background = rounded(0xFFD8E2DA.toInt(), 0)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(1)
                ).apply {
                    topMargin = dp(14)
                    bottomMargin = dp(12)
                }
            })
            addView(rulesSection)
        }
    }

    private fun composeSetupCard(
        category: FoodCategory,
        onTemplateClick: () -> Unit,
        onChangeCategoryClick: () -> Unit,
        questionInput: View,
        optionsView: View,
        rouletteAction: TextView,
        suggestionSlot: LinearLayout,
        rulesSection: LinearLayout,
        saveTemplateLink: TextView
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = groupedCardBackground()
            setPadding(dp(2), dp(2), dp(2), dp(2))
            layoutParams = blockParams()
            addView(lockedFoodCategoryCard(
                category = category,
                onTemplateClick = onTemplateClick,
                onChangeCategoryClick = onChangeCategoryClick,
                embedded = true
            ))
            addView(View(context).apply {
                background = rounded(0xFFD8E2DA.toInt(), 0)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(1)
                )
            })
            addView(composeContentCard(
                questionInput = questionInput,
                optionsView = optionsView,
                rouletteAction = rouletteAction,
                suggestionSlot = suggestionSlot,
                rulesSection = rulesSection,
                embedded = true
            ))
            addView(View(context).apply {
                background = rounded(0xFFD8E2DA.toInt(), 0)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(1)
                )
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(dp(14), dp(7), dp(14), dp(8))
                background = roundedCorners(
                    color = 0xFFFFFBF5.toInt(),
                    bottomRight = 14,
                    bottomLeft = 14
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                addView(saveTemplateLink.apply {
                    val currentParams = layoutParams as? LinearLayout.LayoutParams
                    layoutParams = (currentParams ?: LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ))
                })
            })
        }
    }

    private fun composeAudiencePreviewCard(
        composeMode: ComposeMode,
        question: String,
        optionCount: Int,
        durationSeconds: Int,
        requireInviteCode: Boolean,
        allowParticipantOptions: Boolean,
        revealSelections: Boolean
    ): LinearLayout {
        val soloMode = composeMode == ComposeMode.SOLO
        val previewQuestion = question.trim().ifBlank { "오늘 뭐 먹을까요?" }
        val durationText = formatDurationText(durationSeconds)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = groupedCardBackground()
            addView(TextView(context).apply {
                text = if (soloMode) "결과 카드 미리보기" else "상대에게 이렇게 보여요"
                textSize = 17f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = if (soloMode) {
                    "혼자 정한 결과는 카드로 바로 공유돼요."
                } else {
                    "밥친구는 규칙을 보고 바로 참여해요."
                }
                textSize = 13f
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(5), 0, dp(6))
            })
            addView(TextView(context).apply {
                text = previewQuestion
                textSize = 20f
                setTextColor(0xFFD73B24.toInt())
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setPadding(0, dp(2), 0, dp(6))
            })
            addView(TextView(context).apply {
                text = if (soloMode) {
                    "${if (optionCount > 0) "후보 ${optionCount}개" else "후보 준비 중"} · 내 선택으로 결과 카드 생성"
                } else if (requireInviteCode) {
                    "${if (optionCount > 0) "후보 ${optionCount}개" else "후보 준비 중"} · $durationText · 비공개"
                } else {
                    "${if (optionCount > 0) "후보 ${optionCount}개" else "후보 준비 중"} · $durationText"
                }
                textSize = 14f
                setTextColor(0xFF526158.toInt())
                setPadding(0, 0, 0, dp(5))
            })
            addView(composePreviewRow(
                if (soloMode) "공유 프리뷰" else "상대 버튼",
                if (soloMode) "결과 카드" else "밥판 참여",
                true
            ))
            if (!soloMode) {
                addView(TextView(context).apply {
                    text = "${if (revealSelections) "밥친구별 공개" else "득표수만 공개"} · 후보 추가 ${if (allowParticipantOptions) "가능" else "불가"}"
                    textSize = 13f
                    setTextColor(0xFF6F5A4D.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                    setPadding(0, dp(5), 0, 0)
                })
            }
        }
    }

    private fun composeRulesCard(
        summaryText: TextView,
        toggleLink: TextView,
        content: LinearLayout
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(context).apply {
                    text = "밥판 규칙"
                    textSize = 17f
                    setTextColor(0xFF10251D.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(toggleLink)
            })
            addView(summaryText)
            addView(content)
        }
    }

    private fun composeRulesSummary(
        composeMode: ComposeMode,
        durationSeconds: Int,
        requireInviteCode: Boolean,
        allowParticipantOptions: Boolean,
        revealSelections: Boolean
    ): String {
        val modeText = if (composeMode == ComposeMode.SOLO) "내 선택" else "밥신호"
        val inviteText = if (composeMode == ComposeMode.TOGETHER && requireInviteCode) "비공개" else null
        val addText = if (allowParticipantOptions) "후보 추가 가능" else "후보 추가 불가"
        val revealText = if (revealSelections) "밥친구별 선택 공개" else "득표수만 공개"
        return listOfNotNull(modeText, formatDurationText(durationSeconds), inviteText, addText, revealText).joinToString(" · ")
    }

    private fun composePreviewRow(label: String, value: String, positive: Boolean): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(3), 0, dp(3))
            addView(TextView(context).apply {
                text = label
                textSize = 13f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(context).apply {
                text = value
                textSize = 11f
                setTextColor(if (positive) 0xFF245341.toInt() else 0xFF8B5C45.toInt())
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(5), dp(10), dp(5))
                background = rounded(
                    if (positive) 0xFFEAF6EF.toInt() else 0xFFFFF1E8.toInt(),
                    16,
                    if (positive) 0xFF94C6A8.toInt() else 0xFFE0B49E.toInt(),
                    1
                )
            })
        }
    }

    private fun ruleBadgeRow(label: String, value: String, positive: Boolean): LinearLayout {
        return ruleBadgeRow(label, ruleBadgeText(value, positive))
    }

    private fun ruleBadgeRow(label: String, valueView: TextView): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(5), 0, dp(5))
            addView(TextView(context).apply {
                text = label
                textSize = 14f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(valueView)
        }
    }

    private fun ruleBadgeText(value: String, positive: Boolean): TextView {
        return TextView(this).apply {
            text = value
            textSize = 12f
            setTextColor(if (positive) 0xFF245341.toInt() else 0xFF8B5C45.toInt())
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = rounded(
                if (positive) 0xFFEAF6EF.toInt() else 0xFFFFF1E8.toInt(),
                18,
                if (positive) 0xFF94C6A8.toInt() else 0xFFE0B49E.toInt(),
                1
            )
        }
    }

    private fun startRuleRemainingTicker(poll: NearbyPoll, target: TextView) {
        val ticker = object : Runnable {
            override fun run() {
                if (!target.isAttachedToWindow) return
                target.text = poll.remainingText()
                if (!poll.hasEnded()) {
                    handler.postDelayed(this, 1_000L)
                }
            }
        }
        handler.post(ticker)
    }

    private fun showPublishedPoll(poll: NearbyPoll) {
        val ended = poll.hasEnded()
        val receivedVotes = votesFor(poll.id)
        val receivedVoteNames = voteNamesFor(poll.id)
        val receivedVoteAvatars = voteAvatarsFor(poll.id)
        setPage("밥판")
        rememberScreen { showPublishedPoll(poll) }
        page.addView(breadcrumb("홈", "밥판", "내가 연 밥판"))
        page.addView(topBar("내가 연 밥판"))
        page.addView(avatarInfoCard(if (ended) "밥판 종료" else "밥신호 발신 중", poll.question, "", selfAvatarId, poll))
        if (!ended) {
            page.addView(joinQrHostCard(poll))
            page.addView(pollParticipationCard(poll))
        }
        val hasMyVote = receivedVotes.containsKey(userId)
        if (ended || hasMyVote || sharedResultPollIds.contains(poll.id)) {
            page.addView(publishedPollStageCard(poll, hasMyVote, receivedVotes.size))
        }
        if (!ended) {
            if (hasMyVote) {
                page.addView(statusCard("나의 참여", "내 선택: ${receivedVotes[userId].orEmpty()}"))
            } else {
                page.addView(hostSelectionCard(poll))
                if (poll.allowParticipantOptions) {
                    page.addView(participantOptionComposer(poll))
                }
            }
        }
        if (hasMyVote) {
            page.addView(label("현재 메뉴 판세"))
            poll.options.forEach { option ->
                val count = receivedVotes.values.count { it == option }
                val percent = count * 100 / receivedVotes.size
                page.addView(resultRow(option, count, percent))
            }
            page.addView(label("밥친구 ${receivedVotes.size}명"))
            page.addView(participantTagBar(receivedVoteNames.map { (id, name) ->
                name to (receivedVoteAvatars[id] ?: avatarIdForUser(id))
            }).apply { layoutParams = blockParams() })
        }
        if (!sharedResultPollIds.contains(poll.id)) {
            if (hasMyVote || ended) {
                page.addView(primaryButton(if (hasMyVote) "오늘의 밥결정 카드 만들기" else "메뉴 결정") { endPollAndShareResult(poll) }.apply {
                    tutorialEndTarget = this
                })
            } else {
                page.addView(statusCard("선택 후 결정", "메뉴를 하나 고르면 오늘의 밥결정 카드를 만들 수 있습니다."))
            }
        }
    }

    private fun pollParticipationCard(poll: NearbyPoll): TextView {
        return statusCard("밥판 참여 현황", pollParticipationSnapshot(poll).detailText())
    }

    private fun joinQrHostCard(poll: NearbyPoll): LinearLayout {
        val joinUri = buildJoinUri(poll)
        val privatePoll = poll.inviteCode.isNotBlank()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(13), dp(16), dp(13))
            background = groupedCardBackground(0xFFFFF0D9.toInt(), 0xFFD73B24.toInt())
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = if (privatePoll) "비공개 밥판 코드" else "밥판 참여 QR"
                textSize = 13f
                setTextColor(0xFF8B5C45.toInt())
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
            })
            if (privatePoll) {
                addView(TextView(context).apply {
                    text = poll.inviteCode
                    textSize = 34f
                    letterSpacing = 0.08f
                    setTextColor(0xFFD73B24.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                    setPadding(0, dp(4), 0, dp(4))
                })
            }
            addView(TextView(context).apply {
                text = if (privatePoll) {
                    "같이 먹을 사람에게 이 번호나 QR을 알려주세요."
                } else {
                    "자동 연결이 늦으면 같이 먹을 사람이 QR로 이 밥판을 먼저 찾을 수 있어요."
                }
                textSize = 12f
                setTextColor(0xFF526158.toInt())
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, if (privatePoll) 0 else dp(8), 0, 0)
            })
            createJoinQrBitmap(joinUri, dp(156))?.let { qrBitmap ->
                addView(ImageView(context).apply {
                    setImageBitmap(qrBitmap)
                    contentDescription = "밥판 참여 QR"
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    background = rounded(0xFFFFFFFF.toInt(), 16, 0xFFE7B59D.toInt(), 1)
                    layoutParams = LinearLayout.LayoutParams(dp(176), dp(176)).apply {
                        topMargin = dp(12)
                    }
                })
                addView(TextView(context).apply {
                    text = if (privatePoll) {
                        "QR은 연결 힌트로만 쓰고, 실제 밥판 내용은 가까운 기기 연결로 전달됩니다."
                    } else {
                        "QR에는 후보와 참여자 정보가 들어가지 않습니다."
                    }
                    textSize = 12f
                    setTextColor(0xFF526158.toInt())
                    gravity = Gravity.CENTER
                    setPadding(0, dp(8), 0, 0)
                })
            }
        }
    }

    private fun buildJoinUri(poll: NearbyPoll): String {
        return Uri.Builder()
            .scheme("babcross")
            .authority("join")
            .appendQueryParameter("v", "1")
            .appendQueryParameter("poll", poll.id)
            .appendQueryParameter("code", poll.inviteCode)
            .appendQueryParameter("host", joinHostToken(poll.id, poll.proposerId, poll.inviteCode, poll.endAtMillis))
            .appendQueryParameter("exp", poll.endAtMillis.toString())
            .appendQueryParameter("nonce", hash("join-nonce:${poll.id}:${poll.inviteCode}:${poll.endAtMillis}").take(10))
            .build()
            .toString()
    }

    private fun joinHostToken(pollId: String, proposerId: String, inviteCode: String, expiresAtMillis: Long): String {
        return hash("join-host:$pollId:$proposerId:$inviteCode:$expiresAtMillis").take(16)
    }

    private fun createJoinQrBitmap(value: String, sizePx: Int): Bitmap? {
        return runCatching {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
                for (y in 0 until sizePx) {
                    for (x in 0 until sizePx) {
                        setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }
            }
        }.getOrElse {
            appendLog("밥판 QR 생성 실패: ${it.message}")
            null
        }
    }

    private fun handleJoinIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "babcross" || uri.host != "join") return
        val hint = parseJoinHint(uri) ?: run {
            Toast.makeText(this, "유효하지 않은 밥판 QR입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        pendingJoinHint = hint
        if (!tryShowPendingJoinHint()) {
            showJoinPendingPage(hint)
        }
        if (!autoConnectEnabled) {
            setAutoConnectEnabled(true)
        }
        if (hasNearbyPermissions() && disabledConnectionRequirements().isEmpty()) {
            startNearbyConnectionTest()
        } else {
            showPermissionFallbackDialog()
        }
    }

    private fun parseJoinHint(uri: Uri): JoinHint? {
        if (uri.getQueryParameter("v") != "1") return null
        val pollId = uri.getQueryParameter("poll")?.trim().orEmpty()
        val inviteCode = uri.getQueryParameter("code")?.trim().orEmpty()
        val hostToken = uri.getQueryParameter("host")?.trim().orEmpty()
        val expiresAtMillis = uri.getQueryParameter("exp")?.toLongOrNull() ?: return null
        val now = System.currentTimeMillis()
        val valid = pollId.startsWith("poll-") &&
            pollId.length <= MAX_ID_LENGTH &&
            (inviteCode.isBlank() || inviteCode.matches(Regex("\\d{4}"))) &&
            hostToken.matches(Regex("[0-9a-f]{8,32}")) &&
            expiresAtMillis > now &&
            expiresAtMillis <= now + JOIN_QR_MAX_FUTURE_MS
        return if (valid) JoinHint(pollId, inviteCode, hostToken, expiresAtMillis) else null
    }

    private fun showJoinPendingPage(hint: JoinHint) {
        setPage("밥판")
        page.addView(breadcrumb("홈", "밥판", "QR 연결"))
        page.addView(topBar("QR 밥판 연결"))
        page.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(22), dp(18), dp(22))
            background = groupedCardBackground(0xFFFFFBF5.toInt(), 0xFFE7B59D.toInt())
            layoutParams = blockParams()
            addView(ImageView(context).apply {
                setImageResource(R.drawable.bab_cross_launcher)
                contentDescription = "밥크로스"
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = rounded(0xFFFFF1E8.toInt(), 24, 0xFFE7B59D.toInt(), 2)
                clipToOutline = true
                layoutParams = LinearLayout.LayoutParams(dp(112), dp(112)).apply {
                    bottomMargin = dp(18)
                }
            })
            addView(TextView(context).apply {
                text = "밥판을 연결 중 입니다."
                textSize = 23f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                pendingJoinStatusText = this
            })
            addView(TextView(context).apply {
                text = "QR은 밥판을 찾기 위한 힌트로만 사용하고, 실제 메뉴와 참여 정보는 가까운 기기 연결로 받을게요."
                textSize = 15f
                setTextColor(0xFF526158.toInt())
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            })
            addView(TextView(context).apply {
                val remainingSeconds = ((hint.expiresAtMillis - System.currentTimeMillis()) / 1_000L).coerceAtLeast(0L)
                text = "남은 대기 시간 약 ${remainingSeconds / 60L + 1L}분"
                textSize = 13f
                setTextColor(0xFF8A5A44.toInt())
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            })
        })
        page.addView(primaryButton("다시 연결 시도") {
            if (hasNearbyPermissions() && disabledConnectionRequirements().isEmpty()) {
                startNearbyConnectionTest()
                tryShowPendingJoinHint()
            } else {
                showPermissionFallbackDialog()
            }
        })
        page.addView(outlineButton("홈으로") {
            pendingJoinHint = null
            showHome()
        })
        startJoinPendingDots()
    }

    private fun startJoinPendingDots() {
        handler.removeCallbacks(pendingJoinDots)
        pendingJoinDotStep = 0
        pendingJoinStatusText?.text = "밥판을 연결 중 입니다."
        handler.postDelayed(pendingJoinDots, JOIN_PENDING_DOT_MS)
    }

    private fun tryShowPendingJoinHint(): Boolean {
        val hint = pendingJoinHint ?: return false
        if (hint.expiresAtMillis <= System.currentTimeMillis()) {
            pendingJoinHint = null
            pendingJoinStatusText = null
            handler.removeCallbacks(pendingJoinDots)
            Toast.makeText(this, "밥판 QR 시간이 지났습니다.", Toast.LENGTH_SHORT).show()
            return true
        }
        val poll = incomingPolls[hint.pollId] ?: return false
        if (!joinHintMatches(hint, poll)) {
            appendLog("QR 힌트와 다른 밥판은 자동 진입하지 않음")
            return false
        }
        pendingJoinHint = null
        pendingJoinStatusText = null
        handler.removeCallbacks(pendingJoinDots)
        acceptPollFromQr(poll)
        return true
    }

    private fun joinHintMatches(hint: JoinHint, poll: NearbyPoll): Boolean {
        return poll.id == hint.pollId &&
            poll.inviteCode == hint.inviteCode &&
            !poll.hasEnded() &&
            joinHostToken(poll.id, poll.proposerId, poll.inviteCode, poll.endAtMillis) == hint.hostToken
    }

    private fun acceptPollFromQr(poll: NearbyPoll) {
        declinedPollIds -= poll.id
        val alreadyAccepted = acceptedPollIds.contains(poll.id)
        acceptedPollIds += poll.id
        if (!alreadyAccepted) {
            sendPollResponse(poll, POLL_RESPONSE_ACCEPTED)
        }
        scheduleAcceptedPollResponseRetry(poll)
        persistSessionState()
        Toast.makeText(this, "QR로 밥판에 참여합니다.", Toast.LENGTH_SHORT).show()
        openVotePollAfterJoin(poll)
    }

    private fun scheduleAcceptedPollResponseRetry(poll: NearbyPoll) {
        listOf(QR_ACCEPT_RESPONSE_RETRY_MS, QR_ACCEPT_RESPONSE_RETRY_MS * 2).forEach { delayMs ->
            handler.postDelayed({
                if (acceptedPollIds.contains(poll.id) && incomingPolls.containsKey(poll.id) && !poll.hasEnded()) {
                    sendPollResponse(poll, POLL_RESPONSE_ACCEPTED)
                }
            }, delayMs)
        }
    }

    private fun publishedPollStageCard(poll: NearbyPoll, hasMyVote: Boolean, voteCount: Int): LinearLayout {
        val hasResult = sharedResultPollIds.contains(poll.id)
        val (title, message) = when {
            hasResult -> "밥결정 완료" to "오늘의 밥결정 카드가 준비되었습니다."
            poll.hasEnded() -> "밥판 종료" to "제한시간이 끝났습니다. 메뉴 결정으로 결과를 확인할 수 있습니다."
            !hasMyVote -> "내 선택부터 정해요" to "밥판장이 한 표를 고르면 결과 카드까지 바로 만들 수 있습니다."
            voteCount <= 1 -> "결과 카드 공유 준비" to "내 선택은 들어갔습니다. 지금 오늘의 밥결정을 만들고 대화방에 공유할 수 있습니다."
            else -> "메뉴 결정 가능" to "선택 ${voteCount}표가 모였습니다. 지금 메뉴를 결정할 수 있습니다."
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(13), dp(16), dp(13))
            background = groupedCardBackground(0xFFFFFBF5.toInt(), 0xFFE7B59D.toInt())
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = title
                textSize = 17f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = message
                textSize = 13f
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(5), 0, 0)
            })
        }
    }

    private fun showVotePoll(poll: NearbyPoll) {
        setPage("밥판")
        rememberScreen { showVotePoll(poll) }
        page.addView(breadcrumb("홈", "밥판", "메뉴 선택"))
        page.addView(topBar("메뉴 선택"))
        page.addView(avatarInfoCard("밥판", poll.question, "보낸 밥친구: ${poll.proposerName}", resolvedAvatarId(poll.proposerId, poll.proposerAvatarId), poll))
        val submitted = submittedVotes[poll.id]
        when {
            sharedResultsByPoll[poll.id] != null -> {
                page.addView(primaryButton("결정 메뉴 보기") { showSharedResult(sharedResultsByPoll.getValue(poll.id)) })
            }
            submitted != null -> {
                page.addView(statusCard("이미 메뉴 선택 완료", submitted))
            }
            poll.hasEnded() -> {
                page.addView(statusCard("밥판 종료", "제한시간이 지나 더 이상 고를 수 없습니다."))
            }
            else -> {
                page.addView(label("메뉴 후보"))
                poll.options.forEach { option ->
                    page.addView(choicePill(option) {
                        if (System.currentTimeMillis() < voteChoiceGuardUntilMillis) return@choicePill
                        castVote(poll, option)
                    }.apply {
                        val enableDelay = voteChoiceGuardUntilMillis - System.currentTimeMillis()
                        if (enableDelay > 0L) {
                            isEnabled = false
                            alpha = 0.72f
                            postDelayed({
                                isEnabled = true
                                alpha = 1f
                            }, enableDelay)
                        }
                    })
                }
                if (poll.allowParticipantOptions) {
                    page.addView(participantOptionComposer(poll))
                }
            }
        }
    }

    private fun openVotePollAfterJoin(poll: NearbyPoll) {
        voteChoiceGuardUntilMillis = System.currentTimeMillis() + JOIN_TO_CHOICE_GUARD_MS
        showVotePoll(poll)
    }

    private fun showVoteSubmitted(poll: NearbyPoll, option: String) {
        setPage("밥판")
        rememberScreen { showVoteSubmitted(poll, option) }
        page.addView(breadcrumb("홈", "밥판", "선택 완료"))
        page.addView(topBar("선택 완료"))
        page.addView(statusCard("내 밥신호를 보냈습니다", "${poll.question} · $option"))
        val receipt = latestReceipt
        if (receipt == null || receipt.pollId != poll.id) {
            page.addView(bodyText("밥판을 연 기기에 선택 메시지가 전달되면 영수증이 도착합니다."))
        } else {
            page.addView(statusCard("영수증 수신 완료", "내 밥해시 ${receipt.voteHash.take(16)}"))
        }
        sharedResultsByPoll[poll.id]?.let {
            page.addView(primaryButton("결정 메뉴 보기") { showSharedResult(it) })
        }
    }

    private fun showSharedResult(
        result: SharedResult,
        resultDeck: List<SharedResult> = resultDeckFor(result),
        deckIndex: Int = resultDeck.indexOfFirst { it.pollId == result.pollId }.takeIf { it >= 0 } ?: 0
    ) {
        setPage("결과")
        rememberScreen { showSharedResult(result, resultDeck, deckIndex) }
        page.addView(breadcrumb("홈", "결과"))
        page.addView(topBar("오늘의 밥결정"))
        page.addView(resultDeckHeader(result))
        val receipt = latestReceipt?.takeIf { it.pollId == result.pollId } ?: store.loadReceipt(result.pollId)
        page.addView(resultDeckCarousel(resultDeck, deckIndex, receipt))
        page.addView(resultFollowUpActions(result))
        tieBreakSuggestion(result)?.let { suggestion ->
            page.addView(tieBreakSuggestionCard(suggestion))
        }
        page.addView(cardVoteSummary(result))
        if (result.participantSelections.isNotEmpty()) {
            page.addView(participantSelectionSummary(result))
        } else if (result.participantNames.isNotEmpty()) {
            page.addView(participantTagBar(result.participantNames.mapIndexed { index, name ->
                val id = result.participantIds.getOrNull(index).orEmpty()
                name to (result.participantAvatarIds[id] ?: avatarIdForUser(id))
            }).apply {
                layoutParams = blockParams()
            })
        }
        if (!result.revealSelections) {
            page.addView(bodyText("이 밥판은 밥친구별 선택 내역을 공개하지 않습니다."))
        }
        page.addView(resultActions(result))
    }

    private fun tieBreakSuggestionCard(menu: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(0xFFFFF0D9.toInt(), 14, 0xFFE7B59D.toInt(), 1)
            layoutParams = blockParams()
            addView(ImageView(context).apply {
                setImageResource(R.drawable.bab_cross_launcher)
                contentDescription = "밥크로스 추천"
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = rounded(0xFFFFFFFF.toInt(), 16, 0xFFE7B59D.toInt(), 1)
                clipToOutline = true
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                    rightMargin = dp(12)
                }
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply {
                    text = "동률이에요"
                    textSize = 12f
                    setTextColor(0xFF8B5C45.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = "밥크로스는 $menu(으)로 결정하면 어떨까 해요."
                    textSize = 15f
                    setTextColor(0xFF10251D.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, dp(4), 0, 0)
                })
            })
        }
    }

    private fun participantSelectionSummary(result: SharedResult): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(14))
            background = rounded(0xFFFFFFFF.toInt(), 12, 0xFFE7B59D.toInt(), 1)
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = "밥친구별 선택"
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(0xFF10251D.toInt())
            })
            result.participantIds.forEachIndexed { index, participantId ->
                val selectedOption = result.participantSelections[participantId] ?: return@forEachIndexed
                val participantName = result.participantNames.getOrNull(index) ?: participantId.take(8)
                addView(participantSelectionRow(
                    name = participantName,
                    avatarId = result.participantAvatarIds[participantId] ?: avatarIdForUser(participantId),
                    selectedOption = selectedOption
                ))
            }
        }
    }

    private fun participantSelectionRow(name: String, avatarId: Int, selectedOption: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, 0)
            addView(AvatarTileView(avatarId).apply {
                layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                    rightMargin = dp(8)
                }
            })
            addView(TextView(context).apply {
                text = name
                textSize = 14f
                setTextColor(0xFF245341.toInt())
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(context).apply {
                text = selectedOption
                textSize = 14f
                setTextColor(0xFFD73B24.toInt())
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(6), dp(12), dp(6))
                background = rounded(0xFFFFF1E8.toInt(), 16, 0xFFE7B59D.toInt(), 1)
            })
        }
    }

    private fun resultDeckFor(current: SharedResult): List<SharedResult> {
        val resultsByPollId = linkedMapOf<String, SharedResult>()
        store.loadResultHistory().forEach { result -> resultsByPollId[result.pollId] = result }
        sharedResultsByPoll.values.forEach { result -> resultsByPollId.putIfAbsent(result.pollId, result) }
        resultsByPollId.putIfAbsent(current.pollId, current)
        return resultsByPollId.values.sortedByDescending { it.createdAtMillis }
    }

    private fun resultDeckHeader(result: SharedResult): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = rounded(0xFFFFFFFF.toInt(), 14, 0xFFE0E7DD.toInt())
            layoutParams = blockParams()
            addView(AvatarTileView(resolvedAvatarId(result.proposerId, result.proposerAvatarId)).apply {
                layoutParams = LinearLayout.LayoutParams(dp(42), dp(42)).apply {
                    rightMargin = dp(10)
                }
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply {
                    text = "밥판장 ${result.proposerName}"
                    textSize = 15f
                    setTextColor(0xFF10251D.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(TextView(context).apply {
                    text = friendlyTime(result.createdAtMillis)
                    textSize = 13f
                    setTextColor(0xFF647268.toInt())
                    gravity = Gravity.END
                    setTypeface(typeface, Typeface.BOLD)
                })
            })
        }
    }

    private fun resultDeckCarousel(resultDeck: List<SharedResult>, deckIndex: Int, currentReceipt: VoteReceipt?): FrameLayout {
        val currentResult = resultDeck[deckIndex]
        val previousResult = resultDeck.getOrNull(deckIndex - 1)
        val nextResult = resultDeck.getOrNull(deckIndex + 1)
        val currentCard = decisionShareCard(currentResult, currentReceipt)
        val previousCard = previousResult?.let { result ->
            decisionShareCard(result, store.loadReceipt(result.pollId))
        }
        val nextCard = nextResult?.let { result ->
            decisionShareCard(result, store.loadReceipt(result.pollId))
        }
        listOfNotNull(previousCard, currentCard, nextCard).forEach { card ->
            card.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(460),
                Gravity.CENTER
            )
        }
        return FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            setPadding(dp(30), 0, dp(30), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(472)
            ).apply {
                bottomMargin = dp(12)
            }
            previousCard?.let { card ->
                addView(card)
            }
            nextCard?.let { card ->
                addView(card)
            }
            addView(currentCard)
            post {
                layoutResultDeckCards(currentCard, previousCard, nextCard, 0f, 0)
            }
            attachResultDeckSwipe(
                resultDeck = resultDeck,
                deckIndex = deckIndex,
                currentResult = currentResult,
                currentCard = currentCard,
                previousCard = previousCard,
                nextCard = nextCard
            )
        }
    }

    private fun FrameLayout.layoutResultDeckCards(
        currentCard: View,
        previousCard: View?,
        nextCard: View?,
        dragX: Float,
        directionHint: Int
    ) {
        val cardWidth = currentCard.width.takeIf { it > 0 } ?: width.coerceAtLeast(1)
        val sideOffset = cardWidth * 0.68f
        val progress = (abs(dragX) / (cardWidth * RESULT_DECK_COMMIT_RATIO)).coerceIn(0f, 1f)
        currentCard.apply {
            translationX = dragX
            rotation = (dragX / cardWidth) * 4f
            scaleX = 1f - progress * 0.04f
            scaleY = 1f - progress * 0.04f
            alpha = 1f - progress * 0.14f
        }
        previousCard?.apply {
            val active = dragX > 0f || directionHint > 0
            translationX = -sideOffset + dragX
            rotation = -2.5f + progress * 2.5f
            scaleX = if (active) 0.94f + progress * 0.06f else 0.92f
            scaleY = scaleX
            alpha = if (active) 0.62f + progress * 0.38f else 0.48f
            visibility = View.VISIBLE
        }
        nextCard?.apply {
            val active = dragX < 0f || directionHint < 0
            translationX = sideOffset + dragX
            rotation = 2.5f - progress * 2.5f
            scaleX = if (active) 0.94f + progress * 0.06f else 0.92f
            scaleY = scaleX
            alpha = if (active) 0.62f + progress * 0.38f else 0.48f
            visibility = View.VISIBLE
        }
    }

    private fun FrameLayout.attachResultDeckSwipe(
        resultDeck: List<SharedResult>,
        deckIndex: Int,
        currentResult: SharedResult,
        currentCard: View,
        previousCard: View?,
        nextCard: View?
    ) {
        var downX = 0f
        var downY = 0f
        var horizontalDrag = false
        var verticalDrag = false
        var navigating = false
        var longPressConsumed = false
        val longPressSlop = dp(10)
        val longPressRunnable = Runnable {
            longPressConsumed = true
            parent?.requestDisallowInterceptTouchEvent(true)
            currentCard.performSelectionHaptic()
            copyDecisionCardImage(currentResult, currentCard)
        }
        setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    horizontalDrag = false
                    verticalDrag = false
                    navigating = false
                    longPressConsumed = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    currentCard.animate().cancel()
                    previousCard?.animate()?.cancel()
                    nextCard?.animate()?.cancel()
                    layoutResultDeckCards(currentCard, previousCard, nextCard, 0f, 0)
                    handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.x - downX
                    val deltaY = event.y - downY
                    if (abs(deltaX) > longPressSlop || abs(deltaY) > longPressSlop) {
                        handler.removeCallbacks(longPressRunnable)
                    }
                    if (longPressConsumed) {
                        return@setOnTouchListener true
                    }
                    if (!horizontalDrag && !verticalDrag) {
                        when {
                            abs(deltaY) > dp(10) && abs(deltaY) > abs(deltaX) * 1.15f -> {
                                verticalDrag = true
                                parent?.requestDisallowInterceptTouchEvent(false)
                                layoutResultDeckCards(currentCard, previousCard, nextCard, 0f, 0)
                                return@setOnTouchListener false
                            }
                            abs(deltaX) > dp(10) && abs(deltaX) > abs(deltaY) * 1.12f -> {
                                horizontalDrag = true
                                parent?.requestDisallowInterceptTouchEvent(true)
                            }
                        }
                    }
                    if (horizontalDrag) {
                        parent?.requestDisallowInterceptTouchEvent(true)
                        val blockedDirection = (deltaX > 0f && previousCard == null) || (deltaX < 0f && nextCard == null)
                        val dragX = if (blockedDirection) deltaX * 0.18f else deltaX * 0.74f
                        layoutResultDeckCards(currentCard, previousCard, nextCard, dragX, deltaX.signDirection())
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPressRunnable)
                    parent?.requestDisallowInterceptTouchEvent(false)
                    if (longPressConsumed) {
                        animateResultDeckReset(currentCard, previousCard, nextCard)
                        return@setOnTouchListener true
                    }
                    val deltaX = event.x - downX
                    val deltaY = event.y - downY
                    val commitWidth = currentCard.width.takeIf { it > 0 } ?: view.width
                    if (horizontalDrag && abs(deltaX) > commitWidth * RESULT_DECK_COMMIT_RATIO && abs(deltaX) > abs(deltaY) * 1.15f) {
                        val nextIndex = if (deltaX < 0) deckIndex + 1 else deckIndex - 1
                        if (nextIndex in resultDeck.indices) {
                            navigating = true
                            view.performSelectionHaptic()
                            animateResultDeckCommit(currentCard, previousCard, nextCard, deltaX.signDirection()) {
                                showResultDeckItem(resultDeck, nextIndex)
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "더 볼 밥결정이 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    if (!navigating) {
                        animateResultDeckReset(currentCard, previousCard, nextCard)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPressRunnable)
                    parent?.requestDisallowInterceptTouchEvent(false)
                    animateResultDeckReset(currentCard, previousCard, nextCard)
                    true
                }
                else -> true
            }
        }
    }

    private fun FrameLayout.animateResultDeckReset(currentCard: View, previousCard: View?, nextCard: View?) {
        val cardWidth = currentCard.width.takeIf { it > 0 } ?: width.coerceAtLeast(1)
        val sideOffset = cardWidth * 0.68f
        currentCard.animate()
            .translationX(0f)
            .rotation(0f)
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(160L)
            .start()
        previousCard?.animate()
            ?.translationX(-sideOffset)
            ?.rotation(-2.5f)
            ?.scaleX(0.94f)
            ?.scaleY(0.94f)
            ?.alpha(0.62f)
            ?.setDuration(160L)
            ?.start()
        nextCard?.animate()
            ?.translationX(sideOffset)
            ?.rotation(2.5f)
            ?.scaleX(0.94f)
            ?.scaleY(0.94f)
            ?.alpha(0.62f)
            ?.setDuration(160L)
            ?.start()
    }

    private fun FrameLayout.animateResultDeckCommit(
        currentCard: View,
        previousCard: View?,
        nextCard: View?,
        direction: Int,
        onDone: () -> Unit
    ) {
        val cardWidth = (currentCard.width.takeIf { it > 0 } ?: width.coerceAtLeast(1)).toFloat()
        val enteringCard = if (direction < 0) nextCard else previousCard
        currentCard.animate()
            .translationX(if (direction < 0) -cardWidth else cardWidth)
            .rotation(if (direction < 0) -5f else 5f)
            .scaleX(0.94f)
            .scaleY(0.94f)
            .alpha(0f)
            .setDuration(150L)
            .start()
        enteringCard?.animate()
            ?.translationX(0f)
            ?.rotation(0f)
            ?.scaleX(1f)
            ?.scaleY(1f)
            ?.alpha(1f)
            ?.setDuration(150L)
            ?.withEndAction { onDone() }
            ?.start() ?: onDone()
    }

    private fun Float.signDirection(): Int {
        return if (this < 0f) -1 else 1
    }

    private fun showResultDeckItem(resultDeck: List<SharedResult>, deckIndex: Int) {
        val previousRestoring = restoringScreen
        restoringScreen = true
        showSharedResult(resultDeck[deckIndex], resultDeck, deckIndex)
        restoringScreen = previousRestoring
    }

    private fun resultActions(result: SharedResult): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = blockParams()
            addView(resultIconButton("밥판 템플릿 저장", ResultAction.SAVE_TEMPLATE, 0xFFB37A19.toInt()) {
                store.saveTemplate(resultAsTemplate(result, "template-${System.currentTimeMillis()}"))
                Toast.makeText(this@MainActivity, "밥판 템플릿 저장 완료", Toast.LENGTH_SHORT).show()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(58)
                ).apply {
                    topMargin = dp(10)
                }
            })
        }
    }

    private fun participantOptionComposer(poll: NearbyPoll): LinearLayout {
        val optionInput = inputBox("새 메뉴 후보 입력", "").apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                rightMargin = dp(8)
            }
            setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    keyboardLiftTarget = view
                    keyboardLiftToBottom = true
                    view.postDelayed({ liftParticipantOptionInput() }, KEYBOARD_SCROLL_DELAY_MS)
                } else if (keyboardLiftTarget === view) {
                    keyboardLiftTarget = null
                    keyboardLiftToBottom = false
                }
            }
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = "새 메뉴 후보로 고르기"
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(0xFF526158.toInt())
                setPadding(dp(2), dp(10), 0, dp(7))
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(optionInput)
                addView(compactButton("제출", BUTTON_CHOICE) {
                    val suggestion = normalizedOption(optionInput.text.toString())
                    when {
                        suggestion.isBlank() -> Toast.makeText(this@MainActivity, "메뉴 후보를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                        suggestion.length > MAX_OPTION_LENGTH -> Toast.makeText(this@MainActivity, "메뉴 후보는 ${MAX_OPTION_LENGTH}자 이하로 입력해 주세요.", Toast.LENGTH_SHORT).show()
                        suggestion !in poll.options && poll.options.size >= MAX_POLL_OPTION_COUNT -> Toast.makeText(this@MainActivity, "메뉴 후보는 최대 ${MAX_POLL_OPTION_COUNT}개까지 추가할 수 있습니다.", Toast.LENGTH_SHORT).show()
                        else -> castVote(poll, suggestion)
                    }
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(dp(70), dp(48))
                })
            })
        }
    }

    private fun resultAsTemplate(result: SharedResult, id: String): PollTemplate {
        val durationSeconds = result.durationSeconds.coerceIn(CUSTOM_DURATION_MIN_SECONDS, CUSTOM_DURATION_MAX_SECONDS)
        return PollTemplate(
            id = id,
            title = result.question,
            question = result.question,
            options = result.options,
            durationMinutes = ((durationSeconds + 59) / 60).coerceAtLeast(1),
            durationSeconds = durationSeconds,
            allowParticipantOptions = result.allowParticipantOptions,
            revealSelections = result.revealSelections,
            categoryKey = inferredFoodCategory(result.options).name
        )
    }

    private fun demoSharedResult(): SharedResult {
        val templates = store.loadTemplates().filter { it.builtIn && it.options.size >= 3 }
        val template = templates.randomOrNull() ?: PollTemplate(
            id = "demo",
            title = "밥판 체험",
            question = "오늘 점심 뭐 먹지?",
            options = listOf("국밥", "돈까스", "샐러드"),
            durationMinutes = 1,
            durationSeconds = 60,
            builtIn = true
        )
        val participantNames = listOf("급식대장", "면치기요정", "국물수호자", selfName)
        val participantIds = participantNames.mapIndexed { index, name -> "demo-$index-$name" }
        val winner = template.options.random()
        val selections = linkedMapOf<String, String>()
        participantIds.forEachIndexed { index, participantId ->
            selections[participantId] = when (index) {
                0, 1 -> winner
                else -> template.options[(template.options.indexOf(winner) + index) % template.options.size]
            }
        }
        val counts = template.options.associateWith { option ->
            selections.values.count { it == option }
        }
        val pollId = "demo-${System.currentTimeMillis()}"
        return SharedResult(
            pollId = pollId,
            proposerId = userId,
            proposerName = selfName,
            proposerAvatarId = selfAvatarId,
            question = template.question,
            options = template.options,
            counts = counts,
            participantIds = participantIds,
            participantNames = participantNames,
            participantAvatarIds = participantIds.associateWith { id -> avatarIdForUser(id) },
            participantSelections = selections,
            participantCount = participantIds.size,
            createdAtMillis = System.currentTimeMillis(),
            resultHash = SharedResult.computeHash(
                pollId = pollId,
                question = template.question,
                options = template.options,
                counts = counts,
                participantIds = participantIds,
                participantSelections = selections
            ),
            durationSeconds = template.durationSeconds,
            allowParticipantOptions = template.allowParticipantOptions,
            revealSelections = true
        )
    }

    private fun showSimulationResult(
        result: SharedResult = demoSharedResult(),
        step: DemoStep = DemoStep.OPEN
    ) {
        startRealFlowTutorial(result, step)
    }

    private fun startRealFlowTutorial(result: SharedResult = demoSharedResult(), step: DemoStep = DemoStep.OPEN) {
        tutorialResult = result
        showRealFlowTutorialStep(step, result)
    }

    private fun showRealFlowTutorialStep(step: DemoStep, result: SharedResult = tutorialResult ?: demoSharedResult()) {
        tutorialResult = result
        when (step) {
            DemoStep.OPEN -> {
                showHome()
                pageScrollView?.post {
                    showRealFlowCoach(
                        step = step,
                        targetProvider = { onboardingHomeCreateTarget },
                        primaryAction = { showRealFlowTutorialStep(DemoStep.CATEGORY, result) }
                    )
                }
            }
            DemoStep.CATEGORY -> {
                showComposeCategoryPicker()
                pageScrollView?.post {
                    showRealFlowCoach(
                        step = step,
                        targetProvider = { tutorialCategoryTarget },
                        primaryAction = { showRealFlowTutorialStep(DemoStep.QUESTION, result) }
                    )
                }
            }
            DemoStep.QUESTION -> {
                showCompose(resultAsTemplate(result, "draft-tutorial-${System.currentTimeMillis()}"), inferredFoodCategory(result.options))
                pageScrollView?.post {
                    showRealFlowCoach(
                        step = step,
                        targetProvider = { tutorialQuestionTarget },
                        primaryAction = { showRealFlowTutorialStep(DemoStep.OPTIONS, result) }
                    )
                }
            }
            DemoStep.OPTIONS -> {
                showRealFlowCoach(
                    step = step,
                    targetProvider = { tutorialOptionsTarget },
                    primaryAction = { showRealFlowTutorialStep(DemoStep.SIGNAL, result) }
                )
            }
            DemoStep.SIGNAL -> {
                showRealFlowCoach(
                    step = step,
                    targetProvider = { tutorialPublishTarget },
                    primaryAction = {
                        val poll = createTutorialPoll(result)
                        showPublishedPoll(poll)
                        pageScrollView?.post { showRealFlowTutorialStep(DemoStep.RESPOND, result) }
                    }
                )
            }
            DemoStep.RESPOND -> {
                showRealFlowCoach(
                    step = step,
                    targetProvider = { tutorialResponseTarget },
                    primaryAction = {
                        val poll = tutorialPoll ?: createTutorialPoll(result)
                        applyTutorialVote(poll)
                        pageScrollView?.post { showRealFlowTutorialStep(DemoStep.END, result) }
                    }
                )
            }
            DemoStep.END -> {
                showRealFlowCoach(
                    step = step,
                    targetProvider = { null },
                    primaryAction = {
                        hideCoachOverlay()
                        clearTutorialPoll()
                        Toast.makeText(this, "이제 밥판을 열어 볼까요?", Toast.LENGTH_SHORT).show()
                        showCompose(resultAsTemplate(result, "draft-tutorial-ready-${System.currentTimeMillis()}"), inferredFoodCategory(result.options))
                    }
                )
            }
        }
    }

    private fun showRealFlowCoach(step: DemoStep, targetProvider: () -> View?, primaryAction: () -> Unit) {
        showCoachOverlay(
            step = OnboardingCoachStep(
                title = "${step.ordinal + 1}. ${simulationStepTitle(step)}",
                message = realFlowTutorialMessage(step),
                targetProvider = targetProvider,
                primaryLabel = "다음",
                secondaryLabel = "닫기",
                primaryAction = primaryAction,
                secondaryAction = { cancelRealFlowTutorial() }
            ),
            stepNumber = step.ordinal + 1,
            stepCount = DemoStep.values().size,
            onPrimary = {
                hideCoachOverlay()
                primaryAction()
            },
            onSecondary = { cancelRealFlowTutorial() }
        )
    }

    private fun cancelRealFlowTutorial() {
        hideCoachOverlay()
        clearTutorialPoll()
    }

    private fun realFlowTutorialMessage(step: DemoStep): String {
        return when (step) {
            DemoStep.OPEN -> "실제 홈 화면의 `밥판 열기`에서 시작합니다."
            DemoStep.CATEGORY -> "종류를 고르면 질문과 후보 작성 화면으로 이동합니다."
            DemoStep.QUESTION -> "밥친구에게 보일 질문을 입력하는 자리입니다."
            DemoStep.OPTIONS -> "메뉴 후보를 2개 이상 등록합니다. 이 영역에서 직접 추가하거나 수정할 수 있습니다."
            DemoStep.SIGNAL -> "준비가 끝나면 실제 작성 화면 하단의 `밥신호 보내기`를 누릅니다."
            DemoStep.RESPOND -> "밥판 화면에서 후보를 눌러 응답합니다."
            DemoStep.END -> "튜토리얼은 여기서 끝입니다. 이제 실제 밥판 열기 화면으로 이동해 바로 시작할 수 있습니다."
        }
    }

    private fun createTutorialPoll(result: SharedResult): NearbyPoll {
        clearTutorialPoll()
        val poll = NearbyPoll(
            id = "tutorial-${UUID.randomUUID()}",
            proposerId = userId,
            proposerName = selfName,
            proposerAvatarId = selfAvatarId,
            question = result.question,
            options = result.options,
            durationMinutes = 5,
            durationSeconds = 300,
            endAtMillis = System.currentTimeMillis() + 300_000L,
            allowParticipantOptions = result.allowParticipantOptions,
            revealSelections = result.revealSelections
        )
        tutorialPoll = poll
        activePolls[poll.id] = poll
        receivedVotesByPoll[poll.id] = linkedMapOf()
        receivedVoteNamesByPoll[poll.id] = linkedMapOf()
        receivedVoteAvatarsByPoll[poll.id] = linkedMapOf()
        voteEndpointIdsByPoll[poll.id] = linkedMapOf()
        invitedPeersByPoll[poll.id] = linkedMapOf()
        pollResponsesByPoll[poll.id] = linkedMapOf()
        persistSessionState()
        return poll
    }

    private fun clearTutorialPoll() {
        val pollId = tutorialPoll?.id ?: return
        activePolls.remove(pollId)
        incomingPolls.remove(pollId)
        receivedVotesByPoll.remove(pollId)
        receivedVoteNamesByPoll.remove(pollId)
        receivedVoteAvatarsByPoll.remove(pollId)
        voteEndpointIdsByPoll.remove(pollId)
        invitedPeersByPoll.remove(pollId)
        pollResponsesByPoll.remove(pollId)
        hostConnectedCountsByPoll.remove(pollId)
        hostReadyCountsByPoll.remove(pollId)
        submittedVotes.remove(pollId)
        acceptedPollIds.remove(pollId)
        declinedPollIds.remove(pollId)
        completionPromptShownKeys.removeAll { key -> key == pollId || key.startsWith("$pollId:") }
        sharedResultPollIds.remove(pollId)
        seenIncomingPollIds.remove(pollId)
        seenResultPollIds.remove(pollId)
        sharedResultsByPoll.remove(pollId)
        if (sharedResult?.pollId == pollId) {
            sharedResult = null
        }
        if (latestReceipt?.pollId == pollId) {
            latestReceipt = null
        }
        tutorialPoll = null
        persistSessionState()
    }

    private fun applyTutorialVote(poll: NearbyPoll) {
        val option = poll.options.firstOrNull() ?: return
        votesFor(poll.id)[userId] = option
        voteNamesFor(poll.id)[userId] = selfName
        voteAvatarsFor(poll.id)[userId] = selfAvatarId
        persistSessionState()
        showPublishedPoll(activePolls[poll.id] ?: poll)
    }

    private fun showSimulationCoach(step: DemoStep, result: SharedResult) {
        val coachStep = when (step) {
            DemoStep.OPEN -> OnboardingCoachStep(
                title = "1. 밥판 열기",
                message = "새 밥판은 여기서 시작합니다. 홈 또는 하단 `밥판` 탭에서도 같은 흐름으로 들어갑니다.",
                targetProvider = { simulationCoachTarget },
                primaryLabel = "다음",
                secondaryLabel = "닫기",
                primaryAction = { showSimulationResult(result, DemoStep.CATEGORY) },
                secondaryAction = { hideCoachOverlay() }
            )
            DemoStep.CATEGORY -> OnboardingCoachStep(
                title = "2. 종류 선택",
                message = "종류를 먼저 고르면 작성 중 후보 추천과 템플릿이 그 종류에 맞춰집니다.",
                targetProvider = { simulationCoachTarget },
                primaryLabel = "다음",
                secondaryLabel = "닫기",
                primaryAction = { showSimulationResult(result, DemoStep.QUESTION) },
                secondaryAction = { hideCoachOverlay() }
            )
            DemoStep.QUESTION -> OnboardingCoachStep(
                title = "3. 질문 입력하기",
                message = "밥친구에게 보일 질문입니다. 예: `오늘 점심 뭐 먹지?`",
                targetProvider = { simulationCoachTarget },
                primaryLabel = "다음",
                secondaryLabel = "닫기",
                primaryAction = { showSimulationResult(result, DemoStep.OPTIONS) },
                secondaryAction = { hideCoachOverlay() }
            )
            DemoStep.OPTIONS -> OnboardingCoachStep(
                title = "4. 메뉴 후보 등록하기",
                message = "후보는 2개 이상 필요합니다. 직접 입력하거나 룰렛으로 빠르게 채울 수 있습니다.",
                targetProvider = { simulationCoachTarget },
                primaryLabel = "다음",
                secondaryLabel = "닫기",
                primaryAction = { showSimulationResult(result, DemoStep.SIGNAL) },
                secondaryAction = { hideCoachOverlay() }
            )
            DemoStep.SIGNAL -> OnboardingCoachStep(
                title = "5. 밥신호 보내기",
                message = "질문과 후보가 준비되면 근처 밥친구에게 밥신호를 보냅니다.",
                targetProvider = { simulationCoachTarget },
                primaryLabel = "다음",
                secondaryLabel = "닫기",
                primaryAction = { showSimulationResult(result, DemoStep.RESPOND) },
                secondaryAction = { hideCoachOverlay() }
            )
            DemoStep.RESPOND -> OnboardingCoachStep(
                title = "6. 응답하기",
                message = "밥친구는 밥판 참여 후 후보 중 하나를 고릅니다. 내 밥판에서도 내가 먼저 선택할 수 있습니다.",
                targetProvider = { simulationCoachTarget },
                primaryLabel = "다음",
                secondaryLabel = "닫기",
                primaryAction = { showSimulationResult(result, DemoStep.END) },
                secondaryAction = { hideCoachOverlay() }
            )
            DemoStep.END -> OnboardingCoachStep(
                title = "7. 종료하기",
                message = "선택이 모이면 밥판을 종료하고 오늘의 밥결정 카드로 결과를 확인합니다.",
                targetProvider = { null },
                primaryLabel = "실제 밥판 열기",
                secondaryLabel = "닫기",
                primaryAction = {
                    store.saveDemoCompleted()
                    clearTutorialPoll()
                    Toast.makeText(this, "체험 후보로 실제 밥판을 열어볼게요.", Toast.LENGTH_SHORT).show()
                    showCompose(resultAsTemplate(result, "draft-simulation-ready-${System.currentTimeMillis()}"), inferredFoodCategory(result.options))
                },
                secondaryAction = { hideCoachOverlay() }
            )
        }
        showCoachOverlay(
            step = coachStep,
            stepNumber = step.ordinal + 1,
            stepCount = DemoStep.values().size,
            onPrimary = {
                hideCoachOverlay()
                coachStep.primaryAction?.invoke()
            },
            onSecondary = {
                hideCoachOverlay()
                coachStep.secondaryAction?.invoke()
            }
        )
    }

    private fun showDiagnostics(runSimulation: Boolean = false, autoStart: Boolean = false) {
        setPage("설정")
        rememberScreen { showDiagnostics(runSimulation, autoStart) }
        page.addView(breadcrumb("홈", "설정", "밥친구 찾기 점검"))
        page.addView(topBar("밥친구 찾기 점검"))
        page.addView(bodyText("밥친구가 보이지 않을 때 필요한 상태를 확인하고 다시 찾습니다."))
        connectionStatusView = statusCard("밥친구 찾기 상태", connectionStatusText())
        page.addView(connectionStatusView)
        page.addView(outdoorConnectionChecklistCard())
        page.addView(buttonRow(
            compactButton("밥친구 다시 찾기", BUTTON_PRIMARY) { startNearbyConnectionTest() },
            compactButton("권한 다시 확인", BUTTON_OUTLINE) { requestNearbyPermissions() }
        ))
        page.addView(outlineButton("설정 열기") { openAppSettings() })
        page.addView(outlineButton("밥판 체험하기") { showSimulationResult() })
        val detailPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            layoutParams = blockParams()
        }
        logView = TextView(this).apply {
            text = "Bab-Cross connection detail\n내 밥닉네임: $selfName\n"
            textSize = 13f
            setTextIsSelectable(true)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(0xFFF7F8F5.toInt(), 12)
        }
        detailPanel.addView(compactButtonRow(
            compactButton("테스트 신호", BUTTON_QUIET) {
                nearby.sendToAll(NearVoteMessage.ping(selfName).toJson())
            },
            compactButton("광고만", BUTTON_QUIET) { nearby.startAdvertising() },
            compactButton("탐색만", BUTTON_QUIET) { nearby.startDiscovery() }
        ))
        detailPanel.addView(quietButton("로컬 시뮬레이션 로그 실행") { runLocalSimulation() })
        detailPanel.addView(logView.apply { layoutParams = blockParams() })
        lateinit var detailToggle: Button
        detailToggle = quietButton("상세 기록 보기") {
            val showDetails = detailPanel.visibility != View.VISIBLE
            detailPanel.visibility = if (showDetails) View.VISIBLE else View.GONE
            detailToggle.text = if (showDetails) "상세 기록 숨기기" else "상세 기록 보기"
        }
        page.addView(detailToggle)
        page.addView(detailPanel)
        if (runSimulation) {
            runLocalSimulation()
        }
        if (autoStart) {
            startNearbyConnectionTest()
        }
    }

    private fun setPage(selectedMenu: String, compactTitle: String = selectedMenu) {
        composeSuggestionToken++
        val pageSidePadding = dp(18)
        val pageBaseTopPadding = dp(20)
        val pageBottomPadding = dp(28)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFF8E8.toInt())
        }
        compactTitleBarView = null
        compactTitleTextView = null
        expandedTitleView = null
        stickyHomeTopCardView = null
        pageFrameRoot = null
        stickyBottomActionView = null
        onboardingHomeAvatarTarget = null
        onboardingHomeCreateTarget = null
        onboardingHomeDemoTarget = null
        onboardingSettingsTarget = null
        simulationCoachTarget = null
        tutorialCategoryTarget = null
        tutorialQuestionTarget = null
        tutorialOptionsTarget = null
        tutorialPublishTarget = null
        tutorialResponseTarget = null
        tutorialEndTarget = null
        pendingJoinStatusText = null
        handler.removeCallbacks(pendingJoinDots)
        stickyBottomPaddingExtra = 0
        keyboardLiftTarget = null
        keyboardLiftToBottom = false
        val scroll = ScrollView(this).apply {
            setBackgroundColor(0xFFFFF8E8.toInt())
            clipToPadding = false
            isFillViewport = true
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setOnScrollChangeListener { _, _, scrollY, _, _ ->
                updateCompactTitleBar(scrollY)
            }
        }
        pageScrollView = scroll
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pageSidePadding, pageBaseTopPadding + systemStatusTopInset(), pageSidePadding, pageBottomPadding)
        }
        scroll.addView(page)
        val contentFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            pageFrameRoot = this
            addView(scroll)
            addView(compactTitleBar())
            compactTitleTextView?.text = compactTitle
            if (selectedMenu == "홈") {
                addView(stickyHomeTopCard().also { stickyHomeTopCardView = it })
            }
            ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
                val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                val keyboardBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                val keyboardScrollSpace = if (keyboardBottom > 0) keyboardBottom + dp(24) else 0
                page.setPadding(
                    pageSidePadding,
                    pageBaseTopPadding + statusTop,
                    pageSidePadding,
                    pageBottomPadding + keyboardScrollSpace + stickyBottomPaddingExtra
                )
                compactTitleBarView?.setPadding(dp(18), dp(12) + statusTop, dp(18), dp(12))
                stickyHomeTopCardView?.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP
                ).apply {
                    topMargin = statusTop + dp(8)
                    leftMargin = dp(12)
                    rightMargin = dp(12)
                }
                if (keyboardBottom > 0 && keyboardLiftTarget != null) {
                    scroll.post { liftParticipantOptionInput() }
                }
                insets
            }
            post { ViewCompat.requestApplyInsets(this) }
        }
        root.addView(contentFrame)
        root.addView(bottomMenu(selectedMenu))
        setContentView(root)
    }

    private fun setStickyBottomAction(view: View) {
        val container = pageFrameRoot ?: return
        stickyBottomActionView?.removeFromParent()
        stickyBottomActionView = view
        stickyBottomPaddingExtra = dp(86)
        container.addView(view, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        ).apply {
            leftMargin = dp(14)
            rightMargin = dp(14)
            bottomMargin = dp(12)
        })
        ViewCompat.requestApplyInsets(container)
    }

    private fun liftParticipantOptionInput() {
        val target = keyboardLiftTarget ?: return
        pageScrollView?.let { scroll ->
            target.requestRectangleOnScreen(Rect(0, -dp(18), target.width, target.height + dp(96)), true)
            if (keyboardLiftToBottom) {
                scroll.smoothScrollTo(0, page.height)
            }
        }
    }

    private fun rememberScreen(renderer: () -> Unit) {
        if (!restoringScreen) {
            currentScreen?.let { screenBackStack.addLast(it) }
        }
        currentScreen = renderer
    }

    private fun goBackInApp() {
        val previous = if (screenBackStack.isEmpty()) null else screenBackStack.removeLast()
        if (previous == null) {
            if (currentScreen != null) {
                restoringScreen = true
                showHome()
                restoringScreen = false
            }
            return
        }
        restoringScreen = true
        previous()
        restoringScreen = false
    }

    private fun header(title: String, subtitle: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(18))
            addView(ImageView(context).apply {
                setImageResource(R.drawable.bab_cross_launcher)
                contentDescription = "밥크로스 마스코트"
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = rounded(0xFFFFF1E8.toInt(), 22, 0xFFE7B59D.toInt(), 2)
                clipToOutline = true
                layoutParams = LinearLayout.LayoutParams(dp(92), dp(92)).apply {
                    rightMargin = dp(16)
                }
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply {
                    text = title
                    textSize = 31f
                    setTextColor(0xFF10251D.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = subtitle
                    textSize = 16f
                    setTextColor(0xFF526158.toInt())
                    setPadding(0, dp(8), 0, 0)
                })
            })
        }
    }

    private fun homeDashboardCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(0xFFFFFFFF.toInt(), 18, 0xBFD73B24.toInt(), 2)
            layoutParams = blockParams()
            addView(babCrossBrandPane(compact = false).apply {
                minimumHeight = dp(104)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = dp(10)
                }
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(118), ViewGroup.LayoutParams.WRAP_CONTENT)
                addView(profileSummaryButton(compact = true, nameWidthDp = 54).apply {
                    onboardingHomeAvatarTarget = this
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                })
                addView(homeActionButton("밥판 열기", "", true) { showCompose() }.apply {
                    onboardingHomeCreateTarget = this
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(8)
                    }
                })
            })
        }
    }

    private fun stickyHomeTopCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            elevation = dp(8).toFloat()
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = rounded(0xFFFFFFFF.toInt(), 16, 0xBFD73B24.toInt(), 2)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            ).apply {
                topMargin = systemStatusTopInset() + dp(8)
                leftMargin = dp(12)
                rightMargin = dp(12)
            }
            addView(babCrossBrandPane(compact = true).apply {
                minimumHeight = dp(76)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = dp(10)
                }
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(116), ViewGroup.LayoutParams.WRAP_CONTENT)
                addView(profileSummaryButton(compact = true, nameWidthDp = 48).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                })
                addView(homeActionButton("밥판 열기", "", true) { showCompose() }.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(6)
                    }
                })
            })
        }
    }

    private fun babCrossBrandPane(compact: Boolean): FrameLayout {
        return FrameLayout(this).apply {
            clipChildren = true
            clipToPadding = true
            background = rounded(0xFFFFF1E8.toInt(), if (compact) 13 else 16, 0x66E7B59D, 1)
            addView(ImageView(context).apply {
                setImageResource(R.drawable.bab_cross_launcher)
                contentDescription = "밥크로스 마스코트"
                scaleType = ImageView.ScaleType.CENTER_CROP
                alpha = if (compact) 0.18f else 0.22f
                layoutParams = FrameLayout.LayoutParams(
                    if (compact) dp(76) else dp(104),
                    if (compact) dp(76) else dp(104),
                    Gravity.LEFT or Gravity.CENTER_VERTICAL
                ).apply {
                    leftMargin = if (compact) -dp(14) else -dp(18)
                }
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(if (compact) dp(54) else dp(82), 0, dp(10), 0)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                addView(BabCrossWordmarkView(context, compact).apply {
                    contentDescription = "밥크로스"
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        if (compact) dp(25) else dp(36)
                    )
                })
                if (!compact) {
                    addView(TextView(context).apply {
                        text = "근처 밥친구와 메뉴를 정합니다"
                        textSize = 13f
                        setTextColor(0xFF526158.toInt())
                        maxLines = 2
                        ellipsize = TextUtils.TruncateAt.END
                        setPadding(0, dp(5), 0, 0)
                    })
                }
            })
        }
    }

    private fun homeAvatarButton(): FrameLayout {
        return FrameLayout(this).apply {
            isClickable = true
            isFocusable = true
            contentDescription = "내 밥닉네임 관리"
            setOnClickListener { showMyPage() }
            background = oval(0xFFFFF1E8.toInt(), 0xFF7CAD93.toInt(), 2)
            setPadding(dp(3), dp(3), dp(3), dp(3))
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
            addView(AvatarTileView(selfAvatarId, contentInsetDp = 0, clipAsCircle = true).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
            })
        }
    }

    private fun resultFollowUpActions(result: SharedResult): LinearLayout {
        val mapMenu = mapSearchMenuFor(result)
        val hasVotes = resultHasVotes(result)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = blockParams()
            if (hasVotes) {
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = rounded(0xFFFFFFFF.toInt(), 16, 0xFFE0E7DD.toInt(), 1)
                addView(TextView(context).apply {
                    text = "이제 어디로 갈까"
                    textSize = 17f
                    setTextColor(0xFF10251D.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = "결정 메뉴를 바로 찾고, 대화방에 보내고, 같은 후보로 다시 열 수 있어요."
                    textSize = 13f
                    setTextColor(0xFF526158.toInt())
                    setPadding(0, dp(5), 0, dp(10))
                })
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    addView(resultIconButton("지도에서 찾기", ResultAction.MAP, 0xFF3D8B67.toInt()) {
                        if (mapMenu == null) {
                            Toast.makeText(this@MainActivity, "결정 메뉴가 있어야 지도를 열 수 있습니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            openMapSearch(mapMenu)
                        }
                    }.apply {
                        layoutParams = LinearLayout.LayoutParams(0, dp(58), 1f).apply {
                            rightMargin = dp(8)
                        }
                    })
                    addView(resultIconButton("공유하기", ResultAction.SHARE, 0xFF3D8B67.toInt()) {
                        shareDecisionText(result)
                    }.apply {
                        layoutParams = LinearLayout.LayoutParams(0, dp(58), 1f).apply {
                            leftMargin = dp(8)
                        }
                    })
                })
            }
            addView(resultIconButton("이 후보로 다시 열기", ResultAction.REVOTE, 0xFFD73B24.toInt()) {
                showCompose(resultAsTemplate(result, "draft-result-${System.currentTimeMillis()}"))
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(58)
                ).apply {
                    topMargin = if (hasVotes) dp(10) else 0
                }
            })
        }
    }

    private fun historyReuseShelf(results: List<SharedResult>): LinearLayout {
        val suggestions = historyReuseSuggestions(results)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(0xFFFFFFFF.toInt(), 16, 0xFFE0E7DD.toInt(), 1)
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = "바로 다시 열기"
                textSize = 17f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = "지난 결정에서 다시 쓸 후보를 골라 새 밥판을 빠르게 엽니다."
                textSize = 13f
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(5), 0, dp(8))
            })
            suggestions.forEach { suggestion ->
                addView(historyReuseSuggestionRow(suggestion))
            }
        }
    }

    private fun historyReuseSuggestionRow(suggestion: HistoryReuseSuggestion): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(11), dp(12), dp(11))
            background = rounded(0xFFFFFBF5.toInt(), 12, 0xFFE7B59D.toInt(), 1)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = dp(10)
                }
                addView(TextView(context).apply {
                    text = suggestion.label
                    textSize = 12f
                    setTextColor(0xFF647268.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = suggestion.menu
                    textSize = 16f
                    setTextColor(0xFF10251D.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setPadding(0, dp(3), 0, dp(2))
                })
                addView(TextView(context).apply {
                    text = suggestion.detail
                    textSize = 12f
                    setTextColor(0xFF526158.toInt())
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                })
            })
            addView(compactButton("다시 열기", BUTTON_PRIMARY) {
                showCompose(historyReuseTemplate(suggestion))
            }.apply {
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(dp(88), dp(40))
            })
        }
    }

    private fun homeActionButton(title: String, subtitle: String, primary: Boolean, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            background = if (primary) {
                rounded(0xFFD73B24.toInt(), 15)
            } else {
                rounded(0xFFFFF1E8.toInt(), 15, 0xBFD73B24.toInt(), 1)
            }
            addView(TextView(context).apply {
                text = title
                textSize = 17f
                setTextColor(if (primary) 0xFFFFFFFF.toInt() else 0xFFD73B24.toInt())
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                includeFontPadding = false
                setPadding(dp(6), dp(9), dp(6), dp(9))
            })
            if (subtitle.isNotBlank()) {
                addView(TextView(context).apply {
                    text = subtitle
                    textSize = 12f
                    setTextColor(if (primary) 0xFFFFE5D9.toInt() else 0xFF8B5C45.toInt())
                    gravity = Gravity.CENTER
                    setPadding(0, dp(3), 0, 0)
                })
            }
        }
    }

    private fun bodyText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(0xFF526158.toInt())
            setPadding(0, dp(4), 0, dp(12))
        }
    }

    private fun versionFooter(): TextView {
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "-"
        return TextView(this).apply {
            text = "현재 버전 v$versionName"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(0xFF647268.toInt())
            setPadding(0, dp(24), 0, dp(8))
            layoutParams = blockParams()
        }
    }

    private fun inputBox(
        label: String,
        defaultValue: String,
        multiLine: Boolean = false,
        numberOnly: Boolean = false
    ): EditText {
        return EditText(this).apply {
            hint = label
            setText(defaultValue)
            textSize = 16f
            setPadding(dp(18), dp(12), dp(18), dp(12))
            background = rounded(0xFFFFFFFF.toInt(), 12, 0xFFE0B49E.toInt())
            inputType = when {
                numberOnly -> InputType.TYPE_CLASS_NUMBER
                multiLine -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                else -> InputType.TYPE_CLASS_TEXT
            }
            if (multiLine) {
                minLines = 3
                gravity = Gravity.TOP
            }
            layoutParams = blockParams()
        }
    }

    private fun EditText.afterTextChanged(action: () -> Unit) {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                action()
            }
        })
    }

    private inner class OptionTagEditor(
        initialOptions: List<String>,
        private val onOptionsChanged: () -> Unit = {}
    ) {
        private val options = initialOptions.map { it.trim() }.filter { it.isNotBlank() }.distinct().toMutableList()
        private val rouletteOptions = linkedSetOf<String>()
        private val recentlyAddedOptions = linkedSetOf<String>()
        private var editingIndex: Int? = null
        private val tags = WrappingTagLayout()
        private val newOptionInput = inputBox("메뉴 후보 추가", "").apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                rightMargin = dp(8)
            }
        }
        val view = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = blockParams()
            addView(tags.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(newOptionInput)
                addView(compactButton("추가", BUTTON_CHOICE) { submitOption() }.apply {
                    updateButton = this
                    layoutParams = LinearLayout.LayoutParams(dp(70), dp(48))
                })
            })
        }
        private lateinit var updateButton: Button

        init {
            render()
        }

        fun values(): List<String> = options.toList()

        fun addOption(option: String): Boolean {
            val candidate = option.trim()
            if (candidate.isBlank()) return false
            if (options.any { it == candidate }) {
                Toast.makeText(this@MainActivity, "이미 있는 메뉴 후보입니다.", Toast.LENGTH_SHORT).show()
                return false
            }
            if (candidate.length > MAX_OPTION_LENGTH) {
                Toast.makeText(this@MainActivity, "메뉴 후보는 ${MAX_OPTION_LENGTH}자 이하로 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return false
            }
            if (options.size >= MAX_POLL_OPTION_COUNT) {
                Toast.makeText(this@MainActivity, "메뉴 후보는 최대 ${MAX_POLL_OPTION_COUNT}개까지 추가할 수 있습니다.", Toast.LENGTH_SHORT).show()
                return false
            }
            options += candidate
            recentlyAddedOptions.clear()
            resetEditor()
            render()
            onOptionsChanged()
            return true
        }

        fun addOptions(candidates: List<String>, markAsRoulette: Boolean = false): List<String> {
            val slots = (MAX_POLL_OPTION_COUNT - options.size).coerceAtLeast(0)
            if (slots == 0) return emptyList()
            val additions = candidates
                .map { it.trim() }
                .filter { it.isNotBlank() && it.length <= MAX_OPTION_LENGTH }
                .distinct()
                .filterNot { candidate -> options.any { it == candidate } }
                .take(slots)
            if (additions.isEmpty()) return emptyList()
            options += additions
            recentlyAddedOptions.clear()
            if (markAsRoulette) {
                additions.forEach { option ->
                    val normalized = normalizedOption(option)
                    rouletteOptions += normalized
                    recentlyAddedOptions += normalized
                }
            }
            resetEditor()
            render()
            onOptionsChanged()
            return additions
        }

        private fun submitOption() {
            val candidate = newOptionInput.text.toString().trim()
            if (candidate.isBlank()) {
                Toast.makeText(this@MainActivity, "메뉴 후보를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return
            }
            if (candidate.length > MAX_OPTION_LENGTH) {
                Toast.makeText(this@MainActivity, "메뉴 후보는 ${MAX_OPTION_LENGTH}자 이하로 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return
            }
            val selectedIndex = editingIndex
            if (options.any { it == candidate } && (selectedIndex == null || options[selectedIndex] != candidate)) {
                Toast.makeText(this@MainActivity, "이미 있는 메뉴 후보입니다.", Toast.LENGTH_SHORT).show()
                return
            }
            if (selectedIndex == null) {
                if (options.size >= MAX_POLL_OPTION_COUNT) {
                    Toast.makeText(this@MainActivity, "메뉴 후보는 최대 ${MAX_POLL_OPTION_COUNT}개까지 추가할 수 있습니다.", Toast.LENGTH_SHORT).show()
                    return
                }
                options += candidate
                recentlyAddedOptions.clear()
            } else {
                val previousOption = normalizedOption(options[selectedIndex])
                rouletteOptions -= previousOption
                recentlyAddedOptions -= previousOption
                options[selectedIndex] = candidate
            }
            resetEditor()
            render()
            onOptionsChanged()
        }

        private fun selectForEdit(index: Int) {
            editingIndex = index
            newOptionInput.setText(options[index])
            newOptionInput.selectAll()
            newOptionInput.hint = "메뉴 후보 수정"
            updateButton.text = "수정"
        }

        private fun resetEditor() {
            editingIndex = null
            newOptionInput.text.clear()
            newOptionInput.hint = "메뉴 후보 추가"
            updateButton.text = "추가"
        }

        private fun render() {
            tags.removeAllViews()
            options.forEachIndexed { index, option ->
                val normalized = normalizedOption(option)
                val fromRoulette = normalized in rouletteOptions
                val justAdded = normalized in recentlyAddedOptions
                tags.addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(5), dp(7), dp(5))
                    background = rounded(
                        when {
                            justAdded -> 0xFFFFF0D9.toInt()
                            fromRoulette -> 0xFFEAF6EF.toInt()
                            else -> 0xFFFFF1E8.toInt()
                        },
                        20,
                        when {
                            justAdded -> 0xFFE7B59D.toInt()
                            fromRoulette -> 0xFF94C6A8.toInt()
                            else -> 0xFFE7B59D.toInt()
                        }
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        dp(44)
                    ).apply {
                        rightMargin = dp(8)
                        bottomMargin = dp(8)
                    }
                    addView(TextView(context).apply {
                        text = option
                        textSize = 15f
                        setTextColor(0xFF123126.toInt())
                        setTypeface(typeface, Typeface.BOLD)
                        setPadding(0, 0, dp(10), 0)
                        setOnClickListener { selectForEdit(index) }
                    })
                    if (fromRoulette) {
                        addView(TextView(context).apply {
                            text = if (justAdded) "방금" else "룰렛"
                            textSize = 10f
                            setTextColor(0xFFFFFFFF.toInt())
                            setTypeface(typeface, Typeface.BOLD)
                            gravity = Gravity.CENTER
                            setPadding(dp(6), dp(2), dp(6), dp(2))
                            background = rounded(if (justAdded) 0xFFD73B24.toInt() else 0xFF3D8B67.toInt(), 12)
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                rightMargin = dp(6)
                            }
                        })
                    }
                    addView(TextView(context).apply {
                        text = "×"
                        textSize = 20f
                        gravity = Gravity.CENTER
                        setTextColor(0xFF8B1E1E.toInt())
                        setOnClickListener {
                            val removedOption = normalizedOption(options[index])
                            rouletteOptions -= removedOption
                            recentlyAddedOptions -= removedOption
                            options.removeAt(index)
                            if (editingIndex == index) {
                                resetEditor()
                            } else if (editingIndex != null && editingIndex!! > index) {
                                editingIndex = editingIndex!! - 1
                            }
                            render()
                            onOptionsChanged()
                        }
                    })
                })
            }
        }
    }

    private inner class WrappingTagLayout : ViewGroup(this) {
        override fun generateDefaultLayoutParams(): LayoutParams {
            return MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        override fun generateLayoutParams(attrs: android.util.AttributeSet?): LayoutParams {
            return MarginLayoutParams(context, attrs)
        }

        override fun generateLayoutParams(params: LayoutParams?): LayoutParams {
            return if (params == null) generateDefaultLayoutParams() else MarginLayoutParams(params)
        }

        override fun checkLayoutParams(params: LayoutParams?): Boolean {
            return params is MarginLayoutParams
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val maxWidth = MeasureSpec.getSize(widthMeasureSpec)
            val usableWidth = (maxWidth - paddingLeft - paddingRight).coerceAtLeast(0)
            var lineWidth = 0
            var lineHeight = 0
            var totalHeight = paddingTop + paddingBottom
            var measuredWidth = paddingLeft + paddingRight

            for (index in 0 until childCount) {
                val child = getChildAt(index)
                if (child.visibility == GONE) continue
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
                val params = child.layoutParams as MarginLayoutParams
                val childWidth = child.measuredWidth + params.leftMargin + params.rightMargin
                val childHeight = child.measuredHeight + params.topMargin + params.bottomMargin
                if (lineWidth > 0 && lineWidth + childWidth > usableWidth) {
                    totalHeight += lineHeight
                    measuredWidth = measuredWidth.coerceAtLeast(paddingLeft + paddingRight + lineWidth)
                    lineWidth = childWidth
                    lineHeight = childHeight
                } else {
                    lineWidth += childWidth
                    lineHeight = lineHeight.coerceAtLeast(childHeight)
                }
            }
            totalHeight += lineHeight
            measuredWidth = measuredWidth.coerceAtLeast(paddingLeft + paddingRight + lineWidth)
            setMeasuredDimension(
                resolveSize(measuredWidth, widthMeasureSpec),
                resolveSize(totalHeight, heightMeasureSpec)
            )
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            val usableRight = right - left - paddingRight
            var cursorX = paddingLeft
            var cursorY = paddingTop
            var lineHeight = 0

            for (index in 0 until childCount) {
                val child = getChildAt(index)
                if (child.visibility == GONE) continue
                val params = child.layoutParams as MarginLayoutParams
                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight
                val nextRight = cursorX + params.leftMargin + childWidth + params.rightMargin
                if (cursorX > paddingLeft && nextRight > usableRight) {
                    cursorX = paddingLeft
                    cursorY += lineHeight
                    lineHeight = 0
                }
                val childLeft = cursorX + params.leftMargin
                val childTop = cursorY + params.topMargin
                child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)
                cursorX += params.leftMargin + childWidth + params.rightMargin
                lineHeight = lineHeight.coerceAtLeast(params.topMargin + childHeight + params.bottomMargin)
            }
        }
    }

    private fun topBar(title: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(16))
            addView(TextView(context).apply {
                text = title
                textSize = 24f
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(profileSummaryButton())
            compactTitleTextView?.text = title
            expandedTitleView = this
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                updateCompactTitleBar(pageScrollView?.scrollY ?: 0)
            }
        }
    }

    private fun compactTitleBar(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(dp(18), dp(12) + systemStatusTopInset(), dp(18), dp(12))
            background = rounded(0xFFFFFFFF.toInt(), 0, 0xFFD8E2DA.toInt(), 1)
            elevation = dp(6).toFloat()
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            )
            addView(TextView(context).apply {
                textSize = 18f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                compactTitleTextView = this
            })
            addView(profileSummaryButton(compact = true))
            compactTitleBarView = this
        }
    }

    private fun profileSummaryButton(compact: Boolean = false, nameWidthDp: Int? = null): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            contentDescription = "내 밥닉네임 관리"
            setPadding(dp(4), dp(2), dp(8), dp(2))
            background = rounded(0xFFFFFBF5.toInt(), if (compact) 15 else 17, 0xFFE7B59D.toInt(), 1)
            setOnClickListener { showMyPage() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(profileAvatarButton(compact = compact))
            addView(View(context).apply {
                background = rounded(0xFFE0B49E.toInt(), 0)
                layoutParams = LinearLayout.LayoutParams(dp(1), if (compact) dp(20) else dp(22)).apply {
                    leftMargin = dp(3)
                    rightMargin = dp(6)
                }
            })
            addView(TextView(context).apply {
                text = selfName
                textSize = if (compact) 12f else 13f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    dp(nameWidthDp ?: if (compact) 62 else 82),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            })
        }
    }

    private fun profileAvatarButton(compact: Boolean = false): FrameLayout {
        val size = if (compact) dp(44) else dp(48)
        val frameSize = if (compact) dp(42) else dp(46)
        return FrameLayout(this).apply {
            isClickable = true
            isFocusable = true
            contentDescription = "내 밥닉네임 관리"
            layoutParams = LinearLayout.LayoutParams(size, size)
            setOnClickListener { showMyPage() }
            addView(FrameLayout(context).apply {
                setPadding(dp(2), dp(2), dp(2), dp(2))
                background = oval(0xFFFFF1E8.toInt(), 0xFF7CAD93.toInt(), 2)
                layoutParams = FrameLayout.LayoutParams(frameSize, frameSize, Gravity.CENTER)
                addView(AvatarTileView(selfAvatarId, contentInsetDp = 0, clipAsCircle = true).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                    )
                })
            })
        }
    }

    private fun updateCompactTitleBar(scrollY: Int) {
        val triggerY = expandedTitleView?.top
            ?: page.getChildAt(0)?.bottom
            ?: 0
        val shouldShow = triggerY > 0 && scrollY >= triggerY
        stickyHomeTopCardView?.let { stickyCard ->
            stickyCard.visibility = if (shouldShow) View.VISIBLE else View.GONE
            compactTitleBarView?.visibility = View.GONE
            return
        }
        compactTitleBarView?.visibility = if (shouldShow) View.VISIBLE else View.GONE
    }

    private fun bottomMenu(selected: String): LinearLayout {
        val buttons = listOf(
            menuItem("홈", NavigationIcon.HOME, selected == "홈") { showHome() },
            menuItem("밥판", NavigationIcon.CREATE, selected == "밥판") { showCompose() },
            menuItem("결과", NavigationIcon.RESULTS, selected == "결과") { showHistory() },
            menuItem("설정", NavigationIcon.SETTINGS, selected == "설정") { showSettings() }
        )
        return LinearLayout(this).apply {
            val sidePadding = dp(12)
            val topPadding = dp(5)
            val baseBottomPadding = dp(8)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(sidePadding, topPadding, sidePadding, baseBottomPadding + systemNavigationBottomInset())
            background = rounded(0xFFFFFFFF.toInt(), 0, 0xFFD8E2DA.toInt(), 1)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, dp(67), 1f).apply {
                    rightMargin = dp(6)
                }
                buttons.forEach { button ->
                    button.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    addView(button)
                }
            })
            addView(topConnectionBadge())
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val navigationBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                view.setPadding(sidePadding, topPadding, sidePadding, baseBottomPadding + navigationBottom)
                insets
            }
            post { ViewCompat.requestApplyInsets(this) }
        }
    }

    private fun menuItem(label: String, icon: NavigationIcon, selected: Boolean, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(View(context).apply {
                background = rounded(if (selected) 0xFFD73B24.toInt() else 0x00000000, 2)
                layoutParams = LinearLayout.LayoutParams(dp(34), dp(3)).apply {
                    bottomMargin = dp(5)
                }
            })
            addView(NavigationThumbnailView(icon, selected).apply {
                layoutParams = LinearLayout.LayoutParams(dp(34), dp(32)).apply {
                    bottomMargin = dp(3)
                }
            })
            addView(TextView(context).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(if (selected) 0xFFD73B24.toInt() else 0xFF617168.toInt())
            })
            setOnClickListener { onClick() }
            if (label == "설정") {
                onboardingSettingsTarget = this
            }
        }
    }

    private fun resultIconButton(label: String, icon: ResultAction, tint: Int, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            background = rounded(0xFFFFFFFF.toInt(), 16, 0xFFD8E2DA.toInt())
            isClickable = true
            isFocusable = true
            contentDescription = label
            addView(ResultActionIconView(icon, tint).apply {
                layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                    rightMargin = dp(9)
                }
            })
            addView(TextView(context).apply {
                text = label
                textSize = 14f
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(0xFF23362D.toInt())
            })
            setOnClickListener { onClick() }
        }
    }

    private fun topConnectionBadge(): FrameLayout {
        return FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(62), dp(54))
            clipChildren = false
            setOnClickListener { showConnectionPopup() }
            addView(View(context).apply {
                alpha = 0f
                background = connectionPulseBackground()
                layoutParams = FrameLayout.LayoutParams(dp(46), dp(46), Gravity.CENTER)
                topConnectionBadgePulseView = this
            })
            addView(FrameLayout(context).apply {
                background = rounded(0xFFFFFEFA.toInt(), 22, connectionBadgeStrokeColor(), 2)
                elevation = dp(2).toFloat()
                layoutParams = FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER)
                contentDescription = connectionStateLabel()
                addView(ConnectionSignalView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    topConnectionSignalView = this
                })
                topConnectionBadgeContainerView = this
            })
        }
    }

    private fun showConnectionPopup() {
        lateinit var dialog: AlertDialog
        val content = connectionPopupContent { dialog.dismiss() }
        dialog = AlertDialog.Builder(this)
            .setView(content)
            .create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    private fun connectionPopupContent(close: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(17), dp(18), dp(16))
            background = rounded(0xFFFFFEFA.toInt(), 20, connectionBadgeStrokeColor(), 2)
            addView(connectionPopupHeader())
            addView(connectionPopupBody())
            addView(connectionPopupActionRow(close))
        }
    }

    private fun connectionPopupHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(FrameLayout(context).apply {
                background = rounded(0xFFFFFFFF.toInt(), 22, connectionBadgeStrokeColor(), 2)
                layoutParams = LinearLayout.LayoutParams(dp(42), dp(42)).apply {
                    rightMargin = dp(12)
                }
                addView(ConnectionSignalView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                })
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply {
                    text = connectionStateLabel()
                    textSize = 20f
                    setTextColor(0xFF10251D.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = connectionPopupSummary()
                    textSize = 13f
                    setTextColor(0xFF526158.toInt())
                    setPadding(0, dp(3), 0, 0)
                })
            })
        }
    }

    private fun connectionPopupBody(): LinearLayout {
        val peers = nearby.connectedPeerNames()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(14), 0, dp(8))
            addView(TextView(context).apply {
                text = connectionPopupDetail()
                textSize = 14f
                setTextColor(0xFF263B31.toInt())
                setLineSpacing(dp(2).toFloat(), 1f)
            })
            addView(TextView(context).apply {
                text = buildList {
                    add("• $selfName (나)")
                    peers.forEach { name -> add("• $name") }
                    if (peers.isEmpty()) {
                        add("• 연결된 밥친구 없음")
                    }
                }.joinToString("\n")
                textSize = 13f
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(10), 0, 0)
            })
        }
    }

    private fun connectionPopupActionRow(close: () -> Unit): LinearLayout {
        val actions = connectionPopupActions(close)
        return compactButtonRow(*actions.toTypedArray()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun connectionPopupActions(close: () -> Unit): List<Button> {
        return when {
            !autoConnectEnabled -> listOf(
                compactButton("자동 연결 켜기", BUTTON_PRIMARY) {
                    close()
                    setAutoConnectEnabled(true)
                },
                compactButton("닫기", BUTTON_OUTLINE) { close() }
            )
            !hasNearbyPermissions() -> listOf(
                compactButton("권한 허용", BUTTON_PRIMARY) {
                    close()
                    requestNearbyPermissions()
                },
                compactButton("앱 설정", BUTTON_OUTLINE) {
                    close()
                    openAppSettings()
                }
            )
            disabledConnectionRequirements().isNotEmpty() -> listOf(
                compactButton("기기 설정", BUTTON_PRIMARY) {
                    close()
                    openAppSettings()
                },
                compactButton("닫기", BUTTON_OUTLINE) { close() }
            )
            displayConnectionCount() == 0 -> listOf(
                compactButton("닫기", BUTTON_OUTLINE) { close() }
            )
            else -> listOf(
                compactButton("닫기", BUTTON_OUTLINE) { close() }
            )
        }
    }

    private fun connectionPopupSummary(): String {
        return when {
            !autoConnectEnabled -> "회색 · 자동 연결 꺼짐"
            !hasNearbyPermissions() -> "빨강 · 권한 조치 필요"
            disabledConnectionRequirements().isNotEmpty() -> "노랑 · 기기 설정 확인 필요"
            displayConnectionCount() == 0 -> "노랑 · 밥친구 대기 중"
            else -> "초록 · 밥친구 연결됨"
        }
    }

    private fun connectionPopupDetail(): String {
        return when {
            !autoConnectEnabled -> "자동 연결을 켜면 앱이 주변 밥친구를 다시 찾습니다."
            !hasNearbyPermissions() -> connectionFixText() + "\n위치 권한은 위치 추적이 아니라 Nearby 연결에만 사용됩니다."
            disabledConnectionRequirements().isNotEmpty() -> {
                val missing = disabledConnectionRequirements().joinToString(" · ")
                "$missing 상태를 켜면 주변 탐색이 안정적으로 이어집니다."
            }
            displayConnectionCount() == 0 -> "아직 들어온 밥친구가 없습니다. 주변 밥친구가 들어오면 자동으로 연결 상태가 바뀝니다."
            else -> "연결 상태가 정상입니다."
        }
    }

    private fun breadcrumb(vararg items: String): TextView {
        return TextView(this).apply {
            text = items.joinToString(" > ")
            textSize = 13f
            setTextColor(0xFF647268.toInt())
            setPadding(dp(2), 0, 0, dp(12))
            layoutParams = blockParams()
        }
    }

    private fun actionCard(title: String, subtitle: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, dp(16), 0)
            background = groupedCardBackground()
            setOnClickListener { onClick() }
            layoutParams = blockParams()
            addView(View(context).apply {
                background = rounded(0xFFD73B24.toInt(), 16)
                layoutParams = LinearLayout.LayoutParams(dp(6), ViewGroup.LayoutParams.MATCH_PARENT)
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(12), dp(16))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply {
                    text = title
                    textSize = 18f
                    setTextColor(0xFF10251D.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = subtitle
                    textSize = 14f
                    setTextColor(0xFF526158.toInt())
                    setPadding(0, dp(5), 0, 0)
                })
            })
            addView(TextView(context).apply {
                text = "›"
                textSize = 28f
                setTextColor(0xFF8AA093.toInt())
            })
        }
    }

    private fun resultActionCard(title: String, timeText: String, result: SharedResult, onClick: () -> Unit): FrameLayout {
        return FrameLayout(this).apply {
            background = rounded(0xFFFFFFFF.toInt(), 16)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(112)
            ).apply {
                bottomMargin = dp(12)
            }
            addView(resultCardAvatarPane(resolvedAvatarId(result.proposerId, result.proposerAvatarId)))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(78), dp(12), dp(14), dp(12))
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL
                )
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        rightMargin = dp(10)
                    }
                    addView(TextView(context).apply {
                        text = timeText
                        textSize = 12f
                        setTextColor(0xFF647268.toInt())
                    })
                    addView(TextView(context).apply {
                        text = title
                        textSize = 16f
                        setTextColor(0xFF10251D.toInt())
                        setTypeface(typeface, Typeface.BOLD)
                        maxLines = 2
                        ellipsize = TextUtils.TruncateAt.END
                        setPadding(0, dp(4), 0, 0)
                    })
                    addView(TextView(context).apply {
                        text = "후보 ${result.options.size}개 · ${decisionMenuHeadline(result)}"
                        textSize = 12f
                        setTextColor(0xFF526158.toInt())
                        maxLines = 1
                        ellipsize = TextUtils.TruncateAt.END
                        setPadding(0, dp(3), 0, 0)
                    })
                })
                addView(TextView(context).apply {
                    text = "다시 열기"
                    textSize = 12f
                    setTextColor(0xFFFFFFFF.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    isClickable = true
                    isFocusable = true
                    setPadding(dp(10), dp(7), dp(10), dp(7))
                    minHeight = dp(38)
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                    background = rounded(0xFFD73B24.toInt(), 16)
                    setOnClickListener {
                        showCompose(resultAsTemplate(result, "draft-history-${System.currentTimeMillis()}"))
                    }
                })
            })
            addView(View(context).apply {
                background = rounded(0x00FFFFFF, 16, 0xBFD73B24.toInt(), 2)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            })
        }
    }

    private fun resultCardAvatarPane(avatarId: Int): FrameLayout {
        return FrameLayout(this).apply {
            clipChildren = true
            clipToPadding = true
            background = rounded(0xFFFFE2D2.toInt(), 16)
            layoutParams = FrameLayout.LayoutParams(
                dp(62),
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.LEFT
            )
            addView(AvatarTileView(
                avatarId = avatarId,
                contentInsetDp = 0,
                cornerRadiusDp = 16,
                sourceInsetRatio = 0f,
                transparentBackground = true
            ).apply {
                layoutParams = FrameLayout.LayoutParams(
                    dp(88),
                    dp(88),
                    Gravity.CENTER_VERTICAL or Gravity.LEFT
                )
            })
            addView(View(context).apply {
                background = rounded(0x1AFFFFFF, 16)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            })
            addView(View(context).apply {
                background = rounded(0xFFD73B24.toInt(), 1)
                alpha = 0.74f
                layoutParams = FrameLayout.LayoutParams(
                    dp(2),
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.RIGHT
                )
            })
        }
    }

    private fun decisionShareCard(result: SharedResult, receipt: VoteReceipt?): LinearLayout {
        val hasVotes = resultHasVotes(result)
        val winningOptions = rankedResultOptions(result).filter { option ->
            (result.counts[option] ?: 0) == (result.counts.values.maxOrNull() ?: 0)
        }.filter { option -> (result.counts[option] ?: 0) > 0 }
        val winningText = if (hasVotes) {
            winningOptions.joinToString(", ")
        } else {
            "선택된 메뉴 없음"
        }
        val isWinningTie = winningOptions.size > 1
        val winningCount = winningOptions.maxOfOrNull { option -> result.counts[option] ?: 0 } ?: 0
        val total = result.counts.values.sum().coerceAtLeast(1)
        val winPercent = if (winningCount > 0) winningCount * 100 / total else 0
        val rarity = if (hasVotes) assignedCardRarity(result) else pendingCardRarity()
        val singleWinningMenu = singleWinningMenu(result)
        val winningCalorieText = singleWinningMenu?.let { calorieTextFor(it) }
        return ShinyCardFrameLayout(fixedFontScaleContext(), rarity).apply {
            setPadding(dp(7), dp(7), dp(7), dp(7))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(460)
            ).apply {
                bottomMargin = dp(12)
            }
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(11), dp(10), dp(11), dp(11))
                background = rounded(0xFFFFFBF5.toInt(), 11, rarity.strokeColor, 1)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = rounded(rarity.headerColor, 7, rarity.accentColor, 1)
                    setPadding(dp(10), dp(7), dp(10), dp(7))
                    addView(TextView(context).apply {
                        text = if (hasVotes) responseOrderStars(result, receipt) else "결정 보류"
                        fixedCardTextSize(16f)
                        setTextColor(0xFF4B261D.toInt())
                        setTypeface(typeface, Typeface.BOLD)
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    addView(rarityProgressTrack(rarity))
                })
                addView(FrameLayout(context).apply {
                    setPadding(dp(12), dp(18), dp(12), dp(18))
                    background = rounded(rarity.artColor, 10, rarity.strokeColor, 1)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(250)
                    ).apply {
                        topMargin = dp(9)
                    }
                    addView(ImageView(context).apply {
                        setImageResource(rarity.imageResId)
                        contentDescription = "${rarity.label} 밥크로스 카드 이미지"
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        alpha = 0.24f
                        if (rarity.imageTintColor != 0) {
                            setColorFilter(rarity.imageTintColor, PorterDuff.Mode.SRC_ATOP)
                        }
                        layoutParams = FrameLayout.LayoutParams(dp(206), dp(206), Gravity.CENTER)
                    })
                    hologramAlphaFor(rarity.key).takeIf { alpha -> alpha > 0 }?.let { alpha ->
                        addView(HologramOverlayView(rarity, alpha).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                dp(54),
                                dp(54),
                                Gravity.BOTTOM or Gravity.RIGHT
                            ).apply {
                                bottomMargin = dp(12)
                                rightMargin = dp(12)
                            }
                        })
                    }
                    addView(LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(12), 0, dp(12), 0)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        ).apply {
                            topMargin = dp(14)
                        }
                        addView(AvatarTileView(
                            resolvedAvatarId(result.proposerId, result.proposerAvatarId),
                            contentInsetDp = 0,
                            clipAsCircle = true
                        ).apply {
                            layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                                rightMargin = dp(8)
                            }
                        })
                        addView(TextView(context).apply {
                            text = result.question
                            fixedCardTextSize(13f)
                            setTextColor(0xFF6F5A4D.toInt())
                            gravity = Gravity.CENTER_VERTICAL
                            setTypeface(typeface, Typeface.BOLD)
                            maxLines = 2
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        })
                    })
                    addView(LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER
                        )
                        if (isWinningTie) {
                            addView(TextView(context).apply {
                                text = "공동 1등"
                                fixedCardTextSize(46f)
                                setTextColor(rarity.accentColor)
                                setTypeface(typeface, Typeface.BOLD)
                                gravity = Gravity.CENTER
                                includeFontPadding = false
                                layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            })
                            addView(TextView(context).apply {
                                text = winningText
                                fixedCardTextSize(21f)
                                setTextColor(0xFF6F5A4D.toInt())
                                setTypeface(typeface, Typeface.BOLD)
                                gravity = Gravity.CENTER
                                maxLines = 2
                                setPadding(dp(10), dp(8), dp(10), 0)
                                layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            })
                        } else {
                            addView(TextView(context).apply {
                                text = winningText
                                fixedCardTextSize(if (hasVotes) 56f else 39f)
                                setTextColor(rarity.accentColor)
                                setTypeface(typeface, Typeface.BOLD)
                                gravity = Gravity.CENTER
                                includeFontPadding = false
                                maxLines = 2
                                layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            })
                        }
                        winningCalorieText?.let { calorieText ->
                            addView(TextView(context).apply {
                                text = calorieText
                                fixedCardTextSize(14f)
                                setTextColor(0xFF6F5A4D.toInt())
                                setTypeface(typeface, Typeface.BOLD)
                                gravity = Gravity.CENTER
                                setPadding(dp(12), dp(5), dp(12), dp(5))
                                background = rounded(0xCCFFFFFF.toInt(), 15, 0x66E7B59D, 1)
                                layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    topMargin = dp(10)
                                }
                            })
                        }
                    })
                    if (!hasVotes) {
                        addView(TextView(context).apply {
                            text = "아직 아무도 고르지 않았어요"
                            fixedCardTextSize(14f)
                            setTextColor(0xFF6F5A4D.toInt())
                            gravity = Gravity.CENTER
                            setTypeface(typeface, Typeface.BOLD)
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                            ).apply {
                                bottomMargin = dp(22)
                            }
                        })
                    }
                })
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(9), 0, 0)
                    addView(cardStat("득표/참여", if (winningCount > 0) "${winningCount}/${result.participantCount}" else "0/${result.participantCount}").apply {
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            rightMargin = dp(8)
                        }
                    })
                    addView(cardStat("점유", "${winPercent}%").apply {
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            rightMargin = dp(8)
                        }
                    })
                })
                addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                })
                addView(cardBarcodeFloor(result, receipt))
            })
        }
    }

    private fun cardStat(label: String, value: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(7), dp(6), dp(7), dp(6))
            background = rounded(0xFFFFFFFF.toInt(), 9, 0xFFE7B59D.toInt(), 1)
            addView(TextView(context).apply {
                text = label
                fixedCardTextSize(10f)
                setTextColor(0xFF8B5C45.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = value
                fixedCardTextSize(16f)
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(1), 0, 0)
            })
        }
    }

    private fun TextView.fixedCardTextSize(sizeSp: Float) {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, sizeSp * resources.displayMetrics.density)
    }

    private fun fixedFontScaleContext(): Context {
        val configuration = Configuration(resources.configuration).apply {
            fontScale = 1f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                fontWeightAdjustment = 0
            }
        }
        return createConfigurationContext(configuration)
    }

    private fun rarityProgressTrack(activeRarity: CardRarity): LinearLayout {
        val steps = listOf(
            CARD_RARITY_COMMON to "보통",
            CARD_RARITY_UNCOMMON to "만족",
            CARD_RARITY_RARE to "희귀",
            CARD_RARITY_LEGENDARY to "전설"
        )
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(0x66FFFFFF, 16, 0x55FFFFFF, 1)
            setPadding(dp(3), dp(3), dp(3), dp(3))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(30)
            )
            steps.forEachIndexed { index, (key, label) ->
                val active = key == activeRarity.key
                addView(TextView(context).apply {
                    text = label
                    fixedCardTextSize(10f)
                    gravity = Gravity.CENTER
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(if (active) 0xFFFFFFFF.toInt() else 0xFF7D7D7D.toInt())
                    setPadding(dp(7), 0, dp(7), 0)
                    background = rounded(
                        if (active) activeRarity.accentColor else 0x00000000,
                        13
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ).apply {
                        if (index < steps.lastIndex) {
                            rightMargin = dp(1)
                        }
                    }
                })
            }
        }
    }

    private fun assignedCardRarity(result: SharedResult): CardRarity {
        val savedKey = store.loadResultCardRarity(result.pollId)
        if (savedKey != null) {
            return cardRarityForKey(savedKey)
        }
        val revealIndex = store.nextResultCardRevealIndex()
        val rarityKey = if (revealIndex < CARD_RARITY_INTRO_SEQUENCE.size) {
            CARD_RARITY_INTRO_SEQUENCE[revealIndex]
        } else {
            weightedCardRarityKey(result, revealIndex)
        }
        store.saveResultCardRarity(result.pollId, rarityKey)
        return cardRarityForKey(rarityKey)
    }

    private fun responseOrderStars(result: SharedResult, receipt: VoteReceipt?): String {
        val responseOrder = result.participantIds.takeIf { it.isNotEmpty() } ?: return rarityStars(CARD_RARITY_COMMON)
        val preferredParticipantId = receipt
            ?.takeIf { it.pollId == result.pollId }
            ?.voterId
            ?.takeIf { it in responseOrder }
            ?: userId.takeIf { it in responseOrder }
            ?: responseOrder.firstOrNull()
            ?: return rarityStars(CARD_RARITY_COMMON)
        val starKey = when (responseOrder.indexOf(preferredParticipantId)) {
            0 -> CARD_RARITY_LEGENDARY
            1 -> CARD_RARITY_RARE
            2 -> CARD_RARITY_UNCOMMON
            else -> CARD_RARITY_COMMON
        }
        return rarityStars(starKey)
    }

    private fun pendingCardRarity(): CardRarity {
        return CardRarity(
            key = CARD_RARITY_PENDING,
            label = "보류",
            frameColor = 0xFF5A4B43.toInt(),
            shineColor = 0xFFE0B49E.toInt(),
            strokeColor = 0xFFB98A72.toInt(),
            headerColor = 0xFFFFE6D8.toInt(),
            artColor = 0xFFFFF4ED.toInt(),
            accentColor = 0xFF8B5C45.toInt(),
            imageResId = R.drawable.card_art_common,
            imageTintColor = 0x55FFFFFF
        )
    }

    private fun hologramAlphaFor(rarityKey: String): Int {
        return when (rarityKey) {
            CARD_RARITY_LEGENDARY -> 92
            CARD_RARITY_RARE -> 72
            else -> 0
        }
    }

    private fun rarityStars(rarityKey: String): String {
        val filled = when (rarityKey) {
            CARD_RARITY_LEGENDARY -> 4
            CARD_RARITY_RARE -> 3
            CARD_RARITY_UNCOMMON -> 2
            else -> 1
        }
        return buildString {
            repeat(filled) { append("★") }
            repeat(4 - filled) { append("☆") }
        }
    }

    private fun weightedCardRarityKey(result: SharedResult, revealIndex: Int): String {
        val score = abs(hash("${result.pollId}:$revealIndex").take(8).toLong(16) % CARD_RARITY_WEIGHT_TOTAL).toInt()
        var cursor = 0
        CARD_RARITY_WEIGHTS.forEach { (key, weight) ->
            cursor += weight
            if (score < cursor) return key
        }
        return CARD_RARITY_COMMON
    }

    private fun cardRarityForKey(key: String): CardRarity {
        return when (key) {
            CARD_RARITY_LEGENDARY -> CardRarity(
                key = CARD_RARITY_LEGENDARY,
                label = "전설",
                frameColor = 0xFF3A1A52.toInt(),
                shineColor = 0xFFB76BFF.toInt(),
                strokeColor = 0xFFFFC857.toInt(),
                headerColor = 0xFFFFE7A3.toInt(),
                artColor = 0xFFFFF3C4.toInt(),
                accentColor = 0xFF8B3FD1.toInt(),
                imageResId = R.drawable.card_art_legendary,
                imageTintColor = 0
            )
            CARD_RARITY_RARE -> CardRarity(
                key = CARD_RARITY_RARE,
                label = "희귀",
                frameColor = 0xFF173B5A.toInt(),
                shineColor = 0xFF5DB7FF.toInt(),
                strokeColor = 0xFF69C4FF.toInt(),
                headerColor = 0xFFE3F4FF.toInt(),
                artColor = 0xFFEAF7FF.toInt(),
                accentColor = 0xFF1976B9.toInt(),
                imageResId = R.drawable.card_art_rare,
                imageTintColor = 0
            )
            CARD_RARITY_UNCOMMON -> CardRarity(
                key = CARD_RARITY_UNCOMMON,
                label = "만족",
                frameColor = 0xFF1F4D37.toInt(),
                shineColor = 0xFF62C782.toInt(),
                strokeColor = 0xFF89D6A3.toInt(),
                headerColor = 0xFFE8F7EC.toInt(),
                artColor = 0xFFF0FAF2.toInt(),
                accentColor = 0xFF2F8A57.toInt(),
                imageResId = R.drawable.card_art_uncommon,
                imageTintColor = 0
            )
            else -> CardRarity(
                key = CARD_RARITY_COMMON,
                label = "보통",
                frameColor = 0xFF4B261D.toInt(),
                shineColor = 0xFFB86B3E.toInt(),
                strokeColor = 0xFFD9A441.toInt(),
                headerColor = 0xFFFFE2D2.toInt(),
                artColor = 0xFFFFF1E8.toInt(),
                accentColor = 0xFFD73B24.toInt(),
                imageResId = R.drawable.card_art_common,
                imageTintColor = 0
            )
        }
    }

    private fun cardVoteSummary(result: SharedResult): LinearLayout {
        val rankedOptions = rankedResultOptions(result)
        val rawTotal = result.counts.values.sum()
        val total = rawTotal.coerceAtLeast(1)
        val topCount = result.counts.values.maxOrNull() ?: 0
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(9), dp(10), dp(9))
            background = rounded(0xFFFFFFFF.toInt(), 10, 0xFFE7B59D.toInt(), 1)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(9)
            }
            addView(TextView(context).apply {
                text = "투표 판세"
                textSize = 11f
                setTextColor(0xFF8B5C45.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            if (rawTotal <= 0) {
                addView(TextView(context).apply {
                    text = "참여 0명 · 득표 0표"
                    textSize = 17f
                    setTextColor(0xFF10251D.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, dp(7), 0, 0)
                })
                addView(TextView(context).apply {
                    text = "이번 밥판은 메뉴 결정 없이 보류되었습니다."
                    textSize = 13f
                    setTextColor(0xFF526158.toInt())
                    setPadding(0, dp(4), 0, 0)
                })
                return@apply
            }
            rankedOptions.forEachIndexed { index, option ->
                val count = result.counts[option] ?: 0
                val percent = count * 100 / total
                val isWinner = topCount > 0 && count == topCount
                addView(compactVoteLine(index + 1, option, count, percent, isWinner))
            }
        }
    }

    private fun compactVoteLine(rank: Int, option: String, count: Int, percent: Int, isWinner: Boolean): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(7), 0, 0)
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(context).apply {
                    text = "$rank"
                    gravity = Gravity.CENTER
                    textSize = 10f
                    setTextColor(if (isWinner) 0xFFFFFFFF.toInt() else 0xFF8B5C45.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                    background = rounded(if (isWinner) 0xFFD73B24.toInt() else 0xFFFFF1E8.toInt(), 10, 0xFFE7B59D.toInt(), 1)
                    layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                        rightMargin = dp(8)
                    }
                })
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    addView(TextView(context).apply {
                        text = option
                        textSize = 12f
                        setTextColor(0xFF10251D.toInt())
                        setTypeface(typeface, Typeface.BOLD)
                    })
                    calorieTextFor(option)?.let { calorieText ->
                        addView(TextView(context).apply {
                            text = calorieText
                            textSize = 10f
                            setTextColor(0xFF8B5C45.toInt())
                            setPadding(0, dp(2), 0, 0)
                        })
                    }
                })
                addView(TextView(context).apply {
                    text = "${count}표 · ${percent}%"
                    textSize = 11f
                    setTextColor(if (isWinner) 0xFFD73B24.toInt() else 0xFF526158.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                })
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                background = rounded(0xFFF3E5D0.toInt(), 5)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(6)
                ).apply {
                    topMargin = dp(4)
                }
                addView(View(context).apply {
                    background = rounded(if (isWinner) 0xFFD73B24.toInt() else 0xFF3D8B67.toInt(), 5)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, percent.coerceIn(0, 100).toFloat())
                })
                addView(FrameLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (100 - percent.coerceIn(0, 100)).toFloat())
                })
            })
        }
    }

    private fun cardBarcodeFloor(result: SharedResult, receipt: VoteReceipt?): LinearLayout {
        val barcodeValue = buildString {
            append("result:")
            append(result.resultHash)
            append(":")
            append(result.isHashValid())
            receipt?.let {
                append("|receipt:")
                append(it.voteHash)
            }
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            background = rounded(0xFFFFF8E8.toInt(), 8, 0xFFE7B59D.toInt(), 1)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(BarcodeView(barcodeValue).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(60)
                )
            })
        }
    }

    private fun showDecisionSharePreview(result: SharedResult) {
        val bitmap = createDecisionShareBitmap(result)
        val previewImage = ImageView(this).apply {
            setImageBitmap(bitmap)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = rounded(0xFFFFF8E8.toInt(), 18, 0xFFE7B59D.toInt(), 1)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(420)
            )
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(6))
            addView(previewImage)
            addView(TextView(context).apply {
                text = buildDecisionShareText(result)
                textSize = 13f
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(12), 0, dp(4))
            })
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("공유 카드 미리보기")
            .setView(content)
            .setNegativeButton("닫기", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                shareDecisionCard(result, bitmap)
                dialog.dismiss()
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
                copyDecisionShareText(result)
            }
        }
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "이미지 공유") { _, _ -> }
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "문구만 복사") { _, _ -> }
        dialog.show()
    }

    private fun shareDecisionCard(result: SharedResult, bitmap: Bitmap = createDecisionShareBitmap(result)) {
        val uri = decisionShareImageUri(result, bitmap) ?: run {
            Toast.makeText(this, "공유 이미지를 만들지 못했습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, buildDecisionShareText(result))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "밥결정 카드 공유"))
    }

    private fun copyDecisionCardImage(result: SharedResult, cardView: View) {
        val uri = decisionCardViewImageUri(result, cardView) ?: run {
            Toast.makeText(this, "카드 이미지를 복사하지 못했습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newUri(contentResolver, "밥결정 카드", uri))
        Toast.makeText(this, "결정 카드 이미지를 복사했습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun decisionCardViewImageUri(result: SharedResult, cardView: View): Uri? {
        val width = cardView.width.takeIf { it > 0 } ?: return null
        val height = cardView.height.takeIf { it > 0 } ?: return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.TRANSPARENT)
            cardView.draw(this)
        }
        return decisionShareImageUri(result, bitmap, nameSuffix = "screen")
    }

    private fun decisionShareImageUri(
        result: SharedResult,
        bitmap: Bitmap = createDecisionShareBitmap(result),
        nameSuffix: String = "share"
    ): Uri? {
        val shareDir = File(cacheDir, "shared_results").apply { mkdirs() }
        val safePollId = result.pollId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(shareDir, "bab-cross-$safePollId-$nameSuffix.png")
        cleanupSharedResultCache(shareDir, keepFile = file)
        return runCatching {
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        }.getOrNull()
    }

    private fun cleanupSharedResultCache(shareDir: File, keepFile: File) {
        val cachedCards = shareDir.listFiles { file ->
            file.isFile && file.name.startsWith("bab-cross-") && file.name.endsWith(".png") && file != keepFile
        }.orEmpty()
        cachedCards
            .sortedByDescending { file -> file.lastModified() }
            .drop(MAX_SHARED_RESULT_CACHE_FILES - 1)
            .forEach { file -> runCatching { file.delete() } }
    }

    private fun buildDecisionShareText(result: SharedResult): String {
        return SharedResultInsights.buildShareText(result, userId, SHARE_MAX_VISIBLE_CANDIDATES)
    }

    private fun shareDecisionText(result: SharedResult) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, buildDecisionShareText(result))
        }
        startActivity(Intent.createChooser(intent, "밥결정 보내기"))
    }

    private fun copyDecisionShareText(result: SharedResult) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("오늘의 밥결정", buildDecisionShareText(result)))
        Toast.makeText(this, "공유 문구를 복사했습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun decisionMenuHeadline(result: SharedResult): String {
        return SharedResultInsights.decisionMenuHeadline(result)
    }

    private fun shareCandidateSummary(result: SharedResult): String {
        return SharedResultInsights.shareCandidateSummary(result, SHARE_MAX_VISIBLE_CANDIDATES)
    }

    private fun decisionReasonText(result: SharedResult): String {
        return SharedResultInsights.decisionReasonText(result, userId)
    }

    private fun createDecisionShareBitmap(result: SharedResult): Bitmap {
        val width = SHARE_IMAGE_WIDTH
        val height = SHARE_IMAGE_HEIGHT
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(0xFFFFF8E8.toInt())
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                0xFFFFF4D8.toInt(),
                0xFFFFD6C3.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(
            RectF(SHARE_OUTER_MARGIN, SHARE_OUTER_MARGIN, width - SHARE_OUTER_MARGIN, height - SHARE_OUTER_MARGIN),
            54f,
            54f,
            backgroundPaint
        )
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFBF5.toInt() }
        val cardRect = RectF(SHARE_CARD_LEFT, SHARE_CARD_TOP, width - SHARE_CARD_LEFT, height - SHARE_CARD_BOTTOM)
        canvas.drawRoundRect(cardRect, 44f, 44f, cardPaint)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD73B24.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        canvas.drawRoundRect(cardRect, 44f, 44f, strokePaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8B5C45.toInt()
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF10251D.toInt()
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val menuPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD73B24.toInt()
            textSize = 118f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF526158.toInt()
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val invitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD73B24.toInt()
            textSize = 32f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("BAB-CROSS", SHARE_SAFE_LEFT, 218f, labelPaint)
        val proposerAvatar = transparentAvatarBitmap(resolvedAvatarId(result.proposerId, result.proposerAvatarId))
        val avatarRect = RectF(SHARE_SAFE_LEFT, 256f, SHARE_SAFE_LEFT + 88f, 344f)
        val avatarPath = Path().apply { addOval(avatarRect, Path.Direction.CW) }
        canvas.drawOval(avatarRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFE2D2.toInt() })
        val avatarSave = canvas.save()
        canvas.clipPath(avatarPath)
        canvas.drawBitmap(proposerAvatar, null, Rect(avatarRect.left.toInt(), avatarRect.top.toInt(), avatarRect.right.toInt(), avatarRect.bottom.toInt()), Paint(Paint.ANTI_ALIAS_FLAG))
        canvas.restoreToCount(avatarSave)
        canvas.drawOval(avatarRect, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD73B24.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
        })
        drawFittedTextBlock(canvas, result.question, titlePaint, SHARE_SAFE_LEFT + 112f, 304f, SHARE_SAFE_WIDTH - 112f, 2, 42f)

        val mascot = BitmapFactory.decodeResource(resources, R.drawable.bab_cross_launcher)
        val mascotRect = RectF(332f, 382f, 748f, 798f)
        canvas.drawRoundRect(mascotRect, 46f, 46f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFE2D2.toInt() })
        canvas.drawBitmap(mascot, null, Rect(352, 402, 728, 778), Paint(Paint.ANTI_ALIAS_FLAG))

        val menu = decisionMenuHeadline(result)
        val winningCount = rankedResultOptions(result).firstOrNull()?.let { result.counts[it] ?: 0 } ?: 0
        val totalVotes = result.counts.values.sum()
        val menuTop = 910f
        drawFittedTextBlockCentered(canvas, menu, menuPaint, width / 2f, menuTop, width - SHARE_SAFE_LEFT * 2f, 2, 76f)

        val statText = if (totalVotes > 0) {
            "${winningCount}표 · 참여 ${result.participantCount}명"
        } else {
            "결정 보류 · 참여 ${result.participantCount}명"
        }
        canvas.drawText(statText, SHARE_SAFE_LEFT, 1116f, bodyPaint)
        drawFittedTextBlock(canvas, "후보: ${shareCandidateSummary(result)}", bodyPaint, SHARE_SAFE_LEFT, 1180f, SHARE_SAFE_WIDTH, 2, 27f)
        drawFittedTextBlock(canvas, "이유: ${decisionReasonText(result)}", bodyPaint, SHARE_SAFE_LEFT, 1266f, SHARE_SAFE_WIDTH, 2, 27f)
        canvas.drawText("다음 밥판은 같이 고르기", width / 2f, 1362f, invitePaint)
        return bitmap
    }

    private fun shareCardComment(result: SharedResult): String {
        val menu = winningMenuText(result)
        return if (resultHasVotes(result)) {
            "$menu 쪽으로 밥심이 모였습니다."
        } else {
            "오늘은 더 고민해도 되는 밥판입니다."
        }
    }

    private fun drawTextBlock(
        canvas: Canvas,
        text: String,
        paint: Paint,
        x: Float,
        y: Float,
        maxWidth: Float,
        lineHeight: Float,
        maxLines: Int
    ) {
        var rest = text
        var lineY = y
        repeat(maxLines) { lineIndex ->
            if (rest.isBlank()) return
            val count = paint.breakText(rest, true, maxWidth, null).coerceAtLeast(1)
            val rawLine = rest.take(count)
            val trimmedLine = if (lineIndex == maxLines - 1 && count < rest.length) {
                rawLine.trimEnd().dropLast(1).trimEnd() + "..."
            } else {
                rawLine.trimEnd()
            }
            canvas.drawText(trimmedLine, x, lineY, paint)
            rest = rest.drop(count).trimStart()
            lineY += lineHeight
        }
    }

    private fun drawFittedTextBlock(
        canvas: Canvas,
        text: String,
        paint: Paint,
        x: Float,
        y: Float,
        maxWidth: Float,
        maxLines: Int,
        minTextSize: Float
    ) {
        val fittedPaint = fittedPaintFor(text, paint, maxWidth, maxLines, minTextSize)
        drawTextBlock(canvas, text, fittedPaint, x, y, maxWidth, fittedPaint.textSize * 1.28f, maxLines)
    }

    private fun drawTextBlockCentered(
        canvas: Canvas,
        text: String,
        paint: Paint,
        centerX: Float,
        y: Float,
        maxWidth: Float,
        lineHeight: Float,
        maxLines: Int
    ) {
        var rest = text
        var lineY = y
        repeat(maxLines) { lineIndex ->
            if (rest.isBlank()) return
            val count = paint.breakText(rest, true, maxWidth, null).coerceAtLeast(1)
            val rawLine = rest.take(count)
            val trimmedLine = if (lineIndex == maxLines - 1 && count < rest.length) {
                rawLine.trimEnd().dropLast(1).trimEnd() + "..."
            } else {
                rawLine.trimEnd()
            }
            canvas.drawText(trimmedLine, centerX, lineY, paint)
            rest = rest.drop(count).trimStart()
            lineY += lineHeight
        }
    }

    private fun drawFittedTextBlockCentered(
        canvas: Canvas,
        text: String,
        paint: Paint,
        centerX: Float,
        y: Float,
        maxWidth: Float,
        maxLines: Int,
        minTextSize: Float
    ) {
        val fittedPaint = fittedPaintFor(text, paint, maxWidth, maxLines, minTextSize)
        drawTextBlockCentered(canvas, text, fittedPaint, centerX, y, maxWidth, fittedPaint.textSize * 1.18f, maxLines)
    }

    private fun fittedPaintFor(text: String, paint: Paint, maxWidth: Float, maxLines: Int, minTextSize: Float): Paint {
        val fittedPaint = Paint(paint)
        while (fittedPaint.textSize > minTextSize && wrappedLineCount(text, fittedPaint, maxWidth) > maxLines) {
            fittedPaint.textSize -= 2f
        }
        return fittedPaint
    }

    private fun wrappedLineCount(text: String, paint: Paint, maxWidth: Float): Int {
        var rest = text.trim()
        var lines = 0
        while (rest.isNotBlank() && lines < SHARE_TEXT_WRAP_GUARD_LINES) {
            val count = paint.breakText(rest, true, maxWidth, null).coerceAtLeast(1)
            rest = rest.drop(count).trimStart()
            lines++
        }
        return lines
    }

    private fun pollActionCard(title: String, status: String, poll: NearbyPoll, onClick: () -> Unit): FrameLayout {
        return FrameLayout(this).apply {
            background = rounded(0xFFFFFFFF.toInt(), 16)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(78)
            ).apply {
                bottomMargin = dp(12)
            }
            addView(resultCardAvatarPane(resolvedAvatarId(poll.proposerId, poll.proposerAvatarId)))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(78), dp(10), dp(14), dp(10))
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL
                )
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        rightMargin = dp(10)
                    }
                    addView(TextView(context).apply {
                        text = status
                        textSize = 12f
                        setTextColor(0xFF647268.toInt())
                    })
                    addView(TextView(context).apply {
                        text = title
                        textSize = 16f
                        setTextColor(0xFF10251D.toInt())
                        setTypeface(typeface, Typeface.BOLD)
                        setPadding(0, dp(4), 0, 0)
                        maxLines = 2
                    })
                })
                addView(CountdownRingView(poll, compact = true).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(54), dp(54)).apply {
                        rightMargin = dp(10)
                    }
                })
                addView(TextView(context).apply {
                    text = "›"
                    textSize = 28f
                    setTextColor(0xFF8AA093.toInt())
                })
            })
            addView(View(context).apply {
                background = rounded(0x00FFFFFF, 16, 0xBFD73B24.toInt(), 2)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            })
        }
    }

    private fun infoCard(title: String, value: String, caption: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(0xFFFFFFFF.toInt(), 14)
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = title
                textSize = 13f
                setTextColor(0xFF647268.toInt())
            })
            addView(TextView(context).apply {
                text = value
                textSize = 21f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(4), 0, dp(4))
            })
            addView(TextView(context).apply {
                text = caption
                textSize = 14f
                setTextColor(0xFF526158.toInt())
            })
        }
    }

    private fun identityCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(0xFFFFFFFF.toInt(), 14)
            layoutParams = blockParams()
            setOnClickListener { showMyPage() }
            addView(FrameLayout(context).apply {
                background = oval(0xFFFFF1E8.toInt(), 0xFF7CAD93.toInt(), 2)
                setPadding(dp(3), dp(3), dp(3), dp(3))
                layoutParams = LinearLayout.LayoutParams(dp(AVATAR_CARD_SIZE), dp(AVATAR_CARD_SIZE)).apply {
                    rightMargin = dp(14)
                }
                addView(AvatarTileView(selfAvatarId, contentInsetDp = 0, clipAsCircle = true).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                    )
                })
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply {
                    text = "내 밥닉네임"
                    textSize = 13f
                    setTextColor(0xFF647268.toInt())
                })
                addView(TextView(context).apply {
                    text = selfName
                    textSize = 20f
                    setTextColor(0xFF10251D.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, dp(3), 0, dp(3))
                })
                addView(TextView(context).apply {
                    text = "결과와 밥친구 목록에 표시됩니다."
                    textSize = 13f
                    setTextColor(0xFF526158.toInt())
                })
            })
            addView(TextView(context).apply {
                text = "›"
                textSize = 28f
                setTextColor(0xFF8AA093.toInt())
            })
        }
    }

    private fun experienceCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(18), dp(12))
            background = rounded(0xFFFFF0D9.toInt(), 16, 0xFFE7B59D.toInt())
            isClickable = true
            isFocusable = true
            setOnClickListener { showSimulationResult() }
            layoutParams = blockParams()
            addView(ImageView(context).apply {
                setImageResource(R.drawable.bab_cross_launcher)
                contentDescription = "밥판 체험하기"
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = rounded(0xFFFFFFFF.toInt(), 18, 0xFFE7B59D.toInt(), 2)
                clipToOutline = true
                layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply {
                    rightMargin = dp(14)
                }
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply {
                    text = "처음이시네요"
                    textSize = 20f
                    setTextColor(0xFF10251D.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = "밥판 체험을 먼저 시작해볼까요?"
                    textSize = 13f
                    setTextColor(0xFF526158.toInt())
                    setPadding(0, dp(5), 0, 0)
                })
            })
            addView(TextView(context).apply {
                text = "시작"
                textSize = 14f
                setTextColor(0xFFFFFFFF.toInt())
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(dp(13), dp(8), dp(13), dp(8))
                background = rounded(0xFFD73B24.toInt(), 18)
            })
            onboardingHomeDemoTarget = this
        }
    }

    private fun connectionReadinessCard(): LinearLayout {
        val isReady = autoConnectEnabled && hasNearbyPermissions() && disabledConnectionRequirements().isEmpty()
        val cardColor = if (isReady) 0xFFEAF6EF.toInt() else 0xFFFFF0D9.toInt()
        val strokeColor = if (isReady) 0xFF94C6A8.toInt() else 0xFFE7B59D.toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(cardColor, 16, strokeColor, 1)
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = "연결 준비 상태"
                textSize = 17f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = connectionReadinessText()
                textSize = 13f
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(6), 0, dp(10))
            })
            addView(readinessStatusRow(
                label = "Bluetooth",
                ready = isBluetoothEnabled() && hasBluetoothRuntimePermissions(),
                detail = if (isBluetoothEnabled()) "근처 기기 연결에 사용" else "꺼져 있어 밥친구를 찾기 어렵습니다."
            ))
            addView(readinessStatusRow(
                label = "Wi-Fi",
                ready = isWifiEnabled() && hasNearbyWifiPermission(),
                detail = if (isWifiEnabled()) "근처 기기 탐색에 사용" else "꺼져 있어 주변 탐색이 제한됩니다."
            ))
            addView(readinessStatusRow(
                label = "위치/Nearby 권한",
                ready = hasNearbyPermissions() && isLocationEnabled(),
                detail = if (hasNearbyPermissions() && isLocationEnabled()) {
                    "위치 추적이 아니라 근처 밥친구 연결용입니다."
                } else {
                    "권한 또는 위치 상태 확인이 필요합니다."
                }
            ))
            connectionPreparationButtonRow(compact = true)?.let { addView(it) }
        }
    }

    private fun outdoorConnectionChecklistCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(0xFFFFFFFF.toInt(), 16, 0xFFE0E7DD.toInt(), 1)
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = "밖에서 바로 옆 기기 찾기"
                textSize = 17f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = "두 기기 모두 앱을 켜고, Bluetooth · Wi-Fi · 위치를 켠 뒤 다시 찾아보세요. 인터넷 Wi-Fi에 접속하지 않아도 되지만 Wi-Fi 기능은 켜져 있어야 발견이 안정적입니다."
                textSize = 13f
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(6), 0, dp(10))
            })
            addView(readinessStatusRow(
                label = "내 기기",
                ready = hasNearbyPermissions() && disabledConnectionRequirements().isEmpty(),
                detail = connectionFixText()
            ))
            addView(readinessStatusRow(
                label = "상대 기기",
                ready = false,
                detail = "앱 실행, 권한 허용, Bluetooth/Wi-Fi/위치 켜짐 확인"
            ))
            addView(TextView(context).apply {
                text = "30초 동안 못 찾으면 앱이 주변 탐색을 자동으로 다시 시작합니다."
                textSize = 12f
                setTextColor(0xFFD73B24.toInt())
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(6), 0, 0)
            })
        }
    }

    private fun connectionReadinessText(): String {
        if (!autoConnectEnabled) {
            return "자동 연결이 꺼져 있어 앱 실행 시 주변 밥친구를 찾지 않습니다."
        }
        if (!hasNearbyPermissions() || disabledConnectionRequirements().isNotEmpty()) {
            return connectionFixText() + "\n위치 권한은 위치 추적이 아니라 Nearby 연결을 위해 필요합니다."
        }
        return if (connectedCount == 0) {
            "준비 완료 · 근처 밥친구가 앱을 켜면 자동으로 연결됩니다."
        } else {
            val peerNames = nearby.connectedPeerNames().takeIf { it.isNotEmpty() }?.joinToString(", ")
            "연결됨 · 상대 ${connectedCount}/${NearbyVoteConnectionManager.MAX_CONNECTIONS}명${peerNames?.let { " · $it" }.orEmpty()}"
        }
    }

    private fun trustSummaryCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(0xFFFFFFFF.toInt(), 16, 0xFFE0E7DD.toInt(), 1)
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = "권한과 데이터 안내"
                textSize = 17f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = "위치 기록은 남기지 않고, 밥판에 필요한 정보만 가까운 기기끼리 주고받습니다."
                textSize = 13f
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(5), 0, dp(10))
            })
            addView(trustSummaryRow("수집하지 않아요", "위치 기록 · 서버 저장 · 광고/분석 SDK 전송"))
            addView(trustSummaryRow("주변에 보내요", "밥닉/아바타 · 후보 · 선택 결과 또는 득표 요약"))
        }
    }

    private fun trustSummaryRow(title: String, detail: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(5), 0, dp(7))
            addView(TextView(context).apply {
                text = title
                textSize = 14f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = detail
                textSize = 12f
                setTextColor(0xFF647268.toInt())
                setPadding(0, dp(2), 0, 0)
            })
        }
    }

    private fun readinessStatusRow(label: String, ready: Boolean, detail: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(5), 0, dp(5))
            addView(TextView(context).apply {
                text = if (ready) "✓" else "!"
                textSize = 13f
                setTextColor(0xFFFFFFFF.toInt())
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = oval(if (ready) 0xFF245341.toInt() else 0xFFD73B24.toInt())
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                    rightMargin = dp(10)
                }
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply {
                    text = label
                    textSize = 14f
                    setTextColor(0xFF10251D.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = detail
                    textSize = 12f
                    setTextColor(0xFF647268.toInt())
                    setPadding(0, dp(2), 0, 0)
                })
            })
        }
    }

    private fun autoConnectSettingCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(14), dp(14))
            background = rounded(0xFFFFFFFF.toInt(), 16, 0xFFE0E7DD.toInt(), 1)
            isClickable = true
            isFocusable = true
            layoutParams = blockParams()
            setOnClickListener {
                setAutoConnectEnabled(!autoConnectEnabled)
                showSettings()
            }
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply {
                    text = "자동 연결"
                    textSize = 17f
                    setTextColor(0xFF10251D.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = if (autoConnectEnabled) {
                        "앱 실행 시 근처 밥친구를 자동으로 찾습니다."
                    } else {
                        "꺼져 있으면 밥신호를 받을 수 없습니다."
                    }
                    textSize = 13f
                    setTextColor(0xFF526158.toInt())
                    setPadding(0, dp(5), dp(12), 0)
                })
            })
            addView(CheckBox(context).apply {
                isChecked = autoConnectEnabled
                isClickable = false
                contentDescription = if (autoConnectEnabled) "자동 연결 켜짐" else "자동 연결 꺼짐"
            })
        }
    }

    private fun pollDefaultsSettingCard(): LinearLayout {
        val defaults = store.loadPollDefaults()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(0xFFFFFFFF.toInt(), 16, 0xFFE0E7DD.toInt(), 1)
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = "밥판 기본값"
                textSize = 17f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = "새 밥판을 열 때 반복해서 고르는 값을 미리 정합니다."
                textSize = 13f
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(5), 0, dp(10))
            })
            addView(defaultSettingRow(
                title = "기본 제한시간",
                value = formatDurationText(defaults.durationSeconds),
                onClick = { showDefaultDurationPicker(defaults) }
            ))
            addView(defaultToggleRow(
                title = "밥친구 후보 추가",
                checked = defaults.allowParticipantOptions,
                onClick = {
                    savePollDefaults(defaults.copy(allowParticipantOptions = !defaults.allowParticipantOptions))
                }
            ))
            addView(defaultToggleRow(
                title = "밥친구별 선택 공개",
                checked = defaults.revealSelections,
                onClick = {
                    savePollDefaults(defaults.copy(revealSelections = !defaults.revealSelections))
                }
            ))
        }
    }

    private fun defaultSettingRow(title: String, value: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            addView(TextView(context).apply {
                text = title
                textSize = 14f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(context).apply {
                text = value
                textSize = 13f
                setTextColor(0xFFD73B24.toInt())
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                background = rounded(0xFFFFF1E8.toInt(), 16, 0xFFE0B49E.toInt(), 1)
            })
        }
    }

    private fun defaultToggleRow(title: String, checked: Boolean, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            addView(TextView(context).apply {
                text = title
                textSize = 14f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(CheckBox(context).apply {
                isChecked = checked
                isClickable = false
            })
        }
    }

    private fun showDefaultDurationPicker(defaults: PollDefaults) {
        val choices = listOf(
            30 to "30초",
            60 to "1분",
            180 to "3분",
            300 to "5분",
            600 to "10분",
            900 to "15분"
        )
        val selectedIndex = choices.indexOfFirst { it.first == defaults.durationSeconds }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("기본 제한시간")
            .setSingleChoiceItems(choices.map { it.second }.toTypedArray(), selectedIndex) { dialog, index ->
                savePollDefaults(defaults.copy(durationSeconds = choices[index].first))
                dialog.dismiss()
            }
            .setNegativeButton("닫기", null)
            .show()
    }

    private fun savePollDefaults(defaults: PollDefaults) {
        store.savePollDefaults(defaults)
        Toast.makeText(this, "밥판 기본값을 저장했습니다.", Toast.LENGTH_SHORT).show()
        showSettings()
    }

    private fun connectionFixCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(0xFFFFE8E8.toInt(), 16, 0xFFD76A6A.toInt(), 1)
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = "근처 밥친구 연결 준비"
                textSize = 17f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = connectionFixText() + "\n위치 권한은 위치 추적이 아니라 Nearby 연결을 위해 필요합니다."
                textSize = 13f
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(6), 0, dp(10))
            })
            connectionPreparationButtonRow(compact = false)?.let { addView(it) }
        }
    }

    private fun connectionPreparationButtonRow(compact: Boolean): LinearLayout? {
        val buttons = mutableListOf<Button>()
        val missingPermissions = !hasNearbyPermissions()
        val disabledRequirements = disabledConnectionRequirements().isNotEmpty()
        if (missingPermissions) {
            buttons += compactButton("권한 허용", BUTTON_PRIMARY) { requestNearbyPermissions() }
        }
        if (disabledRequirements) {
            buttons += compactButton(
                if (missingPermissions) "설정 열기" else "기기 설정 켜기",
                if (missingPermissions) BUTTON_OUTLINE else BUTTON_PRIMARY
            ) { openAppSettings() }
        }
        if (buttons.isEmpty()) return null
        return if (compact) {
            compactButtonRow(*buttons.toTypedArray())
        } else {
            buttonRow(*buttons.toTypedArray())
        }
    }

    private fun simulationFlowCard(result: SharedResult, step: DemoStep): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(0xFFFFFFFF.toInt(), 14, 0xFFE7B59D.toInt(), 1)
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = "권한 없이 밥판을 먼저 체험해요"
                textSize = 17f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = simulationStepMessage(step)
                textSize = 13f
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(6), 0, dp(10))
            })
            DemoStep.values().forEachIndexed { index, demoStep ->
                addView(simulationStepRow(
                    number = (index + 1).toString(),
                    title = simulationStepTitle(demoStep),
                    caption = simulationStepCaption(result, demoStep),
                    isCurrent = step == demoStep,
                    isDone = step.ordinal > demoStep.ordinal
                ))
            }
        }
    }

    private fun simulationStepTitle(step: DemoStep): String {
        return when (step) {
            DemoStep.OPEN -> "밥판 열기"
            DemoStep.CATEGORY -> "종류 선택"
            DemoStep.QUESTION -> "질문 입력하기"
            DemoStep.OPTIONS -> "메뉴 후보 등록하기"
            DemoStep.SIGNAL -> "밥신호 보내기"
            DemoStep.RESPOND -> "응답하기"
            DemoStep.END -> "종료하기"
        }
    }

    private fun simulationStepMessage(step: DemoStep): String {
        return when (step) {
            DemoStep.OPEN -> "실제 사용 순서대로 밥판을 여는 흐름을 따라갑니다."
            DemoStep.CATEGORY -> "먼저 어떤 밥판인지 종류를 고릅니다."
            DemoStep.QUESTION -> "밥친구에게 보일 질문을 입력합니다."
            DemoStep.OPTIONS -> "선택할 메뉴 후보를 등록합니다."
            DemoStep.SIGNAL -> "준비된 밥판을 근처 밥친구에게 보냅니다."
            DemoStep.RESPOND -> "참여자는 후보 중 하나로 응답합니다."
            DemoStep.END -> "선택이 모이면 종료하고 결과를 확인합니다."
        }
    }

    private fun simulationStepCaption(result: SharedResult, step: DemoStep): String {
        return when (step) {
            DemoStep.OPEN -> "홈 또는 밥판 탭"
            DemoStep.CATEGORY -> inferredFoodCategory(result.options).label
            DemoStep.QUESTION -> result.question
            DemoStep.OPTIONS -> "${result.options.size}개 후보"
            DemoStep.SIGNAL -> formatDurationText(result.durationSeconds)
            DemoStep.RESPOND -> "밥판 참여 후 메뉴 선택"
            DemoStep.END -> winningMenuText(result)
        }
    }

    private fun simulationGuideCard(title: String, detail: String, chips: List<String>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(14))
            background = rounded(0xFFFFFBF5.toInt(), 14, 0xFFE7B59D.toInt(), 1)
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = title
                textSize = 17f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = detail
                textSize = 13f
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(6), 0, dp(10))
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                chips.take(6).forEach { chip ->
                    addView(TextView(context).apply {
                        text = chip
                        textSize = 14f
                        setTextColor(0xFFD73B24.toInt())
                        setTypeface(typeface, Typeface.BOLD)
                        setPadding(dp(12), dp(7), dp(12), dp(7))
                        background = rounded(0xFFFFF1E8.toInt(), 14, 0xFFE7B59D.toInt(), 1)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = dp(6)
                        }
                    })
                }
            })
        }
    }

    private fun simulationStepRow(
        number: String,
        title: String,
        caption: String,
        isCurrent: Boolean,
        isDone: Boolean
    ): LinearLayout {
        val markerColor = when {
            isCurrent -> 0xFFD73B24.toInt()
            isDone -> 0xFF245341.toInt()
            else -> 0xFFE0B49E.toInt()
        }
        val markerText = if (isDone) "✓" else number
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(5), 0, dp(5))
            addView(TextView(context).apply {
                text = markerText
                textSize = 13f
                setTextColor(0xFFFFFFFF.toInt())
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = rounded(markerColor, 15)
                layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply {
                    rightMargin = dp(10)
                }
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply {
                    text = title
                    textSize = 14f
                    setTextColor(if (isCurrent) 0xFFD73B24.toInt() else 0xFF10251D.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = caption
                    textSize = 12f
                    setTextColor(0xFF647268.toInt())
                    maxLines = 2
                    setPadding(0, dp(2), 0, 0)
                })
            })
        }
    }

    private fun simulationDraftCard(result: SharedResult): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(14))
            background = rounded(0xFFFFFBF5.toInt(), 14, 0xFFE7B59D.toInt(), 1)
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = "가상 밥판"
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(0xFF10251D.toInt())
            })
            addView(TextView(context).apply {
                text = result.question
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(0xFFD73B24.toInt())
                setPadding(0, dp(7), 0, dp(8))
            })
            addView(TextView(context).apply {
                text = result.options.joinToString("  ·  ")
                textSize = 14f
                setTextColor(0xFF526158.toInt())
            })
        }
    }

    private fun avatarInfoCard(title: String, value: String, caption: String, avatarId: Int, timerPoll: NearbyPoll? = null): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(18), dp(12))
            background = groupedCardBackground()
            layoutParams = blockParams()
            addView(AvatarTileView(avatarId).apply {
                layoutParams = LinearLayout.LayoutParams(dp(AVATAR_CARD_SIZE), dp(AVATAR_CARD_SIZE)).apply {
                    rightMargin = dp(14)
                }
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                if (title.isNotBlank()) {
                    addView(TextView(context).apply {
                        text = title
                        textSize = 13f
                        setTextColor(0xFF647268.toInt())
                    })
                }
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(3), 0, dp(3))
                    addView(TextView(context).apply {
                        text = value
                        textSize = 20f
                        setTextColor(0xFF10251D.toInt())
                        setTypeface(typeface, Typeface.BOLD)
                        maxLines = 2
                        ellipsize = TextUtils.TruncateAt.END
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    timerPoll?.let { poll ->
                        addView(CountdownRingView(poll, compact = true).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                dp(54),
                                dp(54)
                            ).apply {
                                leftMargin = dp(10)
                            }
                        })
                    }
                })
                if (caption.isNotBlank()) {
                    addView(TextView(context).apply {
                        text = caption
                        textSize = 13f
                        setTextColor(0xFF526158.toInt())
                    })
                }
            })
        }
    }

    private fun statusCard(title: String, subtitle: String): TextView {
        return TextView(this).apply {
            text = "$title\n$subtitle"
            textSize = 15f
            setTextColor(0xFF294237.toInt())
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = groupedCardBackground(0xFFFFF0D9.toInt(), 0xFFE0B49E.toInt())
            layoutParams = blockParams()
        }
    }

    private fun pastRunnerUpSuggestionCard(option: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(0xFFFFFBF5.toInt(), 16, 0xFFE7B59D.toInt())
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            layoutParams = blockParams()
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply {
                    text = "지난 1주일 아쉬운 2등"
                    textSize = 12f
                    setTextColor(0xFF8B5C45.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = option
                    textSize = 18f
                    setTextColor(0xFFD73B24.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, dp(4), 0, 0)
                })
                addView(TextView(context).apply {
                    text = "지난번엔 살짝 밀렸지만, 오늘은 주인공일지도?"
                    textSize = 13f
                    setTextColor(0xFF6F5A4D.toInt())
                    setPadding(0, dp(4), dp(8), 0)
                })
            })
            addView(TextView(context).apply {
                text = "+ 후보 추가"
                textSize = 13f
                setTextColor(0xFFFFFFFF.toInt())
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = rounded(0xFFD73B24.toInt(), 18)
            })
        }
    }

    private fun weeklyReportEntryCard(label: String, menu: String, detail: String, accentColor: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(0xFFFFFFFF.toInt(), 16, 0xFFE0E7DD.toInt())
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = label
                textSize = 13f
                setTextColor(0xFF526158.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = menu
                textSize = 27f
                setTextColor(accentColor)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(6), 0, dp(5))
            })
            addView(TextView(context).apply {
                text = detail
                textSize = 14f
                setTextColor(0xFF526158.toInt())
            })
        }
    }

    private fun privacyPolicySection(title: String, body: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(0xFFFFFFFF.toInt(), 14, 0xFFE0B49E.toInt())
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = title
                textSize = 17f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = body
                textSize = 15f
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(8), 0, 0)
                setLineSpacing(dp(2).toFloat(), 1.0f)
            })
        }
    }

    private fun connectionBadge(): TextView {
        return TextView(this).apply {
            text = connectionBadgeText()
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(connectionBadgeTextColor())
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = rounded(connectionBadgeBackgroundColor(), 24, connectionBadgeStrokeColor(), 2)
            layoutParams = blockParams()
        }
    }

    private fun emptyCard(title: String, subtitle: String): TextView {
        return TextView(this).apply {
            text = "$title\n$subtitle"
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(42), dp(24), dp(42))
            background = groupedCardBackground()
            layoutParams = blockParams()
        }
    }

    private fun resultRow(
        option: String,
        count: Int,
        percent: Int,
        participants: List<Pair<String, Int>> = emptyList(),
        isEliminated: Boolean = false
    ): FrameLayout {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(15), dp(18), dp(15))
            background = rounded(if (isEliminated) 0xFFFFFBF5.toInt() else 0xFFFFFFFF.toInt(), 16, 0xFFE0E7DD.toInt())
            alpha = if (isEliminated) 0.72f else 1f
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    addView(TextView(context).apply {
                        text = option
                        textSize = 17f
                        setTextColor(0xFF10251D.toInt())
                        setTypeface(typeface, Typeface.BOLD)
                    })
                    calorieTextFor(option)?.let { calorieText ->
                        addView(TextView(context).apply {
                            text = calorieText
                            textSize = 12f
                            setTextColor(0xFF8B5C45.toInt())
                            setPadding(0, dp(3), 0, 0)
                        })
                    }
                })
                addView(TextView(context).apply {
                    text = "${count}명 · $percent%"
                    textSize = 15f
                    setTextColor(0xFFD73B24.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                })
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                background = rounded(0xFFF3E5D0.toInt(), 8)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10)).apply {
                    topMargin = dp(10)
                }
                addView(View(context).apply {
                    background = rounded(0xFF3D8B67.toInt(), 8)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, percent.coerceIn(0, 100).toFloat())
                })
                addView(FrameLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (100 - percent.coerceIn(0, 100)).toFloat())
                })
            })
            if (participants.isNotEmpty()) {
                addView(participantTagBar(participants).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(12)
                    }
                })
            }
        }
        return FrameLayout(this).apply {
            layoutParams = blockParams()
            addView(content, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            if (isEliminated) {
                addView(EliminatedHatchView().apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                })
            }
        }
    }

    private fun participantTagBar(participants: List<Pair<String, Int>>): HorizontalScrollView {
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                participants.forEach { (participant, avatarId) ->
                    addView(LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(5), dp(4), dp(12), dp(4))
                        background = rounded(0xFFFFF1E8.toInt(), 20, 0xFFE7B59D.toInt())
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            rightMargin = dp(8)
                        }
                        addView(AvatarTileView(avatarId).apply {
                            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                                rightMargin = dp(5)
                            }
                        })
                        addView(TextView(context).apply {
                            text = participant
                            textSize = 13f
                            setTextColor(0xFF245341.toInt())
                        })
                    })
                }
            })
        }
    }

    private fun templateTagBar(template: PollTemplate): HorizontalScrollView {
        return tagBar(template.options)
    }

    private fun tagBar(tags: List<String>): HorizontalScrollView {
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                tags.forEach { tag ->
                    addView(tagPill(tag))
                }
            })
        }
    }

    private fun tagPill(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(0xFF245341.toInt())
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(0xFFFFF1E8.toInt(), 18, 0xFFE7B59D.toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = dp(8)
            }
        }
    }

    private fun avatarPicker(
        initialAvatarId: Int,
        onSelected: (Int) -> Unit,
        bindSelector: (((Int) -> Unit) -> Unit)? = null
    ): LinearLayout {
        val choices = mutableListOf<Pair<Int, AvatarTileView>>()
        fun selectAvatar(avatarId: Int) {
            choices.forEach { (choiceAvatarId, choice) ->
                choice.setChosen(choiceAvatarId == avatarId)
            }
            onSelected(avatarId)
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = rounded(0xFFFFFFFF.toInt(), 14, 0xFFE0E7DD.toInt())
            layoutParams = blockParams()
            repeat(AVATAR_ROW_COUNT) { row ->
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    repeat(AVATAR_COLUMN_COUNT) { column ->
                        val avatarId = row * AVATAR_COLUMN_COUNT + column
                        val tile = AvatarTileView(
                            avatarId = avatarId,
                            chosen = avatarId == initialAvatarId,
                            contentInsetDp = 3,
                            clipAsCircle = true
                        ).apply {
                            setOnClickListener {
                                selectAvatar(avatarId)
                            }
                        }
                        choices += avatarId to tile
                        addView(tile, LinearLayout.LayoutParams(0, dp(64), 1f).apply {
                            if (column < AVATAR_COLUMN_COUNT - 1) {
                                rightMargin = dp(5)
                            }
                            if (row < AVATAR_ROW_COUNT - 1) {
                                bottomMargin = dp(5)
                            }
                        })
                    }
                })
            }
            bindSelector?.invoke(::selectAvatar)
        }
    }

    private fun verificationBarcodePanel(result: SharedResult, receipt: VoteReceipt?): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(
                if (result.isHashValid()) 0xFFF4FBF7.toInt() else 0xFFFFF3F3.toInt(),
                16,
                if (result.isHashValid()) 0xFFB7DCC9.toInt() else 0xFFE0B2B2.toInt()
            )
            layoutParams = blockParams()
            addView(BarcodeView("result:${result.resultHash}:${result.isHashValid()}").apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(58)
                )
            })
            receipt?.let {
                addView(BarcodeView("receipt:${it.voteHash}").apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(58)
                    ).apply {
                        topMargin = dp(12)
                    }
                })
            }
        }
    }

    private fun choicePill(text: String, onClick: (() -> Unit)? = null): LinearLayout {
        val calorieText = calorieTextFor(text)
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(12), dp(18), dp(12))
            background = rounded(0xFFFFF1E8.toInt(), 24, 0xFFE7B59D.toInt())
            addView(TextView(context).apply {
                this.text = text
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(0xFF123126.toInt())
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            calorieText?.let {
                addView(TextView(context).apply {
                    this.text = it
                    textSize = 12f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(0xFF8B5C45.toInt())
                    gravity = Gravity.CENTER
                    setPadding(dp(9), dp(4), dp(9), dp(4))
                    background = rounded(0xFFFFFFFF.toInt(), 14, 0xFFE7B59D.toInt(), 1)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        leftMargin = dp(10)
                    }
                })
            }
            if (onClick != null) {
                setOnClickListener {
                    performSelectionHaptic()
                    onClick()
                }
            }
            layoutParams = blockParams()
        }
    }

    private fun View.performSelectionHaptic() {
        isHapticFeedbackEnabled = true
        val feedback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        performHapticFeedback(feedback)
        vibrateSelection()
    }

    private fun vibrateSelection() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        }
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createOneShot(SELECTION_VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateCodeInput() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        }
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createOneShot(CODE_INPUT_VIBRATION_MS, CODE_INPUT_VIBRATION_AMPLITUDE))
    }

    private fun vibrateCodeError() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        }
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0L, CODE_ERROR_VIBRATION_MS, 45L, CODE_ERROR_VIBRATION_MS),
                intArrayOf(0, 220, 0, 220),
                -1
            )
        )
    }

    private fun hostSelectionCard(poll: NearbyPoll): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = rounded(0xFFE9F4FF.toInt(), 14, 0xFF91BBE5.toInt(), 2)
            layoutParams = blockParams()
            addView(TextView(context).apply {
                text = "나의 선택은?"
                textSize = 19f
                setTextColor(0xFF153F68.toInt())
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, 0, 0, dp(4))
            })
            poll.options.forEachIndexed { index, option ->
                addView(choicePill(option) { castVote(poll, option) }.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(8)
                    }
                    if (index == 0) {
                        tutorialResponseTarget = this
                    }
                })
            }
        }
    }

    private fun durationChoiceGrid(
        durationInput: EditText,
        onPresetSelected: () -> Unit,
        onCustomSelected: () -> Unit
    ): LinearLayout {
        val choices = mutableListOf<Pair<TextView, Int?>>()
        fun presetSegment(label: String, seconds: Int): TextView {
            return durationSegment(label) {
                durationInput.setText(seconds.toString())
                onPresetSelected()
                highlightDurationChoice(choices, seconds)
            }.also { choices += it to seconds }
        }
        val customSegment = durationSegment("+") {
            onCustomSelected()
            highlightDurationChoice(choices, null)
        }
        val segments = listOf(
            presetSegment("30초", 30),
            presetSegment("1분", 60),
            presetSegment("5분", 300),
            presetSegment("10분", 600),
            presetSegment("15분", 900),
            presetSegment("30분", 1800),
            customSegment.also { choices += it to null }
        )
        val selectedSeconds = durationInput.text.toString().toIntOrNull()
            ?.takeIf { isPresetDuration(it) }
        highlightDurationChoice(choices, selectedSeconds)
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(0xFFFFF1E8.toInt(), 14, 0xFFE0B49E.toInt(), 1)
            layoutParams = blockParams()
            segments.forEachIndexed { index, segment ->
                addView(segment, LinearLayout.LayoutParams(0, dp(48), 1f))
                if (index < segments.lastIndex) {
                    addView(View(context).apply {
                        background = rounded(0xFFE0B49E.toInt(), 0)
                    }, LinearLayout.LayoutParams(dp(1), ViewGroup.LayoutParams.MATCH_PARENT))
                }
            }
        }
    }

    private fun durationSegment(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 12f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    private fun composeModeSegment(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 14f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    private fun linkActionText(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(0xFFD73B24.toInt())
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            isClickable = true
            isFocusable = true
            minHeight = dp(30)
            setPadding(dp(8), dp(4), 0, dp(4))
            setOnClickListener { onClick() }
        }
    }

    private fun templateActionLink(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = "🍽 $label"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFFD73B24.toInt())
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            minHeight = dp(26)
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(2), 0, dp(2))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun highlightDurationChoice(choices: List<Pair<TextView, Int?>>, selectedSeconds: Int?) {
        choices.forEach { (button, seconds) ->
            val selected = seconds == selectedSeconds
            button.setTextColor(if (selected) 0xFFFFFFFF.toInt() else 0xFFD73B24.toInt())
            button.background = if (selected) {
                rounded(0xFFD73B24.toInt(), 10)
            } else {
                rounded(0x00FFFFFF, 0)
            }
        }
    }

    private fun isPresetDuration(durationSeconds: Int): Boolean {
        return durationSeconds in listOf(30, 60, 300, 600, 900, 1800)
    }

    private fun formatDurationText(durationSeconds: Int): String {
        val seconds = durationSeconds.coerceAtLeast(0)
        val hours = seconds / 3_600
        val minutes = (seconds % 3_600) / 60
        val remainingSeconds = seconds % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
            hours > 0 -> "${hours}시간"
            minutes > 0 && remainingSeconds > 0 -> "${minutes}분 ${remainingSeconds}초"
            minutes > 0 -> "${minutes}분"
            else -> "${remainingSeconds}초"
        }
    }

    private fun extendedDurationChoices(): List<Pair<Int, String>> {
        val choices = mutableListOf<Pair<Int, String>>()
        (10..50 step 10).forEach { seconds -> choices += seconds to "${seconds}초" }
        (1..10).forEach { minutes -> choices += minutes * 60 to "${minutes}분" }
        (20..50 step 10).forEach { minutes -> choices += minutes * 60 to "${minutes}분" }
        (60..480 step 30).forEach { minutes ->
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            val label = if (remainingMinutes == 0) "${hours}시간" else "${hours}시간 ${remainingMinutes}분"
            choices += minutes * 60 to label
        }
        return choices
    }

    private fun closestDurationChoiceIndex(choices: List<Pair<Int, String>>, durationSeconds: Int): Int {
        return choices.indices.minByOrNull { index -> abs(choices[index].first - durationSeconds) } ?: 0
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF526158.toInt())
            setPadding(dp(2), dp(18), 0, dp(8))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun buttonRow(vararg buttons: Button): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = blockParams()
            buttons.forEachIndexed { index, button ->
                button.layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    if (index < buttons.lastIndex) {
                        rightMargin = dp(8)
                    }
                }
                addView(button)
            }
        }
    }

    private fun compactButtonRow(vararg buttons: Button): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = blockParams()
            buttons.forEachIndexed { index, button ->
                button.textSize = 12f
                button.setPadding(0, 0, 0, 0)
                button.layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                    if (index < buttons.lastIndex) {
                        rightMargin = dp(4)
                    }
                }
                addView(button)
            }
        }
    }

    private fun compactButton(text: String, style: Int, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 14f
            setTextColor(
                when (style) {
                    BUTTON_PRIMARY -> 0xFFFFFFFF.toInt()
                    BUTTON_QUIET -> 0xFF526158.toInt()
                    else -> 0xFFD73B24.toInt()
                }
            )
            background = when (style) {
                BUTTON_PRIMARY -> rounded(0xFFD73B24.toInt(), 12)
                BUTTON_QUIET -> rounded(0xFFE9EEE9.toInt(), 12)
                BUTTON_CHOICE -> rounded(0xFFFFF1E8.toInt(), 12, 0xFFE0B49E.toInt())
                else -> rounded(0xFFFFFFFF.toInt(), 12, 0xFFE0B49E.toInt())
            }
            setOnClickListener { onClick() }
        }
    }

    private fun labelActionRow(text: String, actionView: View, bottomPaddingDp: Int = 8): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(18), 0, dp(bottomPaddingDp))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(TextView(context).apply {
                this.text = text
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(actionView.apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    leftMargin = dp(10)
                }
            })
        }
    }

    private fun label(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(18), 0, dp(8))
        }
    }

    private fun primaryButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            isAllCaps = false
            setTextColor(0xFFFFFFFF.toInt())
            background = rounded(0xFFD73B24.toInt(), 14)
            setOnClickListener { onClick() }
            layoutParams = blockParams()
        }
    }

    private fun stickyPrimaryActionButton(text: String, onClick: () -> Unit): Button {
        return primaryButton(text, onClick).apply {
            textSize = 17f
            elevation = dp(8).toFloat()
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54)
            )
        }
    }

    private fun outlineButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            isAllCaps = false
            setTextColor(0xFFD73B24.toInt())
            background = rounded(0xFFFFFFFF.toInt(), 14, 0xFFE0B49E.toInt())
            setOnClickListener { onClick() }
            layoutParams = blockParams()
        }
    }

    private fun quietButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            isAllCaps = false
            setTextColor(0xFF526158.toInt())
            background = rounded(0xFFE9EEE9.toInt(), 14)
            setOnClickListener { onClick() }
            layoutParams = blockParams()
        }
    }

    private fun appendLog(message: String) {
        runOnUiThread {
            if (::logView.isInitialized) {
                logView.append("\n$message")
            }
        }
    }

    private fun startNearbyConnectionTest() {
        if (!autoConnectEnabled) {
            showAutoConnectRequiredDialog()
            updateConnectionStatus()
            return
        }
        if (!hasNearbyPermissions()) {
            showPermissionFallbackDialog()
            return
        }
        val disabledRequirements = disabledConnectionRequirements()
        if (disabledRequirements.isNotEmpty()) {
            showConnectionRequirementDialog(disabledRequirements)
            updateConnectionStatus()
            return
        }
        appendLog("양쪽 기기에서 이 버튼을 누르면 서로를 찾고 연결을 시도합니다.")
        nearby.startNearbyMode()
        applyNearbyRoleMode()
        updateConnectionStatus()
    }

    private fun retryNearbySearchIfWaiting() {
        if (!autoConnectEnabled || connectedCount > 0) return
        if (!hasNearbyPermissions() || disabledConnectionRequirements().isNotEmpty()) return
        appendLog("아직 밥친구를 못 찾아 주변 탐색을 다시 시작합니다.")
        nearby.restartNearbyMode()
        applyNearbyRoleMode()
        updateConnectionStatus()
    }

    private fun showConnectionRequirementDialog(missing: List<String>) {
        val detail = missing.joinToString(" · ")
        appendLog("연결 준비 미완료: $detail")
        AlertDialog.Builder(this)
            .setTitle("연결 준비가 더 필요해요")
            .setMessage("밖에서 바로 옆 기기를 찾을 때도 두 기기 모두 Bluetooth, Wi-Fi, 위치가 켜져 있어야 합니다.\n\n지금 필요한 준비: $detail")
            .setPositiveButton("설정 열기") { _, _ -> openAppSettings() }
            .setNegativeButton("닫기", null)
            .show()
    }

    private fun showAutoConnectRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("자동 연결이 꺼져 있어요")
            .setMessage("QR은 밥판을 찾기 위한 힌트이고, 실제 밥판 내용은 가까운 기기 연결로 받아야 합니다.\n\n자동 연결을 켜면 주변 밥친구를 찾기 시작합니다.")
            .setPositiveButton("자동 연결 켜기") { _, _ -> setAutoConnectEnabled(true) }
            .setNegativeButton("닫기", null)
            .show()
    }

    private fun syncCurrentSessionToPeer(endpointId: String, peerId: String) {
        handler.postDelayed({
            sharedResultsByPoll.values
                .filter { result -> isMyResult(result) && peerId in result.participantIds }
                .forEach { result -> sendResultBlock(result, listOf(endpointId)) }
            val runningPolls = visibleActivePolls().filter { poll -> !poll.hasEnded() }
            if (runningPolls.isNotEmpty()) {
                appendLog("새 연결 기기에 진행 중인 밥판 ${runningPolls.size}건을 자동 전달")
                runningPolls.forEach { poll ->
                    registerInvitedPeer(poll.id, peerId, nearby.connectedPeers()[peerId] ?: peerId.take(8))
                    sendPoll(poll, endpointId)
                    sendHostConnectionSync(poll, endpointId)
                }
                persistSessionState()
            }
        }, CONNECTION_SYNC_DELAY_MS)
    }

    private fun hasRunningHostedPoll(): Boolean {
        return autoConnectEnabled &&
            activePolls.values.any { poll -> !poll.hasEnded() && !sharedResultPollIds.contains(poll.id) }
    }

    private fun hasRunningIncomingPoll(): Boolean {
        return autoConnectEnabled &&
            incomingPolls.values.any { poll -> !poll.hasEnded() && !seenResultPollIds.contains(poll.id) }
    }

    private fun applyNearbyRoleMode() {
        if (!autoConnectEnabled || !hasNearbyPermissions() || disabledConnectionRequirements().isNotEmpty()) return
        val hostActive = hasRunningHostedPoll()
        val participantActive = !hostActive && hasRunningIncomingPoll()
        nearby.maintainNearbyMode(
            keepDiscoveryOpen = hostActive,
            acceptIncomingOnly = hostActive,
            suppressAdvertising = participantActive
        )
    }

    private fun updateLateJoinDiscoveryMode() {
        applyNearbyRoleMode()
    }

    private fun startNearbyHeartbeat() {
        handler.removeCallbacks(nearbyHeartbeat)
        handler.removeCallbacks(nearbyPulse)
        if (!autoConnectEnabled) return
        animateConnectionSearchPulse()
        nearby.restartNearbyMode()
        handler.postDelayed(nearbyHeartbeat, NEARBY_HEARTBEAT_MS)
        handler.postDelayed(nearbyPulse, NEARBY_PULSE_MS)
    }

    private fun applyAutoConnectSetting() {
        if (autoConnectEnabled) {
            if (hasNearbyPermissions()) {
                startNearbyHeartbeat()
            } else {
                showNearbyPermissionIntro()
            }
        } else {
            handler.removeCallbacks(nearbyHeartbeat)
            handler.removeCallbacks(nearbyPulse)
            nearby.stop()
            connectedCount = 0
            updateConnectionStatus()
        }
    }

    private data class OnboardingCoachStep(
        val title: String,
        val message: String,
        val targetProvider: () -> View?,
        val primaryLabel: String = "확인",
        val secondaryLabel: String = "나중에",
        val primaryAction: (() -> Unit)? = null,
        val secondaryAction: (() -> Unit)? = null
    )

    private data class JoinHint(
        val pollId: String,
        val inviteCode: String,
        val hostToken: String,
        val expiresAtMillis: Long
    )

    private fun firstRunCoachSteps(): List<OnboardingCoachStep> {
        return listOf(
            OnboardingCoachStep(
                title = "1. 내 밥닉 확인",
                message = "결과 카드와 밥친구 목록에 보이는 이름입니다. 눌러서 밥닉과 아바타를 바꿀 수 있어요.",
                targetProvider = { onboardingHomeAvatarTarget }
            ),
            OnboardingCoachStep(
                title = "2. 밥판 열기",
                message = "오늘 먹을 후보를 올리고 밥신호를 보내는 시작 버튼입니다. 밥크로스의 가장 중요한 흐름이에요.",
                targetProvider = { onboardingHomeCreateTarget }
            ),
            OnboardingCoachStep(
                title = "3. 먼저 체험하기",
                message = "권한이 부담스럽다면 가상 밥친구로 생성, 선택, 결과 카드 흐름을 먼저 볼 수 있습니다.",
                targetProvider = { onboardingHomeDemoTarget }
            ),
            OnboardingCoachStep(
                title = "4. 연결 준비",
                message = "설정에서 근처 밥친구 연결 상태, 권한, 기본 밥판 규칙을 확인할 수 있습니다.",
                targetProvider = { onboardingSettingsTarget },
                primaryLabel = "근처 밥친구 찾기",
                secondaryLabel = "밥판 체험하기",
                primaryAction = {
                    finishFirstRunOnboarding()
                    if (hasNearbyPermissions()) {
                        applyAutoConnectSetting()
                    } else {
                        requestNearbyPermissions()
                    }
                },
                secondaryAction = {
                    finishFirstRunOnboarding()
                    updateConnectionStatus()
                    showSimulationResult()
                }
            )
        )
    }

    private fun showCoachStep(index: Int) {
        hideCoachOverlay()
        val steps = firstRunCoachSteps()
        val step = steps.getOrNull(index) ?: run {
            finishFirstRunOnboarding()
            updateConnectionStatus()
            return
        }
        showCoachOverlay(
            step = step,
            stepNumber = index + 1,
            stepCount = steps.size,
            onPrimary = {
                val action = step.primaryAction
                if (action == null) {
                    showCoachStep(index + 1)
                } else {
                    hideCoachOverlay()
                    action()
                }
            },
            onSecondary = {
                val action = step.secondaryAction
                hideCoachOverlay()
                if (action == null) {
                    finishFirstRunOnboarding()
                    updateConnectionStatus()
                } else {
                    action()
                }
            }
        )
    }

    private fun showCoachOverlay(
        step: OnboardingCoachStep,
        stepNumber: Int,
        stepCount: Int,
        onPrimary: () -> Unit,
        onSecondary: () -> Unit
    ) {
        val hadOverlay = onboardingCoachOverlay != null
        dismissCoachOverlay(animated = true)
        val target = step.targetProvider()
        target?.requestRectangleOnScreen(Rect(0, 0, target.width, target.height), false)
        handler.postDelayed({
            val content = window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: return@postDelayed
            val highlight = target?.takeIf { it.isShown && it.width > 0 && it.height > 0 }?.let { targetView ->
                val targetLocation = IntArray(2)
                val contentLocation = IntArray(2)
                targetView.getLocationOnScreen(targetLocation)
                content.getLocationOnScreen(contentLocation)
                RectF(
                    (targetLocation[0] - contentLocation[0]).toFloat(),
                    (targetLocation[1] - contentLocation[1]).toFloat(),
                    (targetLocation[0] - contentLocation[0] + targetView.width).toFloat(),
                    (targetLocation[1] - contentLocation[1] + targetView.height).toFloat()
                ).apply { inset(-dp(8).toFloat(), -dp(8).toFloat()) }
            }
            val overlay = OnboardingCoachOverlay(
                step = step,
                stepNumber = stepNumber,
                stepCount = stepCount,
                highlightRect = highlight,
                onPrimary = onPrimary,
                onSecondary = onSecondary
            )
            onboardingCoachOverlay = overlay
            content.addView(
                overlay,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )
        }, if (hadOverlay) COACH_FADE_OUT_MS + 40L else 90L)
    }

    private fun finishFirstRunOnboarding() {
        store.saveOnboardingCompleted()
        hideCoachOverlay()
    }

    private fun hideCoachOverlay() {
        dismissCoachOverlay(animated = true)
    }

    private fun dismissCoachOverlay(animated: Boolean) {
        val overlay = onboardingCoachOverlay ?: return
        onboardingCoachOverlay = null
        overlay.animate().cancel()
        if (!animated) {
            overlay.removeFromParent()
            return
        }
        overlay.animate()
            .alpha(0f)
            .setDuration(COACH_FADE_OUT_MS)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { overlay.removeFromParent() }
            .start()
    }

    private fun showNearbyPermissionIntro() {
        if (permissionIntroShown) {
            requestNearbyPermissions()
            return
        }
        permissionIntroShown = true
        AlertDialog.Builder(this)
            .setTitle("근처 밥친구를 찾기 전에")
            .setMessage(
                "밥크로스는 서버 없이 가까운 기기끼리만 연결합니다.\n\n" +
                    "수집하지 않아요\n" +
                    "- 위치 기록\n" +
                    "- 서버로 보내는 밥닉, 후보, 선택 결과\n" +
                    "- 광고/분석 SDK 사용자 행동 전송\n\n" +
                    "주변 밥판에 전달돼요\n" +
                    "- 밥닉과 아바타\n" +
                    "- 메뉴 후보\n" +
                    "- 선택 결과 또는 득표 요약\n\n" +
                    "권한이 부담스럽다면 먼저 밥판 체험하기로 흐름을 볼 수 있습니다."
            )
            .setPositiveButton("권한 허용하기") { _, _ -> requestNearbyPermissions() }
            .setNegativeButton("체험하기") { _, _ -> showSimulationResult() }
            .setNeutralButton("나중에", null)
            .show()
    }

    private fun showFirstRunOnboarding() {
        pageScrollView?.post { showCoachStep(0) }
    }

    private fun showPermissionFallbackDialog() {
        AlertDialog.Builder(this)
            .setTitle("연결 없이도 괜찮아요")
            .setMessage(
                "Nearby 권한이 꺼져 있어 지금은 근처 밥친구를 찾을 수 없습니다.\n\n" +
                    connectionFixText() + "\n\n" +
                    "위치 기록은 저장하지 않고, 밥판에 필요한 정보만 가까운 참여자 기기에 전달됩니다.\n\n" +
                    "권한이 부담스럽다면 밥판 체험하기에서 생성, 선택, 결과 카드 흐름을 먼저 볼 수 있습니다."
            )
            .setPositiveButton("밥판 체험하기") { _, _ -> showSimulationResult() }
            .setNegativeButton("권한 다시 허용") { _, _ -> requestNearbyPermissions() }
            .setNeutralButton("닫기", null)
            .show()
    }

    private fun setAutoConnectEnabled(enabled: Boolean, toastMessage: String? = null) {
        autoConnectEnabled = enabled
        store.saveAutoConnectEnabled(enabled)
        applyAutoConnectSetting()
        Toast.makeText(
            this,
            toastMessage ?: if (enabled) "자동 연결 켜짐" else "자동 연결 꺼짐",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateConnectionStatus(animateBadge: Boolean = false) {
        runOnUiThread {
            if (::connectionStatusView.isInitialized) {
                connectionStatusView.text = connectionBadgeText()
                connectionStatusView.setTextColor(connectionBadgeTextColor())
                connectionStatusView.background = rounded(connectionBadgeBackgroundColor(), 24, connectionBadgeStrokeColor(), 2)
            }
            topConnectionBadgeContainerView?.background =
                rounded(0xFFFFFEFA.toInt(), 22, connectionBadgeStrokeColor(), 2)
            topConnectionBadgeContainerView?.contentDescription = connectionStateLabel()
            topConnectionBadgePulseView?.let { pulse ->
                pulse.background = connectionPulseBackground()
                if (!autoConnectEnabled) {
                    pulse.animate().cancel()
                    pulse.alpha = 0f
                }
            }
            topConnectionSignalView?.invalidate()
        }
    }

    private fun animateConnectionSearchPulse() {
        if (!autoConnectEnabled) return
        runOnUiThread {
            topConnectionBadgePulseView?.let { pulse ->
                pulse.animate().cancel()
                pulse.background = connectionPulseBackground()
                pulse.scaleX = 1f
                pulse.scaleY = 1f
                pulse.alpha = 0.84f
                pulse.animate()
                    .scaleX(1.34f)
                    .scaleY(1.3f)
                    .alpha(0.26f)
                    .setDuration(500L)
                    .withEndAction {
                        pulse.animate()
                            .scaleX(1.1f)
                            .scaleY(1.08f)
                            .alpha(0.7f)
                            .setDuration(340L)
                            .withEndAction {
                                pulse.animate()
                                    .scaleX(1.28f)
                                    .scaleY(1.24f)
                                    .alpha(0f)
                                    .setDuration(420L)
                                    .start()
                            }
                            .start()
                    }
                    .start()
            }
        }
    }

    private fun connectionStateLabel(): String {
        return when {
            !autoConnectEnabled -> "자동 연결 꺼짐"
            !hasNearbyPermissions() -> "연결 권한 필요"
            disabledConnectionRequirements().isNotEmpty() -> "연결 준비 필요"
            displayConnectionCount() == 0 -> "밥친구 대기 중"
            else -> "밥친구 연결됨"
        }
    }

    private fun connectionBadgeTextColor(): Int {
        return when {
            !autoConnectEnabled -> 0xFF5F6661.toInt()
            !hasNearbyPermissions() -> 0xFF8B1E1E.toInt()
            displayConnectionCount() == 0 -> 0xFF8B1E1E.toInt()
            else -> 0xFF174C8B.toInt()
        }
    }

    private fun connectionBadgeBackgroundColor(): Int {
        return when {
            !autoConnectEnabled -> 0xFFE9ECE9.toInt()
            !hasNearbyPermissions() -> 0xFFFFE8E8.toInt()
            displayConnectionCount() == 0 -> 0xFFFFE8E8.toInt()
            else -> 0xFFE7F1FF.toInt()
        }
    }

    private fun connectionBadgeStrokeColor(): Int {
        return when {
            !autoConnectEnabled -> 0xFF9AA39C.toInt()
            !hasNearbyPermissions() -> 0xFFD76A6A.toInt()
            displayConnectionCount() == 0 -> 0xFFD76A6A.toInt()
            else -> 0xFF5B91D9.toInt()
        }
    }

    private fun connectionPulseBackground(): GradientDrawable {
        val fillColor = when {
            !autoConnectEnabled -> 0x00000000
            displayConnectionCount() == 0 -> 0x22D76A6A
            else -> 0x225B91D9
        }
        return rounded(fillColor, 24, connectionBadgeStrokeColor(), 3)
    }

    private fun connectionSignalActiveColor(): Int {
        return when {
            !autoConnectEnabled -> 0xFF7A827D.toInt()
            !hasNearbyPermissions() -> 0xFFD73B24.toInt()
            disabledConnectionRequirements().isNotEmpty() -> 0xFFE5A11A.toInt()
            displayConnectionCount() == 0 -> 0xFFE5A11A.toInt()
            else -> 0xFF2F9B64.toInt()
        }
    }

    private fun connectionBadgeText(): String {
        if (!autoConnectEnabled) {
            return "자동 연결 꺼짐\n설정에서 자동 연결을 켜면 주변 기기를 찾습니다."
        }
        if (!hasNearbyPermissions()) {
            return "권한 필요\n${connectionFixText()}"
        }
        disabledConnectionRequirements().takeIf { it.isNotEmpty() }?.let { missing ->
            return "연결 준비 필요\n${missing.joinToString(", ")} 상태를 켜면 근처 밥친구를 찾을 수 있습니다."
        }
        val displayConnectedCount = displayConnectionCount()
        if (displayConnectedCount == 0) {
            return "밥친구 대기 중\n${connectionStatusText()}"
        }
        val peerNames = nearby.connectedPeerNames().takeIf { it.isNotEmpty() }?.joinToString(", ")
        return "밥친구 연결됨\n${peerNames?.let { "연결된 밥친구: $it" } ?: "근처 밥친구와 연결되어 있습니다."}"
    }

    private fun connectionStatusText(): String {
        if (!autoConnectEnabled) {
            return "설정에서 자동 연결을 켜면 앱 실행 후 주변 기기와 자동으로 연결합니다."
        }
        if (!hasNearbyPermissions()) {
            return connectionFixText()
        }
        disabledConnectionRequirements().takeIf { it.isNotEmpty() }?.let { missing ->
            return "${missing.joinToString(", ")} 상태를 켜면 자동으로 근처 밥친구를 다시 찾습니다."
        }
        val displayConnectedCount = displayConnectionCount()
        return if (displayConnectedCount == 0) {
            "근처 밥친구를 기다리는 중입니다 · 약 ${NEARBY_HEARTBEAT_MS / 1000}초마다 주변 연결 상태를 확인합니다."
        } else {
            val peerNames = nearby.connectedPeerNames().takeIf { it.isNotEmpty() }?.joinToString(", ")
            peerNames?.let { "연결된 밥친구: $it" } ?: "근처 밥친구와 연결되어 있습니다."
        }
    }

    private fun connectionFixText(): String {
        val items = mutableListOf<String>()
        if (!hasNearbyPermissions()) {
            items += "Nearby/위치 권한 허용"
        }
        disabledConnectionRequirements().forEach { items += "$it 켜기" }
        return if (items.isEmpty()) {
            "연결 준비가 완료되었습니다."
        } else {
            "필요한 준비: ${items.joinToString(" · ")}"
        }
    }

    private fun disabledConnectionRequirements(): List<String> {
        return buildList {
            if (!isBluetoothEnabled()) add("Bluetooth")
            if (!isWifiEnabled()) add("Wi-Fi")
            if (!isLocationEnabled()) add("위치")
        }
    }

    private fun isBluetoothEnabled(): Boolean {
        val manager = getSystemService(BluetoothManager::class.java) ?: return true
        return manager.adapter?.isEnabled ?: true
    }

    private fun isWifiEnabled(): Boolean {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return true
        return wifiManager.isWifiEnabled
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LocationManager::class.java) ?: return true
        return if (Build.VERSION.SDK_INT >= 28) {
            locationManager.isLocationEnabled
        } else {
            val gps = runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
            val network = runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
            gps || network
        }
    }

    private fun localReadyCount(): Int {
        if (!autoConnectEnabled || !hasNearbyPermissions()) return 0
        return connectedCount + 1
    }

    private fun pollParticipantCount(poll: NearbyPoll): Int {
        return pollParticipationSnapshot(poll).participantCount
    }

    private fun pollParticipationSnapshot(poll: NearbyPoll): PollParticipationPolicy.HostSnapshot {
        return PollParticipationPolicy.hostSnapshot(
            selfParticipantId = userId,
            responseByParticipantId = responsesFor(poll.id),
            votedParticipantIds = votesFor(poll.id).keys
        )
    }

    private fun displayConnectionCount(): Int {
        val incomingPoll = visibleIncomingPolls().firstOrNull()
        return incomingPoll?.let { poll -> hostConnectedCountsByPoll[poll.id] } ?: connectedCount
    }

    private fun visibleIncomingPolls(): List<NearbyPoll> {
        return incomingPolls.values
            .filter { poll ->
                !poll.hasEnded() &&
                    !declinedPollIds.contains(poll.id) &&
                    !sharedResultsByPoll.containsKey(poll.id)
            }
            .sortedBy { it.endAtMillis }
    }

    private fun visibleActivePolls(): List<NearbyPoll> {
        return activePolls.values
            .filter { poll -> !poll.hasEnded() }
            .sortedBy { it.endAtMillis }
    }

    private fun votesFor(pollId: String): LinkedHashMap<String, String> {
        return receivedVotesByPoll.getOrPut(pollId) { linkedMapOf() }
    }

    private fun voteNamesFor(pollId: String): LinkedHashMap<String, String> {
        return receivedVoteNamesByPoll.getOrPut(pollId) { linkedMapOf() }
    }

    private fun voteAvatarsFor(pollId: String): LinkedHashMap<String, Int> {
        return receivedVoteAvatarsByPoll.getOrPut(pollId) { linkedMapOf() }
    }

    private fun invitedPeersFor(pollId: String): LinkedHashMap<String, String> {
        return invitedPeersByPoll.getOrPut(pollId) { linkedMapOf() }
    }

    private fun responsesFor(pollId: String): LinkedHashMap<String, String> {
        return pollResponsesByPoll.getOrPut(pollId) { linkedMapOf() }
    }

    private fun registerInvitedPeer(pollId: String, peerId: String, peerName: String) {
        if (peerId != userId) {
            invitedPeersFor(pollId).putIfAbsent(peerId, peerName)
        }
    }

    private fun participationStatusText(poll: NearbyPoll): String? {
        val invitees = invitedPeersByPoll[poll.id] ?: return null
        return PollParticipationPolicy.inviteeStatusText(
            inviteeParticipantIds = invitees.keys,
            responseByParticipantId = responsesFor(poll.id),
            votedParticipantIds = votesFor(poll.id).keys
        )
    }

    private fun isMyResult(result: SharedResult): Boolean {
        return result.proposerId == userId
    }

    private fun babStatusText(poll: NearbyPoll): String {
        return if (poll.hasEnded()) {
            "제한시간 종료 · 연결된 밥친구 ${connectedCount}명"
        } else {
            "${poll.remainingText()} · 연결된 밥친구 ${connectedCount}명에게 밥신호를 보냈습니다."
        }
    }

    private fun rankedResultOptions(result: SharedResult): List<String> {
        return SharedResultInsights.rankedOptions(result)
    }

    private fun historyReuseSuggestions(results: List<SharedResult>): List<HistoryReuseSuggestion> {
        val sortedResults = results
            .distinctBy { it.pollId }
            .sortedByDescending { it.createdAtMillis }
        val suggestions = mutableListOf<HistoryReuseSuggestion>()
        val usedMenus = linkedSetOf<String>()

        fun addSuggestion(label: String, menu: String?, detail: String, sourceResult: SharedResult) {
            val cleanMenu = menu?.trim().orEmpty()
            val normalized = normalizedOption(cleanMenu)
            if (cleanMenu.isBlank() || normalized in usedMenus || suggestions.size >= HISTORY_REUSE_SUGGESTION_LIMIT) return
            suggestions += HistoryReuseSuggestion(
                label = label,
                menu = cleanMenu,
                detail = detail,
                sourceResult = sourceResult
            )
            usedMenus += normalized
        }

        sortedResults.firstOrNull()?.let { result ->
            addSuggestion(
                label = "최근 우승 메뉴",
                menu = mapSearchMenuFor(result),
                detail = "후보 ${result.options.size}개로 다시 열기 · ${friendlyTime(result.createdAtMillis)}",
                sourceResult = result
            )
        }

        sortedResults
            .asSequence()
            .mapNotNull { result -> runnerUpOption(result)?.let { option -> result to option } }
            .firstOrNull()
            ?.let { (result, option) ->
                addSuggestion(
                    label = "지난 2등 메뉴",
                    menu = option,
                    detail = "아쉬운 후보를 맨 앞으로 올려 다시 열기",
                    sourceResult = result
                )
            }

        frequentHistoryOption(sortedResults)?.let { (result, menu, count) ->
            addSuggestion(
                label = "자주 나온 후보",
                menu = menu,
                detail = "지난 결정 ${count}번 등장 · 후보 ${result.options.size}개로 다시 열기",
                sourceResult = result
            )
        }

        sortedResults.forEach { result ->
            if (suggestions.size >= HISTORY_REUSE_SUGGESTION_LIMIT) return@forEach
            addSuggestion(
                label = "또 이걸로 정할까요?",
                menu = mapSearchMenuFor(result) ?: result.options.firstOrNull(),
                detail = "이전 후보 ${result.options.size}개를 그대로 가져옵니다.",
                sourceResult = result
            )
        }
        return suggestions
    }

    private fun runnerUpOption(result: SharedResult): String? {
        val rankedCounts = result.options
            .map { option -> option to (result.counts[option] ?: 0) }
            .filter { (_, count) -> count > 0 }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { result.options.indexOf(it.first) })
        val winningCount = rankedCounts.firstOrNull()?.second ?: return null
        val runnerUpCount = rankedCounts.firstOrNull { (_, count) -> count < winningCount }?.second ?: return null
        return rankedCounts.firstOrNull { (_, count) -> count == runnerUpCount }?.first
    }

    private fun frequentHistoryOption(results: List<SharedResult>): Triple<SharedResult, String, Int>? {
        val appearances = linkedMapOf<String, Triple<SharedResult, String, Int>>()
        results.forEach { result ->
            result.options.forEach { option ->
                val normalized = normalizedOption(option)
                if (normalized.isBlank()) return@forEach
                val current = appearances[normalized]
                appearances[normalized] = Triple(current?.first ?: result, current?.second ?: option, (current?.third ?: 0) + 1)
            }
        }
        return appearances.values.maxWithOrNull(
            compareBy<Triple<SharedResult, String, Int>> { it.third }
                .thenBy { it.first.createdAtMillis }
        )?.takeIf { it.third >= 2 }
    }

    private fun historyReuseTemplate(suggestion: HistoryReuseSuggestion): PollTemplate {
        val source = suggestion.sourceResult
        val options = (listOf(suggestion.menu) + source.options)
            .map { normalizedOption(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_POLL_OPTION_COUNT)
        val durationSeconds = source.durationSeconds.coerceIn(CUSTOM_DURATION_MIN_SECONDS, CUSTOM_DURATION_MAX_SECONDS)
        return PollTemplate(
            id = "draft-history-${System.currentTimeMillis()}",
            title = suggestion.menu,
            question = "오늘도 ${suggestion.menu} 괜찮을까요?",
            options = options,
            durationMinutes = ((durationSeconds + 59) / 60).coerceAtLeast(1),
            durationSeconds = durationSeconds,
            allowParticipantOptions = source.allowParticipantOptions,
            revealSelections = source.revealSelections,
            categoryKey = inferredFoodCategory(options).name
        )
    }

    private fun runnerUpMenuSuggestion(currentQuestion: String, currentOptions: List<String>): String? {
        val normalizedQuestion = normalizedQuestion(currentQuestion)
        if (normalizedQuestion.isBlank()) return null
        val currentOptionSet = currentOptions.map { normalizedOption(it) }.toSet()
        val sinceMillis = System.currentTimeMillis() - RECENT_RUNNER_UP_WINDOW_MS
        return store.loadResultHistory()
            .asSequence()
            .filter { result ->
                result.createdAtMillis >= sinceMillis &&
                    normalizedQuestion(result.question) == normalizedQuestion
            }
            .sortedByDescending { result -> result.createdAtMillis }
            .mapNotNull { result ->
                val rankedCounts = result.options
                    .map { option -> option to (result.counts[option] ?: 0) }
                    .filter { (_, count) -> count > 0 }
                    .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { result.options.indexOf(it.first) })
                val winningCount = rankedCounts.firstOrNull()?.second ?: return@mapNotNull null
                val runnerUpCount = rankedCounts.firstOrNull { (_, count) -> count < winningCount }?.second ?: return@mapNotNull null
                rankedCounts.firstOrNull { (option, count) ->
                    count == runnerUpCount &&
                        normalizedOption(option) !in currentOptionSet &&
                        option.length <= MAX_OPTION_LENGTH
                }?.first
            }
            .firstOrNull()
    }

    private fun weeklyMenuReport(): WeeklyMenuReport {
        val sinceMillis = System.currentTimeMillis() - RECENT_RUNNER_UP_WINDOW_MS
        val recentResults = store.loadResultHistory().filter { result -> result.createdAtMillis >= sinceMillis }
        val selectedScores = linkedMapOf<String, Int>()
        val runnerUpScores = linkedMapOf<String, Int>()
        var totalVotes = 0

        recentResults.forEach { result ->
            val rankedCounts = result.options
                .map { option -> option to (result.counts[option] ?: 0) }
                .filter { (_, count) -> count > 0 }
                .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { result.options.indexOf(it.first) })
            totalVotes += rankedCounts.sumOf { (_, count) -> count }
            val winningCount = rankedCounts.firstOrNull()?.second ?: return@forEach
            rankedCounts
                .filter { (_, count) -> count == winningCount }
                .forEach { (option, count) -> selectedScores[option] = (selectedScores[option] ?: 0) + count }

            val runnerUpCount = rankedCounts.firstOrNull { (_, count) -> count < winningCount }?.second ?: return@forEach
            rankedCounts
                .filter { (_, count) -> count == runnerUpCount }
                .forEach { (option, count) -> runnerUpScores[option] = (runnerUpScores[option] ?: 0) + count }
        }

        fun topMenu(scores: Map<String, Int>): Pair<String, Int>? {
            val topScore = scores.values.maxOrNull() ?: return null
            val topMenus = scores.filterValues { score -> score == topScore }.keys.joinToString(", ")
            return topMenus to topScore
        }

        val selected = topMenu(selectedScores)
        val runnerUp = topMenu(runnerUpScores)
        return WeeklyMenuReport(
            resultCount = recentResults.size,
            totalVotes = totalVotes,
            topSelectedMenu = selected?.first,
            topSelectedDetail = selected?.second?.let { score -> "선택 메뉴 집계 ${score}표" },
            topRunnerUpMenu = runnerUp?.first,
            topRunnerUpDetail = runnerUp?.second?.let { score -> "2등 메뉴 집계 ${score}표 · 다음엔 뒤집을지도 모릅니다." }
        )
    }

    private fun normalizedQuestion(question: String): String {
        return question.trim().replace(Regex("\\s+"), " ")
    }

    private fun winningMenuText(result: SharedResult): String {
        return singleWinningMenu(result) ?: "선택된 메뉴 없음"
    }

    private fun mapSearchMenuFor(result: SharedResult): String? {
        return singleWinningMenu(result) ?: tieBreakSuggestion(result)
    }

    private fun calorieTextFor(menu: String): String? {
        return MenuCalorieCatalog.estimateFor(menu)?.let { hint -> "약 ${hint.kcal} kcal" }
    }

    private fun openMapSearch(menu: String) {
        val query = Uri.encode("$menu 맛집")
        val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query"))
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$query"))
        val launched = runCatching {
            if (geoIntent.resolveActivity(packageManager) != null) {
                startActivity(geoIntent)
            } else {
                startActivity(webIntent)
            }
        }.isSuccess
        if (!launched) {
            Toast.makeText(this, "지도 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun singleWinningMenu(result: SharedResult): String? {
        return SharedResultInsights.singleWinningMenu(result)
    }

    private fun tieBreakSuggestion(result: SharedResult): String? {
        return SharedResultInsights.tieBreakSuggestion(result, userId)
    }

    private fun resultHasVotes(result: SharedResult): Boolean {
        return SharedResultInsights.hasVotes(result)
    }

    private fun showBabDecisionDialog(result: SharedResult) {
        val winningMenu = singleWinningMenu(result) ?: return
        var fireworks: DecisionFireworksView? = null
        val category = inferredFoodCategory(result.options)
        Dialog(this).apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setContentView(decisionCutSceneView(winningMenu, category).apply {
                isClickable = true
                setOnClickListener {
                    if (isShowing) dismiss()
                }
            })
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            setOnShowListener {
                fireworks = showDecisionFireworks()
            }
            setOnDismissListener {
                fireworks?.removeFromParent()
                fireworks = null
            }
            show()
            window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.88f).roundToInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun decisionCutSceneView(winningMenu: String, category: FoodCategory): FrameLayout {
        return FrameLayout(this).apply {
            background = rounded(0xFFFFFBF5.toInt(), 24, 0xFFD73B24.toInt(), 2)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(0xFFFFF1E8.toInt(), 18, 0xFFE7B59D.toInt(), 1)
                setPadding(dp(14), dp(14), dp(14), dp(14))
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                addView(FrameLayout(context).apply {
                    background = rounded(0xFFFFD9C6.toInt(), 18, 0xFFD73B24.toInt(), 1)
                    clipToOutline = true
                    layoutParams = LinearLayout.LayoutParams(dp(108), dp(136)).apply {
                        rightMargin = dp(14)
                    }
                    addView(ImageView(context).apply {
                        setImageResource(R.drawable.bab_cross_launcher)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        alpha = 0.96f
                        contentDescription = "밥크로스 결정 컷"
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    })
                    addView(View(context).apply {
                        background = rounded(0x1AFFFFFF, 18)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    })
                })
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    addView(TextView(context).apply {
                        text = "밥크로스의 한마디"
                        fixedCardTextSize(12f)
                        setTextColor(0xFF8B5C45.toInt())
                        setTypeface(typeface, Typeface.BOLD)
                    })
                    addView(TextView(context).apply {
                        text = decisionCutSceneSubject(category)
                        fixedCardTextSize(17f)
                        setTextColor(0xFF10251D.toInt())
                        setTypeface(typeface, Typeface.BOLD)
                        setPadding(0, dp(6), 0, 0)
                    })
                    addView(TextView(context).apply {
                        text = winningMenu
                        fixedCardTextSize(27f)
                        setTextColor(0xFFFFFFFF.toInt())
                        setTypeface(typeface, Typeface.BOLD)
                        gravity = Gravity.CENTER
                        maxLines = 2
                        setPadding(dp(14), dp(9), dp(14), dp(10))
                        background = rounded(0xFFD73B24.toInt(), 20)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = dp(8)
                        }
                    })
                    calorieTextFor(winningMenu)?.let { calorieText ->
                        addView(TextView(context).apply {
                            text = calorieText
                            fixedCardTextSize(12f)
                            setTextColor(0xFF6F5A4D.toInt())
                            setTypeface(typeface, Typeface.BOLD)
                            setPadding(0, dp(6), 0, 0)
                        })
                    }
                    addView(TextView(context).apply {
                        text = decisionCutSceneClosing(category)
                        fixedCardTextSize(14f)
                        setTextColor(0xFF526158.toInt())
                        setPadding(0, dp(10), 0, 0)
                    })
                    addView(View(context).apply {
                        background = rounded(0xFFD73B24.toInt(), 2)
                        alpha = 0.75f
                        layoutParams = LinearLayout.LayoutParams(dp(64), dp(3)).apply {
                            topMargin = dp(13)
                        }
                    })
                })
            })
            addView(TextView(context).apply {
                text = "결정!"
                fixedCardTextSize(11f)
                setTextColor(0xFFFFFFFF.toInt())
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                background = rounded(0xFF3D8B67.toInt(), 13, 0xFFFFFFFF.toInt(), 1)
                layoutParams = FrameLayout.LayoutParams(dp(58), dp(26), Gravity.TOP or Gravity.RIGHT).apply {
                    topMargin = dp(5)
                    rightMargin = dp(5)
                }
            })
        }
    }

    private fun decisionCutSceneSubject(category: FoodCategory): String {
        return when (category) {
            FoodCategory.OTHER -> "오늘의 선택은"
            else -> "오늘의 ${category.label}${topicParticle(category.label)}"
        }
    }

    private fun topicParticle(text: String): String {
        val lastChar = text.lastOrNull() ?: return "는"
        val hangulIndex = lastChar.code - 0xAC00
        val hasFinalConsonant = hangulIndex in 0..11171 && hangulIndex % 28 != 0
        return if (hasFinalConsonant) "은" else "는"
    }

    private fun decisionCutSceneClosing(category: FoodCategory): String {
        return when (category) {
            FoodCategory.DRINK -> "밥결정 완료. 이제 시원하게 마시면 됩니다."
            FoodCategory.DESSERT,
            FoodCategory.SNACK -> "밥결정 완료. 이제 맛있게 즐기면 됩니다."
            FoodCategory.OTHER -> "밥결정 완료. 이제 그대로 가면 됩니다."
            else -> "밥결정 완료. 이제 맛있게 먹으면 됩니다."
        }
    }

    private fun showDecisionFireworks(): DecisionFireworksView? {
        val content = window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: return null
        val fireworks = DecisionFireworksView()
        content.addView(
            fireworks,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        return fireworks
    }

    private inner class OnboardingCoachOverlay(
        private val step: OnboardingCoachStep,
        private val stepNumber: Int,
        private val stepCount: Int,
        private val highlightRect: RectF?,
        private val onPrimary: () -> Unit,
        private val onSecondary: () -> Unit
    ) : FrameLayout(this@MainActivity) {
        private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFD7C5.toInt()
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
        }
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33D73B24
            style = Paint.Style.STROKE
            strokeWidth = dp(6).toFloat()
        }
        private val coachCard = coachCardView()

        init {
            setWillNotDraw(false)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            isClickable = true
            isFocusable = true
            contentDescription = "${step.title} 안내"
            alpha = 0f
            addView(coachCard)
            post { positionCoachCard() }
            animate()
                .alpha(1f)
                .setDuration(180L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(0x99000000.toInt())
            highlightRect?.let { rect ->
                val radius = dp(20).toFloat()
                canvas.drawRoundRect(rect, radius, radius, clearPaint)
                canvas.drawRoundRect(rect, radius, radius, glowPaint)
                canvas.drawRoundRect(rect, radius, radius, strokePaint)
            }
        }

        private fun coachCardView(): LinearLayout {
            return LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(16), dp(18), dp(16))
                background = rounded(0xFF24352D.toInt(), 18, 0xFF6C8B78.toInt(), 1)
                elevation = dp(12).toFloat()
                addView(TextView(context).apply {
                    text = "$stepNumber/$stepCount"
                    textSize = 12f
                    setTextColor(0xFFFFB49E.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(context).apply {
                    text = step.title
                    textSize = 21f
                    setTextColor(0xFFFFFBF5.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, dp(4), 0, 0)
                })
                addView(TextView(context).apply {
                    text = step.message
                    textSize = 15f
                    setTextColor(0xFFE4EEE7.toInt())
                    setLineSpacing(dp(2).toFloat(), 1.0f)
                    setPadding(0, dp(8), 0, dp(10))
                })
                addView(coachActionRow())
            }
        }

        private fun coachActionRow(): LinearLayout {
            fun coachButton(label: String, primary: Boolean, onClick: () -> Unit): TextView {
                return TextView(context).apply {
                    text = label
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setTypeface(typeface, if (primary) Typeface.BOLD else Typeface.NORMAL)
                    setTextColor(if (primary) 0xFF10251D.toInt() else 0xFFE4EEE7.toInt())
                    setPadding(dp(12), 0, dp(12), 0)
                    background = rounded(
                        if (primary) 0xFFFFB49E.toInt() else 0xFF34473D.toInt(),
                        16,
                        if (primary) 0xFFFFD7C5.toInt() else 0xFF6C8B78.toInt(),
                        1
                    )
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { onClick() }
                }
            }
            return LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.RIGHT
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(34)
                )
                addView(coachButton(step.secondaryLabel, primary = false) { onSecondary() }.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ).apply {
                        rightMargin = dp(8)
                    }
                })
                addView(coachButton(step.primaryLabel, primary = true) { onPrimary() }.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                })
            }
        }

        private fun positionCoachCard() {
            val sideMargin = dp(18)
            val gap = dp(20)
            val bottomSafeInset = ViewCompat.getRootWindowInsets(this)
                ?.getInsets(WindowInsetsCompat.Type.systemBars())
                ?.bottom
                ?: 0
            val bottomLimit = height - sideMargin - bottomSafeInset
            val cardWidth = (width - sideMargin * 2).coerceAtLeast(dp(240))
            coachCard.measure(
                View.MeasureSpec.makeMeasureSpec(cardWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)
            )
            val cardHeight = coachCard.measuredHeight
            val top = highlightRect?.let { rect ->
                val belowTop = rect.bottom.toInt() + gap
                val aboveTop = rect.top.toInt() - cardHeight - gap
                when {
                    belowTop + cardHeight <= bottomLimit -> belowTop
                    aboveTop >= sideMargin -> aboveTop
                    else -> bottomLimit - cardHeight
                }
            } ?: (bottomLimit - cardHeight)
            coachCard.layoutParams = FrameLayout.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = sideMargin
                topMargin = top.coerceAtLeast(sideMargin)
            }
        }
    }

    private fun View.removeFromParent() {
        (parent as? ViewGroup)?.removeView(this)
    }

    private fun friendlyTime(timestampMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = (now - timestampMillis).coerceAtLeast(0L)
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour
        return when {
            diff < minute -> "방금 전"
            diff < hour -> "${diff / minute}분 전"
            diff < day -> "${diff / hour}시간 전"
            diff < 2 * day -> SimpleDateFormat("어제 HH:mm", Locale.KOREA).format(Date(timestampMillis))
            diff < 7 * day -> SimpleDateFormat("E HH:mm", Locale.KOREA).format(Date(timestampMillis))
            else -> SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA).format(Date(timestampMillis))
        }
    }

    private fun runLocalSimulation() {
        simulator.runDemo().forEach { appendLog(it) }
    }

    private fun persistSessionState() {
        val state = JSONObject()
            .put("activePolls", serializePolls(activePolls))
            .put("incomingPolls", serializePolls(incomingPolls))
            .put("receivedVotes", serializeNestedStrings(receivedVotesByPoll))
            .put("receivedVoteNames", serializeNestedStrings(receivedVoteNamesByPoll))
            .put("receivedVoteAvatars", serializeNestedInts(receivedVoteAvatarsByPoll))
            .put("invitedPeers", serializeNestedStrings(invitedPeersByPoll))
            .put("pollResponses", serializeNestedStrings(pollResponsesByPoll))
            .put("hostConnectedCounts", JSONObject(hostConnectedCountsByPoll as Map<*, *>))
            .put("hostReadyCounts", JSONObject(hostReadyCountsByPoll as Map<*, *>))
            .put("submittedVotes", JSONObject(submittedVotes as Map<*, *>))
            .put("acceptedPollIds", JSONArray(acceptedPollIds.toList()))
            .put("declinedPollIds", JSONArray(declinedPollIds.toList()))
            .put("completionPromptShownKeys", JSONArray(completionPromptShownKeys.toList()))
            .put("sharedResults", JSONArray(sharedResultsByPoll.values.map { it.toHistoryJson() }))
        latestReceipt?.let { state.put("latestReceipt", it.toJson()) }
        store.saveSessionState(state.toString())
    }

    private fun clearLocalSessionMemory(clearReceipts: Boolean) {
        activePolls.clear()
        incomingPolls.clear()
        sharedResult = null
        sharedResultsByPoll.clear()
        receivedVotesByPoll.clear()
        receivedVoteNamesByPoll.clear()
        receivedVoteAvatarsByPoll.clear()
        voteEndpointIdsByPoll.clear()
        invitedPeersByPoll.clear()
        pollResponsesByPoll.clear()
        hostConnectedCountsByPoll.clear()
        hostReadyCountsByPoll.clear()
        completionPromptShownKeys.clear()
        submittedVotes.clear()
        acceptedPollIds.clear()
        declinedPollIds.clear()
        declinedPollNotice = null
        sharedResultPollIds.clear()
        seenIncomingPollIds.clear()
        seenResultPollIds.clear()
        if (clearReceipts) {
            latestReceipt = null
        }
    }

    private fun restoreSessionState() {
        val state = runCatching { JSONObject(store.loadSessionState() ?: return) }.getOrNull() ?: return
        restorePolls(state.optJSONObject("activePolls"), activePolls)
        restorePolls(state.optJSONObject("incomingPolls"), incomingPolls)
        restoreNestedStrings(state.optJSONObject("receivedVotes"), receivedVotesByPoll)
        restoreNestedStrings(state.optJSONObject("receivedVoteNames"), receivedVoteNamesByPoll)
        restoreNestedInts(state.optJSONObject("receivedVoteAvatars"), receivedVoteAvatarsByPoll)
        restoreNestedStrings(state.optJSONObject("invitedPeers"), invitedPeersByPoll)
        restoreNestedStrings(state.optJSONObject("pollResponses"), pollResponsesByPoll)
        readIntMap(state.optJSONObject("hostConnectedCounts")).forEach { (pollId, count) -> hostConnectedCountsByPoll[pollId] = count }
        readIntMap(state.optJSONObject("hostReadyCounts")).forEach { (pollId, count) -> hostReadyCountsByPoll[pollId] = count }
        readStringMap(state.optJSONObject("submittedVotes")).forEach { (pollId, option) -> submittedVotes[pollId] = option }
        readStringSet(state.optJSONArray("acceptedPollIds")).forEach { acceptedPollIds += it }
        readStringSet(state.optJSONArray("declinedPollIds")).forEach { declinedPollIds += it }
        readStringSet(state.optJSONArray("completionPromptShownKeys")).forEach { completionPromptShownKeys += it }
        readStringSet(state.optJSONArray("completionPromptShownPollIds")).forEach { completionPromptShownKeys += it }
        val results = state.optJSONArray("sharedResults")
        if (results != null) {
            for (index in 0 until results.length()) {
                runCatching { SharedResult.fromHistoryJson(results.getJSONObject(index)) }.getOrNull()?.let { result ->
                    if (result.isHashValid()) {
                        sharedResultsByPoll[result.pollId] = result
                        sharedResultPollIds += result.pollId
                        sharedResult = result
                    }
                }
            }
        }
        state.optJSONObject("latestReceipt")?.let { latestReceipt = runCatching { VoteReceipt.fromJson(it) }.getOrNull() }
        activePolls.values.forEach { poll ->
            voteEndpointIdsByPoll.putIfAbsent(poll.id, linkedMapOf())
            if (!sharedResultPollIds.contains(poll.id)) {
                scheduleResultShare(poll)
            }
        }
        seenIncomingPollIds += incomingPolls.keys
        seenResultPollIds += sharedResultsByPoll.keys
    }

    private fun serializePolls(polls: Map<String, NearbyPoll>): JSONObject {
        return JSONObject().apply {
            polls.forEach { (pollId, poll) ->
                put(pollId, JSONObject().put("proposerId", poll.proposerId).put("payload", poll.toPayloadJson()))
            }
        }
    }

    private fun restorePolls(raw: JSONObject?, target: LinkedHashMap<String, NearbyPoll>) {
        if (raw == null) return
        raw.keys().forEach { pollId ->
            val stored = raw.optJSONObject(pollId) ?: return@forEach
            val poll = runCatching {
                NearbyPoll.fromPayload(stored.getString("proposerId"), stored.getString("payload"))
            }.getOrNull() ?: return@forEach
            if (!poll.hasEnded() || target === activePolls) {
                target[poll.id] = poll
            }
        }
    }

    private fun serializeNestedStrings(values: Map<String, LinkedHashMap<String, String>>): JSONObject {
        return JSONObject().apply { values.forEach { (key, map) -> put(key, JSONObject(map as Map<*, *>)) } }
    }

    private fun serializeNestedInts(values: Map<String, LinkedHashMap<String, Int>>): JSONObject {
        return JSONObject().apply { values.forEach { (key, map) -> put(key, JSONObject(map as Map<*, *>)) } }
    }

    private fun restoreNestedStrings(raw: JSONObject?, target: LinkedHashMap<String, LinkedHashMap<String, String>>) {
        if (raw == null) return
        raw.keys().forEach { key -> target[key] = LinkedHashMap(readStringMap(raw.optJSONObject(key))) }
    }

    private fun restoreNestedInts(raw: JSONObject?, target: LinkedHashMap<String, LinkedHashMap<String, Int>>) {
        if (raw == null) return
        raw.keys().forEach { key ->
            val source = raw.optJSONObject(key) ?: return@forEach
            target[key] = linkedMapOf<String, Int>().apply {
                source.keys().forEach { nestedKey -> put(nestedKey, source.optInt(nestedKey)) }
            }
        }
    }

    private fun readStringMap(raw: JSONObject?): Map<String, String> {
        if (raw == null) return emptyMap()
        return raw.keys().asSequence().associateWith { key -> raw.optString(key) }
    }

    private fun readIntMap(raw: JSONObject?): Map<String, Int> {
        if (raw == null) return emptyMap()
        return raw.keys().asSequence().associateWith { key -> raw.optInt(key) }
    }

    private fun readStringSet(raw: JSONArray?): Set<String> {
        if (raw == null) return emptySet()
        return (0 until raw.length()).map { index -> raw.getString(index) }.toSet()
    }

    private fun saveIdentity(nextIdentity: String, avatarId: Int) {
        store.saveIdentity(nextIdentity)
        store.saveAvatarId(avatarId)
        selfName = nextIdentity
        selfAvatarId = avatarId
        reconnectForIdentityUpdate()
    }

    private fun reconnectForIdentityUpdate() {
        handler.removeCallbacks(nearbyHeartbeat)
        handler.removeCallbacks(nearbyPulse)
        nearby.stop()
        nearby = NearbyVoteConnectionManager(this, selfName, this)
        simulator = LocalVoteSimulator(selfName)
        connectedCount = 0
        activePolls.replaceAll { _, poll -> poll.copy(proposerName = selfName, proposerAvatarId = selfAvatarId) }
        persistSessionState()
        applyAutoConnectSetting()
    }

    private fun suggestIdentity(): String {
        return suggestIdentityWithAvatar().name
    }

    private fun suggestIdentityWithAvatar(): IdentitySuggestion {
        val adjectives = listOf("따뜻한", "빠른", "조용한", "선명한", "든든한", "가벼운", "밝은", "차분한")
        val objects = listOf(
            "머그컵" to 0,
            "가방" to 1,
            "연필" to 2,
            "우산" to 3,
            "램프" to 4,
            "시계" to 5,
            "나침반" to 6,
            "노트" to 7,
            "헤드폰" to 8,
            "카메라" to 9,
            "열쇠" to 10,
            "화분" to 11,
            "운동화" to 12,
            "종이비행기" to 13,
            "벨" to 14,
            "찻주전자" to 15,
            "안경" to 16,
            "알람시계" to 17,
            "배낭" to 18,
            "풍선" to 19
        )
        val seed = abs((Build.MODEL + System.currentTimeMillis()).hashCode())
        val (objectName, avatarId) = objects[(seed / adjectives.size) % objects.size]
        return IdentitySuggestion(adjectives[seed % adjectives.size] + objectName, avatarId)
    }

    private fun avatarIdForUser(id: String): Int {
        return Math.floorMod(id.hashCode(), AVATAR_COUNT)
    }

    private fun resolvedAvatarId(id: String, avatarId: Int): Int {
        return if (avatarId in 0 until AVATAR_COUNT) avatarId else avatarIdForUser(id)
    }

    private fun publishPoll(
        question: String,
        options: List<String>,
        durationSeconds: Int,
        requireInviteCode: Boolean,
        allowParticipantOptions: Boolean,
        revealSelections: Boolean
    ) {
        val durationMinutes = ((durationSeconds + 59) / 60).coerceAtLeast(1)
        val poll = NearbyPoll(
            id = "poll-${UUID.randomUUID()}",
            proposerId = userId,
            proposerName = selfName,
            proposerAvatarId = selfAvatarId,
            question = question,
            options = options,
            durationMinutes = durationMinutes,
            durationSeconds = durationSeconds,
            endAtMillis = System.currentTimeMillis() + durationSeconds * 1_000L,
            allowParticipantOptions = allowParticipantOptions,
            revealSelections = revealSelections,
            inviteCode = if (requireInviteCode) generateInviteCode() else ""
        )
        activePolls[poll.id] = poll
        receivedVotesByPoll[poll.id] = linkedMapOf()
        receivedVoteNamesByPoll[poll.id] = linkedMapOf()
        receivedVoteAvatarsByPoll[poll.id] = linkedMapOf()
        voteEndpointIdsByPoll[poll.id] = linkedMapOf()
        invitedPeersByPoll[poll.id] = LinkedHashMap(nearby.connectedPeers())
        pollResponsesByPoll[poll.id] = linkedMapOf()
        completionPromptShownKeys.removeAll { key -> key == poll.id || key.startsWith("${poll.id}:") }
        sharedResult = null
        sharedResultPollIds -= poll.id
        seenResultPollIds -= poll.id
        updateLateJoinDiscoveryMode()
        if (autoConnectEnabled) {
            startNearbyConnectionTest()
        }
        sendPoll(poll)
        sendHostConnectionSync(poll)
        scheduleResultShare(poll)
        persistSessionState()
        showPublishedPoll(poll)
    }

    private fun generateInviteCode(): String {
        return Random.nextInt(1_000, 10_000).toString()
    }

    private fun sendPoll(poll: NearbyPoll, endpointId: String? = null) {
        val message = NearVoteMessage(
            type = NearVoteMessageType.POLL,
            senderId = userId,
            payloadJson = poll.toPayloadJson()
        ).toJson()
        if (endpointId == null) {
            nearby.sendToAll(message)
        } else {
            nearby.sendTo(endpointId, message)
        }
    }

    private fun sendPollResponse(poll: NearbyPoll, response: String, endpointId: String? = null) {
        if (response == POLL_RESPONSE_ACCEPTED) {
            acceptedPollIds += poll.id
        }
        val message = NearVoteMessage(
            type = NearVoteMessageType.POLL_RESPONSE,
            senderId = userId,
            payloadJson = JSONObject()
                .put("pollId", poll.id)
                .put("respondentId", userId)
                .put("respondentName", selfName)
                .put("respondentAvatarId", selfAvatarId)
                .put("response", response)
                .toString()
        ).toJson()
        if (endpointId == null) {
            nearby.sendToAll(message)
        } else {
            nearby.sendTo(endpointId, message)
        }
    }

    private fun resendAcceptedPollResponses(endpointId: String? = null) {
        incomingPolls.values
            .filter { poll ->
                acceptedPollIds.contains(poll.id) &&
                    !submittedVotes.containsKey(poll.id) &&
                    !poll.hasEnded()
            }
            .forEach { poll -> sendPollResponse(poll, POLL_RESPONSE_ACCEPTED, endpointId) }
    }

    private fun sendHostConnectionSync(poll: NearbyPoll? = null, endpointId: String? = null) {
        if (!autoConnectEnabled || !hasNearbyPermissions()) return
        val polls = poll?.let { listOf(it) }
            ?: visibleActivePolls().filter { activePoll -> !sharedResultPollIds.contains(activePoll.id) }
        if (polls.isEmpty()) return
        polls.forEach { activePoll ->
            val readyCount = localReadyCount()
            val message = NearVoteMessage(
                type = NearVoteMessageType.CONNECTION_SYNC,
                senderId = userId,
                payloadJson = JSONObject()
                    .put("pollId", activePoll.id)
                    .put("connectedCount", connectedCount)
                    .put("readyCount", readyCount)
                    .toString()
            ).toJson()
            if (endpointId == null) {
                nearby.sendToAll(message)
            } else {
                nearby.sendTo(endpointId, message)
            }
        }
    }

    private fun normalizedOption(option: String): String {
        return option.trim().replace(Regex("\\s+"), " ")
    }

    private fun foodCategoryFor(option: String): FoodCategory {
        return MenuCatalog.categoryKeyFor(option)
            ?.let { key -> runCatching { FoodCategory.valueOf(key) }.getOrNull() }
            ?: FoodCategory.OTHER
    }

    private fun inferredFoodCategory(options: List<String>): FoodCategory {
        val counts = options
            .map { option -> foodCategoryFor(option) }
            .filter { category -> category != FoodCategory.OTHER }
            .groupingBy { it }
            .eachCount()
        return counts.maxByOrNull { (_, count) -> count }?.key ?: FoodCategory.MEAL
    }

    private fun templateFoodCategory(template: PollTemplate): FoodCategory {
        return template.categoryKey
            ?.let { key -> runCatching { FoodCategory.valueOf(key) }.getOrNull() }
            ?: inferredFoodCategory(template.options)
    }

    private fun recentComposeFoodCategory(): FoodCategory? {
        return store.loadRecentFoodCategoryKey()
            ?.let { key -> runCatching { FoodCategory.valueOf(key) }.getOrNull() }
            ?.takeIf { category -> category in selectableFoodCategories() }
    }

    private fun isValidIncomingPoll(poll: NearbyPoll): Boolean {
        val normalizedOptions = poll.options.map { normalizedOption(it) }
        return poll.id.isNotBlank() &&
            poll.id.length <= MAX_ID_LENGTH &&
            poll.proposerId.isNotBlank() &&
            poll.proposerId.length <= MAX_ID_LENGTH &&
            poll.proposerName.length <= MAX_NAME_LENGTH &&
            isValidRemoteAvatarId(poll.proposerAvatarId) &&
            poll.question.isNotBlank() &&
            poll.question.length <= MAX_QUESTION_LENGTH &&
            normalizedOptions.size in 2..MAX_POLL_OPTION_COUNT &&
            normalizedOptions.distinct().size == normalizedOptions.size &&
            normalizedOptions.all { it.isNotBlank() && it.length <= MAX_OPTION_LENGTH } &&
            poll.durationSeconds in CUSTOM_DURATION_MIN_SECONDS..CUSTOM_DURATION_MAX_SECONDS &&
            poll.endAtMillis > 0L
    }

    private fun isValidIncomingResult(result: SharedResult): Boolean {
        val normalizedOptions = result.options.map { normalizedOption(it) }
        val optionSet = normalizedOptions.toSet()
        val totalVotes = result.counts.values.sum()
        val normalizedSelections = result.participantSelections.values.map { normalizedOption(it) }
        return result.pollId.isNotBlank() &&
            result.pollId.length <= MAX_ID_LENGTH &&
            result.proposerId.isNotBlank() &&
            result.proposerId.length <= MAX_ID_LENGTH &&
            result.proposerName.length <= MAX_NAME_LENGTH &&
            isValidRemoteAvatarId(result.proposerAvatarId) &&
            result.question.isNotBlank() &&
            result.question.length <= MAX_QUESTION_LENGTH &&
            normalizedOptions.size in 1..MAX_POLL_OPTION_COUNT &&
            normalizedOptions.distinct().size == normalizedOptions.size &&
            normalizedOptions.all { it.isNotBlank() && it.length <= MAX_OPTION_LENGTH } &&
            result.counts.values.all { it >= 0 && it <= MAX_RESULT_VOTES } &&
            totalVotes <= MAX_RESULT_VOTES &&
            result.participantCount in 0..MAX_RESULT_VOTES &&
            result.participantIds.size <= MAX_RESULT_VOTES &&
            result.participantIds.distinct().size == result.participantIds.size &&
            result.participantIds.all { it.isNotBlank() && it.length <= MAX_ID_LENGTH } &&
            result.participantNames.size <= MAX_RESULT_VOTES &&
            result.participantNames.all { it.length <= MAX_NAME_LENGTH } &&
            result.participantAvatarIds.keys.all { it in result.participantIds } &&
            result.participantAvatarIds.values.all { isValidRemoteAvatarId(it) } &&
            result.participantSelections.keys.all { it in result.participantIds } &&
            (result.revealSelections || result.participantSelections.isEmpty()) &&
            normalizedSelections.all { it in optionSet }
    }

    private fun isValidIncomingMessage(message: NearVoteMessage): Boolean {
        return NearVoteMessageValidator.isValid(
            message = message,
            limits = NearVoteMessageValidator.Limits(
                maxSenderIdLength = MAX_ID_LENGTH,
                maxMessageIdLength = MAX_MESSAGE_ID_LENGTH,
                maxPayloadJsonLength = MAX_NEARBY_PAYLOAD_JSON_LENGTH
            ),
            replayGuard = messageReplayGuard
        )
    }

    private fun isValidRemoteAvatarId(avatarId: Int): Boolean {
        return avatarId == -1 || avatarId in 0 until AVATAR_COUNT
    }

    private fun addSuggestedOptionIfNeeded(poll: NearbyPoll, option: String): NearbyPoll {
        if (option in poll.options) return poll
        val updatedPoll = poll.copy(options = poll.options + option)
        activePolls[poll.id] = updatedPoll
        sendPoll(updatedPoll)
        persistSessionState()
        appendLog("참여자 선택지 추가: $option")
        return updatedPoll
    }

    private fun castVote(poll: NearbyPoll, option: String) {
        val submittedOption = normalizedOption(option)
        val isPublishedByMe = activePolls.containsKey(poll.id)
        val receivedVotes = votesFor(poll.id)
        val receivedVoteNames = voteNamesFor(poll.id)
        val receivedVoteAvatars = voteAvatarsFor(poll.id)
        if (submittedVotes.containsKey(poll.id) && !isPublishedByMe) {
            Toast.makeText(this, "이미 참여한 밥판입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (receivedVotes.containsKey(userId) && isPublishedByMe) {
            Toast.makeText(this, "이미 참여한 밥판입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (poll.hasEnded()) {
            Toast.makeText(this, "종료된 밥판입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (submittedOption.isBlank() || submittedOption.length > MAX_OPTION_LENGTH) {
            Toast.makeText(this, "선택지를 확인해 주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        val newOption = submittedOption !in poll.options
        if (newOption && !poll.allowParticipantOptions) {
            Toast.makeText(this, "새 메뉴 후보를 추가할 수 없는 밥판입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (newOption && poll.options.size >= MAX_POLL_OPTION_COUNT) {
            Toast.makeText(this, "선택지는 최대 ${MAX_POLL_OPTION_COUNT}개까지 추가할 수 있습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        nearby.sendToAll(
            NearVoteMessage(
                type = NearVoteMessageType.VOTE,
                senderId = userId,
                payloadJson = JSONObject()
                    .put("pollId", poll.id)
                    .put("option", submittedOption)
                    .put("voterId", userId)
                    .put("voterName", selfName)
                    .put("voterAvatarId", selfAvatarId)
                    .toString()
            ).toJson()
        )
        if (isPublishedByMe) {
            val updatedPoll = addSuggestedOptionIfNeeded(poll, submittedOption)
            receivedVotes[userId] = submittedOption
            receivedVoteNames[userId] = selfName
            receivedVoteAvatars[userId] = selfAvatarId
            saveLocalReceipt(updatedPoll, submittedOption)
            persistSessionState()
            showPublishedPoll(updatedPoll)
            maybeOfferEarlyClosure(updatedPoll)
        } else {
            submittedVotes[poll.id] = submittedOption
            persistSessionState()
            showVoteSubmitted(poll, submittedOption)
        }
    }

    private fun handleNearbyMessage(endpointId: String, rawMessage: String) {
        val message = runCatching { NearVoteMessage.fromJson(rawMessage) }.getOrElse {
            appendLog("알 수 없는 메시지 형식")
            return
        }
        if (!isValidIncomingMessage(message)) {
            appendLog("허용 범위를 벗어난 주변 메시지는 무시함")
            return
        }
        when (message.type) {
            NearVoteMessageType.PROFILE -> {
                val profile = runCatching { JSONObject(message.payloadJson) }.getOrElse { return }
                val peerId = profile.optString("userId", message.senderId)
                val peerName = profile.optString("name", peerId.take(8))
                val peerAvatarId = profile.optInt("avatarId", -1)
                if (peerId != message.senderId) {
                    appendLog("발신자와 다른 사용자 ID의 프로필은 무시함")
                    return
                }
                if (peerId.isBlank() || peerId.length > MAX_ID_LENGTH || peerName.length > MAX_NAME_LENGTH || !isValidRemoteAvatarId(peerAvatarId)) {
                    appendLog("유효하지 않은 프로필은 무시함")
                    return
                }
                if (peerId == userId) {
                    nearby.disconnectEndpoint(endpointId, "자기 자신으로 확인된 연결 정리")
                    updateConnectionStatus()
                    return
                }
                nearby.identifyPeer(endpointId, peerId, peerName)
                syncCurrentSessionToPeer(endpointId, peerId)
                updateConnectionStatus()
            }
            NearVoteMessageType.POLL -> {
                val poll = runCatching { NearbyPoll.fromPayload(message.senderId, message.payloadJson) }.getOrElse {
                    appendLog("밥판 메시지를 읽지 못함")
                    return
                }
                if (!isValidIncomingPoll(poll)) {
                    appendLog("허용 범위를 벗어난 밥판 메시지는 무시함")
                    return
                }
                if (poll.proposerId == userId) return
                if (declinedPollIds.contains(poll.id)) {
                    appendLog("거절한 밥판은 무시함: ${poll.question}")
                    return
                }
                val alreadyKnown = !seenIncomingPollIds.add(poll.id)
                incomingPolls[poll.id] = poll
                updateLateJoinDiscoveryMode()
                persistSessionState()
                if (acceptedPollIds.contains(poll.id) || submittedVotes.containsKey(poll.id)) {
                    sendPollResponse(poll, POLL_RESPONSE_ACCEPTED, endpointId)
                }
                if (pendingJoinHint?.pollId == poll.id) {
                    runOnUiThread { tryShowPendingJoinHint() }
                    return
                }
                if (alreadyKnown || submittedVotes.containsKey(poll.id)) {
                    appendLog("이미 받은 밥판 갱신: ${poll.question}")
                    return
                }
                runOnUiThread { showPollInvitation(poll) }
            }
            NearVoteMessageType.POLL_RESPONSE -> {
                val payload = runCatching { JSONObject(message.payloadJson) }.getOrElse { return }
                val poll = activePolls[payload.optString("pollId")] ?: return
                val respondentId = payload.optString("respondentId", message.senderId)
                val respondentName = payload.optString("respondentName", respondentId.take(8))
                val respondentAvatarId = payload.optInt("respondentAvatarId", -1)
                val response = payload.optString("response")
                if (respondentId != message.senderId ||
                    respondentId.length > MAX_ID_LENGTH ||
                    respondentName.length > MAX_NAME_LENGTH ||
                    !isValidRemoteAvatarId(respondentAvatarId) ||
                    response !in setOf(POLL_RESPONSE_ACCEPTED, POLL_RESPONSE_DECLINED)
                ) {
                    appendLog("유효하지 않은 참여 응답은 무시함")
                    return
                }
                if (response == POLL_RESPONSE_DECLINED && votesFor(poll.id).containsKey(respondentId)) {
                    appendLog("이미 고른 사용자의 거절 응답은 무시함")
                    return
                }
                nearby.identifyPeer(endpointId, respondentId, respondentName)
                registerInvitedPeer(poll.id, respondentId, respondentName)
                responsesFor(poll.id)[respondentId] = response
                persistSessionState()
                sendHostConnectionSync(poll)
                runOnUiThread {
                    showPublishedPoll(poll)
                    updateConnectionStatus(animateBadge = true)
                    maybeOfferEarlyClosure(poll)
                }
            }
            NearVoteMessageType.VOTE -> {
                val payload = runCatching { JSONObject(message.payloadJson) }.getOrElse {
                    appendLog("선택 메시지를 읽지 못함")
                    return
                }
                var poll = activePolls[payload.optString("pollId")] ?: return
                val receivedVotes = votesFor(poll.id)
                val receivedVoteNames = voteNamesFor(poll.id)
                val receivedVoteAvatars = voteAvatarsFor(poll.id)
                val voterId = payload.optString("voterId", message.senderId)
                val voterName = payload.optString("voterName", voterId.take(8))
                val voterAvatarId = payload.optInt("voterAvatarId", avatarIdForUser(voterId))
                val option = normalizedOption(payload.optString("option"))
                if (option.isBlank()) return
                if (voterId != message.senderId) {
                    appendLog("발신자와 다른 사용자 ID의 표는 무시함")
                    return
                }
                if (voterId.length > MAX_ID_LENGTH || voterName.length > MAX_NAME_LENGTH || !isValidRemoteAvatarId(voterAvatarId)) {
                    appendLog("유효하지 않은 선택자 정보는 무시함")
                    return
                }
                nearby.identifyPeer(endpointId, voterId, voterName)
                if (poll.hasEnded()) {
                    appendLog("종료된 밥판의 선택은 무시함: $voterId")
                    return
                }
                if (receivedVotes.containsKey(voterId)) {
                    appendLog("중복 선택 무시: $voterId")
                    sendReceipt(endpointId, poll, voterId, voterName, receivedVotes.getValue(voterId))
                    return
                }
                val newOption = option !in poll.options
                if (newOption && (!poll.allowParticipantOptions || option.length > MAX_OPTION_LENGTH || poll.options.size >= MAX_POLL_OPTION_COUNT)) {
                    appendLog("허용되지 않은 새 선택지 무시: $option")
                    return
                }
                if (responsesFor(poll.id)[voterId] == POLL_RESPONSE_DECLINED) {
                    appendLog("거절한 사용자의 표는 무시함: $voterName")
                    return
                }
                if (newOption) {
                    poll = addSuggestedOptionIfNeeded(poll, option)
                }
                registerInvitedPeer(poll.id, voterId, voterName)
                responsesFor(poll.id).putIfAbsent(voterId, POLL_RESPONSE_ACCEPTED)
                receivedVotes[voterId] = option
                receivedVoteNames[voterId] = voterName
                receivedVoteAvatars[voterId] = voterAvatarId
                voteEndpointIdsByPoll.getOrPut(poll.id) { linkedMapOf() }[voterId] = endpointId
                sendReceipt(endpointId, poll, voterId, voterName, option)
                persistSessionState()
                runOnUiThread {
                    showPublishedPoll(poll)
                    maybeOfferEarlyClosure(poll)
                }
            }
            NearVoteMessageType.RECEIPT -> {
                val payload = runCatching { JSONObject(message.payloadJson) }.getOrElse {
                    appendLog("영수증 메시지를 읽지 못함")
                    return
                }
                val receipt = runCatching {
                    VoteReceipt(
                        pollId = payload.getString("pollId"),
                        voterId = payload.getString("voterId"),
                        voterName = payload.optString("voterName", payload.getString("voterId")),
                        voteHash = payload.getString("voteHash")
                    )
                }.getOrElse {
                    appendLog("영수증 필드가 올바르지 않음")
                    return
                }
                if (receipt.pollId.length > MAX_ID_LENGTH ||
                    receipt.voterId.length > MAX_ID_LENGTH ||
                    receipt.voterName.length > MAX_NAME_LENGTH ||
                    receipt.voteHash.length != 64
                ) {
                    appendLog("유효하지 않은 영수증은 무시함")
                    return
                }
                if (receipt.voterId == userId) {
                    val submitted = submittedVotes[receipt.pollId] ?: return
                    if (receipt.voteHash != hash("${receipt.pollId}:$userId:$submitted")) {
                        appendLog("유효하지 않은 영수증은 무시함")
                        return
                    }
                    latestReceipt = receipt
                    store.saveReceipt(receipt)
                    persistSessionState()
                    incomingPolls[receipt.pollId]?.let { poll ->
                        runOnUiThread { showVoteSubmitted(poll, submittedVotes[receipt.pollId] ?: "선택 완료") }
                    }
                }
            }
            NearVoteMessageType.CONNECTION_SYNC -> {
                val payload = runCatching { JSONObject(message.payloadJson) }.getOrElse {
                    appendLog("연결 수 동기화 메시지를 읽지 못함")
                    return
                }
                val pollId = payload.optString("pollId")
                val poll = incomingPolls[pollId] ?: return
                if (poll.proposerId != message.senderId) {
                    appendLog("밥판장이 아닌 기기의 연결 수 동기화는 무시함")
                    return
                }
                val hostConnectedCount = payload.optInt("connectedCount", -1)
                val hostReadyCount = payload.optInt("readyCount", -1)
                if (hostConnectedCount !in 0..NearbyVoteConnectionManager.MAX_CONNECTIONS ||
                    hostReadyCount !in 1..(NearbyVoteConnectionManager.MAX_CONNECTIONS + 1) ||
                    hostReadyCount < hostConnectedCount + 1
                ) {
                    appendLog("유효하지 않은 연결 수 동기화는 무시함")
                    return
                }
                val countChanged = hostConnectedCountsByPoll[pollId] != hostConnectedCount ||
                    hostReadyCountsByPoll[pollId] != hostReadyCount
                hostConnectedCountsByPoll[pollId] = hostConnectedCount
                hostReadyCountsByPoll[pollId] = hostReadyCount
                persistSessionState()
                updateConnectionStatus(animateBadge = countChanged)
            }
            NearVoteMessageType.RESULT_BLOCK -> {
                val result = runCatching { SharedResult.fromPayload(message.senderId, message.payloadJson) }.getOrElse {
                    appendLog("결과 블록을 읽지 못함")
                    return
                }
                if (!isValidIncomingResult(result)) {
                    appendLog("허용 범위를 벗어난 결과 블록은 무시함")
                    return
                }
                if (result.proposerId == userId) return
                if (declinedPollIds.contains(result.pollId)) {
                    appendLog("거절한 밥판의 결과는 무시함: ${result.question}")
                    return
                }
                if (!result.isHashValid()) {
                    appendLog("무결성 확인에 실패한 결과는 저장하지 않음: ${result.question}")
                    return
                }
                val alreadyKnown = !seenResultPollIds.add(result.pollId)
                sharedResult = result
                sharedResultsByPoll[result.pollId] = result
                incomingPolls.remove(result.pollId)
                hostConnectedCountsByPoll.remove(result.pollId)
                hostReadyCountsByPoll.remove(result.pollId)
                updateLateJoinDiscoveryMode()
                store.saveResult(result)
                persistSessionState()
                if (alreadyKnown) {
                    appendLog("이미 받은 결과 블록 갱신: ${result.question}")
                    return
                }
                runOnUiThread {
                    showSharedResult(result)
                    showBabDecisionDialog(result)
                }
            }
            else -> Unit
        }
    }

    private fun saveLocalReceipt(poll: NearbyPoll, option: String) {
        val receipt = VoteReceipt(
            pollId = poll.id,
            voterId = userId,
            voterName = selfName,
            voteHash = hash("${poll.id}:$userId:$option")
        )
        latestReceipt = receipt
        store.saveReceipt(receipt)
    }

    private fun sendProfile(endpointId: String) {
        nearby.sendTo(
            endpointId,
            NearVoteMessage(
                type = NearVoteMessageType.PROFILE,
                senderId = userId,
                payloadJson = JSONObject()
                    .put("userId", userId)
                    .put("name", selfName)
                    .put("avatarId", selfAvatarId)
                    .toString()
            ).toJson()
        )
    }

    private fun sendReceipt(endpointId: String, poll: NearbyPoll, voterId: String, voterName: String, option: String) {
        val voteHash = hash("${poll.id}:$voterId:$option")
        nearby.sendTo(
            endpointId,
            NearVoteMessage(
                type = NearVoteMessageType.RECEIPT,
                senderId = userId,
                payloadJson = JSONObject()
                    .put("pollId", poll.id)
                    .put("voterId", voterId)
                    .put("voterName", voterName)
                    .put("voteHash", voteHash)
                    .toString()
            ).toJson()
        )
    }

    private fun shareResultBlock(poll: NearbyPoll) {
        if (sharedResultPollIds.contains(poll.id)) {
            showSharedResult(sharedResultsByPoll[poll.id] ?: return)
            return
        }
        val receivedVotes = votesFor(poll.id)
        val receivedVoteNames = voteNamesFor(poll.id)
        val receivedVoteAvatars = voteAvatarsFor(poll.id)
        val counts = poll.options.associateWith { option ->
            receivedVotes.values.count { it == option }
        }
        val participantIds = receivedVotes.keys.toList()
        val participantNames = participantIds.map { id -> receivedVoteNames[id] ?: id.take(8) }
        val participantAvatarIds = participantIds.associateWith { id ->
            receivedVoteAvatars[id] ?: avatarIdForUser(id)
        }
        val participantSelections = if (poll.revealSelections) {
            participantIds.associateWith { id -> receivedVotes.getValue(id) }
        } else {
            emptyMap()
        }
        val result = SharedResult(
            pollId = poll.id,
            proposerId = userId,
            proposerName = selfName,
            proposerAvatarId = selfAvatarId,
            question = poll.question,
            options = poll.options,
            counts = counts,
            participantIds = participantIds,
            participantNames = participantNames,
            participantAvatarIds = participantAvatarIds,
            participantSelections = participantSelections,
            participantCount = receivedVotes.size,
            createdAtMillis = System.currentTimeMillis(),
            resultHash = SharedResult.computeHash(
                pollId = poll.id,
                question = poll.question,
                options = poll.options,
                counts = counts,
                participantIds = participantIds,
                participantSelections = participantSelections
            ),
            durationSeconds = poll.durationSeconds,
            allowParticipantOptions = poll.allowParticipantOptions,
            revealSelections = poll.revealSelections
        )
        sharedResult = result
        sharedResultsByPoll[poll.id] = result
        sharedResultPollIds += poll.id
        store.saveResult(result)
        persistSessionState()
        updateLateJoinDiscoveryMode()
        val participantEndpoints = participantIds.mapNotNull { participantId ->
            voteEndpointIdsByPoll[poll.id]?.get(participantId) ?: nearby.endpointForPeer(participantId)
        }.distinct()
        sendResultBlock(result, participantEndpoints)
        showSharedResult(result)
        showBabDecisionDialog(result)
    }

    private fun endPollAndShareResult(poll: NearbyPoll) {
        val endedPoll = poll.copy(endAtMillis = System.currentTimeMillis())
        activePolls[poll.id] = endedPoll
        persistSessionState()
        updateLateJoinDiscoveryMode()
        shareResultBlock(endedPoll)
    }

    private fun maybeOfferEarlyClosure(poll: NearbyPoll) {
        val responses = responsesFor(poll.id)
        val receivedVotes = votesFor(poll.id)
        val prompt = PollCompletionPolicy.promptFor(
            PollCompletionPolicy.Input(
                pollId = poll.id,
                isEnded = poll.hasEnded(),
                isResultShared = sharedResultPollIds.contains(poll.id),
                readyCount = pollParticipantCount(poll),
                votedParticipantIds = receivedVotes.keys,
                invitedParticipantIds = invitedPeersFor(poll.id).keys,
                responseByParticipantId = responses,
                selfParticipantId = userId,
                shownPromptKeys = completionPromptShownKeys
            )
        ) ?: return

        completionPromptShownKeys += prompt.key
        persistSessionState()
        showCompletionPromptDialog(poll, prompt.readyCount, prompt.votedCount)
    }

    private fun showCompletionPromptDialog(poll: NearbyPoll, readyCount: Int, votedCount: Int) {
        lateinit var dialog: AlertDialog
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(17), dp(18), dp(16))
            background = rounded(0xFFFFFEFA.toInt(), 18, 0xFFD73B24.toInt(), 2)
            addView(TextView(context).apply {
                text = "모두 골랐어요"
                textSize = 20f
                setTextColor(0xFF10251D.toInt())
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = "참여 가능한 밥친구가 모두 메뉴를 선택했습니다. 지금 오늘의 밥결정을 만들까요?"
                textSize = 14f
                setTextColor(0xFF526158.toInt())
                setPadding(0, dp(7), 0, dp(12))
            })
            addView(TextView(context).apply {
                text = "선택 완료 $votedCount/${readyCount}명"
                textSize = 13f
                setTextColor(0xFFD73B24.toInt())
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(7), dp(12), dp(7))
                background = rounded(0xFFFFF1E8.toInt(), 14, 0xFFE7B59D.toInt(), 1)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(14)
                }
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(compactButton("조금 더 기다리기", BUTTON_OUTLINE) {
                    dialog.dismiss()
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                        rightMargin = dp(6)
                    }
                })
                addView(compactButton("지금 종료", BUTTON_PRIMARY) {
                    dialog.dismiss()
                    endPollAndShareResult(activePolls[poll.id] ?: poll)
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                        leftMargin = dp(6)
                    }
                })
            })
        }
        dialog = AlertDialog.Builder(this)
            .setView(content)
            .create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    private fun sendResultBlock(result: SharedResult, endpointIds: List<String>) {
        appendLog("결과 블록 전송")
        val message = NearVoteMessage(
            type = NearVoteMessageType.RESULT_BLOCK,
            senderId = userId,
            payloadJson = result.toPayloadJson()
        ).toJson()
        endpointIds.forEach { endpointId -> nearby.sendTo(endpointId, message) }
    }

    private fun scheduleResultShare(poll: NearbyPoll) {
        val delay = (poll.endAtMillis - System.currentTimeMillis()).coerceAtLeast(1_000L)
        handler.postDelayed({
            if (activePolls.containsKey(poll.id) && !sharedResultPollIds.contains(poll.id)) {
                shareResultBlock(activePolls.getValue(poll.id))
            }
        }, delay)
    }

    private fun requestNearbyPermissions() {
        permissionLauncher.launch(nearbyPermissions())
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        })
    }

    private fun hasNearbyPermissions(): Boolean {
        return nearbyPermissions().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasBluetoothRuntimePermissions(): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return listOf(
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        ).all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasNearbyWifiPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.NEARBY_WIFI_DEVICES
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun nearbyPermissions(): Array<String> {
        return buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)

            if (Build.VERSION.SDK_INT >= 31) {
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (Build.VERSION.SDK_INT >= 33) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }.toTypedArray()
    }

    private fun blockParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(12)
        }
    }

    private fun rounded(color: Int, radius: Int, strokeColor: Int? = null, strokeWidth: Int = 1): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeColor != null) {
                setStroke(dp(strokeWidth), strokeColor)
            }
        }
    }

    private fun roundedCorners(
        color: Int,
        topLeft: Int = 0,
        topRight: Int = 0,
        bottomRight: Int = 0,
        bottomLeft: Int = 0
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadii = floatArrayOf(
                dp(topLeft).toFloat(),
                dp(topLeft).toFloat(),
                dp(topRight).toFloat(),
                dp(topRight).toFloat(),
                dp(bottomRight).toFloat(),
                dp(bottomRight).toFloat(),
                dp(bottomLeft).toFloat(),
                dp(bottomLeft).toFloat()
            )
        }
    }

    private fun groupedCardBackground(
        fillColor: Int = 0xFFFFFEFA.toInt(),
        strokeColor: Int = 0xFFD4DED7.toInt()
    ): GradientDrawable {
        return rounded(fillColor, 16, strokeColor, 2)
    }

    private fun oval(color: Int, strokeColor: Int? = null, strokeWidth: Int = 1): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            if (strokeColor != null) {
                setStroke(dp(strokeWidth), strokeColor)
            }
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return (alpha.coerceIn(0, 255) shl 24) or (color and 0x00FFFFFF)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun systemNavigationBottomInset(): Int {
        return resources.getIdentifier("navigation_bar_height", "dimen", "android")
            .takeIf { it > 0 }
            ?.let { resources.getDimensionPixelSize(it) }
            ?: dp(16)
    }

    private fun systemStatusTopInset(): Int {
        return resources.getIdentifier("status_bar_height", "dimen", "android")
            .takeIf { it > 0 }
            ?.let { resources.getDimensionPixelSize(it) }
            ?: dp(24)
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private enum class NavigationIcon {
        HOME,
        CREATE,
        RESULTS,
        SETTINGS
    }

    private enum class ResultAction {
        REVOTE,
        SAVE_TEMPLATE,
        MAP,
        SHARE
    }

    private enum class DemoStep {
        OPEN,
        CATEGORY,
        QUESTION,
        OPTIONS,
        SIGNAL,
        RESPOND,
        END
    }

    private enum class ComposeMode {
        SOLO,
        TOGETHER
    }

    private enum class FoodCategory(
        val label: String,
        val backgroundColor: Int,
        val strokeColor: Int,
        val textColor: Int
    ) {
        MEAL("식사", 0xFFFFF1E8.toInt(), 0xFFE0B49E.toInt(), 0xFF8B4B2D.toInt()),
        DISH("요리", 0xFFEAF6EF.toInt(), 0xFF94C6A8.toInt(), 0xFF245341.toInt()),
        DRINK("음료수", 0xFFE8F1FF.toInt(), 0xFF9EB9E6.toInt(), 0xFF244E80.toInt()),
        DESSERT("후식", 0xFFFFECF2.toInt(), 0xFFE6A5B8.toInt(), 0xFF8A3450.toInt()),
        SNACK("간식", 0xFFFFF6D8.toInt(), 0xFFE0C66E.toInt(), 0xFF725A12.toInt()),
        OTHER("기타", 0xFFE9EEE9.toInt(), 0xFFC8D2C9.toInt(), 0xFF526158.toInt())
    }

    private data class WeeklyMenuReport(
        val resultCount: Int,
        val totalVotes: Int,
        val topSelectedMenu: String?,
        val topSelectedDetail: String?,
        val topRunnerUpMenu: String?,
        val topRunnerUpDetail: String?
    )

    private data class HistoryReuseSuggestion(
        val label: String,
        val menu: String,
        val detail: String,
        val sourceResult: SharedResult
    )

    private data class IdentitySuggestion(
        val name: String,
        val avatarId: Int
    )

    private data class CardRarity(
        val key: String,
        val label: String,
        val frameColor: Int,
        val shineColor: Int,
        val strokeColor: Int,
        val headerColor: Int,
        val artColor: Int,
        val accentColor: Int,
        val imageResId: Int,
        val imageTintColor: Int
    )

    private inner class ShinyCardFrameLayout(
        context: Context,
        private val rarity: CardRarity
    ) : LinearLayout(context) {
        private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
        }
        private val frameRect = RectF()
        private val shineMatrix = Matrix()

        init {
            orientation = VERTICAL
            setWillNotDraw(false)
        }

        override fun onDraw(canvas: Canvas) {
            val radius = dp(16).toFloat()
            frameRect.set(0f, 0f, width.toFloat(), height.toFloat())

            framePaint.color = rarity.frameColor
            canvas.drawRoundRect(frameRect, radius, radius, framePaint)

            val cx = width / 2f
            val cy = height / 2f
            val progress = (System.currentTimeMillis() % SHINE_ROTATION_MS).toFloat() / SHINE_ROTATION_MS
            val sweep = SweepGradient(
                cx,
                cy,
                intArrayOf(
                    0x00FFFFFF,
                    withAlpha(rarity.shineColor, 70),
                    0xEEFFFFFF.toInt(),
                    withAlpha(rarity.shineColor, 120),
                    0x00FFFFFF,
                    withAlpha(rarity.shineColor, 70),
                    0xCCFFFFFF.toInt(),
                    withAlpha(rarity.shineColor, 105),
                    0x00FFFFFF
                ),
                floatArrayOf(0f, 0.10f, 0.16f, 0.23f, 0.35f, 0.60f, 0.66f, 0.73f, 1f)
            )
            shineMatrix.reset()
            shineMatrix.setRotate(progress * 360f, cx, cy)
            sweep.setLocalMatrix(shineMatrix)
            shinePaint.shader = sweep
            canvas.drawRoundRect(frameRect, radius, radius, shinePaint)
            shinePaint.shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                intArrayOf(0x22FFFFFF, 0x00FFFFFF, withAlpha(rarity.shineColor, 36)),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(frameRect, radius, radius, shinePaint)
            shinePaint.shader = null

            strokePaint.color = rarity.strokeColor
            val strokeInset = strokePaint.strokeWidth / 2f
            frameRect.inset(strokeInset, strokeInset)
            canvas.drawRoundRect(frameRect, radius, radius, strokePaint)
            frameRect.inset(-strokeInset, -strokeInset)

            if (isAttachedToWindow) {
                postInvalidateDelayed(SHINE_FRAME_MS)
            }
            super.onDraw(canvas)
        }
    }

    private inner class HologramOverlayView(
        private val rarity: CardRarity,
        private val maxAlpha: Int
    ) : View(this) {
        private val holoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1).toFloat()
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1).toFloat()
        }
        private val bounds = RectF()
        private val stickerPath = Path()

        init {
            setWillNotDraw(false)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            bounds.set(0f, 0f, width.toFloat(), height.toFloat())
            val inset = dp(4).toFloat()
            bounds.inset(inset, inset)
            val cornerRadius = dp(12).toFloat()
            stickerPath.reset()
            stickerPath.addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW)

            val saved = canvas.save()
            canvas.clipPath(stickerPath)
            val progress = (System.currentTimeMillis() % HOLOGRAM_SWEEP_MS).toFloat() / HOLOGRAM_SWEEP_MS
            val offset = width * (progress * 1.4f - 0.2f)
            holoPaint.shader = LinearGradient(
                offset - width * 0.55f,
                0f,
                offset + width * 0.35f,
                height.toFloat(),
                intArrayOf(
                    0x00FFFFFF,
                    withAlpha(0xFFFF7AC8.toInt(), maxAlpha / 2),
                    withAlpha(0xFF6BE8FF.toInt(), maxAlpha),
                    withAlpha(rarity.shineColor, maxAlpha),
                    0x00FFFFFF
                ),
                floatArrayOf(0f, 0.28f, 0.48f, 0.68f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(bounds, holoPaint)
            holoPaint.shader = null

            linePaint.color = withAlpha(0xFFFFFFFF.toInt(), (maxAlpha * 1.15f).toInt())
            var x = -height.toFloat() + (progress * dp(28))
            while (x < width + height) {
                canvas.drawLine(x, height.toFloat(), x + height * 0.78f, 0f, linePaint)
                x += dp(28)
            }
            canvas.restoreToCount(saved)

            borderPaint.color = withAlpha(rarity.strokeColor, 210)
            canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, borderPaint)

            if (isAttachedToWindow) {
                postInvalidateDelayed(SHINE_FRAME_MS)
            }
        }
    }

    private inner class ResultActionIconView(
        private val action: ResultAction,
        private val tint: Int
    ) : View(this) {
        private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFF8E4.toInt()
            style = Paint.Style.FILL
        }
        private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tint
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val sx = width / 38f
            val sy = height / 38f
            fun x(value: Float) = value * sx
            fun y(value: Float) = value * sy

            canvas.drawCircle(x(19f), y(19f), x(18f), innerPaint)

            when (action) {
                ResultAction.REVOTE -> {
                    val arrow = Path().apply {
                        moveTo(x(23f), y(12f))
                        cubicTo(x(14f), y(9f), x(9f), y(15f), x(10f), y(21f))
                        cubicTo(x(11f), y(28f), x(19f), y(31f), x(25f), y(27f))
                    }
                    canvas.drawPath(arrow, iconPaint)
                    val head = Path().apply {
                        moveTo(x(22f), y(11f))
                        lineTo(x(27f), y(13f))
                        lineTo(x(23f), y(17f))
                    }
                    canvas.drawPath(head, iconPaint)
                }
                ResultAction.SAVE_TEMPLATE -> {
                    val bookmark = Path().apply {
                        moveTo(x(13f), y(9f))
                        lineTo(x(25f), y(9f))
                        lineTo(x(25f), y(29f))
                        lineTo(x(19f), y(24f))
                        lineTo(x(13f), y(29f))
                        close()
                    }
                    canvas.drawPath(bookmark, iconPaint)
                }
                ResultAction.MAP -> {
                    val pin = Path().apply {
                        moveTo(x(19f), y(30f))
                        cubicTo(x(12f), y(21f), x(10f), y(17f), x(10f), y(14f))
                        cubicTo(x(10f), y(9f), x(14f), y(6f), x(19f), y(6f))
                        cubicTo(x(24f), y(6f), x(28f), y(9f), x(28f), y(14f))
                        cubicTo(x(28f), y(17f), x(26f), y(21f), x(19f), y(30f))
                    }
                    canvas.drawPath(pin, iconPaint)
                    canvas.drawCircle(x(19f), y(14f), x(3.5f), iconPaint)
                }
                ResultAction.SHARE -> {
                    val tray = Path().apply {
                        moveTo(x(10f), y(22f))
                        lineTo(x(10f), y(29f))
                        lineTo(x(28f), y(29f))
                        lineTo(x(28f), y(22f))
                    }
                    canvas.drawPath(tray, iconPaint)
                    canvas.drawLine(x(19f), y(9f), x(19f), y(23f), iconPaint)
                    val head = Path().apply {
                        moveTo(x(19f), y(8f))
                        lineTo(x(13f), y(14f))
                        lineTo(x(17f), y(14f))
                        lineTo(x(17f), y(20f))
                        lineTo(x(21f), y(20f))
                        lineTo(x(21f), y(14f))
                        lineTo(x(25f), y(14f))
                        close()
                    }
                    canvas.drawPath(head, iconPaint)
                }
            }
        }
    }

    private inner class EliminatedHatchView : View(this) {
        private val hatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x994A3A32.toInt()
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
            strokeCap = Paint.Cap.ROUND
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val gap = dp(14).toFloat()
            var startX = -height.toFloat()
            while (startX < width + height) {
                canvas.drawLine(startX, height.toFloat(), startX + height, 0f, hatchPaint)
                startX += gap
            }
        }
    }

    private data class FireworkParticle(
        val originXRatio: Float,
        val originYRatio: Float,
        val angle: Float,
        val speed: Float,
        val color: Int,
        val radius: Float,
        val delayMs: Long
    )

    private inner class DecisionFireworksView : View(this) {
        private val random = Random(System.currentTimeMillis())
        private val colors = intArrayOf(
            0xFFD73B24.toInt(),
            0xFFFFC857.toInt(),
            0xFF3D8B67.toInt(),
            0xFF2F80ED.toInt(),
            0xFFE85D9E.toInt(),
            0xFFFFFFFF.toInt()
        )
        private val particles = buildList {
            addBurst(0.18f, 0.30f, 0L)
            addBurst(0.82f, 0.30f, 90L)
            addBurst(0.28f, 0.18f, 180L)
            addBurst(0.72f, 0.18f, 270L)
            addBurst(0.14f, 0.50f, 380L)
            addBurst(0.86f, 0.50f, 470L)
            addBurst(0.25f, 0.70f, 590L)
            addBurst(0.75f, 0.70f, 680L)
            addBurst(0.50f, 0.14f, 790L)
            addBurst(0.50f, 0.76f, 900L)
        }
        private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val startedAt = System.currentTimeMillis()

        init {
            isClickable = false
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val elapsed = (System.currentTimeMillis() - startedAt) % FIREWORKS_DURATION_MS
            particles.forEach { particle ->
                val age = elapsed - particle.delayMs
                if (age < 0L || age > FIREWORKS_PARTICLE_LIFETIME_MS) return@forEach

                val progress = age.toFloat() / FIREWORKS_PARTICLE_LIFETIME_MS
                val distance = particle.speed * progress
                val gravity = dp(42) * progress * progress
                val originX = width * particle.originXRatio
                val originY = height * particle.originYRatio
                val x = originX + cos(particle.angle) * distance
                val y = originY + sin(particle.angle) * distance + gravity
                val alpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)

                particlePaint.color = particle.color
                particlePaint.alpha = alpha
                trailPaint.color = particle.color
                trailPaint.alpha = (alpha * 0.65f).toInt()
                trailPaint.strokeWidth = particle.radius * 1.2f

                val trail = dp(10) * (1f - progress)
                canvas.drawLine(
                    x - cos(particle.angle) * trail,
                    y - sin(particle.angle) * trail,
                    x,
                    y,
                    trailPaint
                )
                canvas.drawCircle(x, y, particle.radius * (1f + progress * 0.4f), particlePaint)
            }

            if (isAttachedToWindow) {
                postInvalidateOnAnimation()
            }
        }

        private fun MutableList<FireworkParticle>.addBurst(xRatio: Float, yRatio: Float, delayMs: Long) {
            repeat(FIREWORKS_PARTICLES_PER_BURST) { index ->
                val angle = ((PI * 2.0 * index / FIREWORKS_PARTICLES_PER_BURST) + random.nextDouble(-0.16, 0.16)).toFloat()
                add(
                    FireworkParticle(
                        originXRatio = xRatio,
                        originYRatio = yRatio,
                        angle = angle,
                        speed = dp(random.nextInt(96, 186)).toFloat(),
                        color = colors[random.nextInt(colors.size)],
                        radius = dp(random.nextInt(3, 6)).toFloat(),
                        delayMs = delayMs
                    )
                )
            }
        }
    }

    private inner class NavigationThumbnailView(
        private val icon: NavigationIcon,
        private val selected: Boolean
    ) : View(this) {
        private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (selected) 0xFFFFE5D4.toInt() else 0xFFF0F3F0.toInt()
            style = Paint.Style.FILL
        }
        private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (selected) 0xFFD73B24.toInt() else 0xFF526158.toInt()
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val sx = width / 34f
            val sy = height / 32f
            fun x(value: Float) = value * sx
            fun y(value: Float) = value * sy

            canvas.drawRoundRect(
                RectF(x(2f), y(1f), x(32f), y(31f)),
                dp(9).toFloat(),
                dp(9).toFloat(),
                tilePaint
            )
            when (icon) {
                NavigationIcon.HOME -> {
                    val roof = Path().apply {
                        moveTo(x(9f), y(16f))
                        lineTo(x(17f), y(9f))
                        lineTo(x(25f), y(16f))
                    }
                    canvas.drawPath(roof, iconPaint)
                    canvas.drawLine(x(11f), y(15f), x(11f), y(23f), iconPaint)
                    canvas.drawLine(x(23f), y(15f), x(23f), y(23f), iconPaint)
                    canvas.drawLine(x(11f), y(23f), x(23f), y(23f), iconPaint)
                    canvas.drawLine(x(17f), y(19f), x(17f), y(23f), iconPaint)
                }
                NavigationIcon.CREATE -> {
                    canvas.drawRoundRect(RectF(x(10f), y(7f), x(24f), y(25f)), x(2f), y(2f), iconPaint)
                    canvas.drawLine(x(17f), y(12f), x(17f), y(20f), iconPaint)
                    canvas.drawLine(x(13f), y(16f), x(21f), y(16f), iconPaint)
                }
                NavigationIcon.RESULTS -> {
                    canvas.drawLine(x(10f), y(23f), x(25f), y(23f), iconPaint)
                    canvas.drawLine(x(12f), y(23f), x(12f), y(17f), iconPaint)
                    canvas.drawLine(x(17f), y(23f), x(17f), y(11f), iconPaint)
                    canvas.drawLine(x(22f), y(23f), x(22f), y(14f), iconPaint)
                }
                NavigationIcon.SETTINGS -> {
                    canvas.drawLine(x(10f), y(12f), x(24f), y(12f), iconPaint)
                    canvas.drawLine(x(10f), y(20f), x(24f), y(20f), iconPaint)
                    canvas.drawCircle(x(15f), y(12f), x(2f), tilePaint)
                    canvas.drawCircle(x(15f), y(12f), x(2f), iconPaint)
                    canvas.drawCircle(x(20f), y(20f), x(2f), tilePaint)
                    canvas.drawCircle(x(20f), y(20f), x(2f), iconPaint)
                }
            }
        }
    }

    private inner class CountdownRingView(
        private val poll: NearbyPoll,
        private val compact: Boolean = false
    ) : View(this) {
        private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF3E5D0.toInt()
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD73B24.toInt()
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF10251D.toInt()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val stroke = dp(if (compact) 5 else 8).toFloat()
            trackPaint.strokeWidth = stroke
            progressPaint.strokeWidth = stroke
            timePaint.textSize = dp(if (compact) 11 else 17).toFloat()

            val size = width.coerceAtMost(height).toFloat()
            val inset = stroke / 2f + dp(4)
            val bounds = RectF(inset, inset, size - inset, size - inset)
            val totalMillis = (poll.durationSeconds * 1_000L).coerceAtLeast(1L)
            val remainingMillis = (poll.endAtMillis - System.currentTimeMillis()).coerceIn(0L, totalMillis)
            val ratio = remainingMillis.toFloat() / totalMillis.toFloat()
            val isUrgent = remainingMillis <= URGENT_COUNTDOWN_MS

            progressPaint.color = if (isUrgent) 0xFFD33737.toInt() else 0xFFD73B24.toInt()
            timePaint.color = if (isUrgent) 0xFFC82424.toInt() else 0xFF10251D.toInt()

            canvas.drawArc(bounds, -90f, 360f, false, trackPaint)
            canvas.drawArc(bounds, -90f, 360f * ratio, false, progressPaint)
            canvas.drawText(formatRemaining(remainingMillis), width / 2f, height / 2f + dp(5), timePaint)

            if (remainingMillis > 0L) {
                postInvalidateDelayed(1_000L)
            }
        }
    }

    private fun transparentAvatarBitmap(avatarId: Int): Bitmap {
        val normalizedId = avatarId.coerceIn(0, AVATAR_COUNT - 1)
        return transparentAvatarCache.getOrPut(normalizedId) {
            val sourceWidth = avatarSheet.width / AVATAR_COLUMN_COUNT
            val sourceHeight = avatarSheet.height / AVATAR_ROW_COUNT
            val sourceColumn = normalizedId % AVATAR_COLUMN_COUNT
            val sourceRow = normalizedId / AVATAR_COLUMN_COUNT
            val pixels = IntArray(sourceWidth * sourceHeight)
            avatarSheet.getPixels(
                pixels,
                0,
                sourceWidth,
                sourceColumn * sourceWidth,
                sourceRow * sourceHeight,
                sourceWidth,
                sourceHeight
            )
            val background = BooleanArray(pixels.size)
            val visited = BooleanArray(pixels.size)
            val queue = ArrayDeque<Int>()

            fun enqueue(x: Int, y: Int) {
                if (x !in 0 until sourceWidth || y !in 0 until sourceHeight) return
                val index = y * sourceWidth + x
                if (!visited[index] && isAvatarBackgroundPixel(pixels[index])) {
                    visited[index] = true
                    background[index] = true
                    queue.add(index)
                }
            }

            for (x in 0 until sourceWidth) {
                enqueue(x, 0)
                enqueue(x, sourceHeight - 1)
            }
            for (y in 0 until sourceHeight) {
                enqueue(0, y)
                enqueue(sourceWidth - 1, y)
            }
            val seedInset = (sourceWidth.coerceAtMost(sourceHeight) * AVATAR_BACKGROUND_SEED_INSET_RATIO).roundToInt()
            enqueue(seedInset, seedInset)
            enqueue(sourceWidth - seedInset - 1, seedInset)
            enqueue(seedInset, sourceHeight - seedInset - 1)
            enqueue(sourceWidth - seedInset - 1, sourceHeight - seedInset - 1)

            while (queue.isNotEmpty()) {
                val index = queue.removeFirst()
                val x = index % sourceWidth
                val y = index / sourceWidth
                val currentColor = pixels[index]
                fun visit(nx: Int, ny: Int) {
                    if (nx !in 0 until sourceWidth || ny !in 0 until sourceHeight) return
                    val nextIndex = ny * sourceWidth + nx
                    if (visited[nextIndex]) return
                    val nextColor = pixels[nextIndex]
                    if (isAvatarBackgroundPixel(nextColor) && colorDistance(currentColor, nextColor) <= AVATAR_BACKGROUND_COLOR_DISTANCE) {
                        visited[nextIndex] = true
                        background[nextIndex] = true
                        queue.add(nextIndex)
                    }
                }
                visit(x - 1, y)
                visit(x + 1, y)
                visit(x, y - 1)
                visit(x, y + 1)
            }

            pixels.indices.forEach { index ->
                if (background[index]) {
                    pixels[index] = pixels[index] and 0x00FFFFFF
                }
            }
            Bitmap.createBitmap(sourceWidth, sourceHeight, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight)
            }
        }
    }

    private fun isAvatarBackgroundPixel(color: Int): Boolean {
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        val max = maxOf(red, green, blue)
        val min = minOf(red, green, blue)
        val saturation = if (max == 0) 0f else (max - min).toFloat() / max
        val luma = (red * 0.299f) + (green * 0.587f) + (blue * 0.114f)
        return luma >= 142f && saturation <= 0.52f
    }

    private fun colorDistance(first: Int, second: Int): Int {
        val red = ((first shr 16) and 0xFF) - ((second shr 16) and 0xFF)
        val green = ((first shr 8) and 0xFF) - ((second shr 8) and 0xFF)
        val blue = (first and 0xFF) - (second and 0xFF)
        return abs(red) + abs(green) + abs(blue)
    }

    private inner class BabCrossWordmarkView(
        context: Context,
        private val compact: Boolean
    ) : View(context) {
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = 0xFF10251D.toInt()
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            style = Paint.Style.FILL
        }
        private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD73B24.toInt()
            style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val label = "밥크로스"
            textPaint.textSize = dp(if (compact) 21 else 30).toFloat()
            val maxWidth = width.toFloat().coerceAtLeast(1f)
            while (textPaint.measureText(label) > maxWidth && textPaint.textSize > dp(16)) {
                textPaint.textSize -= dp(1).toFloat()
            }
            val baseline = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label, 0f, baseline, textPaint)
            canvas.drawCircle(
                textPaint.measureText(label).coerceAtMost(maxWidth) + dp(5).toFloat(),
                baseline + textPaint.ascent() * 0.32f,
                dp(if (compact) 2 else 3).toFloat(),
                accentPaint
            )
        }
    }

    private inner class AvatarTileView(
        private val avatarId: Int,
        chosen: Boolean = false,
        private val contentInsetDp: Int = 6,
        private val cornerRadiusDp: Int = 12,
        private val clipAsCircle: Boolean = false,
        private val sourceInsetRatio: Float = AVATAR_SOURCE_INSET_RATIO,
        private val transparentBackground: Boolean = false
    ) : View(this) {
        private var isChosen = chosen
        private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val backdropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFF7EE.toInt()
            style = Paint.Style.FILL
        }
        private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD73B24.toInt()
            style = Paint.Style.STROKE
            strokeWidth = dp(3).toFloat()
        }

        fun setChosen(chosen: Boolean) {
            isChosen = chosen
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val normalizedId = avatarId.coerceIn(0, AVATAR_COUNT - 1)
            val bitmap = if (transparentBackground) transparentAvatarBitmap(normalizedId) else avatarSheet
            val sourceWidth = if (transparentBackground) bitmap.width else avatarSheet.width / AVATAR_COLUMN_COUNT
            val sourceHeight = if (transparentBackground) bitmap.height else avatarSheet.height / AVATAR_ROW_COUNT
            val sourceColumn = if (transparentBackground) 0 else normalizedId % AVATAR_COLUMN_COUNT
            val sourceRow = if (transparentBackground) 0 else normalizedId / AVATAR_COLUMN_COUNT
            val sourceInset = (sourceWidth.coerceAtMost(sourceHeight) * sourceInsetRatio)
                .roundToInt()
                .coerceIn(0, sourceWidth.coerceAtMost(sourceHeight) / 3)
            val source = Rect(
                sourceColumn * sourceWidth + sourceInset,
                sourceRow * sourceHeight + sourceInset,
                (sourceColumn + 1) * sourceWidth - sourceInset,
                (sourceRow + 1) * sourceHeight - sourceInset
            )
            val minSide = width.coerceAtMost(height).toFloat()
            val inset = dp(contentInsetDp).toFloat().coerceAtMost(minSide / 4f)
            val edge = (minSide - inset * 2).coerceAtLeast(0f)
            val left = (width - edge) / 2f
            val top = (height - edge) / 2f
            val target = RectF(left, top, left + edge, top + edge)
            val clipPath = Path().apply {
                if (clipAsCircle) {
                    addOval(target, Path.Direction.CW)
                } else {
                    addRoundRect(target, dp(cornerRadiusDp).toFloat(), dp(cornerRadiusDp).toFloat(), Path.Direction.CW)
                }
            }
            val saved = canvas.save()
            canvas.clipPath(clipPath)
            if (!transparentBackground) {
                if (clipAsCircle) {
                    canvas.drawOval(target, backdropPaint)
                } else {
                    canvas.drawRoundRect(
                        target,
                        dp(cornerRadiusDp).toFloat(),
                        dp(cornerRadiusDp).toFloat(),
                        backdropPaint
                    )
                }
            }
            canvas.drawBitmap(bitmap, source, target, tilePaint)
            canvas.restoreToCount(saved)
            if (isChosen) {
                if (clipAsCircle) {
                    canvas.drawOval(target, selectionPaint)
                } else {
                    canvas.drawRoundRect(
                        target,
                        dp(cornerRadiusDp).toFloat(),
                        dp(cornerRadiusDp).toFloat(),
                        selectionPaint
                    )
                }
            }
        }
    }

    private inner class MenuRouletteWheelView(private val labels: List<String>) : View(this) {
        private val wheelBounds = RectF()
        private var wheelRotationDegrees = 0f
        private var selectedIndex: Int? = null
        private var spinning = false
        private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
        }
        private val selectedStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD73B24.toInt()
            style = Paint.Style.STROKE
            strokeWidth = dp(4).toFloat()
        }
        private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x99FFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
            strokeCap = Paint.Cap.ROUND
        }
        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF10251D.toInt()
            textAlign = Paint.Align.CENTER
            textSize = dp(13).toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFF8ED.toInt()
            style = Paint.Style.FILL
        }
        private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD73B24.toInt()
            style = Paint.Style.FILL
        }
        private val colors = listOf(
            0xFFFFD9B5.toInt(),
            0xFFFFF1A8.toInt(),
            0xFFCFEAD8.toInt(),
            0xFFBFD7F2.toInt(),
            0xFFF7C4CF.toInt(),
            0xFFD8C7FF.toInt()
        )

        fun spinTo(index: Int, onEnd: () -> Unit) {
            if (labels.isEmpty()) return
            spinning = true
            selectedIndex = null
            val segment = 360f / labels.size
            val current = wheelRotationDegrees
            val currentNormalized = positiveDegrees(current)
            val desiredNormalized = positiveDegrees(-index * segment - segment / 2f)
            val correction = positiveDegrees(desiredNormalized - currentNormalized)
            val target = current + 360f * ROULETTE_SPIN_REVOLUTIONS + correction
            ValueAnimator.ofFloat(current, target).apply {
                duration = ROULETTE_SPIN_DURATION_MS
                interpolator = DecelerateInterpolator(2.1f)
                addUpdateListener { animator ->
                    wheelRotationDegrees = animator.animatedValue as Float
                    invalidate()
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        spinning = false
                        selectedIndex = index
                        wheelRotationDegrees = positiveDegrees(wheelRotationDegrees)
                        invalidate()
                        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onEnd()
                    }
                })
                start()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (labels.isEmpty()) return
            val size = width.coerceAtMost(height).toFloat()
            val radius = size / 2f - dp(18)
            val centerX = width / 2f
            val centerY = height / 2f + dp(5)
            wheelBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
            val segment = 360f / labels.size
            labels.forEachIndexed { index, label ->
                val startAngle = -90f + wheelRotationDegrees + index * segment
                val centerAngle = startAngle + segment / 2f
                segmentPaint.color = colors[index % colors.size]
                canvas.drawArc(wheelBounds, startAngle, segment, true, segmentPaint)
                canvas.drawArc(wheelBounds, startAngle, segment, true, strokePaint)
                if (!spinning && selectedIndex == index) {
                    canvas.drawArc(
                        RectF(wheelBounds).apply { inset(dp(2).toFloat(), dp(2).toFloat()) },
                        startAngle + 1.5f,
                        segment - 3f,
                        true,
                        selectedStrokePaint
                    )
                }
                drawRouletteTick(canvas, centerX, centerY, radius, startAngle)
                drawRouletteLabel(canvas, label, centerX, centerY, radius, centerAngle, segment)
            }
            canvas.drawCircle(centerX, centerY, dp(30).toFloat(), centerPaint)
            canvas.drawCircle(centerX, centerY, dp(30).toFloat(), strokePaint)
            val centerLabel = selectedIndex?.let { labels.getOrNull(it) }?.takeIf { !spinning } ?: "밥!"
            val centerLabelPaint = Paint(labelPaint).apply {
                textSize = if (centerLabel.length <= 2) dp(13).toFloat() else dp(10).toFloat()
            }
            canvas.drawText(
                centerLabel.take(5),
                centerX,
                centerY - (centerLabelPaint.descent() + centerLabelPaint.ascent()) / 2f,
                centerLabelPaint
            )

            val pointer = Path().apply {
                moveTo(centerX, centerY - radius - dp(4))
                lineTo(centerX - dp(12), centerY - radius - dp(26))
                lineTo(centerX + dp(12), centerY - radius - dp(26))
                close()
            }
            canvas.drawPath(pointer, pointerPaint)
        }

        private fun drawRouletteTick(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, angle: Float) {
            val angleRadians = Math.toRadians(angle.toDouble())
            val outerX = centerX + cos(angleRadians).toFloat() * (radius - dp(4))
            val outerY = centerY + sin(angleRadians).toFloat() * (radius - dp(4))
            val innerX = centerX + cos(angleRadians).toFloat() * (radius - dp(16))
            val innerY = centerY + sin(angleRadians).toFloat() * (radius - dp(16))
            canvas.drawLine(innerX, innerY, outerX, outerY, tickPaint)
        }

        private fun positiveDegrees(value: Float): Float {
            return ((value % 360f) + 360f) % 360f
        }

        private fun drawRouletteLabel(
            canvas: Canvas,
            label: String,
            centerX: Float,
            centerY: Float,
            radius: Float,
            centerAngle: Float,
            segmentDegrees: Float
        ) {
            val labelRadius = radius * 0.62f
            val angleRadians = Math.toRadians(centerAngle.toDouble())
            val x = centerX + cos(angleRadians).toFloat() * labelRadius
            val y = centerY + sin(angleRadians).toFloat() * labelRadius
            val availableWidth = (
                2f * labelRadius * sin(Math.toRadians((segmentDegrees / 2f).toDouble())).toFloat()
            ).coerceAtMost(radius * 0.82f) - dp(10)
            val fittedPaint = Paint(labelPaint)
            while (fittedPaint.measureText(label) > availableWidth && fittedPaint.textSize > dp(10)) {
                fittedPaint.textSize -= dp(1).toFloat()
            }
            val baseline = y - (fittedPaint.descent() + fittedPaint.ascent()) / 2f
            canvas.drawText(label, x, baseline, fittedPaint)
        }
    }

    private inner class ConnectionSignalView(context: Context) : View(context) {
        private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(1).toFloat()
        }
        private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x66FFFFFF
            style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val activeColor = connectionSignalActiveColor()
            val centerX = width / 2f
            val centerY = height / 2f
            val radius = (width.coerceAtMost(height) / 2f - dp(8).toFloat()).coerceAtLeast(dp(10).toFloat())

            haloPaint.color = withAlpha(activeColor, 0x18)
            activePaint.color = activeColor
            ringPaint.color = withAlpha(activeColor, 0x74)

            canvas.drawCircle(centerX, centerY, radius + dp(3).toFloat(), haloPaint)
            canvas.drawCircle(centerX, centerY, radius, activePaint)
            canvas.drawCircle(centerX, centerY, radius + dp(1).toFloat(), ringPaint)
            canvas.drawCircle(centerX - radius * 0.34f, centerY - radius * 0.38f, radius * 0.24f, highlightPaint)
        }

        private fun withAlpha(color: Int, alpha: Int): Int {
            return (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
        }
    }

    private inner class BarcodeView(private val value: String) : View(this) {
        private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF10251D.toInt()
            style = Paint.Style.FILL
        }
        private val guardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD73B24.toInt()
            style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val quietZone = dp(8).toFloat()
            val top = dp(6).toFloat()
            val bottom = height - dp(6).toFloat()
            val availableWidth = (width - quietZone * 2).coerceAtLeast(1f)
            val encoded = hash(value)
            val units = encoded.length * 2 + 6
            val unitWidth = availableWidth / units
            var x = quietZone

            repeat(2) {
                canvas.drawRect(x, top, x + unitWidth, bottom, guardPaint)
                x += unitWidth * 1.5f
            }

            encoded.forEachIndexed { index, char ->
                val nibble = char.digitToIntOrNull(16) ?: 0
                val barUnits = 0.7f + (nibble % 4) * 0.35f
                val barTop = top + if (index % 5 == 0) 0f else dp(5).toFloat()
                canvas.drawRect(x, barTop, x + unitWidth * barUnits, bottom, barPaint)
                x += unitWidth * 2
            }

            repeat(2) {
                canvas.drawRect(x, top, x + unitWidth, bottom, guardPaint)
                x += unitWidth * 1.5f
            }
        }
    }

    private fun formatRemaining(remainingMillis: Long): String {
        val totalSeconds = (remainingMillis / 1_000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else if (minutes > 0) {
            "%d:%02d".format(minutes, seconds)
        } else {
            "$seconds"
        }
    }

    companion object {
        private const val NEARBY_HEARTBEAT_MS = 10_000L
        private const val NEARBY_PULSE_MS = 30_000L
        private const val CONNECTION_SYNC_DELAY_MS = 500L
        private const val KEYBOARD_SCROLL_DELAY_MS = 180L
        private const val COACH_FADE_OUT_MS = 140L
        private const val URGENT_COUNTDOWN_MS = 10_000L
        private const val SELECTION_VIBRATION_MS = 35L
        private const val CODE_INPUT_VIBRATION_MS = 18L
        private const val CODE_INPUT_VIBRATION_AMPLITUDE = 90
        private const val CODE_ERROR_VIBRATION_MS = 55L
        private const val CODE_ERROR_CLEAR_DELAY_MS = 420L
        private const val JOIN_QR_MAX_FUTURE_MS = 24L * 60L * 60L * 1_000L
        private const val JOIN_PENDING_DOT_MS = 450L
        private const val QR_ACCEPT_RESPONSE_RETRY_MS = 700L
        private const val JOIN_TO_CHOICE_GUARD_MS = 520L
        private const val COMPOSE_MENU_SUGGESTION_DELAY_MS = 5_000L
        private const val RECENT_RUNNER_UP_WINDOW_MS = 7L * 24L * 60L * 60L * 1_000L
        private const val HISTORY_REUSE_SUGGESTION_LIMIT = 3
        private const val FIREWORKS_DURATION_MS = 2_200L
        private const val FIREWORKS_PARTICLE_LIFETIME_MS = 1_200L
        private const val FIREWORKS_PARTICLES_PER_BURST = 30
        private const val SHINE_ROTATION_MS = 3_600L
        private const val SHINE_FRAME_MS = 33L
        private const val ROULETTE_SPIN_DURATION_MS = 2_400L
        private const val ROULETTE_RESULT_HOLD_MS = 700L
        private const val ROULETTE_SPIN_REVOLUTIONS = 6
        private const val HOLOGRAM_SWEEP_MS = 4_800L
        private const val RESULT_DECK_COMMIT_RATIO = 0.24f
        private const val SHARE_IMAGE_WIDTH = 1080
        private const val SHARE_IMAGE_HEIGHT = 1440
        private const val SHARE_OUTER_MARGIN = 58f
        private const val SHARE_CARD_LEFT = 112f
        private const val SHARE_CARD_TOP = 120f
        private const val SHARE_CARD_BOTTOM = 126f
        private const val SHARE_SAFE_LEFT = 168f
        private const val SHARE_SAFE_WIDTH = SHARE_IMAGE_WIDTH - SHARE_SAFE_LEFT * 2f
        private const val SHARE_MAX_VISIBLE_CANDIDATES = 4
        private const val MAX_SHARED_RESULT_CACHE_FILES = 6
        private const val SHARE_TEXT_WRAP_GUARD_LINES = 20
        private const val AVATAR_COLUMN_COUNT = 5
        private const val AVATAR_ROW_COUNT = 4
        private const val AVATAR_COUNT = AVATAR_COLUMN_COUNT * AVATAR_ROW_COUNT
        private const val AVATAR_CARD_SIZE = 56
        private const val AVATAR_SOURCE_INSET_RATIO = 0.13f
        private const val AVATAR_BACKGROUND_SEED_INSET_RATIO = 0.16f
        private const val AVATAR_BACKGROUND_COLOR_DISTANCE = 72
        private const val CUSTOM_DURATION_MIN_SECONDS = 10
        private const val CUSTOM_DURATION_MAX_SECONDS = 8 * 60 * 60
        private const val MAX_OPTION_LENGTH = 30
        private const val MAX_ID_LENGTH = 96
        private const val MAX_MESSAGE_ID_LENGTH = 96
        private const val MAX_NEARBY_PAYLOAD_JSON_LENGTH = 64 * 1024
        private const val MAX_NAME_LENGTH = 40
        private const val MAX_QUESTION_LENGTH = 120
        private const val MAX_POLL_OPTION_COUNT = 20
        private const val MAX_RESULT_VOTES = 100
        private const val POLL_RESPONSE_ACCEPTED = "accepted"
        private const val POLL_RESPONSE_DECLINED = "declined"
        private const val BUTTON_PRIMARY = 1
        private const val BUTTON_OUTLINE = 2
        private const val BUTTON_QUIET = 3
        private const val BUTTON_CHOICE = 4
        private const val CARD_RARITY_COMMON = "common"
        private const val CARD_RARITY_UNCOMMON = "uncommon"
        private const val CARD_RARITY_RARE = "rare"
        private const val CARD_RARITY_LEGENDARY = "legendary"
        private const val CARD_RARITY_PENDING = "pending"
        private val CARD_RARITY_INTRO_SEQUENCE = listOf(
            CARD_RARITY_COMMON,
            CARD_RARITY_UNCOMMON,
            CARD_RARITY_RARE,
            CARD_RARITY_LEGENDARY
        )
        private val CARD_RARITY_WEIGHTS = listOf(
            CARD_RARITY_COMMON to 62,
            CARD_RARITY_UNCOMMON to 25,
            CARD_RARITY_RARE to 10,
            CARD_RARITY_LEGENDARY to 3
        )
        private val CARD_RARITY_WEIGHT_TOTAL = CARD_RARITY_WEIGHTS.sumOf { it.second }
        private val menuRouletteOptionsByCategory = mapOf(
            FoodCategory.MEAL to MenuCatalog.rouletteOptionsFor(FoodCategory.MEAL.name),
            FoodCategory.DISH to MenuCatalog.rouletteOptionsFor(FoodCategory.DISH.name),
            FoodCategory.DRINK to MenuCatalog.rouletteOptionsFor(FoodCategory.DRINK.name),
            FoodCategory.DESSERT to MenuCatalog.rouletteOptionsFor(FoodCategory.DESSERT.name),
            FoodCategory.SNACK to MenuCatalog.rouletteOptionsFor(FoodCategory.SNACK.name)
        )
    }
}
