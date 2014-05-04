package com.eHonk.server;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotifyProcessor implements PayloadProcessor {

	@Override
	public void handleMessage(CcsMessage msg) {

		final Map<String, String> payload = new HashMap<String, String>(5);
		final Datastore ds = Datastore.getDatastore();
		final CcsClient client = CcsClient.getInstance();


		final String offender_license_plate = msg.getPayload().get(
		    Constants.PROPERTY_OFFENDER_LICENSE_PLATE);

		try {
			List<Datastore.DriverRecord> offenders = ds
			    .getDrivers(offender_license_plate);

			if(offenders.size()!=0) {
				
				/* build message */
				payload.put(Constants.PROPERTY_MESSAGE_TYPE, Constants.LABEL_NOTIFY_MESSAGE);
				final String license_plate = msg.getPayload().get(
				    Constants.PROPERTY_LICENSE_PLATE);
				payload.put(Constants.PROPERTY_OFFENDED_LICENSE_PLATE, license_plate);
				payload.put(Constants.PROPERTY_OFFENDED_GCM_ID, msg.getFrom());
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar
				    .getInstance().getTime());
				payload.put(Constants.PROPERTY_OFFENSE_TIMESTAMP, timeStamp);
				
				/* send message to all registered drivers */
				for (Datastore.DriverRecord driverRecord : offenders) {
					String jsonRequest = CcsClient.createJsonMessage(
					    driverRecord.getGcmRegId(), /* to */
					    client.getRandomMessageId(msg.getFrom().substring(0, 10)), payload,
					    null, null, // TTL (null -> default-TTL)
					    false);
					client.send(jsonRequest);
				}
			}
			else {
				payload.put(Constants.PROPERTY_MESSAGE_TYPE, Constants.LABEL_UNKNOWNDRIVER_MESSAGE);
				payload.put(Constants.PROPERTY_OFFENDER_LICENSE_PLATE, offender_license_plate);
				/* there is no registered driver */
				String jsonRequest = CcsClient.createJsonMessage(
				    msg.getFrom(), /* to */
				    client.getRandomMessageId(msg.getFrom().substring(0, 10)), payload,
				    null, null, // TTL (null -> default-TTL)
				    false);
				client.send(jsonRequest);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ds.close();
		}

	}

}
