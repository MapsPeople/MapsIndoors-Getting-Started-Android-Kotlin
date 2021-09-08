package com.example.mapsindoorsgettingstartedkotlin

import android.app.Activity
import android.graphics.Color
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
import com.mapsindoors.livesdk.LiveDataDomainTypes
import com.mapsindoors.mapssdk.*
import com.mapsindoors.mapssdk.errors.MIError


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnRouteResultListener {

    private lateinit var mMap: GoogleMap
    private lateinit var mapView: View
    private lateinit var mMapControl: MapControl
    private lateinit var mSearchFragment: SearchFragment
    private lateinit var mNavigationFragment: NavigationFragment
    private lateinit var mBtmnSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var mSearchTxtField: TextInputEditText
    private var mCurrentFragment: Fragment? = null
    private val mUserLocation: MPPoint = MPPoint(38.897389429704695, -77.03740973527613, 0.0)

    private var mpDirectionsRenderer: MPDirectionsRenderer? = null
    private var mpDirectionsService: MPDirectionsService? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Initialize MapsIndoors and set the google api Key, we do not need a listener in this showcase
        MapsIndoors.initialize(applicationContext, "d876ff0e60bb430b8fabb145", null)
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
        val searchBtn = findViewById<ImageButton>(R.id.search_btn)
        searchBtn.setOnClickListener {
            if (mSearchTxtField.text?.length != 0) {
                //There is text inside the search field. So lets do the search.
                search(mSearchTxtField.text.toString())
            }
            //Making sure keyboard is closed.
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }

        val bottomSheet = findViewById<FrameLayout>(R.id.standardBottomSheet)
        mBtmnSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        mBtmnSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (mCurrentFragment != null) {
                        if (mCurrentFragment is NavigationFragment) {
                            //Clears the direction view if the navigation fragment is closed.
                            mpDirectionsRenderer?.clear()
                        }
                        //Clears the map if any searches has been done.
                        mMapControl.clearFilter()
                        //Removes the current fragment from the BottomSheet.
                        removeFragmentFromBottomSheet(mCurrentFragment!!)
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        initMapControl(mapView)
    }

    private fun initMapControl(view: View) {
        //Sets the Google map object and the map view to the MapControl with a configuration
        val mapConfig = MPMapConfig.Builder(this, mMap, view).build() as MPMapConfig

        //Creates a new instance of MapControl
        MapControl.create(mapConfig, this::onMapControlReady)
    }


    private fun onMapControlReady(mapControl: MapControl, miError: MIError?) {
        if (miError == null) {
            // Sets the local MapControl var so that it can be used later
            mMapControl = mapControl
            //No errors so getting the first venue (in the white house solution the only one)
            val venue = MapsIndoors.getVenues()?.currentVenue

            runOnUiThread {
                //Animates the camera to fit the new venue
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        venue?.latLngBoundingBox,
                        19
                    )
                )
            }
            //Enable live data on the map
            enableLiveData()
        }
    }

    private fun search(searchQuery: String) {
        //Query with a string to search on
        val mpQuery = MPQuery.Builder().setQuery(searchQuery).build()
        //Filter for the search query, only taking 30 locations
        val mpFilter = MPFilter.Builder().setTake(30).build()

        //Gets locations
        MapsIndoors.getLocationsAsync(mpQuery, mpFilter) {
                list: List<MPLocation?>?, miError: MIError? ->
            //Check if there is no error and the list is not empty
            if (miError == null && !list.isNullOrEmpty()) {
                //Create a new instance of the search fragment
                mSearchFragment = SearchFragment.newInstance(list, this)
                //Make a transaction to the bottom sheet
                addFragmentToBottomSheet(mSearchFragment)
                //Clear the search text, since we got a result
                mSearchTxtField.text?.clear()

                val filterBehavior =
                    MPFilterBehavior.Builder().setMoveCamera(true).setAnimationDuration(500).build()
                //Calling displaySearch results on the ui thread as camera movement is involved
                runOnUiThread { mMapControl.setFilter(list, filterBehavior) }
            } else {
                val alertDialogTitleTxt: String
                val alertDialogTxt: String
                if (list!!.isEmpty()) {
                    alertDialogTitleTxt = "No results found"
                    alertDialogTxt =
                        "No results could be found for your search text. Try something else"
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

    /**
     * Getter for the MapControl object
     *
     * @return MapControl object for this activity
     */
    fun getMapControl(): MapControl {
        return mMapControl
    }

    /**
     * Getter for the MPDirectionsRenderer object
     *
     * @return MPDirectionRenderer object for this activity
     */
    fun getMpDirectionsRenderer(): MPDirectionsRenderer? {
        return mpDirectionsRenderer
    }

    /**
     * Queries the MPRouting provider with a hardcoded user location and the location the user should be routed to
     *
     * @param mpLocation A MPLocation to navigate to
     */
    fun createRoute(mpLocation: MPLocation) {
        //If MPRoutingProvider has not been instantiated create it here and assign the results call back to the activity.
        if (mpDirectionsService == null) {
            //Creating a configuration for the MPDirectionsService allows us to set a resultListener and a travelMode.
            val directionsConfig = MPDirectionsConfig.Builder()
                .setOnRouteResultListener(this::onRouteResult)
                .setTravelMode(MPTravelMode.WALKING).build()

            mpDirectionsService = MPDirectionsService()
            mpDirectionsService!!.setConfig(directionsConfig)
        }
        //Queries the MPRouting provider for a route with the hardcoded user location and the point from a location.
        mpDirectionsService?.query(mUserLocation, mpLocation.point)
    }

    /**
     * The result callback from the route query. Starts the rendering of the route and opens up a new instance of the navigation fragment on the bottom sheet.
     * @param route the route model used to render a navigation view.
     * @param miError an MIError if anything goes wrong when generating a route
     */
    override fun onRouteResult(@Nullable route: MPRoute?, @Nullable miError: MIError?) {
        //Return if either error is not null or the route is null
        if (miError != null || route == null) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Something went wrong")
                .setMessage("Something went wrong when generating the route. Try again or change your destination/origin")
                .show()
            return
        }
        //Create the MPDirectionsRenderer if it has not been instantiated.
        if (mpDirectionsRenderer == null) {
            mpDirectionsRenderer =
                MPDirectionsRenderer(this, mMap, mMapControl, OnLegSelectedListener { i: Int ->
                    //Listener call back for when the user changes route leg. (By default is only called when a user presses the RouteLegs end marker)
                    mpDirectionsRenderer?.setRouteLegIndex(i)
                    mMapControl.selectFloor(mpDirectionsRenderer!!.legFloorIndex)
                })
        }
        //Set the route on the Directions renderer
        mpDirectionsRenderer?.setRoute(route)
        //Create a new instance of the navigation fragment
        mNavigationFragment = NavigationFragment.newInstance(route, this)
        //Start a transaction and assign it to the BottomSheet
        addFragmentToBottomSheet(mNavigationFragment)
        //As camera movement is involved run this on the UIThread
        runOnUiThread {
            //Starts drawing and adjusting the map according to the route
            mpDirectionsRenderer?.renderOnMap()
        }
    }

    /**
     * Enables live data for the map.
     */
    private fun enableLiveData() {
        //Enabling live data for the three known live data domains that are enabled for this solution.
        mMapControl.enableLiveData(LiveDataDomainTypes.AVAILABILITY_DOMAIN)
        mMapControl.enableLiveData(LiveDataDomainTypes.OCCUPANCY_DOMAIN)
        mMapControl.enableLiveData(LiveDataDomainTypes.POSITION_DOMAIN)
    }

    fun addFragmentToBottomSheet(newFragment: Fragment) {
        if (mCurrentFragment != null) {
            supportFragmentManager.beginTransaction().remove(mCurrentFragment!!).commit()
        }
        supportFragmentManager.beginTransaction().replace(R.id.standardBottomSheet, newFragment)
            .commit()
        mCurrentFragment = newFragment
        //Set the map padding to the height of the bottom sheets peek height. To not obfuscate the google logo.
        runOnUiThread {
            mMapControl.setMapPadding(0, 0, 0, mBtmnSheetBehavior.peekHeight)
            if (mBtmnSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                mBtmnSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    fun removeFragmentFromBottomSheet(fragment: Fragment) {
        if (mCurrentFragment == fragment) {
            mCurrentFragment = null
        }
        supportFragmentManager.beginTransaction().remove(fragment).commit()
        runOnUiThread { mMapControl.setMapPadding(0, 0, 0, 0) }
    }
}