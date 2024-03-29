package pt.amn.moveon.presentation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import pt.amn.moveon.R
import pt.amn.moveon.databinding.FragmentMapBinding
import pt.amn.moveon.domain.models.Country
import pt.amn.moveon.domain.models.MoveOnPlace
import pt.amn.moveon.presentation.viewmodels.MapViewModel
import pt.amn.moveon.common.AppUtils
import pt.amn.moveon.common.LogNavigator
import pt.amn.moveon.common.START_MAP_LATITUDE
import pt.amn.moveon.common.START_MAP_LONGITUDE

@AndroidEntryPoint
class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    private lateinit var googleMapFragment: SupportMapFragment
    private var myMap: GoogleMap? = null

    private val viewModel: MapViewModel by viewModels()

    private var countries = listOf<Country>()
    private var places = listOf<MoveOnPlace>()

    private val handlerException = CoroutineExceptionHandler { _, throwable ->
        LogNavigator.debugMessage("$TAG, exception handled ${throwable.message}")
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + handlerException)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeUI()
    }

    private fun initializeUI() {
        if (!AppUtils.isOnline(context))
            LogNavigator.toastMessage(requireContext(), R.string.error_message_internet_unavailable)

        googleMapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        googleMapFragment.getMapAsync(this@MapFragment)

        viewModel.visitedCountries.observe(viewLifecycleOwner, Observer { resCountries ->
            countries = resCountries
        })

        viewModel.visitedPlaces.observe(viewLifecycleOwner, Observer { resPlaces ->
            places = resPlaces
        })

        binding.run {
            swMap.setOnCheckedChangeListener { _, isChecked ->
                scope.launch {
                    fillMapPoints(isChecked)
                }
            }
        }
    }

    private suspend fun fillMapPoints(isChecked: Boolean) {
        clearMap()

        if (isChecked) {
            setVisitedPlacesFlags(places)
        } else {
            setVisitedCountriesFlags(countries)
        }
    }

    private fun clearMap() {
        myMap?.clear()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.workplaceFragment).isVisible = true
        menu.findItem(R.id.mainmenu_action_back).isVisible = false
        super.onPrepareOptionsMenu(menu)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onMapReady(p0: GoogleMap) {

        myMap = p0
        moveCamera(START_MAP_LATITUDE, START_MAP_LONGITUDE)

        scope.launch {
            fillMapPoints(binding.swMap.isChecked)
        }

    }

    private fun moveCamera(latitude: Double, longitude: Double) {
        myMap?.moveCamera(
            CameraUpdateFactory.newLatLng(
                LatLng(
                    latitude,
                    longitude
                )
            )
        )
    }

    private suspend fun setVisitedCountriesFlags(visitedCountries: List<Country>) {

        withContext(Dispatchers.Main) {
            if (myMap != null) {

                for (country in visitedCountries) {
                    addCountryMarker(country)
                }
            }
        }

    }

    private fun addCountryMarker(country: Country) {

        try {
            myMap?.addMarker(
                MarkerOptions()
                .position(LatLng(country.latitude, country.longitude))
                .title(country.getLocalName())
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker_green)))
        } catch (ex: Exception) {
            LogNavigator.debugMessage("$TAG, Error of adding country at the map $ex")
        }

    }

    private suspend fun setVisitedPlacesFlags(visitedPlaces: List<MoveOnPlace>) =
        withContext(Dispatchers.Main) {
            if (myMap != null) {

                for (place in visitedPlaces) {
                    addPlaceMarker(place)
                }
            }
        }

    private fun addPlaceMarker(place: MoveOnPlace) {

        try {
            myMap?.addMarker(
                MarkerOptions()
                    .position(LatLng(place.latitude, place.longitude))
                    .title(place.name)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker_blue)))
        } catch (ex: Exception) {
            LogNavigator.debugMessage("$TAG, Error of adding place at the map $ex")
        }

    }

    companion object {
        private const val TAG = "MapFragment"
    }
}