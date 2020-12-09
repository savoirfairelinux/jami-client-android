package cx.ring.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cx.ring.daemon.Ringservice;
import cx.ring.databinding.FragPluginHandlersListBinding;
import cx.ring.plugins.PluginUtils;
import cx.ring.settings.pluginssettings.PluginDetails;
import cx.ring.settings.pluginssettings.PluginsListAdapter;
import cx.ring.utils.ConversationPath;


public class PluginHandlersListFragment extends Fragment implements PluginsListAdapter.PluginListItemListener {
    public static final String TAG = "PluginListHandlers";
    private FragPluginHandlersListBinding binding;
    private PluginsListAdapter mAdapter;
    private ConversationPath mPath;


    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragPluginHandlersListBinding.inflate(inflater, container, false);

        binding.handlerList.setHasFixedSize(true);
        mAdapter = new PluginsListAdapter(PluginUtils.getChatHandlersDetails(binding.handlerList.getContext(), mPath.getAccountId(), mPath.getConversationId()), this);
        binding.handlerList.setAdapter(mAdapter);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mPath = ConversationPath.fromBundle(args);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.chatPluginsToolbar.setVisibility(View.VISIBLE);
        binding.chatPluginsToolbar.setOnClickListener(v -> {
            Fragment fragment = getParentFragment();
            if (fragment instanceof ConversationFragment) {
                ConversationFragment parent = (ConversationFragment) fragment;
                parent.hidePluginListHandlers();
            }
        });
    }

    public static PluginHandlersListFragment newInstance(String accountId, String peerId) {
        PluginHandlersListFragment fragment = new PluginHandlersListFragment();

        Bundle args = ConversationPath.toBundle(accountId, peerId);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onPluginItemClicked(PluginDetails pluginDetails) {
        Ringservice.toggleChatHandler(pluginDetails.getmHandlerId(), mPath.getAccountId(), mPath.getConversationId(), pluginDetails.isEnabled());
    }

    @Override
    public void onPluginEnabled(PluginDetails pluginDetails) {
        Ringservice.toggleChatHandler(pluginDetails.getmHandlerId(), mPath.getAccountId(), mPath.getConversationId(), pluginDetails.isEnabled());
    }
}
