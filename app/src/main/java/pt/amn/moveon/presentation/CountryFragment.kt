package pt.amn.moveon.presentation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import dagger.hilt.android.AndroidEntryPoint
import pt.amn.moveon.R
import pt.amn.moveon.databinding.FragmentCountryBinding
import pt.amn.moveon.domain.models.Country
import pt.amn.moveon.domain.models.MoveOnPlace
import pt.amn.moveon.presentation.adapters.PlacesAdapter
import pt.amn.moveon.presentation.viewmodels.CountryViewModel
import pt.amn.moveon.common.AppUtils
import pt.amn.moveon.common.LogNavigator
import pt.amn.moveon.common.loadDrawableImage
import pt.amn.moveon.presentation.adapters.SimpleItemTouchHelperCallback

const val ARG_COUNTRY = "country"

@AndroidEntryPoint
class CountryFragment : Fragment() {

    private var _binding: FragmentCountryBinding? = null
    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    private lateinit var autocompleteSupportFragment: AutocompleteSupportFragment
    private lateinit var country: Country
    private lateinit var adapter: PlacesAdapter
    private val viewModel: CountryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            country = bundle.getParcelable<Country>(ARG_COUNTRY) ?: return
        }
        adapter = PlacesAdapter()

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCountryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setCountry(country)

        binding.run {
            rvPlaces.adapter = adapter
            tvCountry.text = country.getLocalName()
            ivFlag.loadDrawableImage(requireContext(), binding.root, country.flagResId)

            ItemTouchHelper(SimpleItemTouchHelperCallback(viewModel, adapter))
                .attachToRecyclerView(rvPlaces)
        }

        viewModel.placesList.observe(viewLifecycleOwner, Observer { resPlaces ->
            updateData(resPlaces)
        })

        initializeAutocompleteSupportFragment()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.workplaceFragment).isVisible = false
        menu.findItem(R.id.mainmenu_action_back).isVisible = true
        super.onPrepareOptionsMenu(menu)
    }

    override fun onResume() {
        super.onResume()
        autocompleteSupportFragment.setText("")
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun initializeAutocompleteSupportFragment() {

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), AppUtils.getGoogleApiKey())
        }

        autocompleteSupportFragment =
            childFragmentManager.findFragmentById(R.id.country_place_autocomplete_fragment)
                    as AutocompleteSupportFragment

        // Specify the types of place data to return.
        autocompleteSupportFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG
            )
        )

        autocompleteSupportFragment.setHint(getString(R.string.place_name))

        autocompleteSupportFragment.setCountry(country.alpha2)

        autocompleteSupportFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {

                val latlng = place.latLng
                if (latlng != null) {

                    viewModel.addPlace(
                        place.id ?: return,
                        place.latLng?.latitude ?: return,
                        place.latLng?.longitude ?: return,
                        place.name ?: return,
                        country.id
                    )

                    // When a user adds a new place, the country is marked as visited
                    if (!country.visited) {
                        country.visited = true
                        viewModel.changeVisitedFlagOfCountry(country)
                    }

                } else {
                    LogNavigator.debugMessage("$TAG, ${getString(R.string.place_didnt_find)}")
                    LogNavigator.toastMessage(
                        requireContext(), getString(R.string.place_didnt_find))
                }

                autocompleteSupportFragment.setText("")
            }

            override fun onError(status: Status) {
                if (status != Status.RESULT_CANCELED) {
                    LogNavigator.debugMessage("$TAG, ${getString(R.string.place_error_occured)} $status")
                    LogNavigator.toastMessage(
                        requireContext(),
                        getString(R.string.place_error_occured) + status.statusMessage
                    )
                }
            }

        })

    }

    private fun updateData(placesList: List<MoveOnPlace>) {
        adapter.bindPlaces(placesList)
        adapter.notifyDataSetChanged()
    }

    companion object {

        private const val TAG = "CountryFragment"

        @JvmStatic
        fun newInstance(param1: Country) =
            CountryFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_COUNTRY, param1)
                }
            }
    }
}