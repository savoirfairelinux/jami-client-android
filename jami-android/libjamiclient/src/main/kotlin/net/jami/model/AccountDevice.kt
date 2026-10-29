package net.jami.model

data class AccountDevice(val id: String, val name: String, val lastSync: Long = 0) {
    companion object {
        fun fromMap(details: Map<String, String>): AccountDevice =
            AccountDevice(
                details.getValue("id"),
                details.getValue("label"),
                details["lastSync"]?.toLong() ?: 0
            )
    }
}
