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

    /**
     * The vCard sent with a trust request, photo included, must fit in the
     * payload size a DHT value carries reliably.
     */
    @Test
    fun smallVCardFitsInTrustRequest() {
        // Noise does not compress: worst case for the size of the photo
        val side = 400
        val random = Random(42)
        val avatar = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        avatar.setPixels(IntArray(side * side) { random.nextInt() or 0xFF000000.toInt() }, 0, side, 0, 0, side, side)
        val photo = ByteArrayOutputStream().also { avatar.compress(Bitmap.CompressFormat.JPEG, 95, it) }.toByteArray()
        val profile = VCardUtils.writeData("jami:test", "Cyrille", photo)
        File(VCardUtils.localProfilePath(filesDir, accountId), VCardUtils.LOCAL_USER_VCARD_NAME)
            .writeText(VCardUtils.vcardToString(profile)!!)

        val vcard = VCardServiceImpl(testContext).loadSmallVCard(accountId, VCardService.MAX_SIZE_REQUEST).blockingGet()!!

        assertTrue(vcard.photos.isNotEmpty())
        assertTrue(VCardUtils.vcardToString(vcard)!!.length <= VCardService.MAX_SIZE_REQUEST)
    }
}
