package cx.ring.account;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import cx.ring.R;

public class SettingsAdapter extends ArrayAdapter<SettingItem> {

    private Context mContext;

    public SettingsAdapter(@NonNull Context context, int resource, @NonNull List<SettingItem> objects) {
        super(context, resource, objects);
        mContext = context;
    }

    private class ViewHolder {
        TextView title;
        ImageView icon;
        FloatingActionButton fab;
    }

    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder = null;
        SettingItem item = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (view == null) {
            view = mInflater.inflate(R.layout.item_setting, null);
            holder = new ViewHolder();
            holder.title = view.findViewById(R.id.title);
            holder.icon = view.findViewById(R.id.icon);
            view.setTag(holder);
        } else
            holder = (ViewHolder) view.getTag();

        holder.title.setText(mContext.getString(item.getTitleRes()));
        holder.icon.setImageResource(item.getImageId());
        holder.icon.setColorFilter(ContextCompat.getColor(mContext, R.color.white),
                android.graphics.PorterDuff.Mode.SRC_IN);

        return view;
    }

}
