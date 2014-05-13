package com.eHonk;

import java.text.SimpleDateFormat;
import java.util.Random;

public final class Constants {

	/**
	 * @param args
	 */
	
	public static final String PROPERTY_GCM_REG_ID = "registration_id";
	public static final String PROPERTY_LICENSE_PLATE = "license_plate";
	public static final String PROPERTY_OFFENDER_LICENSE_PLATE = "offender_license_plate";
	public static final String PROPERTY_OFFENDED_LICENSE_PLATE = "offended_license_plate";
	public static final String PROPERTY_OFFENDED_GCM_ID = "offended_gcm_id";
	public static final String PROPERTY_OFFENSE_TIMESTAMP = "offense_timestamp";
	public static final String PROPERTY_OFFENDERS_COUNT = "offenders_count";
	public static final String PROPERTY_RESPONSE_TYPE = "response_type";
	
	public static final String VALUE_RESPONSE_COMING = "response_coming";
	public static final String VALUE_RESPONSE_IGNORE = "response_ignore";
	
	public static final String PACKAGE = "com.eHonk";
	
	public static final String PROPERTY_MESSAGE_TYPE = "message_type";
	
	public static final String LABEL_REGISTER_MESSAGE = "register_message";
	public static final String LABEL_UNKNOWNDRIVER_MESSAGE = "unknown_driver_message";
	public static final String LABEL_NOTIFY_MESSAGE = "notify_message";
	public static final String LABEL_NOTIFY_RESPONSE_MESSAGE = "notify_response_message";
	public static final String LABEL_NOTIFYACK_MESSAGE = "notify_ack_message";
	public static final String LABEL_ECHO_MESSAGE = "echo_message";
	public static final String LABEL_CHANGEDRIVER_MESSAGE = "register_update_message";
	public static final String LABEL_NODRIVER_MESSAGE = "iamnotadriver_message";
	
	public static final int MAX_ATTEMPTS = 5;
	public static final int BACKOFF_MILLI_SECONDS = 1000;
	public static final Random random = new Random();

	public static final String TAG = "eHonk GCM";
	
	public static int TIMEOUT = 1000 * 60 * 5;
	
  public static SimpleDateFormat iso8601Format = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
	
}
