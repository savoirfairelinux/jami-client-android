package net.jami.utils

object DonationUtils {
    // First millisecond of 27 November 2023 GMT
    const val startDonationTimeMillis = 1701043200000

    // First millisecond of 1 March 2024 GMT
    const val endDonationTimeMillis = 1709251200000

    fun isDonationPeriod(): Boolean {
        return System.currentTimeMillis() in startDonationTimeMillis until endDonationTimeMillis
    }
}