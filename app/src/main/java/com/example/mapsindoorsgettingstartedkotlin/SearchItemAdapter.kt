package com.example.mapsindoorsgettingstartedkotlin

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapsindoors.mapssdk.MPLocation
import com.mapsindoors.mapssdk.MPLocationDisplayRule


internal class SearchItemAdapter(
    private val mLocations: List<MPLocation?>,
    private val mMapActivity: MapsActivity?
) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context), parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.text.text = mLocations[position]?.name
        holder.itemView.setOnClickListener {
            mLocations[position]?.let { locations -> mMapActivity?.createRoute(locations) }
            //Clearing map to remove the location filter from our search result
            mMapActivity?.getMapControl()?.clearFilter()
        }
        if (mMapActivity != null) {
            val locationDisplayRule: MPLocationDisplayRule? =
                mMapActivity.getMapControl().getDisplayRule(mLocations[position])
            if (locationDisplayRule != null && locationDisplayRule.icon != null) {
                mMapActivity.runOnUiThread(Runnable {
                    holder.imageView.setImageBitmap(
                        locationDisplayRule.icon
                    )
                })
            } else {
                //Location does not have a special displayRule using type Display rule
                val typeDisplayRule: MPLocationDisplayRule? =
                    mMapActivity.getMapControl().getDisplayRule(mLocations[position]?.type)
                if (typeDisplayRule != null) {
                    mMapActivity.runOnUiThread(Runnable {
                        holder.imageView.setImageBitmap(
                            typeDisplayRule.icon
                        )
                    })
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return mLocations.size
    }

}

internal class ViewHolder(inflater: LayoutInflater, parent: ViewGroup?) :
    RecyclerView.ViewHolder(inflater.inflate(R.layout.fragment_search_list_item, parent, false)) {
    val text: TextView
    val imageView: ImageView

    init {
        text = itemView.findViewById(R.id.text)
        imageView = itemView.findViewById(R.id.location_image)
    }
}