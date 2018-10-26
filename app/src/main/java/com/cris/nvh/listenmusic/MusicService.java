package com.cris.nvh.listenmusic;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import static com.cris.nvh.listenmusic.MainActivity.ACTIONS;

public class MusicService extends Service {
	public static final int NOTIFICATION_ID = 28;
	public static final int SECOND_PER_MINUTE = 60;
	public static final int MILIS = 1000;
	public static final int REQUEST_CODE = 10;
	public static final int BROADCAST_REQUEST = 3;
	public static final int[] SONGS = {R.raw.tinh_xua_nghia_cu,
			R.raw.phai_dau_cuoc_tinh, R.raw.attention};
	public static final String[] SONG_DETAILS = {"Tình xưa nghĩa cũ - Jimmy Nguyễn",
			"Phai dấu cuộc tình - Châu Truyền Hùng", "Attention - Charlie Puth"};
	public static final String CONTENT_TITLE = "Music player";
	public static final String THREAD_NAME = "Worker thread";
	public static final String ACTION_PLAY = "com.cris.nvh.listenmusic.ACTION_PLAY";
	public static final String ACTION_PAUSE = "com.cris.nvh.listenmusic.ACTION_PAUSE";
	public static final String ACTION_NEXT = "com.cris.nvh.listenmusic.ACTION_NEXT";
	public static final String ACTION_BACK = "com.cris.nvh.listenmusic.ACTION_BACK";
	public static int sCurrentSong;
	private int mNumMessages;
	private int mSongDuration;
	private int mCurrentPosition;
	private boolean mIsRunning;
	private boolean mIsShowed;
	private String mAction;
	private MediaPlayer mMediaPlayer;
	private Messenger mMessenger;
	private Handler mHandler;
	private AudioBroadcastReceiver mAudioBroadcastReceiver;
	private enum ACTIONS {
		NEXT, PLAY, PAUSE, BACK, SEEK;
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		registerBroadcast();
		mIsRunning = true;
		HandlerThread handlerThread = new HandlerThread(THREAD_NAME);
		handlerThread.start();
		mHandler = new ServiceHandler(handlerThread.getLooper());
		mMessenger = new Messenger(mHandler);

		//restore UI state
		mMediaPlayer = MediaPlayer.create(MusicService.this, SONGS[sCurrentSong]);
		sendDataToMainThread();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// call onRebind() after this callback return
		return true;
	}

	@Override
	public void onDestroy() {
		// cancel notification
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
				.cancel(NOTIFICATION_ID);
		mMediaPlayer = null;
		mIsRunning = false;
		unregisterReceiver(mAudioBroadcastReceiver);
		super.onDestroy();
	}

