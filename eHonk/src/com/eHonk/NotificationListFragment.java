package com.eHonk;

import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class NotificationListFragment extends ListFragment {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	    Bundle savedInstanceState) {
	  return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Database db = Database.getInstance(getActivity());
		ArrayList<Database.OffenseRecord> offenses = null;

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
		}
		
		setListAdapter(new SentNotificationsArrayAdapter( getActivity(), offenses));
	}
}
