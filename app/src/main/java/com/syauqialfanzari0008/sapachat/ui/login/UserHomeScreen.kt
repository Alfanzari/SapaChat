package com.syauqialfanzari0008.sapachat.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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


data class ChatUser(
    val uid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val profileImageUrl: String,
    val username: String = ""
) {
    val displayName: String
        get() = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
            "$firstName $lastName".trim()
        } else {
            email.substringBefore("@").replaceFirstChar { it.uppercase() }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(
    onNavigateToFeed: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToChat: (uid: String, email: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    var allUsersList by remember { mutableStateOf<List<ChatUser>>(emptyList()) }
    var myFriendsUids by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val lastMessageTimestamps = remember { mutableStateMapOf<String, Long>() }

    var showNewMessageSheet by remember { mutableStateOf(false) }
    var sheetSearchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""

    DisposableEffect(Unit) {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        if (currentUserId.isNotEmpty()) {
            db.collection("Users").document(currentUserId).update("isOnline", true)

            listener = db.collection("Users").document(currentUserId)
                .addSnapshotListener { snap, _ ->
                    val friends = snap?.get("friends") as? List<String> ?: emptyList()
                    myFriendsUids = friends
                }
        }
        onDispose {
            if (currentUserId.isNotEmpty()) {
                db.collection("Users").document(currentUserId).update("isOnline", false)
            }
            listener?.remove()
        }
    }

    LaunchedEffect(Unit) {
        db.collection("Users").whereEqualTo("role", "User")
            .addSnapshotListener { result, _ ->
                if (result != null) {
                    val fetchedUsers = result.mapNotNull { doc ->
                        val uid = doc.getString("uid") ?: doc.id
                        val email = doc.getString("email") ?: ""
                        if (uid == currentUserId) return@mapNotNull null

                        ChatUser(
                            uid = uid,
                            email = email,
                            firstName = doc.getString("firstName") ?: "",
                            lastName = doc.getString("lastName") ?: "",
                            profileImageUrl = doc.getString("profileImageUrl") ?: "",
                            username = doc.getString("username") ?: ""
                        )
                    }
                    allUsersList = fetchedUsers
                    isLoading = false
                }
            }
    }

    val friendsList = allUsersList
        .filter { it.uid in myFriendsUids }
        .filter { it.displayName.contains(searchQuery, ignoreCase = true) }
        .sortedByDescending { lastMessageTimestamps[it.uid] ?: 0L }

    val suggestedList = allUsersList
        .filter { it.uid !in myFriendsUids }

        .filter { it.displayName.contains(sheetSearchQuery, ignoreCase = true) || it.username.contains(sheetSearchQuery, ignoreCase = true) }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8F8F8))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { }) { Icon(Icons.Filled.ChatBubble, contentDescription = "Chats", tint = Color.Black, modifier = Modifier.size(28.dp)) }
                IconButton(onClick = onNavigateToFeed) { Icon(Icons.Filled.ViewAgenda, contentDescription = "Feed", tint = Color.Gray, modifier = Modifier.size(28.dp)) }
                IconButton(onClick = onNavigateToSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.Gray, modifier = Modifier.size(28.dp)) }
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Messages", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                IconButton(onClick = { showNewMessageSheet = true }) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = "New Message", tint = Color.Gray, modifier = Modifier.size(28.dp))
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search", color = Color.Gray) },
                trailingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.LightGray, unfocusedBorderColor = Color(0xFFEEEEEE),
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.Black) }
            } else if (friendsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada pesan. Ketuk ikon pena di atas untuk memulai!", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(friendsList, key = { it.uid }) { user ->
                        ChatListItem(
                            user = user,
                            currentUserId = currentUserId,
                            onClick = { onNavigateToChat(user.uid, user.email) },
                            onTimestampUpdate = { timestamp ->
                                lastMessageTimestamps[user.uid] = timestamp
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 1.dp, color = Color(0xFFF0F0F0))
                    }
                }
            }
        }
    }

    if (showNewMessageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNewMessageSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).fillMaxHeight(0.85f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "New Message", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(
                        text = "Cancel",
                        fontSize = 16.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.clickable { showNewMessageSheet = false }
                    )
                }

                OutlinedTextField(
                    value = sheetSearchQuery,
                    onValueChange = { sheetSearchQuery = it },
                    placeholder = { Text("Search name or @username...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.LightGray, unfocusedBorderColor = Color(0xFFEEEEEE),
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "SUGGESTED", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(suggestedList) { user ->
                        SuggestedUserItem(user = user) {
                            db.collection("Users").document(currentUserId)
                                .update("friends", FieldValue.arrayUnion(user.uid))
                            db.collection("Users").document(user.uid)
                                .update("friends", FieldValue.arrayUnion(currentUserId))

                            showNewMessageSheet = false
                            onNavigateToChat(user.uid, user.email)
                        }
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFF0F0F0))
                    }
                }
            }
        }
    }
}


