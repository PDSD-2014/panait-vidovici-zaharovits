package com.eHonk;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.eHonk.Database.OffenseRecord;

public class SentNotificationsArrayAdapter extends ArrayAdapter<OffenseRecord> {
	private final Activity context;
  private final ArrayList<Database.OffenseRecord> offenses;

  static class ViewHolder {
    public ProgressBar progress;
    public TextView text;
  }

  public SentNotificationsArrayAdapter(Activity context, ArrayList<Database.OffenseRecord> offenses) {
    super(context, R.layout.notifications_row_layout, offenses);
    this.context = context;
    this.offenses = offenses;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    
  	View rowView = convertView;

    if (rowView == null) {
      LayoutInflater inflater = context.getLayoutInflater();
      rowView = inflater.inflate(R.layout.notifications_row_layout, null);
      // configure view holder
      ViewHolder viewHolder = new ViewHolder();
      viewHolder.text = (TextView) rowView.findViewById(R.id.offense_label);
      viewHolder.progress = (ProgressBar) rowView
          .findViewById(R.id.offense_progress);
      rowView.setTag(viewHolder);
    }

    // fill data
    ViewHolder holder = (ViewHolder) rowView.getTag();
    OffenseRecord offense = offenses.get(position);
    holder.text.setText(offense.getLicense());

    return rowView;
  }
}
