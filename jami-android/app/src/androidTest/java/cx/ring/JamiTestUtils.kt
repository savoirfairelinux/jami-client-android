package cx.ring

import android.content.Context
import android.net.Uri
import cx.ring.application.JamiApplication
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.services.AccountService
import net.jami.utils.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.HashMap


object AccountUtils {

    private val TAG = AccountUtils::class.java.simpleName

    private const val NAME_SERVER_ADDRESS = "https://ns-test.jami.net"

    /**
     * Create n accounts and register them.
     * This function is blocking.
     *
     * @param count The number of accounts to create.
     * @return The list of registered account names.
     */
    fun createAccountAndRegister(count: Int): List<Account> {

        val baseUsername = "jamitest"
        val time = System.currentTimeMillis()
        val accountService = JamiApplication.instance!!.mAccountService

        val accountObservableList = (0..<count).map { accountCount ->
            val username = "${baseUsername}_${time}_${accountCount}"
            Log.d(TAG, "Account username: $username...")
            accountService.getAccountTemplate(AccountConfig.ACCOUNT_TYPE_JAMI)
                .map { accountDetails: HashMap<String, String> ->
                    accountDetails[ConfigKey.ACCOUNT_ALIAS.key] = "Jami account $accountCount"
                    accountDetails[ConfigKey.RINGNS_HOST.key] = NAME_SERVER_ADDRESS
                    accountDetails
                }.flatMapObservable { details ->
                    Log.d(TAG, "Adding account ...")
                    accountService.addAccount(details)
                }
                .filter { account: Account ->
                    account.registrationState != AccountConfig.RegistrationState.INITIALIZING
                }
                .firstOrError()
                .map { account: Account ->
                    Log.d(TAG, "Registering account ...")
                    accountService.registerName(
                        account, username, AccountService.ACCOUNT_SCHEME_PASSWORD, ""
                    )
                    account
                }
        }

        // Wait for all accounts to be created.
        val accountList: List<Account> =
            Single.zip(accountObservableList) { it.filterIsInstance<Account>() }.blockingGet()

        // Wait for all accounts to be registered.
        Single.zip(
            accountList.map {
                accountService.getObservableAccount(it)
                    .filter { account: Account ->
                        account.registrationState == AccountConfig.RegistrationState.REGISTERED
                    }.firstOrError()
            }
        ) { it }.blockingSubscribe()

        return accountList
    }

    /**
     * Remove all accounts.
     */
    fun removeAllAccounts() =
        JamiApplication.instance!!.mAccountService.observableAccountList.blockingFirst()
            .forEach { JamiApplication.instance!!.mAccountService.removeAccount(it.accountId) }
}

class ImageProvider {

    /**
     * Download images from a list of URLs and save them to the cache directory.
     * @param context The context.
     * @param count The number of images to download (Max=2).
     */
    fun downloadImagesToUri(context: Context, count: Int): List<Uri> {
        if (count > imagesUrlList.size)
            throw Exception("Count is greater than the number of images available.")

        runBlocking {
            val deferredResult = CompletableDeferred<Boolean>()
            CoroutineScope(Dispatchers.IO).launch {
                imagesUrlList.forEach {
                    downloadImageToUri(it, context)?.let { uri ->
                        Log.w(TAG, "Image downloaded to: $uri")
                        downloadedImagesUri.add(uri)
                    } ?: throw Exception("Image download failed.")
                }
                deferredResult.complete(true)
            }
            // This will block the test until the image is downloaded.
            deferredResult.await()
        }

        return downloadedImagesUri
    }

    private val imagesUrlList = listOf(
        "https://jami.net/content/images/2024/05/pexels-los-muertos-crew-7205804.jpg",
        "https://jami.net/content/images/2024/06/pexels-tkirkgoz-11423986.jpg"
    )

    private var downloadedImagesUri: MutableList<Uri> = mutableListOf()

    private var TAG = ImageProvider::class.java.simpleName

    /**
     * Download an image from a URL.
     */
    private fun downloadImageToUri(imageUrl: String, context: Context): Uri? {
        val client = OkHttpClient()
        val request = Request.Builder().url(imageUrl).build()

        return try {
            // Send the request and get the response.
            val response = client.newCall(request).execute()
            val inputStream: InputStream? = response.body?.byteStream()

            // Save the image to a file.
            inputStream?.let {
                val file = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(file)

                val buffer = ByteArray(1024)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.close()
                inputStream.close()

                // Returns the URI.
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}