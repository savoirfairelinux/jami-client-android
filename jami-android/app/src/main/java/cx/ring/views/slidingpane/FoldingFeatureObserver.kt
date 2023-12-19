package cx.ring.views.slidingpane

import android.app.Activity
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

/**
 * A device folding feature observer is used to notify listener when there is a folding feature
 * change.
 */
internal class FoldingFeatureObserver(
    private val windowInfoTracker: WindowInfoTracker,
    private val executor: Executor
) {
    private var job: Job? = null
    private var onFoldingFeatureChangeListener: OnFoldingFeatureChangeListener? = null

    /**
     * Interface definition for a callback to be invoked when there is a folding feature change
     */
    internal interface OnFoldingFeatureChangeListener {
        /**
         * Callback method to update window layout when there is a folding feature change
         */
        fun onFoldingFeatureChange(foldingFeature: FoldingFeature)
    }

    /**
     * Register a listener that can be notified when there is a folding feature change.
     *
     * @param onFoldingFeatureChangeListener The listener to be added
     */
    fun setOnFoldingFeatureChangeListener(
        onFoldingFeatureChangeListener: OnFoldingFeatureChangeListener
    ) {
        this.onFoldingFeatureChangeListener = onFoldingFeatureChangeListener
    }

    /**
     * Registers a callback for layout changes of the window for the supplied [Activity].
     * Must be called only after the it is attached to the window.
     */
    fun registerLayoutStateChangeCallback(activity: Activity) {
        job?.cancel()
        job = CoroutineScope(executor.asCoroutineDispatcher()).launch {
            windowInfoTracker.windowLayoutInfo(activity)
                .mapNotNull { info -> getFoldingFeature(info) }
                .distinctUntilChanged()
                .collect { nextFeature ->
                    onFoldingFeatureChangeListener?.onFoldingFeatureChange(nextFeature)
                }
        }
    }

    /**
     * Unregisters a callback for window layout changes of the [Activity] window.
     */
    fun unregisterLayoutStateChangeCallback() {
        job?.cancel()
    }

    private fun getFoldingFeature(windowLayoutInfo: WindowLayoutInfo): FoldingFeature? {
        return windowLayoutInfo.displayFeatures
            .firstOrNull { feature -> feature is FoldingFeature } as? FoldingFeature
    }
}