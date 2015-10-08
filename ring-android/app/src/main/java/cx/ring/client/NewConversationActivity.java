package cx.ring.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.SearchView;

import cx.ring.R;
import cx.ring.fragments.ContactListFragment;
import cx.ring.model.CallContact;

public class NewConversationActivity extends Activity implements ContactListFragment.Callbacks {

    //private SearchView searchView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_new_conversation);
    }
/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.newconv_option_menu, menu);
        //searchView = (SearchView) menu.findItem(R.id.contact_search).getActionView();
        return true;
    }
*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCallContact(final CallContact c) {
        if (c.getPhones().size() > 1) {
            final CharSequence colors[] = new CharSequence[c.getPhones().size()];
            int i = 0;
            for (CallContact.Phone p : c.getPhones())
                colors[i++] = p.getNumber();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose a number");
            builder.setItems(colors, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CharSequence selected = colors[which];
                    Intent intent = new Intent()
                            .setClass(NewConversationActivity.this, ConversationActivity.class)
                            .setAction(Intent.ACTION_VIEW)
                            .setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, c.getIds().get(0)))
                            .putExtra("number", selected);
                    startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
                }
            });
            builder.show();
        } else {
            Intent intent = new Intent()
                    .setClass(this, ConversationActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, c.getIds().get(0)));
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
        }
    }

    @Override
    public void onTextContact(final CallContact c) {
        if (c.getPhones().size() > 1) {
            final CharSequence colors[] = new CharSequence[c.getPhones().size()];// {"red", "green", "blue", "black"};
            int i = 0;
            for (CallContact.Phone p : c.getPhones())
                colors[i++] = p.getNumber();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose a number");
            builder.setItems(colors, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CharSequence selected = colors[which];
                    Intent intent = new Intent()
                            .setClass(NewConversationActivity.this, ConversationActivity.class)
                            .setAction(Intent.ACTION_VIEW)
                            .setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, c.getIds().get(0)))
                            .putExtra("number", selected);
                    startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
                }
            });
            builder.show();
        } else {
            Intent intent = new Intent()
                    .setClass(this, ConversationActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, c.getIds().get(0)));
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CONVERSATION);
        }
    }

    @Override
    public void onContactDragged() {

    }

    @Override
    public void toggleDrawer() {

    }

    @Override
    public void onEditContact(CallContact item) {

    }

    @Override
    public void setDragView(RelativeLayout relativeLayout) {

    }

    @Override
    public void toggleForSearchDrawer() {

    }
/*
    @Override
    public SearchView getSearchView() {
        return searchView;
    }*/
}
