package net.jami.utils

object DonationUtils {
    // First millisecond of 8 September 2025 GMT
    const val startDonationTimeMillis = 1757304000

    // First millisecond of 31 October 2025 GMT
    const val endDonationTimeMillis = 1761969599

    fun isDonationPeriod(): Boolean {
        return System.currentTimeMillis() in startDonationTimeMillis until endDonationTimeMillis
    }
}