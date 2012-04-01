
package reminder.main.service;

import java.util.Random;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class AlarmReceiver extends BroadcastReceiver {

	private NotificationManager mNM;

	@Override
	public void onReceive(Context context, Intent intent) {
		mNM =
				(NotificationManager) context
						.getSystemService(context.NOTIFICATION_SERVICE);
		try {
			Bundle bundle = intent.getExtras();
			String message = bundle.getString("alarm_message");
			Notification.Builder builder =
					new Notification.Builder(context);
			builder.setDefaults(Notification.DEFAULT_ALL);
			builder.setContentTitle("Reminder");
			builder.setContentText(message);
			Notification n = builder.getNotification();
			mNM.notify(new Random(1).nextInt(), n);
		} catch (Exception e) {
		}
	}

}
