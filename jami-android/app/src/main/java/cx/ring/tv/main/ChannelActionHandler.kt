package cx.ring.tv.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ChannelActionHandler : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.w(TAG, "onReceive $intent")
    }

    companion object {
        private val TAG = ChannelActionHandler::class.simpleName!!
    }
}