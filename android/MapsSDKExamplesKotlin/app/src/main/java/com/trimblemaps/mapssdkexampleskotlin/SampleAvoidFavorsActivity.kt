package com.trimblemaps.mapssdkexampleskotlin

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.trimblemaps.account.TrimbleMapsAccountManager
import com.trimblemaps.api.avoidFavors.v1.AssetAvoidFavors
import com.trimblemaps.api.avoidFavors.v1.TrimbleMapsAvoidFavors
import com.trimblemaps.api.avoidFavors.v1.models.AFSetsResponse
import com.trimblemaps.api.avoidFavors.v1.models.AssetAvoidFavorsResponse
import com.trimblemaps.core.EnvironmentCriteria
import com.trimblemaps.mapsdk.TrimbleMaps
import com.trimblemaps.mapsdk.camera.CameraPosition
import com.trimblemaps.mapsdk.geometry.LatLng
import com.trimblemaps.mapsdk.maps.MapView
import com.trimblemaps.mapsdk.maps.Style
import com.trimblemaps.mapsdk.maps.TrimbleMapsMap
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SampleAvoidFavorsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize TrimbleMaps
        TrimbleMaps.getInstance(this)

        setContent {
            MaterialTheme {
                AvoidFavorsScreen()
            }
        }
    }
}

@Composable
fun AvoidFavorsScreen() {
    var map by remember { mutableStateOf<TrimbleMapsMap?>(null) }
    var allSetIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    var selectedSetIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var assetSetIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isDataLoaded by remember { mutableStateOf(false) }
    var isLayerVisible by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // Load set IDs on first composition
    LaunchedEffect(Unit) {
        val avoidFavors = TrimbleMapsAvoidFavors.builder()
            .pageNumber(0)
            .pageSize(25)
            .build()

        avoidFavors.enqueueCall(object : Callback<AFSetsResponse> {
            override fun onResponse(
                call: Call<AFSetsResponse?>,
                response: Response<AFSetsResponse?>
            ) {
                var ids = arrayListOf<Int>()
                if (response.body() != null) {
                    for (set in response.body()!!.afSets()) {
                        ids.add(set.setID())
                    }
                }
                allSetIds = ids
                isDataLoaded = true
                Log.d("AvoidFavors", "Loaded ${ids.size} set IDs: $ids")
            }

            override fun onFailure(
                call: Call<AFSetsResponse?>,
                t: Throwable
            ) {
                Log.e("AvoidFavors", "Failed to load set IDs", t)
                isDataLoaded = false
                errorMessage = "Failed to load set IDs: ${t.message}"
            }

        })

        if (TrimbleMapsAccountManager.getAccount()!!.assetId() != null) {
            val assetAvoidFavors = AssetAvoidFavors.builder()
                .vehicleId(TrimbleMapsAccountManager.getAccount()!!.assetId()!!)
                .build()

            assetAvoidFavors.enqueueCall(object : Callback<AssetAvoidFavorsResponse> {
                override fun onResponse(
                    call: Call<AssetAvoidFavorsResponse?>,
                    response: Response<AssetAvoidFavorsResponse?>
                ) {
                    var ids = arrayListOf<Int>()
                    if (response.body() != null) {
                        for (set in response.body()!!.data()) {
                            ids.add(set.setId())
                        }
                    }
                    assetSetIds = ids
                    Log.d("AvoidFavors", "Loaded ${ids.size} Asset set IDs: $ids")
                }

                override fun onFailure(
                    call: Call<AssetAvoidFavorsResponse?>,
                    t: Throwable
                ) {
                    Log.e("AvoidFavors", "Failed to load asset set IDs", t)
                }
            })
        } else {
            Log.e("AvoidFavors", "Please authenticate with an asset login to retrieve Avoid Favor set IDs for an asset")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map View
        TrimbleMapView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { trimbleMapsMap ->
                map = trimbleMapsMap
                Log.d("AvoidFavors", "Map ready")
            }
        )

        // Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    map?.let { m ->
                        isLayerVisible = !isLayerVisible
                        m.setAvoidFavorsVisibility(isLayerVisible)

                        if (isLayerVisible) {
                            // Apply current filter
                            if (selectedSetIds.isEmpty()) {
                                m.setAvoidFavorsFilter(null)
                            } else {
                                m.setAvoidFavorsFilter(ArrayList(selectedSetIds))
                            }
                        }

                        Log.d("AvoidFavors", "Layer ${if (isLayerVisible) "shown" else "hidden"}")
                    }
                }
            ) {
                Text(if (isLayerVisible) "Hide Avoid/Favors" else "Show Avoid/Favors")
            }

            Button(
                onClick = { showFilterDialog = true },
                enabled = isDataLoaded
            ) {
                Text("Filter by Set ID")
            }

            Button(
                onClick = {
                    map?.let { m ->
                        // Enable AF layer is it's not already visible
                        if (!isLayerVisible) {
                            isLayerVisible = true
                            m.setAvoidFavorsVisibility(true)
                        }

                        // Apply filter to only show AF Sets for the asset
                        if (assetSetIds.isEmpty()) {
                            m.setAvoidFavorsFilter(null)
                            Log.d("AvoidFavors", "No AFs associated with asset, showing all AFs in account")
                        } else {
                            m.setAvoidFavorsFilter(ArrayList(assetSetIds))
                            selectedSetIds = assetSetIds.toSet()
                            Log.d("AvoidFavors", "Showing Asset AFs: ${assetSetIds}")
                        }
                    }
                },
                enabled = isDataLoaded
            ) {
                Text("Filter by Asset AFs")
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            allSetIds = allSetIds,
            selectedSetIds = selectedSetIds,
            onDismiss = { showFilterDialog = false },
            onUpdate = { newSelection ->
                selectedSetIds = newSelection
                showFilterDialog = false

                // Apply filter to map
                map?.let { m ->
                    if (selectedSetIds.isEmpty()) {
                        m.setAvoidFavorsFilter(null)
                        Log.d("AvoidFavors", "Filter cleared - showing all sets")
                    } else {
                        m.setAvoidFavorsFilter(ArrayList(selectedSetIds))
                        Log.d("AvoidFavors", "Filter applied with ${selectedSetIds.size} set IDs: $selectedSetIds")
                    }
                }
            }
        )
    }

    // Error Dialog
    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error Loading Set IDs") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Loading Dialog
    if (!isDataLoaded && errorMessage == null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Loading") },
            text = { Text("Set IDs are loading. Please wait...") },
            confirmButton = { }
        )
    }
}

