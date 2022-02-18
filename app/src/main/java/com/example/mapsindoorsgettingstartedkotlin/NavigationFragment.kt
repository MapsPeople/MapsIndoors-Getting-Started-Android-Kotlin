package com.example.mapsindoorsgettingstartedkotlin

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.mapsindoors.mapssdk.*
import java.util.concurrent.TimeUnit

class NavigationFragment : Fragment() {
    private var mRoute: Route? = null
    private var mLocation: MPLocation? = null

    private var actionNames: Array<String?>? = null

    private var mMapsActivity: MapsActivity? = null

    @Nullable
    override fun onCreateView(inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_navigation, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        //Assigning views
        val locationNameTxtView = view.findViewById<TextView>(R.id.location_name)
        locationNameTxtView.text = "To " + mLocation?.name

        val routeCollectionAdapter =
            RouteCollectionAdapter(this)
        val mViewPager: ViewPager2 = view.findViewById(R.id.stepViewPager)
        mViewPager.adapter = routeCollectionAdapter
        mViewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                //When a page is selected call the renderer with the index
                mMapsActivity?.getMpDirectionsRenderer()?.setRouteLegIndex(position)
                //Update the floor on mapcontrol if the floor might have changed for the routing
                mMapsActivity?.getMpDirectionsRenderer()?.currentFloor?.let {floorIndex ->
                    mMapsActivity?.getMapControl()?.selectFloor(floorIndex)
                }
            }
        })

        val closeBtn = view.findViewById<ImageView>(R.id.close_btn)

        //Button for closing the bottom sheet. Clears the route through directionsRenderer as well, and changes map padding.
        closeBtn.setOnClickListener {
            mMapsActivity!!.removeFragmentFromBottomSheet(this)
            mMapsActivity!!.getMpDirectionsRenderer()?.clear()
        }
    }

    fun getStepName(startStep: RouteStep, endStep: RouteStep): String {
        val startStepStartPointZIndex = startStep.startLocation?.zIndex
        val startStepStartFloorName = startStep.startLocation?.floorName
        var highway: String? = null
        getActionNames().forEach {
            it?.let {
                if (startStep.highway == it) {
                    highway = if (it == Highway.STEPS) {
                        "stairs"
                    }else {
                        it
                    }
                }
            }
        }
        if (highway != null) {
            return String.format("Take %s to %s %s", highway, "Level", if (endStep.endLocation?.floorName.isNullOrEmpty()) endStep.endLocation?.zIndex else endStep.endLocation?.floorName)
        }
        var result = "Walk to next step"

        if (startStepStartFloorName == endStep.endLocation?.floorName) {
            return result
        }

        val endStepEndFloorName = endStep.endLocation?.floorName

        result = if (TextUtils.isEmpty(endStepEndFloorName)) {
            String.format("Level %s to %s", if (TextUtils.isEmpty(startStepStartFloorName)) startStepStartPointZIndex else startStepStartFloorName, endStep.endPoint.zIndex)
        } else {
            String.format("Level %s to %s", if (TextUtils.isEmpty(startStepStartFloorName)) startStepStartPointZIndex else startStepStartFloorName, endStepEndFloorName)
        }
        return result
    }

    fun getActionNames(): Array<String?> {
        if (actionNames == null) {
            actionNames = arrayOf(
                Highway.ELEVATOR,
                Highway.ESCALATOR,
                Highway.STEPS,
                Highway.TRAVELATOR,
                Highway.RAMP,
                Highway.WHEELCHAIRRAMP,
                Highway.WHEELCHAIRLIFT,
                Highway.LADDER
            )
        }
        return actionNames!!
    }

    inner class RouteCollectionAdapter(fragment: Fragment?) :
        FragmentStateAdapter(fragment!!) {

        override fun createFragment(position: Int): Fragment {
            if (position == mRoute?.legs?.size!! - 1) {
                return RouteLegFragment.newInstance("Walk to " + mLocation?.name, mRoute?.legs!![position]?.distance?.toInt(), mRoute?.legs!![position]?.duration?.toInt())
            } else {
                var leg = mRoute?.legs!![position]
                var firstStep = leg.steps.first()
                var lastFirstStep = mRoute?.legs!![position + 1].steps.first()
                var lastStep = mRoute?.legs!![position + 1].steps.last()

                var firstBuilding = MapsIndoors.getBuildings()?.getBuilding(firstStep.startPoint.latLng)
                var lastBuilding  = MapsIndoors.getBuildings()?.getBuilding(lastStep.startPoint.latLng)
                return if (firstBuilding != null && lastBuilding != null) {
                    RouteLegFragment.newInstance(getStepName(lastFirstStep, lastStep), leg.distance.toInt(), leg.duration.toInt())
                }else if (firstBuilding != null) {
                    RouteLegFragment.newInstance("Exit: " + firstBuilding.name,  leg.distance.toInt(), leg.duration.toInt())
                }else {
                    RouteLegFragment.newInstance("Enter: " + lastBuilding?.name,  leg.distance.toInt(), leg.duration.toInt())
                }
            }
        }

        override fun getItemCount(): Int {
            mRoute?.legs?.let { legs->
                return legs.size
            }
            return 0
        }
    }

    companion object {
        fun newInstance(route: Route?, mapsActivity: MapsActivity?, location: MPLocation?): NavigationFragment {
            val fragment = NavigationFragment()
            fragment.mRoute = route
            fragment.mLocation = location
            fragment.mMapsActivity = mapsActivity
            return fragment
        }
    }
}