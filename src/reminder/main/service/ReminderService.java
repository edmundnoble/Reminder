
package reminder.main.service;

import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import reminder.main.ReminderActivity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

//TODO: REMOVE.
public class ReminderService extends Service {

	/**
	 * Handler of incoming messages from clients. //
	 */
	class IncomingHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case FIELD_CREATE_VALUE:
					times.put(msg.arg1, -1L);
					messages.put(msg.arg1, (String) msg.obj);
					break;
				case TIME_SET_VALUE:
					Log.i("ReminderService", "Time #" + msg.arg1
							+ " set to " + msg.obj);
					times.put(msg.arg1, (Long) msg.obj);
					break;
				case STR_SET_VALUE:
					Log.i("ReminderService", "Message #" + msg.arg1
							+ " set to " + msg.obj);
					messages.put(msg.arg1, (String) msg.obj);
					break;
				case DEL_VALUE:
					Log.i("ReminderService", "Values of form " + msg.arg1
							+ " deleted!");
					times.remove(msg.arg1);
					messages.remove(msg.arg1);
					break;
				default:
					Log.i("ReminderService", "Received malformed request.");
					super.handleMessage(msg);
			}
			for (int i = 0; i < messages.size(); i++) {
				entries.put(messages.get(i), times.get(i));
			}

			SharedPreferences prefs =
					getSharedPreferences("ReminderPrefs", 0);
			SharedPreferences.Editor editor = prefs.edit();

			for (int i = 1; i < times.size(); i++) {
				editor.putLong("Time", times.get(i));
				editor.putString("Message", messages.get(i));
			}

		}
	}

	public static final int DEL_VALUE = 3;
	/**
	 * Command to service to set a new value. This can be sent to the service to
	 * supply a new value, and will be sent by the service to any registered
	 * clients with the new value.
	 */
	public static final int FIELD_CREATE_VALUE = 0;
	public static final int STR_SET_VALUE = 2;
	public static final int TIME_SET_VALUE = 1;

	private Hashtable<String, Long> entries =
			new Hashtable<String, Long>();
	AlarmManager mAM;
	Hashtable<Integer, String> messages = new Hashtable<Integer, String>();
	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	/** For showing and hiding our notification. */
	NotificationManager mNM;

	/** Holds last value set by a client. */
	int mValue = 0;

	Hashtable<Integer, Long> times = new Hashtable<Integer, Long>();

	/**
	 * When binding to the service, we return an interface to our messenger for
	 * sending messages to the service.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		Log.i("ReminderService", "Bound!");
		return mMessenger.getBinder();
	}

	/**
	 * Show a notification while this service is running.
	 */

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i("ReminderService", "Started!");
		SharedPreferences prefs = getSharedPreferences("ReminderPrefs", 0);
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mAM = (AlarmManager) getSystemService(ALARM_SERVICE);
		Map<String, ?> prefMap = prefs.getAll();
		SharedPreferences.Editor editor = prefs.edit();
		for (int i = 1; i < ReminderActivity.getInstances(); i++) {
			if (prefMap.get("Time" + i) == null) {
				editor.putLong("Time", new Date().getTime());
				editor.putString("Message", "");
			}
		}
		editor.commit();
		updateAlarms(mAM);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private void updateAlarms(AlarmManager am) {

	}

}