@Composable
fun SuggestedUserItem(user: ChatUser, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF00796B)),
            contentAlignment = Alignment.Center
        ) {
            if (user.profileImageUrl.isNotEmpty()) {
                AsyncImage(model = user.profileImageUrl, contentDescription = "Profile Picture", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(text = user.displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(2.dp))


            val subText = if (user.username.isNotEmpty()) "@${user.username}" else user.email
            Text(text = subText, fontSize = 13.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ChatListItem(
    user: ChatUser,
    currentUserId: String,
    onClick: () -> Unit,
    onTimestampUpdate: (Long) -> Unit
) {
    var lastMessageText by remember { mutableStateOf("Tap to start chatting...") }
    var lastMessageTime by remember { mutableStateOf("Now") }
    var isNewMessage by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()
    val roomId = if (currentUserId < user.uid) "$currentUserId-${user.uid}" else "${user.uid}-$currentUserId"

    LaunchedEffect(roomId) {
        db.collection("ChatRooms").document(roomId).collection("Messages")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val doc = snapshot.documents[0]
                    val text = doc.getString("text") ?: ""
                    val imageUrl = doc.getString("imageUrl") ?: ""
                    val audioUrl = doc.getString("audioUrl") ?: ""
                    val senderId = doc.getString("senderId") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: 0L

                    lastMessageText = when {
                        text.isNotBlank() -> text
                        audioUrl.isNotBlank() -> "🎤 Voice Note"
                        imageUrl.isNotBlank() -> "📷 Photo"
                        else -> ""
                    }

                    if (timestamp > 0L) {
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        lastMessageTime = sdf.format(Date(timestamp))
                        onTimestampUpdate(timestamp)
                    }
                    isNewMessage = senderId != currentUserId
                } else {
                    lastMessageText = "Tap to start chatting..."
                    lastMessageTime = "Now"
                    isNewMessage = false
                    onTimestampUpdate(0L)
                }
            }
    }

    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF00796B)),
            contentAlignment = Alignment.Center
        ) {
            if (user.profileImageUrl.isNotEmpty()) {
                AsyncImage(model = user.profileImageUrl, contentDescription = "Profile Picture", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(text = user.displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.displayName, fontWeight = if (isNewMessage) FontWeight.ExtraBold else FontWeight.Bold, fontSize = 16.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = lastMessageText, color = if (isNewMessage) Color.Black else Color.DarkGray, fontWeight = if (isNewMessage) FontWeight.SemiBold else FontWeight.Normal, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (isNewMessage) {
                Box(modifier = Modifier.background(Color(0xFFFF7A45), shape = RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(text = "NEW", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(text = lastMessageTime, color = if (isNewMessage) Color.Black else Color.Gray, fontSize = 12.sp, fontWeight = if (isNewMessage) FontWeight.Bold else FontWeight.Normal)
        }
    }
}