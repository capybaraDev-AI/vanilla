/*
 * Copyright (C) 2010, 2011 Christopher Eby <kreed@kreed.org>
 * Copyright 2017-2020 Adrian Ulrich <adrian@blinkenlights.ch>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ch.blinkenlights.android.vanilla;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public final class CoverBitmap {
	public static final int STYLE_OVERLAPPING_BOX = 0;
	public static final int STYLE_INFO_BELOW = 1;
	public static final int STYLE_NO_INFO = 2;
	private static final boolean DEBUG_PAINT = false;
	private static final float SLACK_RATIO = 0.95f;

	private static int TEXT_SIZE = -1;
	private static int TEXT_SIZE_BIG;
	private static int PADDING;
	private static int TOP_PADDING;
	private static int BOTTOM_PADDING;

	private static void loadTextSizes(Context context) {
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		TEXT_SIZE = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, metrics);
		TEXT_SIZE_BIG = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, metrics);
		PADDING = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, metrics);
		TOP_PADDING = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, metrics);
		BOTTOM_PADDING = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, metrics);
	}

	private static void drawText(Canvas canvas, String text, int left, int top, int width, int maxWidth, Paint paint) {
	    canvas.save();
	    int margin = (int)(paint.getTextSize() * 0.5f);
	    canvas.clipRect(left - margin, top, left + maxWidth + margin, top + paint.getTextSize() * 2);
	    canvas.drawText(text, left, top - paint.ascent(), paint);
	    canvas.restore();
	}

	/**
	 * Splits text into at most two lines if it exceeds maxWidth.
	 * Tries to split at a space near the middle of the string.
	 */
	private static String[] splitText(String text, int maxWidth, Paint paint) {
		if (paint.measureText(text) <= maxWidth) {
			return new String[]{ text };
		}
		int mid = text.length() / 2;
		int splitPos = -1;
		for (int i = 0; i < mid; i++) {
			if (mid - i > 0 && text.charAt(mid - i) == ' ') { splitPos = mid - i; break; }
			if (mid + i < text.length() && text.charAt(mid + i) == ' ') { splitPos = mid + i; break; }
		}
		if (splitPos > 0) {
			return new String[]{ text.substring(0, splitPos).trim(), text.substring(splitPos).trim() };
		}
		return new String[]{ text };
	}

	public static Bitmap createBitmap(Context context, int style, Bitmap coverArt, Song song, int width, int height) {
		switch (style) {
		case STYLE_OVERLAPPING_BOX:
			return createOverlappingBitmap(context, coverArt, song, width, height);
		case STYLE_INFO_BELOW:
			return createSeparatedBitmap(context, coverArt, song, width, height);
		case STYLE_NO_INFO:
			return createScaledBitmap(coverArt, width, height);
		default:
			throw new IllegalArgumentException("Invalid bitmap type given: " + style);
		}
	}

	private static Bitmap createOverlappingBitmap(Context context, Bitmap cover, Song song, int width, int height) {
		if (TEXT_SIZE == -1)
			loadTextSizes(context);

		Paint paint = new Paint();
		paint.setAntiAlias(true);

		String title = song.title == null ? "" : song.title;
		String album = song.album == null ? "" : song.album;
		String artist = song.artist == null ? "" : song.artist;

		int titleSize = TEXT_SIZE_BIG;
		int subSize = TEXT_SIZE;
		int padding = PADDING;

		paint.setTextSize(titleSize);
		int titleWidth = (int)paint.measureText(title);
		paint.setTextSize(subSize);
		int albumWidth = (int)paint.measureText(album);
		int artistWidth = (int)paint.measureText(artist);

		int boxWidth = Math.min(width, Math.max(titleWidth, Math.max(artistWidth, albumWidth)) + padding * 2);
		int boxHeight = Math.min(height, titleSize + subSize * 2 + padding * 4);

		int coverWidth = 0;
		int coverHeight = 0;
		if (cover != null) {
			cover = createScaledBitmap(cover, width, height);
			coverWidth = cover.getWidth();
			coverHeight = cover.getHeight();
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);

		if (coverWidth != 0) {
			canvas.drawBitmap(cover, (width - coverWidth) / 2, (height - coverHeight) / 2, new Paint());
		}

		int left = (width - boxWidth) / 2;
		int top = (height - boxHeight) / 2;
		int right = (width + boxWidth) / 2;
		int bottom = (height + boxHeight) / 2;

		paint.setARGB(150, 0, 0, 0);
		canvas.drawRect(left, top, right, bottom, paint);

		int maxWidth = boxWidth - padding * 2;
		paint.setARGB(255, 255, 255, 255);
		top += padding;
		left += padding;

		paint.setTextSize(titleSize);
		drawText(canvas, title, left, top, titleWidth, maxWidth, paint);
		top += titleSize + padding;

		paint.setTextSize(subSize);
		drawText(canvas, album, left, top, albumWidth, maxWidth, paint);
		top += subSize + padding;

		drawText(canvas, artist, left, top, artistWidth, maxWidth, paint);

		return bitmap;
	}

	private static Bitmap createSeparatedBitmap(Context context, Bitmap cover, Song song, int width, int height) {
		if (TEXT_SIZE == -1)
			loadTextSizes(context);

		int textSize = TEXT_SIZE;
		int textSizeBig = TEXT_SIZE_BIG;
		int padding = PADDING;
		int bottomPadding = BOTTOM_PADDING;

		int[] colors = ThemeHelper.getDefaultCoverColors(context);
		int textColor = 0xFF000000 + (0xFFFFFF - (colors[0] & 0xFFFFFF));
		boolean verticalMode = width > height;

		String title = song.title == null ? "" : song.title;
		String album = song.album == null ? "" : song.album;
		String artist = song.artist == null ? "" : song.artist;

		int margin = (int)(textSize * 0.8f);
		int maxTextWidth = (verticalMode ? width / 2 : width) - margin * 2;

		Paint paint = new Paint();
		paint.setAntiAlias(true);

		paint.setTextSize(textSizeBig);
		String[] titleLines = splitText(title, maxTextWidth, paint);
		paint.setTextSize(textSize);
		String[] albumLines = splitText(album, maxTextWidth, paint);
		String[] artistLines = splitText(artist, maxTextWidth, paint);

		int textTotalHeight = padding
			+ textSizeBig * titleLines.length
			+ padding
			+ textSize * albumLines.length
			+ padding
			+ textSize * artistLines.length
			+ padding;

		int textStart = (verticalMode
			? (height - textTotalHeight) / 2
			: height - bottomPadding - textTotalHeight);

		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);

		if (cover != null) {
			Bitmap scaled;
			if (verticalMode) {
				int hw = width / 2;
				scaled = createScaledBitmap(cover, Math.min(hw, height), Math.min(hw, height));
				canvas.drawBitmap(scaled, (hw - scaled.getWidth()) / 2, (height - scaled.getHeight()) / 2, null);
			} else {
				scaled = createScaledBitmap(cover, width, textStart);
				canvas.drawBitmap(scaled, (width - scaled.getWidth()) / 2, 0, null);
			}
		}

		int top = textStart;
		int tshift = (verticalMode ? width / 2 : 0);

		PorterDuffColorFilter filter = new PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC_ATOP);
		paint.setColorFilter(filter);

		// Title
		paint.setTextSize(textSizeBig);
		for (String line : titleLines) {
			int twidth = (int)paint.measureText(line);
			int tstart = tshift + (width - tshift - twidth) / 2;
			drawText(canvas, line, tstart, top, width, twidth + margin * 2, paint);
			top += textSizeBig;
		}
		top += padding;

		// Album
		paint.setAlpha(0xAA);
		paint.setTextSize(textSize);
		for (String line : albumLines) {
			int twidth = (int)paint.measureText(line);
			int tstart = tshift + (width - tshift - twidth) / 2;
			drawText(canvas, line, tstart, top, width, twidth + margin * 2, paint);
			top += textSize;
		}
		top += padding;

		// Artist
		for (String line : artistLines) {
			int twidth = (int)paint.measureText(line);
			int tstart = tshift + (width - tshift - twidth) / 2;
			drawText(canvas, line, tstart, top, width, twidth + margin * 2, paint);
			top += textSize;
		}

		return bitmap;
	}

	private static Bitmap createScaledBitmap(Bitmap source, int width, int height) {
		int sourceWidth = source.getWidth();
		int sourceHeight = source.getHeight();
		float scale = Math.min((float)width / sourceWidth, (float)height / sourceHeight);
		sourceWidth *= scale;
		sourceHeight *= scale;
		Bitmap scaled = Bitmap.createScaledBitmap(source, (int)(SLACK_RATIO * sourceWidth), (int)(SLACK_RATIO * sourceHeight), true);
		return createBorderedBitmap(scaled, sourceWidth, sourceHeight);
	}

	private static Bitmap createBorderedBitmap(Bitmap source, int width, int height) {
		Bitmap dst = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(dst);

		BitmapShader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setShader(shader);

		RectF rect = new RectF(0.0f, 0.0f, source.getWidth(), source.getHeight());
		float radius = 12;
		canvas.translate((height - source.getHeight()) / 2, (width - source.getWidth()) / 2);
		canvas.drawRoundRect(rect, radius, radius, paint);
		return dst;
	}

	public static Bitmap generateDefaultCover(Context context, int width, int height) {
		int size = Math.min(width, height);
		int[] colors = ThemeHelper.getDefaultCoverColors(context);
		int rgb_background = colors[0];
		int rgb_note_inner = colors[1];

		final int line_thickness = size / 10;
		final int line_vertical = line_thickness * 5;
		final int line_horizontal = line_thickness * 3;
		final int circle_radius = line_thickness * 2;

		final int total_len_x = circle_radius * 2 + line_horizontal - line_thickness;
		final int total_len_y = circle_radius + line_vertical + (line_thickness / 2);
		final int xoff = circle_radius + (size - total_len_x) / 2;
		final int yoff = size - circle_radius - (size - total_len_y) / 2;

		Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		bitmap.eraseColor(rgb_background);

		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(rgb_note_inner);

		Canvas canvas = new Canvas(bitmap);
		canvas.drawCircle(xoff, yoff, circle_radius, paint);
		int lpos = xoff + circle_radius - line_thickness;
		int tpos = yoff - line_vertical;
		canvas.drawRoundRect(new RectF(lpos, tpos, lpos + line_thickness, yoff), 0, 0, paint);
		int hdiff = tpos - (line_thickness / 2);
		canvas.drawRoundRect(new RectF(lpos, hdiff, lpos + line_horizontal, hdiff + line_thickness), line_thickness, line_thickness, paint);

		return bitmap;
	}

	public static Bitmap generatePlaceholderCover(Context context, int width, int height, String title) {
		if (title == null || width < 1 || height < 1)
			return null;

		final float textSize = width * 0.4f;

		title = title.replaceFirst("(?i)^The ", "");
		title = title.replaceAll("[ <>_-]", "");
		String subText = (title + "  ").substring(0, 2);

		if (Character.UnicodeBlock.of(subText.charAt(0)) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
			subText = subText.substring(0, 1);
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();

		TypedValue tv = new TypedValue();
		context.getTheme().resolveAttribute(R.attr.themed_letter_tile_colors, tv, false);
		TypedArray colors = context.getResources().obtainTypedArray(tv.data);
		int color = colors.getColor(Math.abs(title.hashCode()) % colors.length(), 0);
		colors.recycle();
		paint.setColor(color);

		paint.setStyle(Paint.Style.FILL);
		canvas.drawPaint(paint);

		paint.setARGB(255, 255, 255, 255);
		paint.setAntiAlias(true);
		paint.setTextSize(textSize);

		Rect bounds = new Rect();
		paint.getTextBounds(subText, 0, subText.length(), bounds);
		canvas.drawText(subText, (width / 2f) - bounds.exactCenterX(), (height / 2f) - bounds.exactCenterY(), paint);
		return bitmap;
	}
}
