
package reminder.main;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import reminder.main.service.ReminderService;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

//TODO: OPTIMIZE. EVERYTHING.

public class ReminderActivity extends Activity implements
		OnTimeChangedListener, OnKeyListener, OnItemClickListener,
		OnMenuItemClickListener {

	private class ReminderConnection implements ServiceConnection {

		// TODO: REMOVE.
		boolean connected = false;

		private Message sm, lm;

		boolean timeSet = false, messageSet = false;

		public void deleteFields() {
			Message m =
					Message.obtain(null, ReminderService.DEL_VALUE,
							instances, 0);
			try {
				mService.send(m);
			} catch (RemoteException e) {
			}
		}

		public void onServiceConnected(ComponentName className,
				IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			mService = new Messenger(service);
			connected = true;
			Log.i("ReminderActivity", "Connected!");
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;
			connected = false;
			Log.i("ReminderActivity", "Disconnected!");

		}

		public void sendEntry() {
			timeSet = false;
			messageSet = false;
			try {
				mService.send(sm);
				mService.send(lm);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			entries.put((String) sm.obj, (Long) lm.obj);
		}

		public void updateMessage(String message, int formNum) {
			sm =
					Message.obtain(null, ReminderService.STR_SET_VALUE,
							message);
			sm.arg1 = formNum;
			messageSet = true;
			if (messageSet && timeSet) {
				sendEntry();
			}

		}

		public void updateTime(Long value, int formNum) {
			Log.i("ReminderConnection", "Time updated to "
					+ new Date(value).getHours());
			Message lm =
					Message.obtain(null, ReminderService.TIME_SET_VALUE,
							value);
			lm.arg1 = formNum;
			timeSet = true;
			if (messageSet && timeSet) {
				sendEntry();
			}
		}

	}

	private static int instances = 1;

	public static int getInstances() {
		return instances;
	}

	private Hashtable<String, Long> entries =
			new Hashtable<String, Long>();
	private MenuItem deleteItem, addItem;

	private AlarmManager nAM;

	private ReminderConnection mConnection;

	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;

	/** Messenger for communicating with service. */
	Messenger mService = null;

	ListView reminderListView;

	private EditText textField;

	/** Called when the activity is first created. */
	private TimePicker timePicker;

	public void deleteForm(View v) {
		if (instances == 1) {
			return;
		}
		mConnection.deleteFields();
		stop();
		instances--;

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_menu, menu);
		// addItem = (MenuItem) findViewById(R.id.menu_insert);
		// MenuItem deleteItem = (MenuItem) findViewById(R.id.menu_delete);
		addItem = menu.getItem(0);
		MenuItem deleteItem = menu.getItem(1);
		Log.i("ReminderActivity", "AddItem being null is "
				+ (addItem == null) + ", DeleteItem being null is "
				+ (deleteItem == null));
		addItem.setOnMenuItemClickListener(this);
		return true;
	}

	/**
	 * Class for interacting with the main interface of the service.
	 */

	void doBindService(ServiceConnection mConnection) {
		// Establish a connection with the service. We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		bindService(new Intent(ReminderActivity.this,
				ReminderService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mIsBound = true;

	}

	void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	private boolean isMyServiceRunning() {
		ActivityManager manager =
				(ActivityManager) getSystemService(ACTIVITY_SERVICE);

		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if ("reminder.main.service.ReminderService"
					.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	public void msgChange(View v) {
		mConnection.updateMessage(((EditText) v).getText().toString(),
				instances);
	}

	public void newForm(View v) {
		instances++;
		Intent newFormIntent =
				new Intent(getApplicationContext(), this.getClass());
		startActivity(newFormIntent);
	}

	ArrayAdapter<String> adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		nAM = (AlarmManager) getSystemService(ALARM_SERVICE);
		textField = (EditText) findViewById(R.id.editText1);
		timePicker = (TimePicker) findViewById(R.id.timePicker1);
		timePicker.setOnTimeChangedListener(this);
		textField.setOnKeyListener(this);
		reminderListView = (ListView) findViewById(R.id.listView1);
		entries.put("LOL", -1L);
		ArrayList<String> adapterValues = new ArrayList<String>();
		for (String element : entries.keySet()) {
			adapterValues.add(element);
		}
		adapter =
				new ArrayAdapter<String>(this,
						android.R.layout.simple_list_item_1,
						android.R.id.text1, adapterValues);
		reminderListView.setAdapter(adapter);
		reminderListView.setOnItemClickListener(this);
		if (!isMyServiceRunning()) {
			Intent serviceIntent =
					new Intent(getApplicationContext(),
							reminder.main.service.ReminderService.class);
			startService(serviceIntent);
		}
		mConnection = new ReminderConnection();
		doBindService(mConnection);
	}

	public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
		// TODO Auto-generated method stub
		mConnection.updateMessage(((EditText) arg0).getText().toString(),
				instances);
		entries.keys();
		return false;
	}

	public void saveAlarms(View v) {
		// TODO: ALARMS.
		for (String key : entries.keySet()) {

		}
	}

	public void onTimeChanged(TimePicker arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		mConnection.updateTime(new Date(0, 0, 0, arg1, arg2, 0).getTime(),
				instances);
	}

	private void stop() {
		Handler handler = new Handler();
		handler.post(new Runnable() {

			public void run() {
				finish();
			}
		});
	}

	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		Date date = new Date(entries.get(((TextView) arg1).getText()));
		timePicker.setCurrentHour(date.getHours());

	}

	public boolean onMenuItemClick(MenuItem arg0) {
		if (arg0.getItemId() == R.id.menu_delete) {
			deleteItem(reminderListView);
			return true;
		}
		else if (arg0.getItemId() == R.id.menu_insert) {
			addItem(reminderListView);
			return true;
		}
		return false;
	}

	private void addItem(ListView listView) {
		entries.put("New Reminder", -1L);
		ArrayList<String> strlist = new ArrayList<String>();
		for (String element : entries.keySet()) {
			strlist.add(element);
		}
		ArrayAdapter<String> adapter =
				new ArrayAdapter<String>(this,
						android.R.layout.simple_list_item_1,
						android.R.id.text1, strlist);
		listView.setAdapter(adapter);
	}

	private void updateList() {

	}

	private void deleteItem(ListView listView) {
		if (listView.getSelectedView() == null) {
			return;
		}
		Log.i("ReminderActivity", entries.get("LOL").toString());
		Log.i("ReminderActivity", ((TextView) listView.getSelectedView())
				.getText().toString());
		entries.remove(((TextView) listView.getSelectedView()).getText()
				.toString());
		ArrayList<String> strList = new ArrayList<String>();
		for (String element : entries.keySet()) {
			Log.i("ReminderActivity", "KeySet contains " + element);
			strList.add(element);
		}
		ArrayAdapter<String> adapter =
				new ArrayAdapter<String>(this,
						android.R.layout.simple_list_item_1,
						android.R.id.text1, strList);
		listView.setAdapter(adapter);
	}
}

class StartReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent arg1) {
		Intent startServiceIntent =
				new Intent(context,
						reminder.main.service.ReminderService.class);
		context.startService(startServiceIntent);
	}
}
