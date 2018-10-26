package com.cris.nvh.listenmusic;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
	public static final String[] ACTIONS = {"NEXT", "PLAY", "PAUSE", "BACK", "SEEK"};
	public static final String DURATION = "DURATION";
	public static final String IS_PLAYED = "IS_PLAYED";
	public static TextView sCurrentTime;
	public static TextView sDuration;
	public static TextView sSongName;
	public static SeekBar sSeekBar;
	public static Handler sHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			String[] data = (String[]) msg.obj;
			if (data != null) {
				sSongName.setText(data[0]);
				sDuration.setText(data[1]);
				sSeekBar.setProgress(Integer.valueOf(data[2]));
				sSeekBar.setMax(Integer.valueOf(data[3]));
				sCurrentTime.setText(data[4]);
			}
		}
	};
	private Messenger mMessenger;
	private ImageButton mNextBtn;
	private ImageButton mPlayOrPause;
	private ImageButton mPreviousBtn;
	private boolean mIsPlayed = false;
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			mMessenger = new Messenger(iBinder);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mMessenger = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mIsPlayed = getPreferences(Context.MODE_PRIVATE).getBoolean(IS_PLAYED, false);
		setContentView(R.layout.activity_main);
		initView();
	}

	@Override
	protected void onStart() {
		super.onStart();
		Intent intent = new Intent(this, MusicService.class);
		bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
		startService(intent);
	}

	@Override
	protected void onDestroy() {
		SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
		editor.putInt(DURATION, sSeekBar.getMax())
				.putBoolean(IS_PLAYED, mIsPlayed);
		editor.commit();
		unbindService(mServiceConnection);
		if (!mIsPlayed) {
			Intent intent = new Intent(this, MusicService.class);
			stopService(intent);
		}
		super.onDestroy();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.next_button:
				nextSong();
				break;
			case R.id.play_pause_button:
				if (mIsPlayed) pauseSong();
				else playSong();
				break;
			case R.id.previous_button:
				previousSong();
				break;
		}
	}

	public void initView() {
		mNextBtn = (ImageButton) findViewById(R.id.next_button);
		mPreviousBtn = (ImageButton) findViewById(R.id.previous_button);
		mPlayOrPause = (ImageButton) findViewById(R.id.play_pause_button);
		if (mIsPlayed)
			mPlayOrPause.setImageResource(R.drawable.pause);
		else
			mPlayOrPause.setImageResource(R.drawable.play);

		sSongName = (TextView) findViewById(R.id.song_name);
		sCurrentTime = (TextView) findViewById(R.id.current_time);
		sDuration = (TextView) findViewById(R.id.song_duration);
		sSeekBar = (SeekBar) findViewById(R.id.seek_bar);
		sSeekBar.setMax(getPreferences(Context.MODE_PRIVATE).getInt(DURATION, 0));

		setViewListener();;
	}

	public void setViewListener() {
		mNextBtn.setOnClickListener(this);
		mPlayOrPause.setOnClickListener(this);
		mPreviousBtn.setOnClickListener(this);
		sSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
				// if user seek bar manually
				if (fromUser && progress != seekBar.getMax()) {
					seekSong(progress);
				} else if (progress == seekBar.getMax())
					nextSong();
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
	}

	public void nextSong() {
		mIsPlayed = true;
		mPlayOrPause.setImageResource(R.drawable.pause);
		Message message = Message.obtain();
		message.obj = ACTIONS[0];
		try {
			mMessenger.send(message);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void pauseSong() {
		Message message = new Message();
		mIsPlayed = false;
		mPlayOrPause.setImageResource(R.drawable.play);
		message.obj = ACTIONS[2];
		try {
			mMessenger.send(message);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void playSong() {
		Message message = new Message();
		mIsPlayed = true;
		mPlayOrPause.setImageResource(R.drawable.pause);
		message.obj = ACTIONS[1];
		try {
			mMessenger.send(message);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void previousSong() {
		mIsPlayed = true;
		mPlayOrPause.setImageResource(R.drawable.pause);
		Message message = Message.obtain();
		message.obj = ACTIONS[3];
		try {
			mMessenger.send(message);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void seekSong(int progress) {
		Message message = Message.obtain();
		message.arg1 = progress;
		message.obj = ACTIONS[4];
		try {
			mMessenger.send(message);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
