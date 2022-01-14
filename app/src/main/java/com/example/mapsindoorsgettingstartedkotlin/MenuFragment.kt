package com.example.mapsindoorsgettingstartedkotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapsindoors.mapssdk.MenuInfo

class MenuFragment : Fragment() {
    private var mMenuInfos: List<MenuInfo?>? = null
    private var mMapActivity: MapsActivity? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // For the brevity of this guide, we will reuse the bottom sheet used in the searchFragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view as RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = mMenuInfos?.let { menuInfos -> MenuItemAdapter(menuInfos, mMapActivity) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onDestroyView() {
        mMapActivity?.getMapControl()?.clearMap()
        super.onDestroyView()
    }

    companion object {
        fun newInstance(menuInfos: List<MenuInfo?>?, mapsActivity: MapsActivity?): MenuFragment {
            val fragment = MenuFragment()
            fragment.mMenuInfos = menuInfos
            fragment.mMapActivity = mapsActivity
            return fragment
        }
    }
}
