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
package net.jami.utils

import net.jami.utils.FileUtils.moveFile
import ezvcard.VCard
import ezvcard.property.FormattedName
import ezvcard.property.Uid
import ezvcard.property.RawProperty
import ezvcard.io.text.VCardWriter
import ezvcard.VCardVersion
import kotlin.Throws
import ezvcard.Ezvcard
import ezvcard.parameter.ImageType
import ezvcard.property.Photo
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.IOException
import java.io.StringWriter
import java.lang.Exception
import java.util.HashMap

object VCardUtils {
    val TAG = VCardUtils::class.simpleName!!
    const val MIME_PROFILE_VCARD = "x-ring/ring.profile.vcard"
    const val VCARD_KEY_MIME_TYPE = "mimeType"
    const val VCARD_KEY_PART = "part"
    const val VCARD_KEY_OF = "of"
    const val LOCAL_USER_VCARD_NAME = "profile.vcf"
    private const val VCARD_MAX_SIZE = 1024L * 1024L * 8

    fun readData(vcard: VCard?): Pair<String?, ByteArray?> {
        var contactName: String? = null
        var photo: ByteArray? = null
        if (vcard != null) {
            if (vcard.photos.isNotEmpty()) {
                photo = try {
                    vcard.photos[0].data
                } catch (e: Exception) {
                    Log.w(TAG, "Can't read photo from VCard", e)
                    null
                }
            }
            val fname = vcard.formattedName
            if (fname != null) {
                if (!fname.value.isNullOrEmpty()) {
                    contactName = fname.value
                }
            }
        }
        return Pair(contactName, photo)
    }

    fun writeData(uri: String?, displayName: String?, picture: ByteArray?): VCard =
        VCard().apply {
            formattedName = FormattedName(displayName)
            uid = Uid(uri)
            if (picture != null)
                addPhoto(Photo(picture, ImageType.JPEG))
            removeProperties(RawProperty::class.java)
        }

    /**
     * Parse the "elements" of the mime attributes to build a proper hashtable
     *
     * @param mime the mimetype as returned by the daemon
     * @return a correct hashtable, null if invalid input
     */
    fun parseMimeAttributes(mime: String): HashMap<String, String> {
        val elements = mime.split(";")
        val messageKeyValue = HashMap<String, String>()
        if (elements.size < 2) {
            return messageKeyValue
        }
        messageKeyValue[VCARD_KEY_MIME_TYPE] = elements[0]
        val pairs = elements[1].split(",")
        for (pair in pairs) {
            val kv = pair.split("=")
            messageKeyValue[kv[0].trim { it <= ' ' }] = kv[1]
        }
        return messageKeyValue
    }

    fun savePeerProfileToDisk(vcard: VCard?, accountId: String, filename: String, filesDir: File) {
        saveToDisk(vcard, filename, peerProfilePath(filesDir, accountId))
    }

    fun saveLocalProfileToDisk(vcard: VCard, accountId: String, filesDir: File): Single<VCard> =
        Single.fromCallable {
            saveToDisk(vcard, LOCAL_USER_VCARD_NAME, localProfilePath(filesDir, accountId))
            vcard
        }

    /**
     * Saves a vcard string to an internal new vcf file.
     *
     * @param vcard    the VCard to save
     * @param filename the filename of the vcf
     * @param path     the path of the vcf
     */
    private fun saveToDisk(vcard: VCard?, filename: String, path: File) {
        if (vcard == null || filename.isEmpty()) {
            return
        }
        if (!path.exists()) {
            path.mkdirs()
        }
        val file = File(path, filename)
        try {
            VCardWriter(file, VCardVersion.V2_1).use { writer ->
                writer.vObjectWriter.foldedLineWriter.lineLength = null
                writer.write(vcard)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while saving VCard to disk", e)
        }
    }

    @Throws(IOException::class)
    fun loadPeerProfileFromDisk(filesDir: File, filename: String?, accountId: String): VCard? {
        val profileFolder = peerProfilePath(filesDir, accountId)
        return loadFromDisk(File(profileFolder, filename))
    }

    fun loadLocalProfileFromDisk(filesDir: File, accountId: String): Single<VCard> =
        Single.fromCallable {
            val path = localProfilePath(filesDir, accountId).absolutePath
            loadFromDisk(File(path, LOCAL_USER_VCARD_NAME))!!
        }

    fun loadLocalProfileFromDiskWithDefault(filesDir: File, accountId: String): Single<VCard> =
        loadLocalProfileFromDisk(filesDir, accountId)
            .onErrorReturn { setupDefaultProfile(filesDir, accountId) }

    /**
     * Loads the vcard file from the disk
     *
     * @param path the filename of the vcard
     * @return the VCard or null
     */
    @Throws(IOException::class)
    private fun loadFromDisk(path: File?): VCard? {
        if (path == null || !path.exists()) {
            // Log.d(TAG, "vcardPath not exist " + path);
            return null
        }
        val length = path.length()
        if (length > VCARD_MAX_SIZE) {
            Log.w(TAG, "vcardPath too big: ${path.length() / 1024} kB")
            return null
        }
        return Ezvcard.parse(path).first()
    }

    fun vcardToString(vcard: VCard?): String? {
        val writer = StringWriter()
        val vcwriter = VCardWriter(writer, VCardVersion.V2_1)
        vcwriter.vObjectWriter.foldedLineWriter.lineLength = null
        var stringVCard: String?
        try {
            vcwriter.write(vcard)
            stringVCard = writer.toString()
            vcwriter.close()
            writer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error while converting VCard to String", e)
            stringVCard = null
        }
        return stringVCard
    }

    fun isEmpty(vCard: VCard): Boolean {
        val name = vCard.formattedName
        return (name == null || name.value.isEmpty()) && vCard.photos.isEmpty()
    }

    private fun peerProfilePath(filesDir: File, accountId: String): File {
        val accountDir = File(filesDir, accountId)
        return File(accountDir, "profiles").apply { mkdirs() }
    }

    private fun localProfilePath(filesDir: File, accountId: String): File =
        File(filesDir, accountId).apply { mkdir() }

    private fun setupDefaultProfile(filesDir: File, accountId: String): VCard {
        val vcard = VCard()
        vcard.uid = Uid(accountId)
        saveLocalProfileToDisk(vcard, accountId, filesDir)
            .subscribeOn(Schedulers.io())
            .subscribe({}) { e -> Log.e(TAG, "Error while saving vcard", e) }
        return vcard
    }

    fun accountProfileReceived(filesDir: File, accountId: String, vcard: File): Single<VCard> =
        Single.fromCallable {
            val card = loadFromDisk(vcard)!!
            saveLocalProfileToDisk(card, accountId, filesDir)
                .subscribeOn(Schedulers.io())
                .subscribe({}) { e -> Log.e(TAG, "Error while saving vcard", e) }
            card
        }.subscribeOn(Schedulers.io())

    fun peerProfileReceived(filesDir: File, accountId: String, peerId: String, vcard: File): Single<VCard> =
        Single.fromCallable<VCard> {
            val filename = "$peerId.vcf"
            val peerProfilePath = peerProfilePath(filesDir, accountId)
            val file = File(peerProfilePath, filename)
            moveFile(vcard, file)
            loadFromDisk(file)!!
        }.subscribeOn(Schedulers.io())

}