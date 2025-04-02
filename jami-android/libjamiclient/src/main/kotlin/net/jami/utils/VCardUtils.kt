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
package net.jami.utils

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
    const val VCARD_KEY_MIME_TYPE = "mimeType"
    const val ACCOUNT_PROFILE_NAME = "profile"
    const val LOCAL_USER_VCARD_NAME = "$ACCOUNT_PROFILE_NAME.vcf"
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

    fun pictureTypeFromMime(mimeType: String?): String =
        when (mimeType?.lowercase()) {
            null -> ""
            "image/jpeg" -> "JPEG"
            "image/png" -> "PNG"
            else -> "JPEG"
        }

    fun resetCustomProfileName(accountId: String, filename: String, filesDir: File) {
        if (filename.isEmpty()) {
            return
        }
        val customProfileName = File(customPeerProfilePath(filesDir, accountId), "$filename.txt")
        if (customProfileName.exists()) {
            customProfileName.delete()
        }
    }

    fun resetCustomProfilePicture(accountId: String, filename: String, filesDir: File) {
        if (filename.isEmpty()) {
            return
        }
        val customProfilePicture = File(customPeerProfilePath(filesDir, accountId), filename)
        if (customProfilePicture.exists()) {
            customProfilePicture.delete()
        }
    }


    fun loadLocalProfileFromDisk(filesDir: File, accountId: String): Single<VCard> =
        Single.fromCallable {
            val path = localProfilePath(filesDir, accountId).absolutePath
            loadFromDisk(File(path, LOCAL_USER_VCARD_NAME))!!
        }

    fun loadLocalProfileFromDiskWithDefault(filesDir: File, accountId: String): Single<VCard> =
        loadLocalProfileFromDisk(filesDir, accountId)
            .onErrorReturn { defaultProfile(accountId) }

    /**
     * Loads the vcard file from the disk
     *
     * @param path the filename of the vcard
     * @return the VCard or null
     */
    @Throws(IOException::class)
    fun loadFromDisk(path: File): VCard? {
        if (!path.exists()) {
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

    fun saveToCustomProfiles(name: String?, picture: ByteArray?, accountId: String,
                             filename: String, filesDir: File) {
        if (filename.isEmpty()) {
            return
        }
        val customProfileDir = customPeerProfilePath(filesDir, accountId)
        if (!customProfileDir.exists()) {
            customProfileDir.mkdirs()
        }
        name?.let { File(customProfileDir, "$filename.txt").writeText(it) }
        picture?.let { File(customProfileDir, filename).writeBytes(it) }
    }

    fun getCustomProfile(accountId: String, filename: String, filesDir: File):
            Pair<String?, ByteArray?> {
        if (filename.isEmpty()) {
            return Pair(null, null)
        }
        val customProfileDir = customPeerProfilePath(filesDir, accountId)
        val finalName = File(customProfileDir, "$filename.txt").takeIf { it.exists() }?.readText()
        val finalPicture = File(customProfileDir, filename).takeIf { it.exists() }?.readBytes()
        return Pair(finalName, finalPicture)
    }

    fun isEmpty(vCard: VCard): Boolean {
        val name = vCard.formattedName
        return (name == null || name.value.isEmpty()) && vCard.photos.isEmpty()
    }

    fun customPeerProfilePath(filesDir: File, accountId: String): File {
        val accountDir = File(filesDir, accountId)
        return File(accountDir, "CustomPeerProfiles").apply { mkdirs() }
    }

    fun peerProfilePath(filesDir: File, accountId: String): File {
        val accountDir = File(filesDir, accountId)
        return File(accountDir, "profiles").apply { mkdirs() }
    }
    fun peerProfileCachePath(cacheDir: File, accountId: String): File {
        val accountDir = File(cacheDir, accountId)
        return File(accountDir, "profiles").apply { mkdirs() }
    }

    fun localProfilePath(filesDir: File, accountId: String): File =
        File(filesDir, accountId).apply { mkdir() }

    fun localProfileCachePath(cacheDir: File, accountId: String): File =
        File(cacheDir, accountId).apply { mkdir() }

    private fun defaultProfile(accountId: String): VCard = VCard().apply { Uid(accountId) }

    fun loadProfile(vcard: File): Single<VCard> =
        Single.fromCallable { loadFromDisk(vcard)!! }
            .subscribeOn(Schedulers.io())

}