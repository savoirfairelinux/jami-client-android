/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.jami.home

import net.jami.model.DonationSettings
import net.jami.mvp.RootPresenter
import net.jami.services.PreferencesService
import net.jami.utils.DonationUtils
import javax.inject.Inject

class HomePresenter @Inject constructor(
    private val mPreferencesService: PreferencesService,
) : RootPresenter<HomeView>() {

    var donationCardIsVisible = false

    override fun bindView(view: HomeView) {
        super.bindView(view)

        if (!DonationUtils.isDonationPeriod())
            return

        mCompositeDisposable.add(mPreferencesService.donationSettings().subscribe { settings ->
            // No need to show the reminder if user specified not to display it anymore
            if (!settings.donationReminderVisibility) {
                showDonationReminder(false)
                return@subscribe
            }

            // Show the reminder if it's been more than 7 days since the last time it was dismissed
            val lastDismissed = settings.lastDismissed
            val elapsedDay = (System.currentTimeMillis() - lastDismissed) / 1000 / 60 / 60 / 24

            showDonationReminder(elapsedDay >= 7)
        })
    }

    private fun showDonationReminder(show: Boolean) {
        donationCardIsVisible = show
        view?.showDonationReminder(show)
    }

    fun setDonationReminderDismissed() {
        mPreferencesService.setDonationSettings(
            DonationSettings(lastDismissed = System.currentTimeMillis())
        )
    }

    fun clickQRSearch() {
        view?.goToQRFragment()
    }

    fun clickNewSwarm() {
        view?.startNewSwarm()
    }

    companion object {
        private val TAG = HomePresenter::class.simpleName!!
    }
}