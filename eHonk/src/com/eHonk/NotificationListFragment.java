package com.eHonk;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.eHonk.NotificationDetailActivity.PlaceholderFragment;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class NotificationListFragment extends ListFragment {
	
	private ArrayList<Database.OffenseRecord> offenses = null;
	//private ArrayList<CountDownTimer> timers = new ArrayList<CountDownTimer>();
	
	/* interval updating progress bars */
	//private static final int cdt_tick = 4000;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	    Bundle savedInstanceState) {
	  return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Database db = Database.getInstance(getActivity());

		long timestampmili = (new Date()).getTime() - Constants.TIMEOUT;

		db.lock.lock();

		try {
			offenses = db.getLastOffenses(Database.NOTIFICATIONS_LOG_TABLE_NAME_SENT,
			    Constants.iso8601Format.format(new Date(timestampmili)));
			if(offenses==null || offenses.isEmpty()) {
				/* garbage collect - delete all sent notifications, as there is none recent */
				db.deleteAll(Database.NOTIFICATIONS_LOG_TABLE_NAME_SENT);
			}
		} finally {
			db.lock.unlock();
		}

		if(offenses==null || offenses.isEmpty()) {
			/* no recent sent notifications */
			Toast.makeText(getActivity(),
			    getActivity().getString(R.string.empty_notifications_list),
			    Toast.LENGTH_LONG).show();
			/* close list */
			getActivity().getSupportFragmentManager().popBackStack(null, 0);
			return;
		}
		
		setListAdapter(new SentNotificationsArrayAdapter( getActivity(), offenses));
	}
}
