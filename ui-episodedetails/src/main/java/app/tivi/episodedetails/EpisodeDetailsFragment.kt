/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.tivi.episodedetails

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import app.tivi.TiviMvRxBottomSheetFragment
import app.tivi.common.epoxy.SwipeAwayCallbacks
import app.tivi.episodedetails.databinding.FragmentEpisodeDetailsBinding
import app.tivi.extensions.doOnApplyWindowInsets
import app.tivi.extensions.resolveThemeColor
import app.tivi.extensions.updateConstraintSets
import app.tivi.showdetails.ShowDetailsNavigator
import app.tivi.ui.motionlayout.FabShowHideTransitionListener
import com.airbnb.epoxy.EpoxyTouchHelper
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.parcel.Parcelize
import javax.inject.Inject

class EpisodeDetailsFragment : TiviMvRxBottomSheetFragment() {
    companion object {
        @JvmStatic
        fun create(id: Long): EpisodeDetailsFragment {
            return EpisodeDetailsFragment().apply {
                arguments = bundleOf(MvRx.KEY_ARG to Arguments(id))
            }
        }
    }

    @Parcelize
    data class Arguments(val episodeId: Long) : Parcelable

    private val viewModel: EpisodeDetailsViewModel by fragmentViewModel()
    @Inject lateinit var episodeDetailsViewModelFactory: EpisodeDetailsViewModel.Factory

    @Inject lateinit var controller: EpisodeDetailsEpoxyController
    @Inject lateinit var showDetailsNavigator: ShowDetailsNavigator

    private lateinit var binding: FragmentEpisodeDetailsBinding

    private val bottomSheetDialog: BottomSheetDialog
        get() = requireDialog() as BottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentEpisodeDetailsBinding.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.epDetailsRv.setController(controller)

        binding.epDetailsRoot.doOnApplyWindowInsets { v, insets, _ ->
            (v as MotionLayout).updateConstraintSets {
                constrainHeight(R.id.ep_details_status_bar_anchor, insets.systemWindowInsetTop)
            }
        }

        binding.epDetailsRoot.setTransitionListener(FabShowHideTransitionListener(
                binding.epDetailsFab, R.id.episode_details_expanded, R.id.episode_details_collapsed))

        binding.epDetailsToolbar.setNavigationOnClickListener { bottomSheetDialog.dismiss() }

        binding.epDetailsFab.setOnClickListener {
            withState(viewModel) { state ->
                when (state.action) {
                    EpisodeDetailsViewState.Action.WATCH -> viewModel.markWatched()
                    EpisodeDetailsViewState.Action.UNWATCH -> viewModel.markUnwatched()
                }
            }
        }

        val context = requireContext()
        val swipeCallback = object : SwipeAwayCallbacks<EpDetailsWatchItemBindingModel_>(
                context.getDrawable(R.drawable.ic_eye_off_24dp)!!,
                context.resources.getDimensionPixelSize(R.dimen.spacing_large),
                context.getColor(R.color.swipe_away_background),
                context.resolveThemeColor(R.attr.colorSecondary)
        ) {
            override fun onSwipeCompleted(
                model: EpDetailsWatchItemBindingModel_,
                itemView: View,
                position: Int,
                direction: Int
            ) {
                model.watch().also(viewModel::removeWatchEntry)
            }

            override fun isSwipeEnabledForModel(model: EpDetailsWatchItemBindingModel_): Boolean {
                return model.watch() != null
            }
        }

        EpoxyTouchHelper.initSwiping(binding.epDetailsRv)
                .let { if (view.layoutDirection == View.LAYOUT_DIRECTION_RTL) it.right() else it.left() }
                .withTarget(EpDetailsWatchItemBindingModel_::class.java)
                .andCallbacks(swipeCallback)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.run {
            updateLayoutParams {
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }

        // Need to remove the fitSystemWindows flag from the MDC Dialog, otherwise it will be
        // padded above the navigation bar
        bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.container)?.run {
            fitsSystemWindows = false
            setPadding(0, 0, 0, 0)
        }
    }

    override fun invalidate() {
        withState(viewModel) { state ->
            binding.state = state
            controller.setData(state)
        }
    }
}