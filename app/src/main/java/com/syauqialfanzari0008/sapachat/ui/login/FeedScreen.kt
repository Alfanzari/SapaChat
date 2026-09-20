package com.syauqialfanzari0008.sapachat.ui.login

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

// Model Data Postingan
data class FeedPost(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorProfilePic: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val likes: List<String> = emptyList(),
    val commentCount: Int = 0
)

// Model Data Komentar
data class PostComment(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorProfilePic: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

fun formatFeedDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""

    var currentUserName by remember { mutableStateOf("User") }
    var currentUserProfilePic by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var isLoadingFeed by remember { mutableStateOf(true) }

    // State Bottom Sheet Post
    var showCreatePostSheet by remember { mutableStateOf(false) }
    var newPostText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isPublishing by remember { mutableStateOf(false) }

    // State Bottom Sheet Comment
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }
    var comments by remember { mutableStateOf<List<PostComment>>(emptyList()) }
    var newCommentText by remember { mutableStateOf("") }
    var isSendingComment by remember { mutableStateOf(false) }

    val createPostSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            showCreatePostSheet = true
        }
    }

    // Ambil Data Profil
    LaunchedEffect(currentUserId) {
        db.collection("Users").document(currentUserId).get().addOnSuccessListener { doc ->
            val fName = doc.getString("firstName") ?: ""
            val lName = doc.getString("lastName") ?: ""
            val email = doc.getString("email") ?: ""
            currentUserName = if (fName.isNotEmpty()) "$fName $lName".trim() else email.substringBefore("@")
            currentUserProfilePic = doc.getString("profileImageUrl") ?: ""
        }
    }

    // Ambil Feed
    LaunchedEffect(Unit) {
        db.collection("FeedPosts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(FeedPost::class.java)?.copy(id = doc.id)
                    }
                    isLoadingFeed = false
                }
            }
    }

    // Ambil Komentar
    LaunchedEffect(activeCommentPostId) {
        if (activeCommentPostId != null) {
            db.collection("FeedPosts").document(activeCommentPostId!!).collection("Comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        comments = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(PostComment::class.java)?.copy(id = doc.id)
                        }
                    }
                }
        } else {
            comments = emptyList()
        }
    }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToHome) { Icon(Icons.Filled.ChatBubble, contentDescription = "Chats", tint = Color.Gray, modifier = Modifier.size(28.dp)) }
                IconButton(onClick = { }) { Icon(Icons.Filled.ViewAgenda, contentDescription = "Feed", tint = Color.Black, modifier = Modifier.size(28.dp)) }
                IconButton(onClick = onNavigateToSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.Gray, modifier = Modifier.size(28.dp)) }
            }
        },
        // FAB berbentuk kotak hitam ala referensi
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreatePostSheet = true },
                containerColor = Color.Black,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Buat Post")
            }
        },
        containerColor = Color.White // Latar belakang putih bersih
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Header Feed ala referensi (Home, Welcome, dan Ikon Pengaturan)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp).statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Home", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    Text(text = "Welcome $currentUserName \uD83D\uDC4B", fontSize = 14.sp, color = Color.Gray)
                }

                // Ikon bulat di kanan atas
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.size(48.dp).border(1.dp, Color(0xFFE5E5E5), RoundedCornerShape(14.dp))
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Color.Black)
                }
            }

            HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))

            if (isLoadingFeed) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.Black) }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp) // Jarak ekstra agar tidak tertutup FAB
                ) {
                    if (posts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Belum ada feed. Tekan tombol + di bawah!", color = Color.Gray)
                            }
                        }
                    } else {
                        items(posts) { post ->
                            MinimalistPostItem(
                                post = post,
                                currentUserId = currentUserId,
                                onLikeClick = {
                                    val postRef = db.collection("FeedPosts").document(post.id)
                                    if (currentUserId in post.likes) {
                                        postRef.update("likes", FieldValue.arrayRemove(currentUserId))
                                    } else {
                                        postRef.update("likes", FieldValue.arrayUnion(currentUserId))
                                    }
                                },
                                onCommentClick = { activeCommentPostId = post.id },
                                onShareClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, "Lihat feed dari ${post.authorName} di SapaChat: \n\n\"${post.text}\"\n\nAyo gabung sekarang!")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Bagikan via..."))
                                },
                                onDeleteClick = {
                                    db.collection("FeedPosts").document(post.id).delete()
                                        .addOnSuccessListener { Toast.makeText(context, "Postingan dihapus", Toast.LENGTH_SHORT).show() }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- BOTTOM SHEET: BUAT POSTINGAN ---
    if (showCreatePostSheet) {
        ModalBottomSheet(onDismissRequest = { showCreatePostSheet = false }, sheetState = createPostSheetState, containerColor = Color.White) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).fillMaxHeight(0.85f)) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Buat Postingan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Button(
                        onClick = {
                            if (newPostText.isNotBlank() || selectedImageUri != null) {
                                isPublishing = true
                                fun saveToFirestore(imageUrl: String = "") {
                                    val newPost = FeedPost(authorId = currentUserId, authorName = currentUserName, authorProfilePic = currentUserProfilePic, text = newPostText, imageUrl = imageUrl, timestamp = System.currentTimeMillis())
                                    db.collection("FeedPosts").add(newPost).addOnSuccessListener {
                                        isPublishing = false
                                        showCreatePostSheet = false
                                        newPostText = ""
                                        selectedImageUri = null
                                    }
                                }
                                if (selectedImageUri != null) {
                                    com.cloudinary.android.MediaManager.get().upload(selectedImageUri)
                                        .unsigned("ml_default12").callback(object : com.cloudinary.android.callback.UploadCallback {
                                            override fun onStart(requestId: String) {}
                                            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                                            override fun onSuccess(requestId: String, resultData: Map<*, *>) { saveToFirestore(resultData["secure_url"].toString()) }
                                            override fun onError(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) { isPublishing = false }
                                            override fun onReschedule(requestId: String, error: com.cloudinary.android.callback.ErrorInfo) {}
                                        }).dispatch()
                                } else {
                                    saveToFirestore()
                                }
                            }
                        },
                        enabled = !isPublishing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        if (isPublishing) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        else Text("Post", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                }

                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newPostText,
                    onValueChange = { newPostText = it },
                    placeholder = { Text("Apa yang sedang kamu pikirkan?", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent)
                )

                if (selectedImageUri != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp).padding(vertical = 8.dp)) {
                        AsyncImage(model = selectedImageUri, contentDescription = "Selected", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                        IconButton(onClick = { selectedImageUri = null }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    modifier = Modifier.fillMaxWidth().clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Image, contentDescription = "Photo", tint = Color.Black, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Tambahkan Foto dari Galeri", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // --- BOTTOM SHEET: KOMENTAR ---
    if (activeCommentPostId != null) {
        ModalBottomSheet(onDismissRequest = { activeCommentPostId = null }, sheetState = commentSheetState, containerColor = Color.White) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
                Text("Komentar", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
                HorizontalDivider(color = Color(0xFFF0F0F0))

                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 24.dp)) {
                    items(comments) { comment ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE0E0E0)), contentAlignment = Alignment.Center) {
                                if (comment.authorProfilePic.isNotEmpty()) AsyncImage(model = comment.authorProfilePic, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Text(comment.authorName.take(1).uppercase(), color = Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(comment.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(formatFeedDate(comment.timestamp), fontSize = 10.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(comment.text, fontSize = 14.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }

                Surface(color = Color.White, shadowElevation = 8.dp) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCommentText, onValueChange = { newCommentText = it },
                            placeholder = { Text("Tulis komentar...", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.LightGray, unfocusedBorderColor = Color(0xFFEEEEEE))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newCommentText.isNotBlank()) {
                                    isSendingComment = true
                                    val newComment = PostComment(authorId = currentUserId, authorName = currentUserName, authorProfilePic = currentUserProfilePic, text = newCommentText, timestamp = System.currentTimeMillis())
                                    val postRef = db.collection("FeedPosts").document(activeCommentPostId!!)
                                    postRef.collection("Comments").add(newComment).addOnSuccessListener {
                                        postRef.update("commentCount", FieldValue.increment(1))
                                        newCommentText = ""
                                        isSendingComment = false
                                    }
                                }
                            },
                            modifier = Modifier.background(Color.Black, CircleShape),
                            enabled = !isSendingComment
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// Kartu Postingan Minimalis ala Referensi (Outlined, Text Overlay pada Gambar)
@Composable
fun MinimalistPostItem(post: FeedPost, currentUserId: String, onLikeClick: () -> Unit, onCommentClick: () -> Unit, onShareClick: () -> Unit, onDeleteClick: () -> Unit) {
    val isLikedByMe = currentUserId in post.likes
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0)) // Border luar tipis
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header (Avatar dengan titik merah/hijau, Nama, Info waktu)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE0E0E0)), contentAlignment = Alignment.Center) {
                        if (post.authorProfilePic.isNotEmpty()) {
                            AsyncImage(model = post.authorProfilePic, contentDescription = "Profile", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Text(post.authorName.take(1).uppercase(), color = Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    // Titik indikator ala referensi
                    Box(modifier = Modifier.size(10.dp).align(Alignment.BottomEnd).background(Color(0xFFE57373), CircleShape).border(1.dp, Color.White, CircleShape))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = post.authorName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = formatFeedDate(post.timestamp), color = Color.Gray, fontSize = 11.sp)
                }

                if (post.authorId == currentUserId) {
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.Gray)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = Color.White) {
                            DropdownMenuItem(text = { Text("Hapus Postingan", color = Color.Red) }, onClick = { showMenu = false; onDeleteClick() })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body: Jika ada gambar, tampilkan gambar dengan teks yang di-overlay di bawahnya
            if (post.imageUrl.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp))) {
                    AsyncImage(
                        model = post.imageUrl, contentDescription = "Post Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradien hitam transparan dari bawah agar teks terbaca
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 300f
                            )
                        )
                    )
                    // Teks Postingan di-overlay di atas gambar (maks 3 baris)
                    if (post.text.isNotBlank()) {
                        Text(
                            text = post.text,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                        )
                    }
                }
            } else {
                // Jika tidak ada gambar, teks tampil biasa
                if (post.text.isNotBlank()) {
                    Text(text = post.text, fontSize = 15.sp, color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer: Ikon bergaris (Outlined) ala referensi
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onLikeClick() }.padding(end = 16.dp)) {
                    Icon(imageVector = if (isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Like", tint = if (isLikedByMe) Color(0xFFFF5252) else Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = post.likes.size.toString(), color = Color.Gray, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onCommentClick() }.padding(end = 16.dp)) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = post.commentCount.toString(), color = Color.Gray, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Ikon Share di pojok kanan bawah
                IconButton(onClick = onShareClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.Send, contentDescription = "Share", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}