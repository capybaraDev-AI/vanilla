/*
 * Copyright (C) 2017 Adrian Ulrich <adrian@blinkenlights.ch>
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

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;
import android.os.Bundle;
import android.view.Window;

public class AudioSearchActivity extends PlaybackActivity {

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Intent intent = getIntent();
		String action = (intent == null ? null : intent.getAction());
		if (action == null || !action.equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)) {
			finish();
			return;
		}
		if (PermissionRequestActivity.requestPermissions(this, intent)) {
			finish();
			return;
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		final String query = intent.getExtras().getString(SearchManager.QUERY);
		final Context ctx = getApplicationContext();

		new Thread(() -> {
			MediaAdapter adapter = new MediaAdapter(ctx, MediaUtils.TYPE_SONG, null, null);
			adapter.setFilter(query);
			QueryTask task = adapter.buildSongQuery(Song.FILLED_PROJECTION);
			task.mode = SongTimeline.MODE_PLAY;
			PlaybackService service = PlaybackService.get(ctx);
			service.pause();
			service.setShuffleMode(SongTimeline.SHUFFLE_ALBUMS);
			service.emptyQueue();
			service.addSongs(task);
			if (service.getTimelineLength() > 0) {
				service.play();
			}
			runOnUiThread(this::finish);
		}).start();
	}
}
