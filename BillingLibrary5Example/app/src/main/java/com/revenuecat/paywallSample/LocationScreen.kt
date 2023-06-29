package com.revenuecat.paywallSample

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

val revenueCatSecondaryColor = Color(android.graphics.Color.parseColor("#576cdb"))

@SuppressLint("MissingPermission")
@Composable
fun LocationOfferScreen(onClick: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var location by remember { mutableStateOf<Location?>(null) }
    val conference = Location(LocationManager.GPS_PROVIDER).apply {
        latitude = 52.500182
        longitude = 13.271062
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestLocation(coroutineScope, context) {
                location = it
            }
        }
    }

    Column {
        if (location == null) {
            Button(modifier = Modifier
                .border(
                    2.dp, revenueCatSecondaryColor, RoundedCornerShape(8.dp)
                )
                .shadow(0.dp)
                .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(14.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.White, contentColor = revenueCatSecondaryColor
                ),
                onClick = {
                    if (ActivityCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        requestLocation(coroutineScope, context) {
                            location = it
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }) {
                Text("Are you at droidcon Berlin? 👀")
            }
        }

        location?.let {
            if (it.distanceTo(conference) < 1000) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(revenueCatSecondaryColor, RoundedCornerShape(8.dp))
                    .clickable { onClick() }
                    .padding(16.dp),
                    contentAlignment = Alignment.CenterStart) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.droidconsf),
                            contentDescription = "logo",
                            modifier = Modifier
                                .height(100.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Column {
                            Text(
                                text = "You're at droidcon SF!",
                                style = MaterialTheme.typography.h6,
                                textAlign = TextAlign.Start,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Get your special 50% off discount for the first year.",
                                style = MaterialTheme.typography.body1,
                                textAlign = TextAlign.Start,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun requestLocation(
    scope: CoroutineScope, context: Context, callback: (Location?) -> Unit
) {
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    scope.launch {
        try {
            val location: Location? = fusedLocationClient.lastLocation.await()
            callback(location)
        } catch (e: Exception) {
            callback(null)
        }
    }
}
