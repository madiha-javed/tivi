/*
 * Copyright 2019 Google LLC
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

package app.tivi.interactors

import androidx.paging.PagedList
import androidx.paging.RxPagedListBuilder
import app.tivi.data.entities.SortOption
import app.tivi.data.repositories.followedshows.FollowedShowsRepository
import app.tivi.data.resultentities.FollowedShowEntryWithShow
import app.tivi.util.AppRxSchedulers
import io.reactivex.BackpressureStrategy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.flow.asFlow
import javax.inject.Inject

class ObserveFollowedShows @Inject constructor(
    private val schedulers: AppRxSchedulers,
    private val followedShowsRepository: FollowedShowsRepository
) : PagingInteractor<ObserveFollowedShows.Parameters, FollowedShowEntryWithShow>() {
    override fun createObservable(params: Parameters): Flow<PagedList<FollowedShowEntryWithShow>> {
        val source = followedShowsRepository.observeFollowedShows(params.sort, params.filter)
        return RxPagedListBuilder(source, params.pagingConfig)
                .setBoundaryCallback(params.boundaryCallback)
                .setFetchScheduler(schedulers.io)
                .setNotifyScheduler(schedulers.main)
                .buildFlowable(BackpressureStrategy.LATEST)
                .asFlow()
    }

    data class Parameters(
        val filter: String? = null,
        val sort: SortOption,
        override val pagingConfig: PagedList.Config,
        override val boundaryCallback: PagedList.BoundaryCallback<FollowedShowEntryWithShow>? = null
    ) : PagingInteractor.Parameters<FollowedShowEntryWithShow>
}