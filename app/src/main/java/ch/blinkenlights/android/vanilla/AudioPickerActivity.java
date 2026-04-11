/*
 * Copyright (C) 2014-2017 Adrian Ulrich <adrian@blinkenlights.ch>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package ch.blinkenlights.android.vanilla;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioPickerActivity extends PlaybackActivity {
	private Button mCancelButton;
	private Button mEnqueueButton;
	private Button mPlayButton;
	private TextView mTextView;
	private ProgressBar mProgressBar;
	private Song mSong;
	private Thread mWorkerThread;
	private final AtomicBoolean mCancelled = new AtomicBoolean(false);

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		Intent intent = getIntent();
		if (intent == null) {
			finish();
			return;
		}

		if (PermissionRequestActivity.requestPermissions(this, intent)) {
			finish();
			return;
		}

		Uri uri = intent.getData();
		if (uri == null) {
			finish();
			return;
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.audiopicker);

		mCancelButton = (Button)findViewById(R.id.cancel);
		mCancelButton.setEnabled(true);
		mCancelButton.setOnClickListener(this);

		mEnqueueButton = (Button)findViewById(R.id.enqueue);
		mEnqueueButton.setOnClickListener(this);

		mPlayButton = (Button)findViewById(R.id.play);
		mPlayButton.setOnClickListener(this);

		mTextView = (TextView)findViewById(R.id.filepath);
		mProgressBar = (ProgressBar)findViewById(R.id.progress);

		mWorkerThread = new Thread(() -> {
			Song song = getSongForUri(uri);
			if (!mCancelled.get()) {
				runOnUiThread(() -> onSongResolved(song));
			}
		});
		mWorkerThread.start();
	}

	@Override
	public void onClick(View view) {
		int mode;
		QueryTask query;

		switch (view.getId()) {
			case R.id.play:
				mode = SongTimeline.MODE_PLAY;
				break;
			case R.id.enqueue:
				mode = SongTimeline.MODE_ENQUEUE;
				break;
			default:
				mCancelled.set(true);
				finish();
				return;
		}

		if (mSong.id < 0) {
			query = MediaUtils.buildFileQuery(mSong.path, Song.FILLED_PROJECTION, false);
		} else {
			query = MediaUtils.buildQuery(MediaUtils.TYPE_SONG, mSong.id, Song.FILLED_PROJECTION, null);
		}

		query.mode = mode;
		PlaybackService service = PlaybackService.get(this);
		service.addSongs(query);
		finish();
	}

	private void onSongResolved(Song song) {
		mSong = song;

		if (song == null) {
			finish();
			return;
		}

		if (PlaybackService.hasInstance())
			mEnqueueButton.setEnabled(true);

		mPlayButton.setEnabled(true);

		String displayName = song.title;
		if ("".equals(song.title))
			displayName = new File(song.path).getName();

		mTextView.setText(displayName);
		mTextView.setVisibility(View.VISIBLE);
		mProgressBar.setVisibility(View.GONE);
	}

	private Song getSongForUri(Uri uri) {
		Song song = new Song(-1);
		Cursor cursor = null;

		if (uri.getScheme().equals("content")) {
			if (uri.getHost().equals("media")) {
				cursor = getCursorForMediaContent(uri);
			} else {
				cursor = getCursorForAnyContent(uri);
			}
		}

		if (uri.getScheme().equals("file")) {
			cursor = MediaUtils.getCursorForFileQuery(uri.getPath());
		}

		if (cursor != null) {
			if (cursor.moveToNext()) {
				song.populate(cursor);
			}
			cursor.close();
		}
		return song.isFilled() ? song : null;
	}

	private Cursor getCursorForMediaContent(Uri uri) {
		Cursor cursor = null;
		Cursor pathCursor = getContentResolver().query(uri, new String[]{ MediaStore.Audio.Media.DATA }, null, null, null);
		if (pathCursor != null) {
			if (pathCursor.moveToNext()) {
				String mediaPath = pathCursor.getString(0);
				if (mediaPath != null) {
					QueryTask query = MediaUtils.buildFileQuery(mediaPath, Song.FILLED_PROJECTION, false);
					cursor = query.runQuery(getApplicationContext());
				}
			}
			pathCursor.close();
		}
		return cursor;
	}

	private Cursor getCursorForAnyContent(Uri uri) {
		Cursor cursor = null;
		File outFile = null;
		InputStream ins = null;
		OutputStream ous = null;

		try {
			byte[] buffer = new byte[8192];
			ins = getContentResolver().openInputStream(uri);
			outFile = File.createTempFile("cached-download-", ".bin", getCacheDir());
			ous = new FileOutputStream(outFile);

			int len;
			while ((len = ins.read(buffer)) != -1) {
				ous.write(buffer, 0, len);
				if (mCancelled.get()) {
					throw new IOException("Canceled");
				}
			}
			outFile.deleteOnExit();
		} catch (IOException e) {
			if (outFile != null) {
				outFile.delete();
			}
			outFile = null;
		} finally {
			try { if (ins != null) ins.close(); } catch (IOException e) {}
			try { if (ous != null) ous.close(); } catch (IOException e) {}
		}

		if (outFile != null) {
			cursor = MediaUtils.getCursorForFileQuery(outFile.getPath());
		}
		return cursor;
	}
}
