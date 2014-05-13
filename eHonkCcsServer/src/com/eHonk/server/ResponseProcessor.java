package com.eHonk.server;

import java.util.HashMap;
import java.util.Map;

public class ResponseProcessor implements PayloadProcessor {

	@Override
	public void handleMessage(CcsMessage msg) {

		CcsClient client = CcsClient.getInstance();
		String msgId = client.getRandomMessageId(msg.getFrom().substring(0, 10));

		final String offended_gcmId = msg.getPayload().get(
		    Constants.PROPERTY_OFFENDED_GCM_ID);

		String jsonRequest = CcsClient.createJsonMessage(offended_gcmId, msgId,
		    msg.getPayload(), null, null, // TTL (null -> default-TTL)
		    false);
		client.send(jsonRequest);

	}

}
