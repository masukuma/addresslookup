package com.nobledesignlabs.lookupaddress;

import org.java_websocket.util.Base64.InputStream;

import com.google.gson.Gson;
import com.nobledesignlabs.entities.User;
import com.nobledesignlabs.utils.DownloadImageTask;

import android.app.Activity;
import android.app.NotificationManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class NotificationView extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notification);

		// ---look up the notification manager service---
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// ---cancel the notification that we started---
		Bundle extras = getIntent().getExtras();

		nm.cancel(extras.getInt("notificationID"));

		TextView tv_msg = (TextView) this.findViewById(R.id.tvMessage);
		tv_msg.setText(extras.getString("message"));
		this.setTitle(extras.getString("title"));
		// picture

		String picts = extras.getString("picture");
		if (picts != null && !"".equals(picts)) {
			new DownloadImageTask((ImageView) findViewById(R.id.imageView1))
					.execute(picts);
		}
		//new DownloadImageTask((ImageView) findViewById(R.id.imageView1))
		//		.execute(extras.getString("picture"));
	}

	
}
