package com.eHonk;

import java.text.ParseException;
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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class NotificationListFragment extends ListFragment {

	private ArrayList<Database.OffenseRecord> offenses = null;

	private static final int cdt_tick = 4000;
	private CountDownTimer cdt = null;
	private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;

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
			if (offenses == null || offenses.isEmpty()) {
				/*
				 * garbage collect - delete all sent notifications, as there is none
				 * recent
				 */
				db.deleteAll(Database.NOTIFICATIONS_LOG_TABLE_NAME_SENT);
			}
		} finally {
			db.lock.unlock();
		}

		if (offenses == null || offenses.isEmpty()) {
			/* no recent sent notifications */
			Toast.makeText(getActivity(),
			    getActivity().getString(R.string.empty_notifications_list),
			    Toast.LENGTH_LONG).show();
			/* close list */
			getActivity().getSupportFragmentManager().popBackStack(null, 0);
			return;
		}

		setListAdapter(new SentNotificationsArrayAdapter(getActivity(), offenses));
	}

	@Override
	public void onStart() {
		super.onStart();

		if (getListAdapter() == null)
			return;

		getListView().setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				mScrollState = scrollState;
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
			    int visibleItemCount, int totalItemCount) {
			}
		});

		cdt = new CountDownTimer(Constants.TIMEOUT, cdt_tick) {

			@Override
			public void onTick(long millisUntilFinished) {

				if (mScrollState == OnScrollListener.SCROLL_STATE_IDLE) {

					ListView mListView = getListView();
					int start = mListView.getFirstVisiblePosition();
					long current_time = System.currentTimeMillis();

					for (int i = start, j = mListView.getLastVisiblePosition(); i <= j; i++) {

						View view = mListView.getChildAt(i - start);
						Database.OffenseRecord offense = (Database.OffenseRecord) mListView
						    .getItemAtPosition(i);

						long expire_date = 0;
						try {
							expire_date = Constants.iso8601Format.parse(
							    offense.getTimestamp()).getTime()
							    + Constants.TIMEOUT;
						} catch (ParseException e) {
							e.printStackTrace();
						}

						long timeout = expire_date - current_time;
						if (timeout > 0) {
							View rowView = mListView.getAdapter().getView(i, view, mListView);
							ProgressBar pb = (ProgressBar) rowView
							    .findViewById(R.id.offense_progress);
							int progress = (int) ((timeout * pb.getMax()) / Constants.TIMEOUT);
							pb.setProgress(progress);
						}

					}
				}
			}

			@Override
			public void onFinish() {

			}
		};

		this.cdt.start();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (cdt != null) {
			cdt.cancel();
			cdt = null;
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		int start = l.getFirstVisiblePosition();
		Database.OffenseRecord offense = (Database.OffenseRecord) l
		    .getItemAtPosition(position - start);

		int status = offense.getStatusCode();
		if (status == Database.NOTIFICATION_STATUS_NREG) {
			Toast.makeText(
			    getActivity(),
			    getActivity().getApplicationContext().getString(
			        R.string.tw_notification_status_nreg), Toast.LENGTH_SHORT).show();
		} else if (status == Database.NOTIFICATION_STATUS_NOTIFIED) {

			long interval = 0;
			try {
	      interval =  Constants.iso8601Format.parse(offense.getTimestamp()).getTime() + Constants.TIMEOUT - System.currentTimeMillis();
      } catch (ParseException e) {
      	e.printStackTrace();
      	return;
      }
						
			int interval_minutes = (int)(interval / (1000 * 60));
			interval_minutes++;
			
			Toast.makeText(
			    getActivity(),
			    String.format(
			        getActivity().getApplicationContext().getString(
			            R.string.toast_waiting_driver), interval_minutes), Toast.LENGTH_SHORT).show();
		} else if (status == Database.NOTIFICATION_STATUS_ACK_OK) {
			Toast.makeText(
			    getActivity(),
			    getActivity().getApplicationContext().getString(
			        R.string.tw_notification_status_ack_ok), Toast.LENGTH_SHORT)
			    .show();
		} else if (status == Database.NOTIFICATION_STATUS_ACK_IGN) {
			Toast.makeText(
			    getActivity(),
			    getActivity().getApplicationContext().getString(
			        R.string.tw_notification_status_ack_ign), Toast.LENGTH_SHORT)
			    .show();
		}

	}

}
