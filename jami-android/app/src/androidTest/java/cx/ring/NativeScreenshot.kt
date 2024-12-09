package cx.ring

import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.Screenshot
import net.jami.utils.Log
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

object NativeScreenshot {
    private var methodName: String? = null
    private var className: String? = null
    private val SCREENSHOT_NAME_VALIDATION: Pattern = Pattern.compile("[a-zA-Z0-9_-]+")
    /**
     * Captures screenshot using Androidx Screenshot library and stores in the filesystem.
     * Special Cases:
     * If the screenshotName contains spaces or does not pass validation, the corresponding
     * screenshot is not visible on BrowserStack's Dashboard.
     * If there is any runtime exception while capturing screenshot, the method throws
     * Exception and the test might fail if the exception is not handled properly.
     * @param screenshotName    a screenshot identifier
     * @return path to the screenshot file
     */
    fun capture(screenshotName: String): String {
        Log.w("NativeScreenshot", "Capturing screenshot: $screenshotName")

        val testClass = findTestClassTraceElement(Thread.currentThread().stackTrace)
        className = testClass.className.replace("[^A-Za-z0-9._-]".toRegex(), "_")
        methodName = testClass.methodName
        val screenCaptureProcessor = EspressoScreenCaptureProcessor()
        if (!SCREENSHOT_NAME_VALIDATION.matcher(screenshotName).matches()) {
            throw IllegalArgumentException("ScreenshotName must match ${SCREENSHOT_NAME_VALIDATION.pattern()}.")
        } else {
            val capture = Screenshot.capture()
            capture.format = Bitmap.CompressFormat.PNG
            capture.name = screenshotName
            try {
                return screenCaptureProcessor.process(capture)
            } catch (e: IOException) {
                throw RuntimeException("Unable to capture screenshot.", e)
            }
        }
    }
    /**
     * Extracts the currently executing test's trace element based on the test runner
     * or any framework being used.
     * @param trace stacktrace of the currently running test
     * @return StackTrace Element corresponding to the current test being executed.
     */
    private fun findTestClassTraceElement(trace: Array<StackTraceElement>): StackTraceElement {
        for (i in trace.indices.reversed()) {
            val element = trace[i]
            if ("android.test.InstrumentationTestCase" == element.className && "runMethod" == element.methodName) {
                return extractStackElement(trace, i)
            }
            if ("org.junit.runners.model.FrameworkMethod\$1" == element.className && "runReflectiveCall" == element.methodName) {
                return extractStackElement(trace, i)
            }
            if ("cucumber.runtime.model.CucumberFeature" == element.className && "run" == element.methodName) {
                return extractStackElement(trace, i)
            }
        }
        throw IllegalArgumentException("Could not find test class!")
    }
    /**
     * Based on the test runner or framework being used, extracts the exact traceElement.
     * @param trace stacktrace of the currently running test
     * @param i a reference index
     * @return trace element based on the index passed
     */
    private fun extractStackElement(trace: Array<StackTraceElement>, i: Int): StackTraceElement {
        val testClassTraceIndex = if (Build.VERSION.SDK_INT >= 23) i - 2 else i - 3
        return trace[testClassTraceIndex]
    }
    private class EspressoScreenCaptureProcessor : BasicScreenCaptureProcessor() {
        companion object {
            private const val SCREENSHOT = "screenshots"
        }
        init {
            val screenshotDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString(), SCREENSHOT)
            val classDir = File(screenshotDir, className)
            Log.w("NativeScreenshot", "Screenshot directory: $classDir")
            mDefaultScreenshotPath = File(classDir, methodName)
        }
        /**
         * Converts the filename to a standard path to be stored on device.
         * Example: "post_addition" converts to "1648038895211_post_addition"
         * which is later suffixed by the file extension i.e. png.
         * @param filename  a screenshot identifier
         * @return custom filename format
         */
        override fun getFilename(filename: String): String {
            return "${System.currentTimeMillis()}_$filename"
        }
    }
}