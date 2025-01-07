/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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
package cx.ring.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Pair
import androidx.preference.*
import cx.ring.R
import cx.ring.account.AccountEditionFragment
import cx.ring.mvp.BasePreferenceFragment
import cx.ring.utils.AndroidFileUtils
import cx.ring.views.CredentialPreferenceDialog
import cx.ring.views.CredentialsPreference
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.SecurityAccountPresenter
import net.jami.account.SecurityAccountView
import net.jami.model.AccountConfig
import net.jami.model.AccountCredentials
import net.jami.model.ConfigKey
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.security.cert.CertificateFactory
import java.util.*

@AndroidEntryPoint
class SecurityAccountFragment : BasePreferenceFragment<SecurityAccountPresenter>(), SecurityAccountView {
    private var credentialsCategory: PreferenceCategory? = null
    private var tlsCategory: PreferenceCategory? = null
    private val editCredentialListener = Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
        // We need the old and new value to correctly edit the list of credentials
        val result = newValue as Pair<AccountCredentials, AccountCredentials>
        presenter.credentialEdited(result.first, result.second)
        false
    }
    private val addCredentialListener = Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
        val result = newValue as Pair<AccountCredentials, AccountCredentials>
        presenter.credentialAdded(result.first, result.second)
        false
    }
    private val filePickerListener = Preference.OnPreferenceClickListener { preference: Preference ->
        if (preference.key.contentEquals(ConfigKey.TLS_CA_LIST_FILE.key)) {
            performFileSearch(SELECT_CA_LIST_RC)
        }
        if (preference.key.contentEquals(ConfigKey.TLS_PRIVATE_KEY_FILE.key)) {
            performFileSearch(SELECT_PRIVATE_KEY_RC)
        }
        if (preference.key.contentEquals(ConfigKey.TLS_CERTIFICATE_FILE.key)) {
            performFileSearch(SELECT_CERTIFICATE_RC)
        }
        true
    }
    private val tlsListener = Preference.OnPreferenceChangeListener { preference: Preference, newValue: Any ->
        var newValue = newValue
        val key = ConfigKey.fromString(preference.key)
        if (key == ConfigKey.TLS_ENABLE) {
            if (newValue as Boolean) {
                presenter.tlsChanged(ConfigKey.STUN_ENABLE, false)
            }
        }
        if (key == ConfigKey.SRTP_KEY_EXCHANGE) {
            newValue = if (newValue as Boolean) "sdes" else ""
        }
        if (preference !is TwoStatePreference) {
            preference.summary = newValue as String
        }
        presenter.tlsChanged(key, newValue)
        true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.account_security_prefs)
        credentialsCategory = findPreference<PreferenceCategory>("Account.credentials")?.apply {
            findPreference<Preference>("Add.credentials")?.onPreferenceChangeListener = addCredentialListener
        }
        tlsCategory = findPreference("TLS.category")
        presenter.init(requireArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY)!!)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (parentFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return
        }
        if (preference is CredentialsPreference) {
            val preferenceDialog = CredentialPreferenceDialog.newInstance(preference.getKey())
            preferenceDialog.setTargetFragment(this, 0)
            preferenceDialog.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun addAllCredentials(credentials: List<AccountCredentials>) {
        for ((i, cred) in credentials.withIndex()) {
            val toAdd = CredentialsPreference(preferenceManager.context).apply {
                key = "credential$i"
                isPersistent = false
                creds = cred
                icon = null
            }
            credentialsCategory!!.addPreference(toAdd)
            toAdd.onPreferenceChangeListener = editCredentialListener
        }
    }

    override fun removeAllCredentials() {
        var i = 0
        while (true) {
            val toRemove = credentialsCategory!!.findPreference<Preference>("credential$i") ?: break
            credentialsCategory!!.removePreference(toRemove)
            i++
        }
    }

    override fun setDetails(config: AccountConfig, tlsMethods: Array<String>) {
        for (i in 0 until tlsCategory!!.preferenceCount) {
            val current = tlsCategory!!.getPreference(i)
            val key = ConfigKey.fromString(current.key)
            if (current is TwoStatePreference) {
                if (key === ConfigKey.SRTP_KEY_EXCHANGE) {
                    current.isChecked = config[key] == "sdes"
                } else {
                    current.isChecked = config.getBool(key!!)
                }
            } else {
                if (key === ConfigKey.TLS_CA_LIST_FILE) {
                    val crt = File(config[ConfigKey.TLS_CA_LIST_FILE])
                    current.summary = crt.name
                    setFeedbackIcon(current, crt)
                    current.onPreferenceClickListener = filePickerListener
                } else if (key === ConfigKey.TLS_PRIVATE_KEY_FILE) {
                    val pem = File(config[ConfigKey.TLS_PRIVATE_KEY_FILE])
                    current.summary = pem.name
                    setFeedbackIcon(current, pem)
                    current.onPreferenceClickListener = filePickerListener
                } else if (key === ConfigKey.TLS_CERTIFICATE_FILE) {
                    val pem = File(config[ConfigKey.TLS_CERTIFICATE_FILE])
                    current.summary = pem.name
                    setFeedbackIcon(current, pem)
                    checkForRSAKey(pem)
                    current.onPreferenceClickListener = filePickerListener
                } else if (key === ConfigKey.TLS_METHOD) {
                    val listPref = current as ListPreference
                    val curVal = config[key]
                    listPref.entries = tlsMethods
                    listPref.entryValues = tlsMethods
                    listPref.value = curVal
                    current.setSummary(curVal)
                } else if (current is EditTextPreference) {
                    val value = config[key!!]
                    current.text = value
                    current.setSummary(value)
                } else {
                    current.summary = config[key!!]
                }
            }
            current.onPreferenceChangeListener = tlsListener
        }
    }

    fun checkCertificate(f: File): Boolean {
        return try {
            val fis = FileInputStream(f.absolutePath)
            val cf = CertificateFactory.getInstance("X509")
            cf.generateCertificate(fis)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun findRSAKey(f: File?): Boolean {
        // NOTE: This check is not complete but better than nothing.
        try {
            val scanner = Scanner(f)
            while (scanner.hasNextLine()) {
                if (scanner.nextLine().contains("-----BEGIN RSA PRIVATE KEY-----")) return true
            }
        } catch (e: FileNotFoundException) {
        }
        return false
    }

    private fun checkForRSAKey(f: File) {
        tlsCategory!!.findPreference<Preference>(ConfigKey.TLS_PRIVATE_KEY_FILE.key)!!.isEnabled = !findRSAKey(f)
    }

    private fun setFeedbackIcon(current: Preference?, certFile: File) {
        val c = current!!.context
        val isKey = ConfigKey.TLS_PRIVATE_KEY_FILE.key == current.key
        if (isKey && findRSAKey(certFile) || !isKey && checkCertificate(certFile)) {
            val icon = c.getDrawable(R.drawable.baseline_check_circle_24)!!
            icon.setTint(c.resources.getColor(R.color.green_500))
            current.icon = icon
        } else {
            val icon = c.getDrawable(R.drawable.baseline_error_24)!!
            icon.setTint(c.resources.getColor(R.color.colorError))
            current.icon = icon
        }
    }

    fun performFileSearch(requestCodeToSet: Int) {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        val intent = Intent(Intent.ACTION_GET_CONTENT)

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.type = "*/*"
        startActivityForResult(intent, requestCodeToSet)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_CANCELED) {
            return
        }
        val uri = data!!.data ?: return
        val myFile = File(AndroidFileUtils.getRealPathFromURI(requireContext(), uri))
        var key: ConfigKey? = null
        val preference: Preference?
        when (requestCode) {
            SELECT_CA_LIST_RC -> {
                preference = tlsCategory!!.findPreference(ConfigKey.TLS_CA_LIST_FILE.key)
                preference!!.summary = myFile.name
                key = ConfigKey.TLS_CA_LIST_FILE
                setFeedbackIcon(preference, myFile)
            }
            SELECT_PRIVATE_KEY_RC -> {
                preference = tlsCategory!!.findPreference(ConfigKey.TLS_PRIVATE_KEY_FILE.key)
                preference!!.summary = myFile.name
                key = ConfigKey.TLS_PRIVATE_KEY_FILE
                setFeedbackIcon(preference, myFile)
            }
            SELECT_CERTIFICATE_RC -> {
                preference = tlsCategory!!.findPreference(ConfigKey.TLS_CERTIFICATE_FILE.key)
                preference!!.summary = myFile.name
                key = ConfigKey.TLS_CERTIFICATE_FILE
                setFeedbackIcon(preference, myFile)
                checkForRSAKey(myFile)
            }
            else -> {
            }
        }
        presenter.fileActivityResult(key, myFile.absolutePath)
    }

    companion object {
        val TAG = SecurityAccountFragment::class.java.simpleName
        private const val DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG"
        private const val SELECT_CA_LIST_RC = 42
        private const val SELECT_PRIVATE_KEY_RC = 43
        private const val SELECT_CERTIFICATE_RC = 44
    }
}