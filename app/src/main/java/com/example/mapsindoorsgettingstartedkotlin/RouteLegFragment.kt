package com.example.mapsindoorsgettingstartedkotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import com.mapsindoors.mapssdk.RouteLeg
import java.util.*
import java.util.concurrent.TimeUnit

class RouteLegFragment : Fragment() {
    private var mStep: String? = null
    private var mDuration: Int? = null
    private var mDistance: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_route_leg, container, false)
    }

    override fun onViewCreated(
        view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val stepTextView = view.findViewById<TextView>(R.id.stepTextView)
        val distanceTextView = view.findViewById<TextView>(R.id.distanceTextView)
        val durationTextView = view.findViewById<TextView>(R.id.durationTextView)

        stepTextView.text = mStep
        if (Locale.getDefault().country == "US") {
            distanceTextView.text = (mDistance?.times(3.281))?.toInt().toString() + " feet"
        }else {
            distanceTextView.text = mDistance?.toString() + " m"
        }
        mDuration?.let {
            if (it < 60) {
                durationTextView.text = it.toString() + " sec"
            }else {
                durationTextView.text = TimeUnit.MINUTES.convert(it.toLong(), TimeUnit.SECONDS).toString() + " min"
            }
        }
    }

    companion object {
        fun newInstance(step: String, distance: Int?, duration: Int?) =
            RouteLegFragment().apply {
                mStep = step
                mDistance = distance
                mDuration = duration
            }
    }
}