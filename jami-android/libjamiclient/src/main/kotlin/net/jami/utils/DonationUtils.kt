package net.jami.utils

object DonationUtils {
    // First millisecond of 8 September 2025 GMT
    const val startDonationTimeMillis = 1757304000000

    // First millisecond of 15 November 2025 GMT
    const val endDonationTimeMillis = 1763269199999

    fun isDonationPeriod(): Boolean {
        return System.currentTimeMillis() in startDonationTimeMillis until endDonationTimeMillis
    }
}