/*
 * Copyright (C) 2015-2023 Adrian Ulrich <adrian@blinkenlights.ch>
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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.os.Build;
import android.os.Bundle;

import java.util.Arrays;
import java.util.ArrayList;

public class PermissionRequestActivity extends Activity {

	private Intent mCallbackIntent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mCallbackIntent = getIntent().getExtras().getParcelable("callbackIntent");
		ArrayList<String> allPerms = new ArrayList<>(Arrays.asList(getNeededPermissions()));
		allPerms.addAll(Arrays.asList(getOptionalPermissions()));
		requestPermissions(allPerms.toArray(new String[0]), 0);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		ArrayList<String> neededPerms = new ArrayList<>(Arrays.asList(getNeededPermissions()));
		int grantedPermissions = 0;

		for (int i = 0; i < permissions.length; i++) {
			if (!neededPerms.contains(permissions[i]))
				continue;
			if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
				grantedPermissions++;
		}

		finish();

		if (grantedPermissions == neededPerms.size()) {
			if (mCallbackIntent != null) {
				mCallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(mCallbackIntent);
			}
			android.os.Process.killProcess(android.os.Process.myPid());
		}
	}

	public static void showWarning(final LibraryActivity activity, final Intent intent) {
		LayoutInflater inflater = LayoutInflater.from(activity);
		View view = inflater.inflate(R.layout.permission_request, null, false);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PermissionRequestActivity.requestPermissions(activity, intent);
			}
		});
		ViewGroup parent = (ViewGroup)activity.findViewById(R.id.content);
		parent.addView(view, -1);
	}

	public static boolean requestPermissions(Activity activity, Intent callbackIntent) {
		boolean havePermissions = havePermissions(activity);
		if (!havePermissions) {
			Intent intent = new Intent(activity, PermissionRequestActivity.class);
			intent.putExtra("callbackIntent", callbackIntent);
			activity.startActivity(intent);
		}
		return !havePermissions;
	}

	public static boolean havePermissions(Context context) {
		for (String permission : getNeededPermissions()) {
			if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}
		return true;
	}

	private static String[] getNeededPermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			return new String[] { Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_IMAGES };
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			return new String[] { Manifest.permission.READ_EXTERNAL_STORAGE };
		}
		return new String[] { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE };
	}

	private static String[] getOptionalPermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			return new String[] { Manifest.permission.POST_NOTIFICATIONS };
		}
		return new String[]{};
	}
}