@Composable
fun TrimbleMapView(
    modifier: Modifier = Modifier,
    onMapReady: (TrimbleMapsMap) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { trimbleMapsMap ->
                val position = CameraPosition.Builder()
                    .target(LatLng(39.987032, -105.105344))
                    .zoom(13.0)
                    .build()
                trimbleMapsMap.cameraPosition = position
                trimbleMapsMap.setStyle(Style.TrimbleMobileStyle.MOBILE_DAY)
                onMapReady(trimbleMapsMap)
            }
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

@Composable
fun FilterDialog(
    allSetIds: List<Int>,
    selectedSetIds: Set<Int>,
    onDismiss: () -> Unit,
    onUpdate: (Set<Int>) -> Unit
) {
    var tempSelection by remember { mutableStateOf(selectedSetIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Set IDs to Display") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                // Select All / Clear All Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { tempSelection = allSetIds.toSet() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Select All")
                    }

                    Button(
                        onClick = { tempSelection = emptySet() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Checkbox List
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(allSetIds) { setId ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = tempSelection.contains(setId),
                                onCheckedChange = { checked ->
                                    tempSelection = if (checked) {
                                        tempSelection + setId
                                    } else {
                                        tempSelection - setId
                                    }
                                }
                            )
                            Text(
                                text = "Set ID: $setId",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onUpdate(tempSelection) }) {
                Text("Update")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}