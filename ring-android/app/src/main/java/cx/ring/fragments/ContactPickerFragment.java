package cx.ring.fragments;

import android.app.Dialog;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.adapters.SmartListAdapter;
import cx.ring.client.HomeActivity;
import cx.ring.databinding.FragContactPickerBinding;
import net.jami.facades.ConversationFacade;
import net.jami.model.Contact;
import net.jami.smartlist.SmartListViewModel;
import cx.ring.viewholders.SmartListViewHolder;
import cx.ring.views.AvatarDrawable;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@AndroidEntryPoint
public class ContactPickerFragment extends BottomSheetDialogFragment {

    public static final String TAG = ContactPickerFragment.class.getSimpleName();

    private FragContactPickerBinding binding = null;
    private SmartListAdapter adapter;
    private final CompositeDisposable mDisposableBag = new CompositeDisposable();

    private String mAccountId =  null;
    private final Set<Contact> mCurrentSelection = new HashSet<>();

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
        //setRetainInstance(true);
        //((JamiApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog bdialog = super.onCreateDialog(savedInstanceState);
        if (bdialog instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) bdialog;
            BottomSheetBehavior<FrameLayout> behavior = dialog.getBehavior();
            behavior.setFitToContents(false);
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        }
        return bdialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mDisposableBag.add(mConversationFacade.getContactList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(conversations -> {
                    if (binding == null)
                        return;
                    adapter.update(conversations);
                }));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragContactPickerBinding.inflate(getLayoutInflater(), container, false);
        adapter = new SmartListAdapter(null, new SmartListViewHolder.SmartListListeners() {
            @Override
            public void onItemClick(SmartListViewModel item) {
                mAccountId = item.getAccountId();

                boolean checked = !item.isChecked();
                item.setChecked(checked);
                adapter.update(item);

                Runnable remover = () -> {
                    mCurrentSelection.remove(item.getContacts().get(0));
                    if (mCurrentSelection.isEmpty())
                        binding.createGroupBtn.setEnabled(false);
                    item.setChecked(false);
                    adapter.update(item);
                    View v = binding.selectedContacts.findViewWithTag(item);
                    if (v != null)
                        binding.selectedContacts.removeView(v);
                };

                if (checked) {
                    if (mCurrentSelection.add(item.getContacts().get(0))) {
                        Chip chip = new Chip(requireContext(), null, R.style.Widget_MaterialComponents_Chip_Entry);
                        chip.setText(item.getContactName());
                        chip.setChipIcon(new AvatarDrawable.Builder()
                                .withViewModel(item)
                                .withCircleCrop(true)
                                .withCheck(false)
                                .build(binding.getRoot().getContext()));
                        chip.setCloseIconVisible(true);
                        chip.setTag(item);
                        chip.setOnCloseIconClickListener(v -> remover.run());
                        binding.selectedContacts.addView(chip);
                    }
                    binding.createGroupBtn.setEnabled(true);
                } else {
                    remover.run();
                }
            }

            @Override
            public void onItemLongClick(SmartListViewModel item) {

            }
        }, mDisposableBag);
        binding.createGroupBtn.setOnClickListener(v -> mDisposableBag.add(mConversationFacade.createConversation(mAccountId, mCurrentSelection)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(conversation -> {
                    ((HomeActivity) requireActivity()).startConversation(mAccountId, conversation.getUri());
                    Dialog dialog = getDialog();
                    if (dialog != null)
                        dialog.cancel();
                })));
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