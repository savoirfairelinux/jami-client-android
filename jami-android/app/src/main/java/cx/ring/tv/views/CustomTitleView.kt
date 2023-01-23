/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package cx.ring.tv.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import kotlin.jvm.JvmOverloads
import android.widget.RelativeLayout
import androidx.leanback.widget.TitleViewAdapter
import android.widget.TextView
import android.widget.ImageButton
import cx.ring.tv.views.CustomTitleView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.leanback.widget.SearchOrbView
import cx.ring.R

/**
 * Custom title view to be used in [androidx.leanback.app.BrowseFragment].
 */
class CustomTitleView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0) :
    RelativeLayout(context, attrs, defStyle), TitleViewAdapter.Provider {

    val root: View = LayoutInflater.from(context).inflate(R.layout.tv_titleview, this)
    private val mAliasView: TextView = root.findViewById(R.id.account_alias)
    private val mTitleView: TextView = root.findViewById(R.id.title_text)
    val logoView: ImageView = root.findViewById(R.id.title_photo_contact)
    private val mSearchOrbView: View = root.findViewById<SearchOrbView>(R.id.title_orb).apply {
        setOnKeyListener(OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                settingsButton.requestFocus()
                return@OnKeyListener true
            }
            false
        })
    }
    val settingsButton: ImageButton = root.findViewById(R.id.title_settings)

    private val mTitleViewAdapter: TitleViewAdapter = object : TitleViewAdapter() {
        override fun getSearchAffordanceView(): View {
            return mSearchOrbView
        }

        override fun setTitle(titleText: CharSequence?) {
            this@CustomTitleView.setTitle(titleText)
        }

        override fun setBadgeDrawable(drawable: Drawable?) {
            //NOOP
        }

        override fun setOnSearchClickedListener(listener: OnClickListener) {
            mSearchOrbView.setOnClickListener(listener)
        }

        override fun updateComponentsVisibility(flags: Int) {
            val visibility = if (flags and SEARCH_VIEW_VISIBLE == SEARCH_VIEW_VISIBLE) VISIBLE else INVISIBLE
            mSearchOrbView.visibility = visibility
        }
    }

    fun setAlias(alias: CharSequence?) {
        if (alias == null || alias.toString().isEmpty()) {
            Log.e(TAG, "Null alias")
            return
        }
        mAliasView.text = alias
        mAliasView.visibility = VISIBLE
    }

    fun setTitle(title: CharSequence?) {
        if (title == null || title.toString().isEmpty()) {
            Log.e(TAG, "Null title")
            return
        }
        mTitleView.text = title
        mTitleView.visibility = VISIBLE
        logoView.visibility = VISIBLE
        settingsButton.visibility = VISIBLE
    }

    override fun getTitleViewAdapter(): TitleViewAdapter {
        return mTitleViewAdapter
    }

    private val isAliasDefined: Boolean
        get() = mAliasView.text.toString().isNotEmpty()
    private val isTitleDefined: Boolean
        get() = mTitleView.text.toString().isNotEmpty()

    companion object {
        private val TAG = CustomTitleView::class.simpleName!!
    }

    init {
        clipChildren = false
        clipToPadding = false
    }
}
