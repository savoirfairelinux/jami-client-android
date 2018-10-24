package cx.ring.client;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.facades.ConversationFacade;
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.CallContact;
import cx.ring.views.AvatarDrawable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class ContactDetailsActivity extends AppCompatActivity {

    @Inject
    @Singleton
    ConversationFacade mConversationFacade;

    @BindView(R.id.contact_image)
    ImageView contactView;

    @BindView(R.id.contact_action_list)
    RecyclerView contactActionList;

    private SharedPreferences mPreferences;

    private CallContact mContact = null;

    interface IContactAction {
        void onAction();
    }

    class ContactAction {
        final int icon;
        int iconTint;
        final String title;
        final IContactAction callback;

        ContactAction(int i, String t) {
            icon = i;
            iconTint = Color.BLACK;
            title = t;
            callback = null;
        }

        ContactAction(int i, int tint, String t, IContactAction cb) {
            icon = i;
            iconTint = tint;
            title = t;
            callback = cb;
        }

        void setIconTint(int tint) {
            iconTint = tint;
        }
    }

    class ContactActionView extends RecyclerView.ViewHolder {
        @BindView(R.id.action_icon)
        ImageView icon;

        @BindView(R.id.action_title)
        TextView description;

        IContactAction callback;

        public ContactActionView(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(view -> {
                if (callback != null)
                    callback.onAction();
            });
        }
    }

    class ContactActionAdapter extends RecyclerView.Adapter<ContactActionView> {
        private final ArrayList<ContactAction> actions = new ArrayList<>();

        @NonNull
        @Override
        public ContactActionView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_action, parent, false);
            return new ContactActionView(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ContactActionView holder, int position) {
            ContactAction action = actions.get(position);
            holder.icon.setImageResource(action.icon);
            ImageViewCompat.setImageTintList(holder.icon, ColorStateList.valueOf(action.iconTint));
            holder.description.setText(action.title);
            holder.callback = action.callback;
        }

        @Override
        public int getItemCount() {
            return actions.size();
        }
    }

    private final ContactActionAdapter adapter = new ContactActionAdapter();
    private final CompositeDisposable mDisposableBag = new CompositeDisposable();

    private ContactAction colorAction;
    private int colorActionPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_details);
        ButterKnife.bind(this);
        RingApplication.getInstance().getRingInjectionComponent().inject(this);

        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.toolbar_layout);
        collapsingToolbarLayout.setTitle("");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri != null) {
            List<String> segments = uri.getPathSegments();
            if (segments.size() >= 3) {
                String account = segments.get(1);
                String contactUri = segments.get(2);
                mDisposableBag.add(mConversationFacade
                        .startConversation(account, new cx.ring.model.Uri(contactUri))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(conversation -> {
                            CallContact contact = conversation.getContact();
                            mPreferences = getSharedPreferences(conversation.getLastAccountUsed() + "_" + conversation.getContact().getPrimaryNumber(), Context.MODE_PRIVATE);
                            int color = mPreferences.getInt(ConversationFragment.KEY_PREFERENCE_CONVERSATION_COLOR, getResources().getColor(R.color.color_primary_light));
                            colorAction.setIconTint(color);
                            adapter.notifyItemChanged(colorActionPosition);
                            collapsingToolbarLayout.setBackgroundColor(color);
                            collapsingToolbarLayout.setTitle(contact.getDisplayName());
                            collapsingToolbarLayout.setContentScrimColor(color);
                            contactView.setImageDrawable(new AvatarDrawable(this, contact, false));
                            mContact = contact;
                        }));

                adapter.actions.add(new ContactAction(R.drawable.ic_call_white, "Start Audio Call"));
                adapter.actions.add(new ContactAction(R.drawable.ic_videocam_white, "Start Video Call"));
                adapter.actions.add(new ContactAction(R.drawable.ic_chat_white, "Send Message"));
                adapter.actions.add(new ContactAction(R.drawable.baseline_clear_all_24, "Clear history"));
                adapter.actions.add(new ContactAction(R.drawable.ic_block_white, "Block contact"));
                colorAction = new ContactAction(R.drawable.item_color_background, 0, "Choose color", () -> {
                    ColorChooserBottomSheet frag = new ColorChooserBottomSheet();
                    frag.setCallback(color -> {
                        collapsingToolbarLayout.setBackgroundColor(color);
                        collapsingToolbarLayout.setContentScrimColor(color);
                        colorAction.setIconTint(color);
                        adapter.notifyItemChanged(colorActionPosition);
                        mPreferences.edit().putInt(ConversationFragment.KEY_PREFERENCE_CONVERSATION_COLOR, color).apply();
                    });
                    frag.show(getSupportFragmentManager(), "colorChooser");
                });
                adapter.actions.add(colorAction);
                colorActionPosition = adapter.actions.size()-1;
                contactActionList.setAdapter(adapter);
            }
        }
    }

    @Override
    protected void onDestroy() {
        adapter.actions.clear();
        mDisposableBag.clear();
        super.onDestroy();
    }
}
