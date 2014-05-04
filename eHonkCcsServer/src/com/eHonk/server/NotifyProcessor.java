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

		Map<String, String> payload = new HashMap<String, String>(5);
		payload.put(Constants.PROPERTY_MESSAGE_TYPE, Constants.LABEL_NOTIFY_MESSAGE);

		final String license_plate = msg.getPayload().get(
		    Constants.PROPERTY_LICENSE_PLATE);
		payload.put(Constants.PROPERTY_OFFENDED_LICENSE_PLATE, license_plate);
		payload.put(Constants.PROPERTY_OFFENDED_GCM_ID, msg.getFrom());
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar
		    .getInstance().getTime());
		payload.put(Constants.PROPERTY_OFFENSE_TIMESTAMP, timeStamp);

		final String offender_license_plate = msg.getPayload().get(
		    Constants.PROPERTY_OFFENDER_LICENSE_PLATE);

		final Datastore ds = Datastore.getDatastore();
		final CcsClient client = CcsClient.getInstance();

		try {
			List<Datastore.DriverRecord> offenders = ds
			    .getDrivers(offender_license_plate);

			for (Datastore.DriverRecord driverRecord : offenders) {
				String jsonRequest = CcsClient.createJsonMessage(
				    driverRecord.getGcmRegId(), /* to */
				    client.getRandomMessageId(msg.getFrom().substring(0, 10)), payload,
				    null, null, // TTL (null -> default-TTL)
				    false);
				client.send(jsonRequest);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
