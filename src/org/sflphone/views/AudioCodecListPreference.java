//package org.sflphone.views;
//
//import java.util.ArrayList;
//
//import org.sflphone.R;
//import org.sflphone.model.Codec;
//import org.sflphone.views.dragsortlv.DragSortListView;
//
//import android.content.Context;
//import android.preference.Preference;
//import android.util.AttributeSet;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.AdapterView;
//import android.widget.AdapterView.OnItemClickListener;
//import android.widget.BaseAdapter;
//import android.widget.CheckBox;
//import android.widget.LinearLayout.LayoutParams;
//import android.widget.TextView;
//
//public class AudioCodecListPreference extends Preference {
//
//    CodecAdapter listAdapter;
//
//    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
//        @Override
//        public void drop(int from, int to) {
//            if (from != to) {
//                Codec item = listAdapter.getItem(from);
//                listAdapter.remove(item);
//                listAdapter.insert(item, to);
//
//            }
//        }
//
//    };
//
//    private ArrayList<Codec> originalStates;
//
//    public AudioCodecListPreference(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        listAdapter = new CodecAdapter(getContext());
//
//    }
//
//    @Override
//    protected View onCreateView(ViewGroup parent) {
//
//        DragSortListView v = (DragSortListView) LayoutInflater.from(getContext()).inflate(R.layout.dialog_codecs_list, null);
//
//        v.setDropListener(onDrop);
//        v.setAdapter(listAdapter);
//        v.setOnItemClickListener(new OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
//                listAdapter.getItem(pos).toggleState();
//                listAdapter.notifyDataSetChanged();
//                callChangeListener(listAdapter.getDataset());
//                setList(listAdapter.getDataset());
//
//            }
//        });
//        // layout.addView(v, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
//        return v;
//    }
//
//    @Override
//    public View getView(final View convertView, final ViewGroup parent) {
//        final View v = super.getView(convertView, parent);
//        final int width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
//        final int height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
//        final LayoutParams params = new LayoutParams(width, 600);
//        v.setLayoutParams(params);
//        return v;
//    }
//
//    // @Override
//    // protected void onPrepareDialogBuilder(Builder builder) {
//    //
//    // builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
//    //
//    // @Override
//    // public void onClick(DialogInterface dialog, int which) {
//    // callChangeListener(listAdapter.getDataset());
//    // setList(listAdapter.getDataset());
//    // }
//    // });
//    //
//    // builder.setNegativeButton(android.R.string.no, new OnClickListener() {
//    //
//    // @Override
//    // public void onClick(DialogInterface dialog, int which) {
//    // setList(originalStates);
//    // }
//    // });
//    // super.onPrepareDialogBuilder(builder);
//    // }
//
//    // @Override
//    // protected View onCreateDialogView() {
//    // // LinearLayout layout = new LinearLayout(getContext());
//    // // layout.setOrientation(LinearLayout.VERTICAL);
//    // // layout.setPadding(6, 6, 6, 6);
//    //
//    // DragSortListView v = (DragSortListView) LayoutInflater.from(getContext()).inflate(R.layout.dialog_codecs_list, null);
//    //
//    // v.setDropListener(onDrop);
//    // v.setAdapter(listAdapter);
//    // v.setOnItemClickListener(new OnItemClickListener() {
//    //
//    // @Override
//    // public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
//    // listAdapter.getItem(pos).toggleState();
//    // listAdapter.notifyDataSetChanged();
//    //
//    // }
//    // });
//    // // layout.addView(v, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
//    // return v;
//    // }
//
//    
//
//    public void setList(ArrayList<Codec> codecs) {
//        originalStates = new ArrayList<Codec>(codecs.size());
//        for (Codec c : codecs) {
//            originalStates.add(new Codec(c));
//        }
//        listAdapter.setDataset(codecs);
//        listAdapter.notifyDataSetChanged();
//    }
//
//    public ArrayList<String> getActiveCodecList() {
//        ArrayList<String> results = new ArrayList<String>();
//        for (int i = 0; i < listAdapter.getCount(); ++i) {
//            if (listAdapter.getItem(i).isEnabled()) {
//                results.add(listAdapter.getItem(i).getPayload().toString());
//            }
//        }
//        return results;
//    }
//
//}
