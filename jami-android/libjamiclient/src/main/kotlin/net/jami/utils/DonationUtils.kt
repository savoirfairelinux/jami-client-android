package net.jami.utils

object DonationUtils {
    // First millisecond of 17 October 2023 GMT
    const val startDonationTimeMillis = 1697500800000

    // Last millisecond of 31 December 2023 GMT
    const val endDonationTimeMillis = 1704067199999

    fun isDonationPeriod(): Boolean {
        return System.currentTimeMillis() in startDonationTimeMillis until endDonationTimeMillis
    }
}