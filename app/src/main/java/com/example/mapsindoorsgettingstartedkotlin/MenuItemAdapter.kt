package com.example.mapsindoorsgettingstartedkotlin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mapsindoors.mapssdk.*
import com.mapsindoors.mapssdk.errors.MIError
import java.io.IOException
import java.net.URL


internal class MenuItemAdapter(
    private val mMenuInfos: List<MenuInfo?>,
    private val mMapActivity: MapsActivity?
) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context), parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //Setting the the text on the text view to the name of the location
        holder.text.text = mMenuInfos[position]?.name

        // When a category is selected, we want to filter the map s.t. it only shows the locations in that
        // category
        holder.itemView.setOnClickListener { view ->
            // empty query, we do not need to query anything specific
            val query = MPQuery.Builder().build()
            // filter created on the selected category key
            val filter = MPFilter.Builder().setCategories(
                listOf(
                    mMenuInfos[position]?.categoryKey
                )
            ).build()
            MapsIndoors.getLocationsAsync(
                query, filter
            ) { locations: List<MPLocation?>?, error: MIError? ->
                if (error == null && locations != null) {
                    mMapActivity?.getMapControl()?.displaySearchResults(locations)
                }
            }
        }

        // if there exists an icon for this menuItem, then we will use it
        val iconUrl = mMenuInfos[position]?.iconUrl
        if (iconUrl != null) {
            // As we need to download the image, it has to be offloaded from the main thread
            Thread {
                val image: Bitmap = try {
                    val url = URL(iconUrl)
                    BitmapFactory.decodeStream(url.openConnection().getInputStream())
                } catch (ignored: IOException) {
                    return@Thread
                }
                //Set the image while on the main thread
                Handler(Looper.getMainLooper()).post {
                    holder.imageView.setImageBitmap(
                        image
                    )
                }
            }.start()
        }

    }

    override fun getItemCount(): Int {
        return mMenuInfos.size
    }

}