package com.smusic.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smusic.app.PlayerViewModel

@Composable
private fun TopBar(title: String, onBack: () -> Unit, onRescan: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = N.Muted)
        }
        Text(
            title,
            Modifier.weight(1f),
            color = N.Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box {
            IconButton(onClick = { open = true }) {
                Icon(Icons.Filled.MoreVert, null, tint = N.Muted)
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                modifier = Modifier.background(Color(0xFF2B2B2B))
            ) {
                DropdownMenuItem(
                    text = { Text("Rescan library", color = N.Text) },
                    onClick = { open = false; onRescan() }
                )
            }
        }
    }
}

@Composable
private fun WheelIcon(icon: ImageVector, modifier: Modifier, tint: Color = N.Text, onClick: () -> Unit) {
    Box(
        modifier.size(52.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp)) }
}

@Composable
private fun ClickWheel(vm: PlayerViewModel) {
    Box(Modifier.size(240.dp).neuRaised(120.dp, 12.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(92.dp).neuInset(46.dp))
        WheelIcon(
            Icons.Filled.Shuffle, Modifier.align(Alignment.TopCenter).padding(top = 22.dp),
            tint = if (vm.shuffle) N.Accent else N.Muted
        ) { vm.toggleShuffle() }
        WheelIcon(Icons.Filled.SkipPrevious, Modifier.align(Alignment.CenterStart).padding(start = 14.dp)) { vm.previous() }
        WheelIcon(Icons.Filled.SkipNext, Modifier.align(Alignment.CenterEnd).padding(end = 14.dp)) { vm.next() }
        WheelIcon(
            if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp)
        ) { vm.togglePlay() }
    }
}

@Composable
fun PlayerScreen(vm: PlayerViewModel, onBack: () -> Unit) {
    val song = vm.currentSong
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar((song?.album ?: "Smusic").uppercase(), onBack) { vm.loadSongs() }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side = (minOf(maxWidth, maxHeight) - 24.dp).coerceIn(120.dp, 300.dp)
            Box(Modifier.size(side).neuRaised(28.dp, 10.dp).padding(8.dp)) {
                AlbumArt(vm, song, Modifier.fillMaxSize(), 22.dp)
            }
        }

        Text(
            song?.title ?: "Nothing playing",
            color = N.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            song?.artist ?: "Pick a song from your library",
            color = N.Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))

        val progress = if (vm.duration > 0) (vm.position.toFloat() / vm.duration).coerceIn(0f, 1f) else 0f
        SeekBar(progress, { vm.seekTo(it) })
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmt(vm.position), color = N.Muted, fontSize = 11.sp)
            Text("-" + fmt(vm.duration - vm.position), color = N.Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(20.dp))
        ClickWheel(vm)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun LibraryScreen(vm: PlayerViewModel, onExit: () -> Unit, onOpenPlayer: () -> Unit) {
    val current = vm.currentSong
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)
    ) {
        TopBar("SMUSIC  •  ${vm.songs.size}", onExit) { vm.loadSongs() }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fav = current != null && current.id in vm.favorites
            NeuButton(46.dp, 16.dp, onClick = { current?.let { vm.toggleFavorite(it.id) } }) {
                Icon(
                    if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null,
                    tint = if (fav) N.Accent else N.Text, modifier = Modifier.size(20.dp)
                )
            }
            Box(
                Modifier.size(112.dp).neuRaised(26.dp, 9.dp).padding(7.dp).clickable(onClick = onOpenPlayer)
            ) { AlbumArt(vm, current, Modifier.fillMaxSize(), 20.dp) }
            NeuButton(46.dp, 16.dp, onClick = { vm.loadSongs() }) {
                Icon(Icons.Filled.Refresh, null, tint = N.Text, modifier = Modifier.size(20.dp))
            }
        }

        if (vm.loaded && vm.songs.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "لا توجد أغاني على الجهاز",
                    color = N.Muted, fontSize = 15.sp, textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(vm.songs, key = { _, s -> s.id }) { i, s ->
                    val isCurrent = s.id == vm.currentId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .then(if (isCurrent) Modifier.neuInset(16.dp) else Modifier)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { vm.play(s); onOpenPlayer() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${i + 1}", color = if (isCurrent) N.Accent else N.Muted,
                            fontSize = 20.sp, fontWeight = FontWeight.Light,
                            modifier = Modifier.width(38.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.title, color = N.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                s.artist, color = N.Muted, fontSize = 11.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(Icons.Filled.MoreVert, null, tint = N.Muted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        if (current != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .neuRaised(22.dp, 7.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onOpenPlayer)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArt(vm, current, Modifier.size(46.dp), 12.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    current.title, Modifier.weight(1f), color = N.Text, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                val fav = current.id in vm.favorites
                IconButton(onClick = { vm.toggleFavorite(current.id) }) {
                    Icon(
                        if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null,
                        tint = if (fav) N.Accent else N.Text
                    )
                }
                IconButton(onClick = { vm.togglePlay() }) {
                    Icon(if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = N.Text)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
