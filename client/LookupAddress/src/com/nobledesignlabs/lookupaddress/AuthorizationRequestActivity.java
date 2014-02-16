package com.nobledesignlabs.lookupaddress;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;
import com.nobledesignlabs.oauth.Oauth2Params;
import com.nobledesignlabs.utils.CommonStuff;
import com.nobledesignlabs.utils.DownloadImageTask;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.NotificationManager;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class AuthorizationRequestActivity extends Activity {

	private Button btnReject;
	private Button btnAuthorize;
	// private LatLng address;
	private LatLng currentLocation;
	private String addressname;
	private SocketIO socket;
	private String token;
	private String saddress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_authorization_request);

		createSockets();
		// ---look up the notification manager service---
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// ---cancel the notification that we started---
		Bundle extras = getIntent().getExtras();

		nm.cancel(extras.getInt("notificationID"));

		saddress = extras.getString("address");
		token = extras.getString("token");
		// /double lng = extras.getDouble("lng");

		TextView tv_msg = (TextView) this.findViewById(R.id.tvMessage);
		tv_msg.setText(extras.getString("message"));
		this.setTitle(extras.getString("title"));
		// picture
		String picts = extras.getString("picture");
		if (picts != null && !"".equals(picts)) {
			new DownloadImageTask((ImageView) findViewById(R.id.imgRequestor))
					.execute(picts);
		}

		btnAuthorize = (Button) findViewById(R.id.btnAuthorize);
		if (btnAuthorize != null) {
			btnAuthorize.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					try {
						JSONObject address = new JSONObject(saddress);
						if (socket == null || !socket.isConnected()) {
							createSockets();
						}
						socket.emit("privateaddressauthorization", address,
								token, CommonStuff.versionName,
								CommonStuff.versionNumber);
						callfinish();
					} catch (JSONException e) {
						// TODO Auto-generated catch
						// block
						e.printStackTrace();
					}
				}
			});
		}

		btnReject = (Button) findViewById(R.id.btnReject);
		if (btnReject != null) {
			btnReject.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					try {
						JSONObject address = new JSONObject(saddress);
						if (socket == null || !socket.isConnected()) {
							createSockets();
						}
						socket.emit("privateaddressrejection", address, token,
								CommonStuff.versionName,
								CommonStuff.versionNumber);
						callfinish();
					} catch (JSONException e) {
						// TODO Auto-generated catch
						// block
						e.printStackTrace();
					}
				}

			});
		}
	}

	private void callfinish() {
		// TODO Auto-generated method stub
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
				finish();
			}
		}, 500);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.authorization_request, menu);
		return true;
	}

	private void createSockets() {
		// TODO Auto-generated method stub
		try {
			socket = new SocketIO(CommonStuff.serverName);

			socket.connect(new IOCallback() {

				public void onMessage(JSONObject json, IOAcknowledge ack) {
					try {
						System.out.println("Server said:" + json.toString(2));

					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

				public void onMessage(String data, IOAcknowledge ack) {
					System.out.println("Server said: " + data);
				}

				public void onError(SocketIOException socketIOException) {
					System.out.println("an Error occured");
					socketIOException.printStackTrace();
				}

				public void onDisconnect() {
					System.out.println("Connection terminated.");
				}

				public void onConnect() {
					System.out.println("Connection established");
				}

				public void on(String event, IOAcknowledge ack, Object... args) {
					System.out
							.println("Server triggered event '" + event + "'");
					if (event.equals("updated")) {
						runOnUiThread(new Runnable() {
							public void run() {

							}
						});
					}
				}
			});

		} catch (Exception ex) {
		}
	}

}
