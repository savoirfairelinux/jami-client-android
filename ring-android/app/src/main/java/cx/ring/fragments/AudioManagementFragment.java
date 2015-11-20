/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *          Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package cx.ring.fragments;

import java.io.File;
import java.util.ArrayList;

import android.content.Intent;
import cx.ring.R;
import cx.ring.model.account.AccountDetail;
import cx.ring.model.account.AccountDetailAdvanced;
import cx.ring.model.account.Account;
import cx.ring.model.Codec;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;
import cx.ring.views.dragsortlv.DragSortListView;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class AudioManagementFragment extends PreferenceFragment
{
    static final String TAG = AudioManagementFragment.class.getSimpleName();

    protected Callbacks mCallbacks = sDummyCallbacks;
    ArrayList<Codec> codecs;
    private DragSortListView mCodecList;
    CodecAdapter listAdapter;
    private static final Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public IDRingService getRemoteService() {
            return null;
        }
        @Override
        public LocalService getService() {
            return null;
        }
        @Override
        public Account getAccount() {
            return null;
        }
    };

    public interface Callbacks extends LocalService.Callbacks {
        Account getAccount();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
        try {
            codecs = (ArrayList<Codec>) mCallbacks.getRemoteService().getCodecList(mCallbacks.getAccount().getAccountID());
            //mCallbacks.getService().getRingtoneList();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (from != to) {
                Codec item = listAdapter.getItem(from);
                listAdapter.remove(item);
                listAdapter.insert(item, to);
                try {
                    mCallbacks.getRemoteService().setActiveCodecList(getActiveCodecList(), mCallbacks.getAccount().getAccountID());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private ListView mPrefsList;

    public ArrayList<Long> getActiveCodecList() {
        ArrayList<Long> results = new ArrayList<>();
        for (int i = 0; i < listAdapter.getCount(); ++i) {
            if (listAdapter.getItem(i).isEnabled()) {
                results.add(listAdapter.getItem(i).getPayload());
            }
        }
        return results;
    }

    private static final int SELECT_RINGTONE_PATH = 40;
    private Preference.OnPreferenceClickListener filePickerListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            performFileSearch(SELECT_RINGTONE_PATH);
            return true;
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_CANCELED)
            return;

        File myFile = new File(data.getData().getPath());
        Log.i(TAG, "file selected:" + data.getData());
        if (requestCode == SELECT_RINGTONE_PATH) {
            findPreference(AccountDetailAdvanced.CONFIG_RINGTONE_PATH).setSummary(myFile.getName());
            mCallbacks.getAccount().getAdvancedDetails().setDetailString(AccountDetailAdvanced.CONFIG_RINGTONE_PATH, myFile.getAbsolutePath());
            mCallbacks.getAccount().notifyObservers();
        }

    }

    public void performFileSearch(int requestCodeToSet) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, requestCodeToSet);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = LayoutInflater.from(getActivity()).inflate(R.layout.frag_audio_mgmt, null);

        mPrefsList = (ListView) rootView.findViewById(android.R.id.list);
        mCodecList = (DragSortListView) rootView.findViewById(R.id.dndlistview);
        mCodecList.setAdapter(listAdapter);
        mCodecList.setDropListener(onDrop);
        mCodecList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                listAdapter.getItem(pos).toggleState();
                listAdapter.notifyDataSetChanged();
                try {
                    mCallbacks.getRemoteService().setActiveCodecList(getActiveCodecList(), mCallbacks.getAccount().getAccountID());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final LinearLayout holder = (LinearLayout) getView().findViewById(R.id.lv_holder);
        final LinearLayout holder_prefs = (LinearLayout) getView().findViewById(R.id.lv_holder_prefs);
        holder.post(new Runnable() {

            @Override
            public void run() {
                setListViewHeight(mCodecList, holder);
                setListViewHeight(mPrefsList, holder_prefs);
            }
        });

    }

    // Sets the ListView holder's height
    public void setListViewHeight(ListView listView, LinearLayout llMain) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        int firstHeight;
        int desiredWidth = MeasureSpec.makeMeasureSpec(listView.getWidth(), MeasureSpec.AT_MOST);

        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
            firstHeight = listItem.getMeasuredHeight();
            totalHeight += firstHeight;
        }

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) llMain.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount()));
        llMain.setLayoutParams(params);
        getView().requestLayout();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.account_audio_prefs);
        listAdapter = new CodecAdapter(getActivity());
        listAdapter.setDataset(codecs);

        setPreferenceDetails(mCallbacks.getAccount().getAdvancedDetails());
        findPreference(AccountDetailAdvanced.CONFIG_RINGTONE_PATH).setEnabled(
                ((SwitchPreference) findPreference(AccountDetailAdvanced.CONFIG_RINGTONE_ENABLED)).isChecked());
        addPreferenceListener(mCallbacks.getAccount().getAdvancedDetails(), changeAudioPreferenceListener);
    }

    Preference.OnPreferenceChangeListener changeAudioPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference instanceof SwitchPreference) {
                if (preference.getKey().contentEquals(AccountDetailAdvanced.CONFIG_RINGTONE_ENABLED))
                    getPreferenceScreen().findPreference(AccountDetailAdvanced.CONFIG_RINGTONE_PATH).setEnabled((Boolean) newValue);
                mCallbacks.getAccount().getAdvancedDetails().setDetailString(preference.getKey(), newValue.toString());
            } else {
                if (preference.getKey().contentEquals(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE)) {
                    preference.setSummary(((String)newValue).contentEquals("overrtp") ? "RTP" : "SIP");
                } else {
                    preference.setSummary((CharSequence) newValue);
                    Log.i(TAG, "Changing" + preference.getKey() + " value:" + newValue);
                    mCallbacks.getAccount().getAdvancedDetails().setDetailString(preference.getKey(), newValue.toString());
                }
            }
            mCallbacks.getAccount().notifyObservers();

            return true;
        }
    };

    private void setPreferenceDetails(AccountDetail details) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Log.i(TAG, "setPreferenceDetails: pref " + p.mKey + " value " + p.mValue);
            Preference pref = findPreference(p.mKey);
            if (pref != null) {
                if (!p.isTwoState) {
                    if (p.mKey.contentEquals(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE)) {
                        pref.setDefaultValue(p.mValue.contentEquals("overrtp") ? "RTP" : "SIP");
                        pref.setSummary(p.mValue.contentEquals("overrtp") ? "RTP" : "SIP");
                    } else {
                        if(pref.getKey().contentEquals(AccountDetailAdvanced.CONFIG_RINGTONE_PATH)){
                            File tmp = new File(p.mValue);
                            pref.setSummary(tmp.getName());
                        } else
                            pref.setSummary(p.mValue);
                    }

                } else {
                    ((SwitchPreference) pref).setChecked(p.mValue.contentEquals("true"));
                }

            } else {
                Log.w(TAG, "pref not found");
            }
        }
    }

    private void addPreferenceListener(AccountDetail details, Preference.OnPreferenceChangeListener listener) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Log.i(TAG, "addPreferenceListener: pref " + p.mKey + p.mValue);
            Preference pref = findPreference(p.mKey);
            if (pref != null) {
                pref.setOnPreferenceChangeListener(listener);
                if (pref.getKey().contentEquals(AccountDetailAdvanced.CONFIG_RINGTONE_PATH))
                    pref.setOnPreferenceClickListener(filePickerListener);
            } else {
                Log.w(TAG, "addPreferenceListener: pref not found");
            }
        }
    }

    public static class CodecAdapter extends BaseAdapter {

        ArrayList<Codec> items;
        private Context mContext;

        public CodecAdapter(Context context) {
            items = new ArrayList<Codec>();
            mContext = context;
        }

        public void insert(Codec item, int to) {
            items.add(to, item);
            notifyDataSetChanged();
        }

        public void remove(Codec item) {
            items.remove(item);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Codec getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            View rowView = convertView;
            CodecView entryView;

            if (rowView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                rowView = inflater.inflate(R.layout.item_codec, null);

                entryView = new CodecView();
                entryView.name = (TextView) rowView.findViewById(R.id.codec_name);
                entryView.samplerate = (TextView) rowView.findViewById(R.id.codec_samplerate);
                entryView.enabled = (CheckBox) rowView.findViewById(R.id.codec_checked);
                rowView.setTag(entryView);
            } else {
                entryView = (CodecView) rowView.getTag();
            }

            Codec codec = items.get(pos);

            if (codec.isSpeex())
                entryView.samplerate.setVisibility(View.VISIBLE);
            else
                entryView.samplerate.setVisibility(View.GONE);

            entryView.name.setText(codec.getName());
            entryView.samplerate.setText(codec.getSampleRate());
            entryView.enabled.setChecked(codec.isEnabled());

            return rowView;

        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isEmpty() {
            return getCount() == 0;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        public void setDataset(ArrayList<Codec> codecs) {
            items = new ArrayList<>(codecs.size());
            for (Codec c : codecs)
                if (c.getType() == Codec.Type.AUDIO)
                    items.add(c);
        }

        /**
         * ******************
         * ViewHolder Pattern
         * *******************
         */
        public class CodecView {
            public TextView name;
            public TextView samplerate;
            public CheckBox enabled;
        }
    }
}
