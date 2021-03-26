package com.example.mapsindoorsgettingstartedkotlin

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.textfield.TextInputEditText
import com.mapsindoors.mapssdk.*
import com.mapsindoors.mapssdk.errors.MIError


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnRouteResultListener {

    private lateinit var mMap: GoogleMap
    private lateinit var mapView: View
    private lateinit var mMapControl: MapControl
    private lateinit var mSearchFragment: SearchFragment
    private lateinit var mNavigationFragment: NavigationFragment
    private lateinit var btmnSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var mSearchTxtField: TextInputEditText
    private var currentFragment: Fragment? = null
    private val mUserLocation: Point = Point(38.897389429704695, -77.03740973527613, 0.0)

    private var mpDirectionsRenderer: MPDirectionsRenderer? = null
    private var mpRoutingProvider: MPRoutingProvider? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        MapsIndoors.initialize(applicationContext, "79f8e7daff76489dace4f9f9")
        MapsIndoors.setGoogleAPIKey(getString(R.string.google_maps_key))

        mapFragment.view?.let {
            mapView = it
        }

        mSearchTxtField = findViewById(R.id.search_edit_txt)
        //Listener for when the user searches through the keyboard
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

        mSearchTxtField.setOnEditorActionListener { textView, i, _ ->
            if (i == EditorInfo.IME_ACTION_DONE || i == EditorInfo.IME_ACTION_SEARCH) {
                if (textView.text.isNotEmpty()) {
                    search(textView.text.toString())
                }
                //Making sure keyboard is closed.
                imm.hideSoftInputFromWindow(textView.windowToken, 0)

                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        //ClickListener to start a search, when the user clicks the search button
        var searchBtn = findViewById<ImageButton>(R.id.search_btn)
        searchBtn.setOnClickListener {
            if (mSearchTxtField.text?.length != 0) {
                //There is text inside the search field. So lets do the search.
                search(mSearchTxtField.text.toString())
            }
            //Making sure keyboard is closed.
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }

        var bottomSheet = findViewById<FrameLayout>(R.id.standardBottomSheet)
        btmnSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        btmnSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (currentFragment != null) {
                        if (currentFragment is NavigationFragment) {
                            //Clears the direction view if the navigation fragment is closed.
                            mpDirectionsRenderer?.clear()
                        }
                        //Clears the map if any searches has been done.
                        mMapControl.clearMap()
                        //Removes the current fragment from the BottomSheet.
                        supportFragmentManager.beginTransaction().remove(currentFragment!!).commit()
                        currentFragment = null
                    }
                    mMapControl.setMapPadding(0, 0, 0, 0)
                } else {
                    mMapControl.setMapPadding(0, 0, 0, btmnSheetBehavior.peekHeight)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mapView?.let { view ->
            initMapControl(view)
        }
    }

    private fun initMapControl(view: View) {
        //Creates a new instance of MapControl
        mMapControl = MapControl(this)
        //Sets the Google map object and the map view to the MapControl
        mMapControl.setGoogleMap(mMap, view)
        mMapControl.init { miError ->
            if (miError == null) {
                //No errors so getting the first venue (in the white house solution the only one)
                val venue = MapsIndoors.getVenues()?.currentVenue

                runOnUiThread {
                    //Animates the camera to fit the new venue
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(venue?.latLngBoundingBox, 19));
                }
            }
        }
    }

    private fun search(searchQuery: String) {
        //Query with a string to search on
        val mpQuery = MPQuery.Builder().setQuery(searchQuery).build()
        //Filter for the search query, only taking 30 locations
        val mpFilter = MPFilter.Builder().setTake(30).build()

        //Gets locations
        MapsIndoors.getLocationsAsync(mpQuery, mpFilter) { list: List<MPLocation?>?, miError: MIError? ->
            //Check if there is no error and the list is not empty
            if (miError == null && !list.isNullOrEmpty()) {
                //Create a new instance of the search fragment
                mSearchFragment = SearchFragment.newInstance(list, this)
                //Make a transaction to the bottom sheet
                supportFragmentManager.beginTransaction().replace(R.id.standardBottomSheet, mSearchFragment).commit()
                //Set the map padding to the height of the bottom sheets peek height. To not obfuscate the google logo.
                mMapControl.setMapPadding(0, 0, 0, btmnSheetBehavior.peekHeight)
                if (btmnSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                    btmnSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                //Assign search fragment to current fragment for ui logic
                currentFragment = mSearchFragment
                //Clear the search text, since we got a result
                mSearchTxtField.text?.clear()
                //Calling displaySearch results on the ui thread as camera movement is involved
                runOnUiThread { mMapControl.displaySearchResults(list, true) }
            } else {
                val alertDialogTitleTxt: String
                val alertDialogTxt: String
                if (list!!.isEmpty()) {
                    alertDialogTitleTxt = "No results found"
                    alertDialogTxt = "No results could be found for your search text. Try something else"
                } else {
                    if (miError != null) {
                        alertDialogTitleTxt = "Error: " + miError.code
                        alertDialogTxt = miError.message
                    } else {
                        alertDialogTitleTxt = "Unknown error"
                        alertDialogTxt = "Something went wrong, try another search text"
                    }
                }
                AlertDialog.Builder(this)
                        .setTitle(alertDialogTitleTxt)
                        .setMessage(alertDialogTxt)
                        .show()
            }
        }
    }

    fun getMapControl(): MapControl {
        return mMapControl
    }

    fun getMpDirectionsRenderer(): MPDirectionsRenderer? {
        return mpDirectionsRenderer
    }

    fun createRoute(mpLocation: MPLocation) {
        //If MPRoutingProvider has not been instantiated create it here and assign the results call back to the activity.
        if (mpRoutingProvider == null) {
            mpRoutingProvider = MPRoutingProvider()
            mpRoutingProvider?.setOnRouteResultListener(this)
        }
        mpRoutingProvider?.setTravelMode(TravelMode.WALKING)
        //Queries the MPRouting provider for a route with the hardcoded user location and the point from a location.
        mpRoutingProvider?.query(mUserLocation, mpLocation.point)
    }

    /**
     * The result callback from the route query. Starts the rendering of the route and opens up a new instance of the navigation fragment on the bottom sheet.
     * @param route the route model used to render a navigation view.
     * @param miError an MIError if anything goes wrong when generating a route
     */
    override fun onRouteResult(@Nullable route: Route?, @Nullable miError: MIError?) {
        //Return if either error is not null or the route is null
        if (miError != null || route == null) {
            //TODO: Tell the user about the route not being able to be created etc.
            return
        }
        //Create the MPDirectionsRenderer if it has not been instantiated.
        if (mpDirectionsRenderer == null) {
            mpDirectionsRenderer = MPDirectionsRenderer(this, mMap, mMapControl, OnLegSelectedListener { i: Int ->
                //Listener call back for when the user changes route leg. (By default is only called when a user presses the RouteLegs end marker)
                mpDirectionsRenderer?.setRouteLegIndex(i)
                mMapControl.selectFloor(mpDirectionsRenderer!!.currentFloor)
            })
        }
        //Set the route on the Directions renderer
        mpDirectionsRenderer?.setRoute(route)
        //Create a new instance of the navigation fragment
        mNavigationFragment = NavigationFragment.newInstance(route, this)
        //Start a transaction and assign it to the BottomSheet
        supportFragmentManager.beginTransaction().replace(R.id.standardBottomSheet, mNavigationFragment).commit()
        if (btmnSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            btmnSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        //Assign the navigation fragment to current fragment. To handle ui logic
        currentFragment = mNavigationFragment
        //As camera movement is involved run this on the UIThread
        runOnUiThread {
            //Starts drawing and adjusting the map according to the route
            mpDirectionsRenderer?.initMap(true)
            mMapControl.setMapPadding(0, 0, 0, btmnSheetBehavior.peekHeight)
        }
    }


}