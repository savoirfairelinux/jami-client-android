package cx.ring.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cx.ring.adapters.ConversationMediaGalleryAdapter
import cx.ring.databinding.FragConversationGalleryBinding
import cx.ring.utils.ConversationPath
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.DataTransfer
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.DeviceRuntimeService
import javax.inject.Inject
import javax.inject.Singleton

@AndroidEntryPoint
class ConversationGalleryFragment : Fragment() {
    private val disposableBag = CompositeDisposable()
    private var binding: FragConversationGalleryBinding? = null
    private lateinit var conversationPath: ConversationPath
    private var adapter: ConversationMediaGalleryAdapter? = null

    @Inject
    @Singleton
    lateinit var accountService: AccountService

    @Inject
    @Singleton
    lateinit var deviceRuntimeService: DeviceRuntimeService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationPath = ConversationPath.fromBundle(arguments)!!
        adapter = ConversationMediaGalleryAdapter(this, deviceRuntimeService)
        disposableBag.add(accountService.searchConversation(
            conversationPath.accountId,
            conversationPath.conversationUri,
            type = "application/data-transfer+json"
        )
            .map { results -> results.results.mapNotNull { i -> if (i is DataTransfer /*&& i.isComplete*/) i else null } }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { adapter?.addSearchResults(it) })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragConversationGalleryBinding.inflate(inflater, container, false).apply {
            resultList.adapter = adapter
            binding = this
        }.root

    override fun onDestroy() {
        disposableBag.dispose()
        adapter = null
        super.onDestroy()
    }

    companion object {
        fun newInstance(accountId: String, conversationId: Uri) = ConversationGalleryFragment().apply {
            arguments = ConversationPath.toBundle(accountId, conversationId)
        }
    }
}
