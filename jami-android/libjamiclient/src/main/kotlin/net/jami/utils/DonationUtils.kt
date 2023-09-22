package net.jami.utils

object DonationUtils {
    // First millisecond of 27 November 2023 GMT
    const val startDonationTimeMillis = 1701043200000

    // Last millisecond of 31 January 2024 GMT
    const val endDonationTimeMillis = 1706745599999

    fun isDonationPeriod(): Boolean {
        return System.currentTimeMillis() in startDonationTimeMillis until endDonationTimeMillis
    }
}