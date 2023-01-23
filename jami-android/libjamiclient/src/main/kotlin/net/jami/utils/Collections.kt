package net.jami.utils

public fun <T> Iterable<T>.takeFirstWhile(n: Int, predicate: (T) -> Boolean): List<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    var count = 0
    val list = ArrayList<T>(n)
    for (item in this) {
        if (predicate(item)) {
            list.add(item)
            if (++count == n)
                break
        }
    }
    return list
}
