package cx.ring.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cx.ring.account.AccountEditionFragment
import cx.ring.databinding.FragPluginHandlersListBinding
import cx.ring.plugins.PluginUtils
import cx.ring.settings.pluginssettings.PluginDetails
import cx.ring.settings.pluginssettings.PluginsListAdapter
import cx.ring.settings.pluginssettings.PluginsListAdapter.PluginListItemListener
import cx.ring.utils.ConversationPath
import net.jami.daemon.JamiService

class PluginHandlersListFragment : Fragment(), PluginListItemListener {
    private var binding: FragPluginHandlersListBinding? = null
    private lateinit var mPath: ConversationPath

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPath = ConversationPath.fromBundle(requireArguments())!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragPluginHandlersListBinding.inflate(inflater, container, false).also { b ->
            b.handlerList.setHasFixedSize(true)
            b.handlerList.adapter = PluginsListAdapter(
                PluginUtils.getChatHandlersDetails(b.handlerList.context, mPath.accountId, mPath.conversationId.removePrefix("swarm:")), this, "")
            binding = b
        }.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.chatPluginsToolbar.visibility = View.VISIBLE
        binding!!.chatPluginsToolbar.setOnClickListener { v: View? ->
            val fragment = parentFragment
            if (fragment is ConversationFragment) {
                fragment.hidePluginListHandlers()
            }
        }
    }

    override fun onPluginItemClicked(pluginDetails: PluginDetails) {
        JamiService.toggleChatHandler(pluginDetails.handlerId, mPath.accountId, mPath.conversationId.removePrefix("swarm:"), pluginDetails.isEnabled)
    }

    override fun onPluginEnabled(pluginDetails: PluginDetails) {
        JamiService.toggleChatHandler(pluginDetails.handlerId, mPath.accountId, mPath.conversationId.removePrefix("swarm:"), pluginDetails.isEnabled)
    }

    companion object {
        val TAG = PluginHandlersListFragment::class.simpleName!!
        fun newInstance(accountId: String, peerId: String): PluginHandlersListFragment {
            val fragment = PluginHandlersListFragment()
            fragment.arguments = ConversationPath.toBundle(accountId, peerId)
            return fragment
        }
    }
}