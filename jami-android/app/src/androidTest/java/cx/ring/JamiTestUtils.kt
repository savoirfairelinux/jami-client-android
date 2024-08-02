package cx.ring

import io.reactivex.rxjava3.core.Single
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.services.AccountService
import java.util.HashMap


object AccountUtils {

    private val TAG = AccountUtils::class.java.simpleName

    private const val NAME_SERVER_ADDRESS = "https://ns-test.jami.net"

    /**
     * Create n accounts and register them.
     * This function is blocking.
     *
     * @param accountService The account service to use.
     * @param count The number of accounts to create.
     * @return The list of registered account names.
     */
    fun createAccountAndRegister(accountService: AccountService, count: Int): List<Account> {

        val baseUsername = "jamitest"
        val time = System.currentTimeMillis()

        val accountObservableList = (0..<count).map { accountCount ->
            val username = "${baseUsername}_${time}_${accountCount}"
            net.jami.utils.Log.d(TAG, "Account username: $username...")
            accountService.getAccountTemplate(AccountConfig.ACCOUNT_TYPE_JAMI)
                .map { accountDetails: HashMap<String, String> ->
                    accountDetails[ConfigKey.ACCOUNT_ALIAS.key] = "Jami account $accountCount"
                    accountDetails[ConfigKey.RINGNS_HOST.key] = NAME_SERVER_ADDRESS
                    accountDetails
                }.flatMapObservable { details ->
                    net.jami.utils.Log.d(TAG, "Adding account ...")
                    accountService.addAccount(details)
                }
                .filter { account: Account ->
                    account.registrationState != AccountConfig.RegistrationState.INITIALIZING
                }
                .firstOrError()
                .map { account: Account ->
                    net.jami.utils.Log.d(TAG, "Registering account ...")
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
     *
     * @param accountService The account service to use.
     */
    fun removeAllAccounts(accountService: AccountService) {
        accountService.observableAccountList.blockingFirst().forEach {
            accountService.removeAccount(it.accountId)
        }
    }
}