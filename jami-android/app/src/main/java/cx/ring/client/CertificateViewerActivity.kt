/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
package cx.ring.client

import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import cx.ring.R
import cx.ring.databinding.ItemCertificateDetailsBinding
import cx.ring.utils.ConversationPath
import net.jami.daemon.JamiService
import net.jami.model.AccountConfig
import net.jami.model.CertificateDetailType
import net.jami.model.CertificateDetail

class CertificateViewerActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = ConversationPath.fromIntent(intent)
        if (path == null) {
            finish()
            return
        }

        setContentView(R.layout.activity_certificate_viewer)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val container = findViewById<LinearLayout>(R.id.certificate_container)
        var certId = path.conversationId
        while(true) {
            val details = JamiService.getCertificateDetails(path.accountId, certId) ?: break
            val issuer = details.get(CertificateDetail.ISSUER.key)
            val selfSigned = issuer == certId
            appendCertificate(container, details, selfSigned)
            if (issuer.isNullOrEmpty() || selfSigned)
                break
            certId = issuer
        }
    }

    private fun formatHexString(hexString: String) = hexString.uppercase().chunked(2).joinToString(":")

    private fun appendDetail(container: ViewGroup, key: Detail, value: String): Boolean {
        if (value.isEmpty() || value == "UNSUPPORTED")
            return false
        container.addView(TextView(layoutInflater.context).apply {
            maxLines = 1
            text = getText(key.title)
            setPadding(0, resources.getDimensionPixelSize(R.dimen.padding_small), 0, 0)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
        })
        container.addView(TextView(layoutInflater.context).apply {
            setTextIsSelectable(true)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            if (key.key.type == CertificateDetailType.Binary) {
                setTypeface(Typeface.MONOSPACE)
                text = value//formatHexString(value)
            } else {
                text = value
            }
        })
        return true
    }

    private fun appendSection(container: ViewGroup, keys: List<Detail>, details: Map<String, String>, title: String) {
        val sectionContainer = LinearLayout(layoutInflater.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(resources.getDimensionPixelSize(R.dimen.padding_large))
        }
        sectionContainer.addView(TextView(layoutInflater.context).apply {
            text = title
            maxLines = 1
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
        })
        keys.forEach { key ->
            details[key.key.key]?.let { value -> appendDetail(sectionContainer, key, value) }
        }
        container.addView(sectionContainer)
    }

    private fun appendTags(container: ViewGroup, tags: List<Detail>, details: Map<String, String>, selfSigned: Boolean) {
        val chipGroup = ChipGroup(container.context).apply {
            isSingleLine = true
            setPadding(resources.getDimensionPixelSize(R.dimen.padding_large), 0, resources.getDimensionPixelSize(R.dimen.padding_large), resources.getDimensionPixelSize(R.dimen.padding_small))
        }
        tags.forEach { tag ->
            val value = details[tag.key.key] ?: return@forEach
            if (value == AccountConfig.TRUE_STR)
                chipGroup.addView(Chip(chipGroup.context, null, com.google.android.material.R.style.Widget_Material3_Chip_Assist).apply {
                    text = getText(tag.title)
                })
        }
        if (selfSigned)
            chipGroup.addView(Chip(chipGroup.context, null, com.google.android.material.R.style.Widget_Material3_Chip_Assist).apply {
                text = getText(R.string.cert_is_self_signed)
            })
        if (chipGroup.childCount != 0)
            container.addView(chipGroup)
    }

    private fun appendCertificate(container: LinearLayout, details: Map<String, String>, selfSigned: Boolean) {
        val layout = ItemCertificateDetailsBinding.inflate(layoutInflater, container, false)
        val certificateTitle = (details[CertificateDetail.COMMON_NAME.key] ?: "").ifEmpty { details[CertificateDetail.UID.key] ?: "" }
        layout.certificateTitle.text = certificateTitle
        CERTIFICATE.sections.forEach { section ->
            appendSection(layout.certificateCard, section.keys, details, getString(section.title))
        }
        appendTags(layout.certificateCard, CERTIFICATE.tags, details, selfSigned)
        container.addView(layout.root)
    }

    companion object {
        private data class Detail(val key: CertificateDetail, @StringRes val title: Int)
        private data class Section(@StringRes val title: Int, val keys: List<Detail>)
        private data class CertificateStructure(val tags: List<Detail>, val sections: List<Section>)

        private val CERTIFICATE = CertificateStructure(
            tags = listOf(
                Detail(CertificateDetail.IS_CA, R.string.cert_is_ca)
            ),
            sections = listOf(
                Section(R.string.cert_subject, listOf(
                    Detail(CertificateDetail.UID, R.string.cert_uid),
                    Detail(CertificateDetail.COMMON_NAME, R.string.cert_common_name),
                    Detail(CertificateDetail.NAME, R.string.cert_name),
                    Detail(CertificateDetail.ORGANIZATION_NAME, R.string.cert_organization)
                )),
                Section(R.string.cert_issuer, listOf(
                    Detail(CertificateDetail.ISSUER_UID, R.string.cert_uid),
                    Detail(CertificateDetail.ISSUER_COMMON_NAME, R.string.cert_common_name),
                    Detail(CertificateDetail.ISSUER_NAME, R.string.cert_name),
                    Detail(CertificateDetail.ISSUER_ORGANIZATION, R.string.cert_organization)
                )),
                Section(R.string.cert_validity, listOf(
                    Detail(CertificateDetail.ACTIVATION_DATE, R.string.cert_valid_from),
                    Detail(CertificateDetail.EXPIRATION_DATE, R.string.cert_valid_to)
                )),
                Section(R.string.cert_public_key, listOf(
                    Detail(CertificateDetail.PUBLIC_KEY_ID, R.string.cert_public_key_id),
                    Detail(CertificateDetail.SUBJECT_KEY_ALGORITHM, R.string.cert_key_algorithm),
                    Detail(CertificateDetail.SUBJECT_KEY, R.string.cert_public_key)
                )),
                Section(R.string.cert_signature, listOf(
                    Detail(CertificateDetail.SIGNATURE_ALGORITHM, R.string.cert_signature_algorithm),
                    Detail(CertificateDetail.PUBLIC_SIGNATURE, R.string.cert_signature)
                ))
            )
        )
    }
}