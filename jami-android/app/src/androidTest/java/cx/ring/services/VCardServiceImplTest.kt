/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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
package cx.ring.services

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.jami.services.VCardService
import net.jami.utils.VCardUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class VCardServiceImplTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    // Profiles of the test, away from the files and cache the daemon manages
    private val filesDir = context.getDir("vcard-service-test", Context.MODE_PRIVATE)
    private val testContext = object : ContextWrapper(context) {
        override fun getFilesDir(): File = this@VCardServiceImplTest.filesDir
    }
    private val accountId = "test"

    @After
    fun tearDown() {
        filesDir.deleteRecursively()
    }

    private fun writeProfile(width: Int, height: Int) {
        // Noise does not compress: worst case for the size of the photo
        val random = Random(42)
        val avatar = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        avatar.setPixels(IntArray(width * height) { random.nextInt() or 0xFF000000.toInt() }, 0, width, 0, 0, width, height)
        val photo = ByteArrayOutputStream().also { avatar.compress(Bitmap.CompressFormat.JPEG, 95, it) }.toByteArray()
        val profile = VCardUtils.writeData("jami:test", "Cyrille", photo)
        File(VCardUtils.localProfilePath(filesDir, accountId), VCardUtils.LOCAL_USER_VCARD_NAME)
            .writeText(VCardUtils.vcardToString(profile)!!)
    }

    private fun loadSmallVCard(maxSize: Int) =
        VCardServiceImpl(testContext).loadSmallVCard(accountId, maxSize).blockingGet()!!

    /**
     * The vCard sent with a trust request, photo included, must fit in the
     * payload size a DHT value carries reliably.
     */
    @Test
    fun smallVCardFitsInTrustRequest() {
        writeProfile(400, 400)

        val vcard = loadSmallVCard(VCardService.MAX_SIZE_REQUEST)

        assertTrue(vcard.photos.isNotEmpty())
        assertTrue(VCardUtils.vcardToString(vcard)!!.length <= VCardService.MAX_SIZE_REQUEST)
    }

    /** Narrow photos are shrunk along their long side, down to one pixel across. */
    @Test
    fun narrowPhotoFitsInTrustRequest() {
        for ((width, height) in listOf(24 to 1800, 2 to 21845, 21845 to 2)) {
            writeProfile(width, height)

            val vcard = loadSmallVCard(VCardService.MAX_SIZE_REQUEST)

            val size = VCardUtils.vcardToString(vcard)!!.length
            assertTrue("${width}x$height: no photo", vcard.photos.isNotEmpty())
            assertTrue("${width}x$height: $size bytes", size <= VCardService.MAX_SIZE_REQUEST)
        }
    }

    /** Better a trust request without photo than one too big to be delivered. */
    @Test
    fun photoIsDroppedWhenItCannotFit() {
        writeProfile(400, 400)
        // Only room for the name: no photo fits
        val maxSize = 1024

        val vcard = loadSmallVCard(maxSize)

        assertTrue(vcard.photos.isEmpty())
        assertEquals("Cyrille", vcard.formattedName.value)
        assertTrue(VCardUtils.vcardToString(vcard)!!.length <= maxSize)
    }
}
