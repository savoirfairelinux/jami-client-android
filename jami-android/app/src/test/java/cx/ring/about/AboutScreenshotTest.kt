package cx.ring.about

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import cx.ring.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xxhdpi")
class AboutScreenshotTest {

    private fun saveScreenshot(bitmap: Bitmap, name: String) {
        val outputDir = File("migration_artifacts/after_screenshots")
        outputDir.mkdirs()
        val file = File(outputDir, "$name.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    @Test
    fun testAboutScreen_01_initial_compose() {
        val controller = Robolectric.buildActivity(HostActivity::class.java).create()
        val activity = controller.get()
        activity.setTheme(R.style.AppTheme)
        controller.start().postCreate(null).resume().visible()

        val composeView = androidx.compose.ui.platform.ComposeView(activity).apply {
            setContent {
                cx.ring.ui.theme.JamiTheme {
                    cx.ring.about.ui.AboutScreen(
                        uiState = cx.ring.about.ui.AboutUiState(
                            releaseVersion = activity.getString(R.string.app_release, "20261106-01")
                        )
                    )
                }
            }
        }

        activity.setContentView(composeView)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        val widthPx = (360 * 3.0).toInt()
        val heightPx = (800 * 3.0).toInt()

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        )
        composeView.layout(0, 0, widthPx, heightPx)
        composeView.draw(canvas)

        saveScreenshot(bitmap, "testAboutScreen_01_initial")
    }

    class HostActivity : AppCompatActivity()
}
