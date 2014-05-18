package com.eHonk;

import java.text.ParseException;
import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Color;
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
    public TextView status;
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
      viewHolder.status = (TextView) rowView.findViewById(R.id.status_label);
      rowView.setTag(viewHolder);
    }

    // fill data
    ViewHolder holder = (ViewHolder) rowView.getTag();
    OffenseRecord offense = offenses.get(position);
    
    holder.text.setText(offense.getLicense());
    
    int status = offense.getStatusCode();
    if(status == Database.NOTIFICATION_STATUS_NREG) {
    	holder.progress.setVisibility(View.GONE);
    	holder.status.setText( context.getString(R.string.tw_notification_status_nreg));
    	holder.status.setTextColor(Color.parseColor("#996600"));
    	
    } else if(status == Database.NOTIFICATION_STATUS_NOTIFIED) {
    	holder.progress.setVisibility(View.VISIBLE);
    	holder.status.setText( context.getString(R.string.tw_notification_status_notified));
    	
    } else if(status == Database.NOTIFICATION_STATUS_ACK_OK) {
    	holder.progress.setVisibility(View.GONE);
    	holder.status.setText( context.getString(R.string.tw_notification_status_ack_ok));
    	holder.status.setTextColor(Color.parseColor("#336600"));
    	
    } else if(status == Database.NOTIFICATION_STATUS_ACK_IGN) {
    	holder.progress.setVisibility(View.GONE);
    	holder.status.setText( context.getString(R.string.tw_notification_status_ack_ign));
    	holder.status.setTextColor(Color.parseColor("#990000"));
    }
    
	  int totalMillis = 0;
    try {
    	totalMillis = (int)(Constants.iso8601Format.parse( offense.getTimestamp()).getTime() + Constants.TIMEOUT - System.currentTimeMillis());
    } catch (ParseException e) {
      e.printStackTrace();
    }
    
    holder.progress.setProgress( (totalMillis * holder.progress.getMax()) / Constants.TIMEOUT);

    return rowView;
  }
}
