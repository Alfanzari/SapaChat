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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.random.Random


data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val audioUrl: String = "",
    val senderId: String = "",
    val timestamp: Long = 0L,
    val liked: Boolean = false,
    val replyToMessageId: String = "",
    val replyToMessageText: String = "",
    val replyToSenderName: String = ""
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

val ChatBgColor = Color(0xFFF7F7F7)
val SenderBubbleColor = Color(0xFF222222)
val ReceiverBubbleColor = Color.White

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

    var isRecording by remember { mutableStateOf(false) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }

    var heartAnimationTrigger by remember { mutableStateOf(0L) }
    var messageToDelete by remember { mutableStateOf<ChatMessage?>(null) }


    var messageToReply by remember { mutableStateOf<ChatMessage?>(null) }

    var receiverName by remember { mutableStateOf(receiverEmail.substringBefore("@")) }
    var receiverProfileImage by remember { mutableStateOf("") }
    var isReceiverTyping by remember { mutableStateOf(false) }

    var currentUserName by remember { mutableStateOf("Me") }
    var currentUserPic by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val roomId = if (currentUserId < receiverUid) "$currentUserId-$receiverUid" else "$receiverUid-$currentUserId"

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) Toast.makeText(context, "Izin mikrofon diperlukan", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(roomId) {
        db.collection("ChatRooms").document(roomId).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                isReceiverTyping = snapshot.getBoolean("typing_$receiverUid") ?: false
            }
        }
    }

    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty()) {
            db.collection("ChatRooms").document(roomId).set(
                mapOf("typing_$currentUserId" to true), SetOptions.merge()
            )
            delay(3000)
            db.collection("ChatRooms").document(roomId).set(
                mapOf("typing_$currentUserId" to false), SetOptions.merge()
            )
        } else {
            db.collection("ChatRooms").document(roomId).set(
                mapOf("typing_$currentUserId" to false), SetOptions.merge()
            )
        }
    }

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
            Toast.makeText(context, "Gagal merekam", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopAndSendRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false

            if (audioFile != null && audioFile!!.exists()) {
                isUploading = true


                val currentReplyToMsg = messageToReply
                messageToReply = null

                com.cloudinary.android.MediaManager.get().upload(audioFile!!.absolutePath)
                    .unsigned("ml_default12")
                    .option("resource_type", "auto")
                    .callback(object : com.cloudinary.android.callback.UploadCallback {
                        override fun onStart(requestId: String) {}
                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val downloadUrl = resultData["secure_url"].toString()
                            val newMessage = ChatMessage(
                                audioUrl = downloadUrl,
                                senderId = currentUserId,
                                timestamp = System.currentTimeMillis(),
                                replyToMessageId = currentReplyToMsg?.id ?: "",
                                replyToMessageText = currentReplyToMsg?.text?.takeIf { it.isNotBlank() } ?: if (currentReplyToMsg?.imageUrl?.isNotEmpty() == true) "Photo" else if (currentReplyToMsg?.audioUrl?.isNotEmpty() == true) "Voice Note" else "",
                                replyToSenderName = if (currentReplyToMsg != null) (if (currentReplyToMsg.senderId == currentUserId) currentUserName else receiverName) else ""
                            )
                            db.collection("ChatRooms").document(roomId).collection("Messages").add(newMessage)
                            isUploading = false
                            audioFile?.delete()
                        }
                        override fun onError(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) { isUploading = false }
                        override fun onReschedule(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {}
                    }).dispatch()
            }
        } catch (e: Exception) {
            isRecording = false
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploading = true


            val currentReplyToMsg = messageToReply
            messageToReply = null

            com.cloudinary.android.MediaManager.get().upload(uri)
                .unsigned("ml_default12")
                .callback(object : com.cloudinary.android.callback.UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val downloadUrl = resultData["secure_url"].toString()
                        val newMessage = ChatMessage(
                            imageUrl = downloadUrl,
                            senderId = currentUserId,
                            timestamp = System.currentTimeMillis(),
                            replyToMessageId = currentReplyToMsg?.id ?: "",
                            replyToMessageText = currentReplyToMsg?.text?.takeIf { it.isNotBlank() } ?: if (currentReplyToMsg?.imageUrl?.isNotEmpty() == true) "Photo" else if (currentReplyToMsg?.audioUrl?.isNotEmpty() == true) "Voice Note" else "",
                            replyToSenderName = if (currentReplyToMsg != null) (if (currentReplyToMsg.senderId == currentUserId) currentUserName else receiverName) else ""
                        )
                        db.collection("ChatRooms").document(roomId).collection("Messages").add(newMessage)
                        isUploading = false
                    }
                    override fun onError(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) { isUploading = false }
                    override fun onReschedule(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {}
                }).dispatch()
        }
    }

    LaunchedEffect(receiverUid) {
        db.collection("Users").document(receiverUid).addSnapshotListener { doc, _ ->
            if (doc != null && doc.exists()) {
                val fName = doc.getString("firstName") ?: ""
                val lName = doc.getString("lastName") ?: ""
                if (fName.isNotEmpty() || lName.isNotEmpty()) receiverName = "$fName $lName".trim()
                receiverProfileImage = doc.getString("profileImageUrl") ?: ""
            }
        }
    }

    LaunchedEffect(currentUserId) {
        db.collection("Users").document(currentUserId).get().addOnSuccessListener { doc ->
            val fName = doc.getString("firstName") ?: ""
            currentUserName = if (fName.isNotEmpty()) fName else "Me"
            currentUserPic = doc.getString("profileImageUrl") ?: ""
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

    Box(modifier = Modifier.fillMaxSize().background(ChatBgColor)) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black, modifier = Modifier.size(20.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = receiverName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                    if (isReceiverTyping) {
                        Text(text = "sedang mengetik...", fontSize = 12.sp, color = Color(0xFF00C853), fontWeight = FontWeight.Medium)
                    }
                }

                Box(modifier = Modifier.size(40.dp))
            }


            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                groupedMessages.forEach { (dateString, messagesForDate) ->
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Surface(color = Color.White, shape = RoundedCornerShape(24.dp)) {
                                Text(dateString, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Black, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                            }
                        }
                    }
                    items(messagesForDate) { message ->
                        val isMine = message.senderId == currentUserId
                        MessageBubbleUI(
                            message = message, isMine = isMine,
                            profilePic = if (isMine) currentUserPic else receiverProfileImage,
                            senderName = if (isMine) currentUserName else receiverName,
                            roomId = roomId,
                            onLikeClick = { isNowLiked ->
                                if (message.id.isNotEmpty()) {
                                    db.collection("ChatRooms").document(roomId).collection("Messages").document(message.id).update("liked", isNowLiked)
                                    if (isNowLiked) heartAnimationTrigger = System.currentTimeMillis()
                                }
                            },
                            onDeleteClick = { msg ->
                                messageToDelete = msg
                            },
                            onReplyClick = { msg ->
                                messageToReply = msg
                            }
                        )
                    }
                }
            }


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {

                if (messageToReply != null) {
                    Surface(
                        color = Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (messageToReply!!.senderId == currentUserId) currentUserName else receiverName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = messageToReply!!.text.takeIf { it.isNotBlank() } ?: if (messageToReply!!.imageUrl.isNotEmpty()) "Photo" else "Voice Note",
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    color = Color.DarkGray
                                )
                            }
                            IconButton(onClick = { messageToReply = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Reply", tint = Color.Black)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 12.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.padding(end = 4.dp), enabled = !isRecording
                        ) { Icon(imageVector = Icons.Default.Add, contentDescription = "Send Image", tint = Color.Gray) }
                    }

                    Surface(
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(
                            topStart = if (messageToReply != null) 0.dp else 50.dp,
                            topEnd = if (messageToReply != null) 0.dp else 50.dp,
                            bottomStart = 50.dp,
                            bottomEnd = 50.dp
                        ),
                        color = Color.White
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp)) {
                            BasicTextField(
                                value = messageText, onValueChange = { messageText = it },
                                textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                                cursorBrush = SolidColor(Color.Black),
                                modifier = Modifier.weight(1f), enabled = !isRecording,
                                decorationBox = { innerTextField ->
                                    if (messageText.isEmpty()) {
                                        Text(if (isRecording) "Recording..." else "Ask anything here..", color = if (isRecording) Color.Red else Color.Gray, fontSize = 16.sp)
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(56.dp).clip(CircleShape)
                            .background(if (isRecording) Color.Red else SenderBubbleColor)
                            .clickable {
                                if (messageText.isNotBlank()) {
                                    val newMessage = ChatMessage(
                                        text = messageText,
                                        senderId = currentUserId,
                                        timestamp = System.currentTimeMillis(),
                                        replyToMessageId = messageToReply?.id ?: "",
                                        replyToMessageText = messageToReply?.text?.takeIf { it.isNotBlank() } ?: if (messageToReply?.imageUrl?.isNotEmpty() == true) "Photo" else if (messageToReply?.audioUrl?.isNotEmpty() == true) "Voice Note" else "",
                                        replyToSenderName = if (messageToReply != null) (if (messageToReply!!.senderId == currentUserId) currentUserName else receiverName) else ""
                                    )
                                    db.collection("ChatRooms").document(roomId).collection("Messages").add(newMessage)

                                    messageText = ""
                                    messageToReply = null
                                    db.collection("ChatRooms").document(roomId).set(
                                        mapOf("typing_$currentUserId" to false), SetOptions.merge()
                                    )
                                } else {
                                    val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (!hasMicPermission) audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    else if (isRecording) stopAndSendRecording() else startRecording()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (messageText.isNotBlank()) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = "Mic", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }

        if (heartAnimationTrigger > 0) FloatingHeartsOverlay(key = heartAnimationTrigger)


        if (messageToDelete != null) {
            AlertDialog(
                onDismissRequest = { messageToDelete = null },
                title = { Text("Tarik Pesan?", fontWeight = FontWeight.Bold) },
                text = { Text("Pesan ini akan dihapus untuk Anda dan lawan bicara. Lanjutkan?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val msgId = messageToDelete!!.id
                            if (msgId.isNotEmpty()) {
                                db.collection("ChatRooms").document(roomId).collection("Messages").document(msgId).delete()
                            }
                            messageToDelete = null
                        }
                    ) {
                        Text("Tarik", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { messageToDelete = null }) { Text("Batal", color = Color.Black) }
                },
                containerColor = Color.White
            )
        }
    }
}

@Composable
fun MessageBubbleUI(
    message: ChatMessage, isMine: Boolean, profilePic: String,
    senderName: String, roomId: String,
    onLikeClick: (Boolean) -> Unit,
    onDeleteClick: (ChatMessage) -> Unit,
    onReplyClick: (ChatMessage) -> Unit
) {
    var localIsLiked by remember(message.liked) { mutableStateOf(message.liked) }
    val timeString = remember(message.timestamp) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)) }
    val textColor = if (isMine) Color.White else Color.Black


    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, animationSpec = tween(150), label = "drag")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > 80f) {
                            onReplyClick(message)
                        }
                        offsetX = 0f
                    },
                    onDragCancel = { offsetX = 0f }
                ) { change, dragAmount ->
                    change.consume()

                    offsetX = (offsetX + dragAmount).coerceIn(0f, 150f)
                }
            },
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
            if (!isMine) {
                ProfilePicSmall(url = profilePic, initial = senderName)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = senderName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            } else {
                Text(text = senderName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                ProfilePicSmall(url = profilePic, initial = senderName)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isMine) {
                Icon(
                    imageVector = Icons.Filled.Favorite, contentDescription = "Like",
                    tint = if (localIsLiked) Color(0xFFFF5252) else Color(0xFFE0E0E0),
                    modifier = Modifier.size(20.dp).padding(end = 6.dp).clickable { localIsLiked = !localIsLiked; onLikeClick(localIsLiked) }
                )
            }

            Surface(
                color = if (isMine) SenderBubbleColor else ReceiverBubbleColor,
                shape = RoundedCornerShape(
                    topStart = 20.dp, topEnd = 20.dp,
                    bottomStart = if (isMine) 20.dp else 4.dp, bottomEnd = if (isMine) 4.dp else 20.dp
                ),
                shadowElevation = if (isMine) 0.dp else 1.dp,
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                if (isMine) {
                                    onDeleteClick(message)
                                }
                            }
                        )
                    }
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {


                    if (message.replyToMessageText.isNotEmpty()) {
                        Surface(
                            color = if (isMine) Color(0xFF444444) else Color(0xFFF0F0F0),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = message.replyToSenderName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isMine) Color.White else Color.Black
                                )
                                Text(
                                    text = message.replyToMessageText,
                                    fontSize = 11.sp,
                                    color = if (isMine) Color.LightGray else Color.DarkGray,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (message.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = message.imageUrl, contentDescription = "Image",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        if (message.text.isNotBlank() || message.audioUrl.isNotBlank()) Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (message.audioUrl.isNotEmpty()) {
                        var isPlaying by remember { mutableStateOf(false) }
                        val mediaPlayer = remember { MediaPlayer() }
                        DisposableEffect(Unit) { onDispose { if (mediaPlayer.isPlaying) mediaPlayer.stop(); mediaPlayer.release() } }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                            IconButton(
                                onClick = {
                                    if (isPlaying) { mediaPlayer.pause(); isPlaying = false }
                                    else {
                                        try {
                                            mediaPlayer.reset(); mediaPlayer.setDataSource(message.audioUrl); mediaPlayer.prepareAsync()
                                            mediaPlayer.setOnPreparedListener { it.start(); isPlaying = true }
                                            mediaPlayer.setOnCompletionListener { isPlaying = false }
                                        } catch (e: Exception) { e.printStackTrace() }
                                    }
                                },
                                modifier = Modifier.size(36.dp).background(if (isMine) Color.White else Color.Black, CircleShape)
                            ) {
                                Icon(imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play/Pause", tint = if (isMine) Color.Black else Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Voice Note", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    if (message.text.isNotBlank()) {
                        Text(text = message.text, color = textColor, fontSize = 15.sp, lineHeight = 22.sp)
                    }
                }
            }

            if (!isMine) {
                Icon(
                    imageVector = Icons.Filled.Favorite, contentDescription = "Like",
                    tint = if (localIsLiked) Color(0xFFFF5252) else Color(0xFFE0E0E0),
                    modifier = Modifier.size(20.dp).padding(start = 6.dp).clickable { localIsLiked = !localIsLiked; onLikeClick(localIsLiked) }
                )
            }
        }
        Text(text = timeString, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp))
    }
}

@Composable
fun ProfilePicSmall(url: String, initial: String) {
    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.DarkGray), contentAlignment = Alignment.Center) {
        if (url.isNotEmpty()) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(initial.take(1).uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

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
            imageVector = Icons.Filled.Favorite, contentDescription = null, tint = Color(0xFFFF5252),
            modifier = Modifier.offset(x = offsetX.value.dp, y = offsetY.value.dp).scale(scale.value).alpha(alpha.value).size(40.dp)
        )
    }
}