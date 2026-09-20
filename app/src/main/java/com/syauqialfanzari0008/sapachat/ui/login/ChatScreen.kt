package com.syauqialfanzari0008.sapachat.ui.login

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

// Model Data Pesan - Ditambahkan 'audioUrl'
data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val audioUrl: String = "", // <-- Variabel baru untuk menampung link Voice Note
    val senderId: String = "",
    val timestamp: Long = 0L,
    val liked: Boolean = false
)

fun formatMessageDate(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val todayYear = calendar.get(Calendar.YEAR)
    val todayDay = calendar.get(Calendar.DAY_OF_YEAR)

    calendar.timeInMillis = timestamp
    val msgYear = calendar.get(Calendar.YEAR)
    val msgDay = calendar.get(Calendar.DAY_OF_YEAR)

    return when {
        todayYear == msgYear && todayDay == msgDay -> "Today"
        todayYear == msgYear && todayDay - msgDay == 1 -> "Yesterday"
        else -> {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    receiverUid: String,
    receiverEmail: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""

    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }

    // State Perekam Suara (Voice Note)
    var isRecording by remember { mutableStateOf(false) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }

    var heartAnimationTrigger by remember { mutableStateOf(0L) }
    var receiverName by remember { mutableStateOf(receiverEmail.substringBefore("@")) }
    var receiverProfileImage by remember { mutableStateOf("") }
    var receiverIsOnline by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val roomId = if (currentUserId < receiverUid) "$currentUserId-$receiverUid" else "$receiverUid-$currentUserId"

    // Peluncur Izin Mikrofon
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Izin mikrofon diperlukan untuk Voice Note", Toast.LENGTH_SHORT).show()
        }
    }

    // Fungsi Memulai Rekaman
    fun startRecording() {
        try {
            val file = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
            audioFile = file
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            isRecording = true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal merekam audio", Toast.LENGTH_SHORT).show()
        }
    }

    // Fungsi Berhenti & Mengirim Rekaman
    fun stopAndSendRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false

            if (audioFile != null && audioFile!!.exists()) {
                isUploading = true
                com.cloudinary.android.MediaManager.get().upload(audioFile!!.absolutePath)
                    .unsigned("ml_default12")
                    .option("resource_type", "auto") // Otomatis mendeteksi file audio
                    .callback(object : com.cloudinary.android.callback.UploadCallback {
                        override fun onStart(requestId: String) {}
                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val downloadUrl = resultData["secure_url"].toString()
                            val newMessage = ChatMessage(audioUrl = downloadUrl, senderId = currentUserId, timestamp = System.currentTimeMillis())
                            db.collection("ChatRooms").document(roomId).collection("Messages").add(newMessage)
                            isUploading = false
                            audioFile?.delete() // Hapus file lokal setelah terkirim
                        }
                        override fun onError(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {
                            isUploading = false
                            Toast.makeText(context, "Gagal mengirim voice note", Toast.LENGTH_SHORT).show()
                        }
                        override fun onReschedule(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {}
                    }).dispatch()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploading = true
            com.cloudinary.android.MediaManager.get().upload(uri)
                .unsigned("ml_default12")
                .callback(object : com.cloudinary.android.callback.UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val downloadUrl = resultData["secure_url"].toString()
                        val newMessage = ChatMessage(imageUrl = downloadUrl, senderId = currentUserId, timestamp = System.currentTimeMillis())
                        db.collection("ChatRooms").document(roomId).collection("Messages").add(newMessage)
                        isUploading = false
                    }
                    override fun onError(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) { isUploading = false }
                    override fun onReschedule(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {}
                }).dispatch()
        }
    }

    LaunchedEffect(receiverUid) {
        db.collection("Users").document(receiverUid)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val fName = doc.getString("firstName") ?: ""
                    val lName = doc.getString("lastName") ?: ""
                    val pfp = doc.getString("profileImageUrl") ?: ""
                    val onlineStatus = doc.getBoolean("isOnline") ?: false

                    if (fName.isNotEmpty() || lName.isNotEmpty()) receiverName = "$fName $lName".trim()
                    receiverProfileImage = pfp
                    receiverIsOnline = onlineStatus
                }
            }
    }

    LaunchedEffect(roomId) {
        db.collection("ChatRooms").document(roomId).collection("Messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    messages = snapshot.documents.mapNotNull { doc -> doc.toObject(ChatMessage::class.java)?.copy(id = doc.id) }
                }
            }
    }

    val groupedMessages = messages.groupBy { formatMessageDate(it.timestamp) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val totalItems = messages.size + groupedMessages.size
            listState.animateScrollToItem(if (totalItems > 0) totalItems - 1 else 0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                Surface(color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp).statusBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Gray) }
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF00796B)), contentAlignment = Alignment.Center) {
                            if (receiverProfileImage.isNotEmpty()) {
                                AsyncImage(model = receiverProfileImage, contentDescription = "Profile", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Text(receiverName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = receiverName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                            Text(
                                text = if (receiverIsOnline) "Online" else "Offline",
                                color = if (receiverIsOnline) Color(0xFF00796B) else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = if (receiverIsOnline) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp).navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 12.dp), color = Color(0xFF90CAF9), strokeWidth = 2.dp)
                        } else {
                            IconButton(
                                onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                modifier = Modifier.padding(end = 4.dp),
                                enabled = !isRecording // Matikan tombol gambar saat merekam
                            ) { Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = "Send Image", tint = Color.Gray) }
                        }
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text(if (isRecording) "Merekam suara... Ketuk setop untuk kirim" else "Type here...", color = if (isRecording) Color.Red else Color.Gray) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(24.dp),
                            enabled = !isRecording, // Matikan ketikan saat merekam
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF5F5F5), unfocusedContainerColor = Color(0xFFF5F5F5),
                                disabledContainerColor = Color(0xFFFFEBEE), disabledBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (messageText.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            val newMessage = ChatMessage(text = messageText, senderId = currentUserId, timestamp = System.currentTimeMillis())
                                            db.collection("ChatRooms").document(roomId).collection("Messages").add(newMessage)
                                            messageText = ""
                                        }
                                    ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFF90CAF9)) }
                                } else {
                                    // Tombol Mic / Berhenti Rekam
                                    IconButton(
                                        onClick = {
                                            val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                            if (!hasMicPermission) {
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                if (isRecording) stopAndSendRecording() else startRecording()
                                            }
                                        }
                                    ) {
                                        if (isRecording) {
                                            Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = Color.Red)
                                        } else {
                                            Icon(Icons.Filled.Mic, contentDescription = "Mic", tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            },
            containerColor = Color(0xFFFAFAFA)
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                groupedMessages.forEach { (dateString, messagesForDate) ->
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text(text = dateString, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        }
                    }

                    items(messagesForDate) { message ->
                        MessageBubble(
                            message = message,
                            currentUserId = currentUserId,
                            onLikeClick = { isNowLiked ->
                                if (message.id.isNotEmpty()) {
                                    db.collection("ChatRooms").document(roomId)
                                        .collection("Messages").document(message.id)
                                        .update("liked", isNowLiked)

                                    if (isNowLiked) heartAnimationTrigger = System.currentTimeMillis()
                                }
                            }
                        )
                    }
                }
            }
        }

        if (heartAnimationTrigger > 0) FloatingHeartsOverlay(key = heartAnimationTrigger)
    }
}

