package com.kyant.backdrop.catalog.data

object VormexPerformancePolicy {
    const val FeedCacheTtlMillis = 30 * 1000L
    const val ProfileCacheTtlMillis = 2 * 60 * 1000L
    const val SupportingDataTtlMillis = 5 * 60 * 1000L
    const val FindPeopleDataTtlMillis = 5 * 60 * 1000L
    const val SearchResultsTtlMillis = 2 * 60 * 1000L
    const val NearbyPeopleTtlMillis = 90 * 1000L
    const val SearchDebounceMillis = 300L
}
