package cx.ring.fragments;

import android.content.Intent;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.adapters.SmartListAdapter;
import cx.ring.application.JamiApplication;
import cx.ring.client.HomeActivity;
import cx.ring.contacts.AvatarFactory;
import cx.ring.databinding.FragContactPickerBinding;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.CallContact;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.utils.ConversationPath;
import cx.ring.viewholders.SmartListViewHolder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class ContactPickerFragment extends BottomSheetDialogFragment {

    public static final String TAG = ContactPickerFragment.class.getSimpleName();

    private FragContactPickerBinding binding = null;
    private SmartListAdapter adapter;
    private final CompositeDisposable mDisposableBag = new CompositeDisposable();

    private String mAccountId =  null;
    private final Set<CallContact> mCurrentSelection = new HashSet<>();

    @Inject
    ConversationFacade mConversationFacade;

    public ContactPickerFragment() {
        // Required empty public constructor
    }

    public static ContactPickerFragment newInstance() {
        return new ContactPickerFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((JamiApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mDisposableBag.add(mConversationFacade.getSmartList()
                .switchMap(viewModels -> viewModels.isEmpty() ? SmartListViewModel.EMPTY_RESULTS
                        : Observable.combineLatest(viewModels, obs -> {
                    List<SmartListViewModel> vms = new ArrayList<>(obs.length);
                    for (Object ob : obs)
                        vms.add((SmartListViewModel) ob);
                    return vms;
                }))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(conversations -> {
                    if (binding == null)
                        return;
                    adapter.update(conversations);
                }));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragContactPickerBinding.inflate(getLayoutInflater(), container, false);
        adapter = new SmartListAdapter(null, new SmartListViewHolder.SmartListListeners() {
            @Override
            public void onItemClick(SmartListViewModel item) {
                if (!mCurrentSelection.add(item.getContact().get(0)))
                    return;
                mAccountId = item.getAccountId();
                Chip chip = new Chip(requireContext(), null, R.style.Widget_MaterialComponents_Chip_Entry);
                chip.setText(item.getContactName());
                chip.setChipIcon(AvatarFactory.getAvatar(binding.getRoot().getContext(), item.getContact()).blockingGet());
                chip.setCloseIconVisible(true);
                chip.setOnCloseIconClickListener(v -> {
                    mCurrentSelection.remove(item.getContact().get(0));
                    binding.selectedContacts.removeView(chip);
                });
                binding.selectedContacts.addView(chip);
            }

            @Override
            public void onItemLongClick(SmartListViewModel item) {

            }
        });
        binding.createGroupBtn.setOnClickListener(v -> mDisposableBag.add(mConversationFacade.createConversation(mAccountId, mCurrentSelection)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(conversation -> startActivity(new Intent(Intent.ACTION_VIEW, ConversationPath.toUri(mAccountId, conversation.getUri()), v.getContext(), HomeActivity.class)))));
        binding.contactList.setAdapter(adapter);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        mDisposableBag.clear();
        binding = null;
        adapter = null;
        super.onDestroyView();
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }
}