	public void registerBroadcast() {
		mAudioBroadcastReceiver = new AudioBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_BACK);
		intentFilter.addAction(ACTION_NEXT);
		intentFilter.addAction(ACTION_PAUSE);
		intentFilter.addAction(ACTION_PLAY);
		registerReceiver(mAudioBroadcastReceiver, intentFilter);
	}


	public void executeRequests() {
		switch (MusicService.ACTIONS.valueOf(mAction)) {
			case NEXT:
				nextRequest();
				break;
			case PLAY:
				playRequest();
				break;
			case PAUSE:
				pauseRequest();
				break;
			case BACK:
				backRequest();
				break;
			case SEEK:
				seekRequest();
				break;
		}
	}

	public void nextRequest() {
		mMediaPlayer.stop();
		if (sCurrentSong < SONGS.length - 1)
			sCurrentSong++;
		else sCurrentSong = 0;
		mMediaPlayer = MediaPlayer.create(MusicService.this, SONGS[sCurrentSong]);
		mMediaPlayer.start();
	}

	public void playRequest() {
		mMediaPlayer.seekTo(mCurrentPosition);
		mMediaPlayer.start();
	}

	public void pauseRequest() {
		mMediaPlayer.pause();
		mCurrentPosition = mMediaPlayer.getCurrentPosition();
	}

	public void backRequest() {
		mMediaPlayer.stop();
		if (sCurrentSong == 0)
			sCurrentSong = SONGS.length - 1;
		else sCurrentSong--;
		mMediaPlayer = MediaPlayer.create(MusicService.this, SONGS[sCurrentSong]);
		mMediaPlayer.start();
	}

	public void seekRequest() {
		if (mMediaPlayer.isPlaying()) {
			mMediaPlayer.seekTo(mCurrentPosition);
			mMediaPlayer.start();
		} else {
			mMediaPlayer.seekTo(mCurrentPosition);
		}
	}

	public void getDataFromUI(Message message) {
		mAction = (String) message.obj;

		// get seekbar progress when user seek bar manually
		if (mAction.equals(ACTIONS[4])) mCurrentPosition = message.arg1;
	}

	public void sendDataToMainThread() {
		Message message = new Message();
		message.obj = getSongInfo();
		MainActivity.sHandler.sendMessage(message);
	}

	public String[] getSongInfo() {
		if (mMediaPlayer == null)
			mMediaPlayer = MediaPlayer.create(MusicService.this, SONGS[sCurrentSong]);
		mCurrentPosition = mMediaPlayer.getCurrentPosition();
		mSongDuration = mMediaPlayer.getDuration();
		String currentTime = convertTime(mCurrentPosition);
		String duration = convertTime(mSongDuration);
		String[] datas = {SONG_DETAILS[sCurrentSong], duration,
				String.valueOf(mCurrentPosition), String.valueOf(mSongDuration), currentTime};
		return datas;
	}

	public String convertTime(int value) {
		int secondUnit = value / MILIS;
		int minute = secondUnit / SECOND_PER_MINUTE;
		int second = secondUnit - minute * SECOND_PER_MINUTE;
		return (minute + ":" + second);
	}

	public void shouldShowNotification() {
		// if next or back request
		if (mAction.equals(ACTIONS[0]) || mAction.equals(ACTIONS[3])) {
			showNotification();
		} else if (!mIsShowed) {
			showNotification();
			mIsShowed = true;
		}
	}

	public void showNotification() {
		// create custom intent action
		Intent play = new Intent(ACTION_PLAY);
		Intent pause = new Intent(ACTION_PAUSE);
		Intent next = new Intent(ACTION_NEXT);
		Intent back = new Intent(ACTION_BACK);
		PendingIntent pendingPlay = PendingIntent.getBroadcast(this, BROADCAST_REQUEST, play, 0);
		PendingIntent pendingPause = PendingIntent.getBroadcast(this, BROADCAST_REQUEST, pause, 0);
		PendingIntent pendingNext = PendingIntent.getBroadcast(this, BROADCAST_REQUEST, next, 0);
		PendingIntent pendingBack = PendingIntent.getBroadcast(this, BROADCAST_REQUEST, back, 0);


		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, REQUEST_CODE,
				intent, 0);
		Bitmap album = BitmapFactory.decodeResource(getResources(), R.drawable.album);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.play)
				.setLargeIcon(album)
				.addAction(R.drawable.next, ACTIONS[0], pendingNext)
				.addAction(R.drawable.pause, ACTIONS[2], pendingPause)
				.addAction(R.drawable.play, ACTIONS[1], pendingPlay)
				.addAction(R.drawable.previous, ACTIONS[3], pendingBack)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
				.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle())
				.setContentTitle(CONTENT_TITLE)
				.setContentText(SONG_DETAILS[sCurrentSong])
				.setContentIntent(pendingIntent) // tap notification and open activity
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setOnlyAlertOnce(true) // alert user with sound or vibration
				.setAutoCancel(false); // keep notification when taps it

		// running service in the foreground
		startForeground(NOTIFICATION_ID, builder.build());
	}

	public class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			mNumMessages++;
			getDataFromUI(msg);
			executeRequests();
			shouldShowNotification();
			if (mNumMessages == 1) {
				new Thread() {
					@Override
					public void run() {
						updateUI();
					}
				}.start();
			}
		}

		public void updateUI() {
			while (mIsRunning) {
				if (mMediaPlayer.isPlaying()) {
					sendDataToMainThread();
					try {
						Thread.sleep(MILIS);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else if (mAction.equals(ACTIONS[4])) // if seek
					sendDataToMainThread();
			}
		}
	}

	public class AudioBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
					R.layout.activity_main);
			switch (action) {
				case ACTION_PLAY:
					playRequest();
					remoteViews.setImageViewResource(R.id.play_pause_button, R.drawable.pause);
					break;
				case ACTION_PAUSE:
					pauseRequest();
					remoteViews.setImageViewResource(R.id.play_pause_button, R.drawable.play);
					break;
				case ACTION_NEXT:
					nextRequest();
					break;
				case ACTION_BACK:
					backRequest();
					break;
			}
		}
	}
}
