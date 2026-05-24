package desu.inugram.helpers

object MenuOrderUtils {
    /**
     * Permutes `rows` to match the saved order in `entries`. Each row is
     * classified to an item via [classify]; unclassified rows anchor to the
     * preceding classified row (or the head if none). Disabled items are dropped.
     */
    inline fun <Row, I : MenuOrderItem> reorder(
        rows: List<Row>,
        entries: List<MenuOrderEntry<I>>,
        classify: (Row) -> I?,
    ): ArrayList<Row> {
        val byItem = HashMap<I, ArrayList<Row>>()
        val unknownAfter = HashMap<I?, ArrayList<Row>>()
        var lastKnown: I? = null
        for (row in rows) {
            val cfgItem = classify(row)
            if (cfgItem != null) {
                byItem.getOrPut(cfgItem) { ArrayList() }.add(row)
                lastKnown = cfgItem
            } else {
                unknownAfter.getOrPut(lastKnown) { ArrayList() }.add(row)
            }
        }

        val ordered = ArrayList<Row>(rows.size)
        unknownAfter.remove(null)?.let { ordered.addAll(it) }
        for (entry in entries) {
            val rs = byItem.remove(entry.item)
            if (rs != null && entry.enabled) ordered.addAll(rs)
            unknownAfter.remove(entry.item)?.let { ordered.addAll(it) }
        }
        // items absent from saved order (e.g. enum extended after save) — append at end
        for ((item, rs) in byItem) {
            ordered.addAll(rs)
            unknownAfter.remove(item)?.let { ordered.addAll(it) }
        }
        // unknowns anchored to a disabled-and-missing item (shouldn't happen, but safe): append
        for ((_, rs) in unknownAfter) ordered.addAll(rs)

        return ordered
    }
}
