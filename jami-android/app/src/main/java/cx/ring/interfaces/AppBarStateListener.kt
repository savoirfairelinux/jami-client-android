package cx.ring.interfaces

import android.view.View

/** An interface to be implemented by Activities or Fragments that host {@link com.google.android.material.appbar.AppBarLayout} */
interface AppBarStateListener {
    fun onToolbarTitleChanged(title: CharSequence)
    fun onAppBarScrollTargetViewChanged(v: View?)
}