@Composable
fun MessageBubble(message: ChatMessage, currentUserId: String, onLikeClick: (Boolean) -> Unit) {
    val isMine = message.senderId == currentUserId
    var localIsLiked by remember(message.liked) { mutableStateOf(message.liked) }
    val timeString = remember(message.timestamp) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMine) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Like",
                tint = if (localIsLiked) Color(0xFFFF5252) else Color(0xFFE0E0E0),
                modifier = Modifier.size(28.dp).padding(end = 6.dp).clickable {
                    localIsLiked = !localIsLiked
                    onLikeClick(localIsLiked)
                }
            )
        }

        Box(
            modifier = Modifier
                .background(
                    color = if (isMine) Color(0xFF90CAF9) else Color(0xFFEBEBEB),
                    shape = if (isMine) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .widthIn(max = 250.dp)
        ) {
            Column {
                // Render Gambar Jika Ada
                if (message.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Shared Image",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    if (message.text.isNotBlank() || message.audioUrl.isNotBlank()) Spacer(modifier = Modifier.height(6.dp))
                }

                // Render Audio Player Jika Ada
                if (message.audioUrl.isNotEmpty()) {
                    var isPlaying by remember { mutableStateOf(false) }
                    val mediaPlayer = remember { MediaPlayer() }

                    // Bersihkan pemutar saat pesan digeser keluar layar
                    DisposableEffect(Unit) {
                        onDispose {
                            if (mediaPlayer.isPlaying) mediaPlayer.stop()
                            mediaPlayer.release()
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    mediaPlayer.pause()
                                    isPlaying = false
                                } else {
                                    try {
                                        mediaPlayer.reset()
                                        mediaPlayer.setDataSource(message.audioUrl)
                                        mediaPlayer.prepareAsync()
                                        mediaPlayer.setOnPreparedListener {
                                            it.start()
                                            isPlaying = true
                                        }
                                        mediaPlayer.setOnCompletionListener {
                                            isPlaying = false
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            },
                            modifier = Modifier.size(36.dp).background(if (isMine) Color(0xFF1565C0) else Color.Gray, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Voice Note", color = if (isMine) Color(0xFF1565C0) else Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Render Teks
                if (message.text.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message.text, color = Color.Black, fontSize = 15.sp,
                            modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                        )
                        Text(text = timeString, color = if (isMine) Color(0xFF1565C0) else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                } else if (message.imageUrl.isNotEmpty() || message.audioUrl.isNotEmpty()) {
                    Text(
                        text = timeString, color = if (isMine) Color(0xFF1565C0) else Color.Gray,
                        fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
            }
        }

        if (!isMine) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Like",
                tint = if (localIsLiked) Color(0xFFFF5252) else Color(0xFFE0E0E0),
                modifier = Modifier.size(28.dp).padding(start = 6.dp).clickable {
                    localIsLiked = !localIsLiked
                    onLikeClick(localIsLiked)
                }
            )
        }
    }
}

// --- KOMPONEN ANIMASI PERNAK PERNIK LOVE ---
@Composable
fun FloatingHeartsOverlay(key: Long) {
    val hearts = remember(key) { List(10) { it } }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        hearts.forEach { index -> AnimatedHeart(index = index) }
    }
}

@Composable
fun AnimatedHeart(index: Int) {
    val offsetY = remember { Animatable(0f) }
    val offsetX = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val scale = remember { Animatable(0f) }

    val randomX = remember { Random.nextInt(-250, 250).toFloat() }
    val randomY = remember { Random.nextInt(-400, -100).toFloat() }
    val delay = remember { Random.nextInt(0, 200) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        launch { offsetY.animateTo(targetValue = randomY, animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)) }
        launch { offsetX.animateTo(targetValue = randomX, animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)) }
        launch {
            scale.animateTo(targetValue = 1.2f, animationSpec = tween(durationMillis = 300))
            scale.animateTo(targetValue = 0.8f, animationSpec = tween(durationMillis = 700))
        }
        launch {
            delay(500)
            alpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 500))
        }
    }

    if (alpha.value > 0f) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color(0xFFFF5252),
            modifier = Modifier.offset(x = offsetX.value.dp, y = offsetY.value.dp).scale(scale.value).alpha(alpha.value).size(40.dp)
        )
    }
}