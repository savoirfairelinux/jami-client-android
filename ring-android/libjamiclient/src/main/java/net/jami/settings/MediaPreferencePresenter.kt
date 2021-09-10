/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.jami.settings

import io.reactivex.rxjava3.core.Scheduler
import net.jami.model.Account
import net.jami.model.Codec
import net.jami.model.ConfigKey
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.DeviceRuntimeService
import net.jami.utils.Log
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class MediaPreferencePresenter @Inject constructor(
    private var mAccountService: AccountService,
    private var mDeviceRuntimeService: DeviceRuntimeService,
    @Named("UiScheduler")
    private var mUiScheduler: Scheduler
) : RootPresenter<MediaPreferenceView>() {
    private var mAccount: Account? = null

    fun init(accountId: String) {
        mAccount = mAccountService.getAccount(accountId)
        mCompositeDisposable.clear()
        mCompositeDisposable.add(mAccountService
            .getObservableAccount(accountId)
            .switchMapSingle { account: Account ->
                mAccountService.getCodecList(accountId)
                    .observeOn(mUiScheduler)
                    .doOnSuccess { codecList: List<Codec> ->
                        val audioCodec = ArrayList<Codec>()
                        val videoCodec = ArrayList<Codec>()
                        for (codec in codecList) {
                            if (codec.type === Codec.Type.AUDIO) {
                                audioCodec.add(codec)
                            } else if (codec.type === Codec.Type.VIDEO) {
                                videoCodec.add(codec)
                            }
                        }
                        view?.accountChanged(account, audioCodec, videoCodec)
                    }
            }
            .subscribe({ }) { Log.e(TAG, "Error loading codec list") })
    }

    fun codecChanged(codecs: ArrayList<Long>) {
        mAccountService.setActiveCodecList(mAccount!!.accountID, codecs)
    }

    fun videoPreferenceChanged(key: ConfigKey, newValue: Any) {
        mAccount!!.setDetail(key, newValue.toString())
        updateAccount()
    }

    private fun updateAccount() {
        mAccountService.setCredentials(mAccount!!.accountID, mAccount!!.credentialsHashMapList)
        mAccountService.setAccountDetails(mAccount!!.accountID, mAccount!!.details)
    }

    companion object {
        val TAG = MediaPreferencePresenter::class.simpleName!!
    }
}