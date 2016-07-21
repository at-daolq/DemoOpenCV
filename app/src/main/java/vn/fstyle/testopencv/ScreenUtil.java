package vn.fstyle.testopencv;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.WindowManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import lombok.Value;
import lombok.experimental.Builder;

/**
 * Created by AsianTech Android Team.
 */
public final class ScreenUtil {

    private static final String TAG = ScreenUtil.class.getSimpleName();

    private ScreenUtil() {
    }

    /**
     * This method is used to get height of screen
     *
     * @param context is current context
     * @return return height screen in pixel
     */
    public static int getHeightScreen(Context context) {
        return getScreenSize(context).getHeight();
    }

    /**
     * This method is used to get width of screen
     *
     * @param context is current context
     * @return return width of screen in pixel
     */
    public static int getWidthScreen(Context context) {
        return getScreenSize(context).getWidth();
    }

    /**
     * This method is used to get scaledDensity of screen
     *
     * @param context is current context
     * @return return scaledDensity of screen in float
     */

    public static float getScaleDensityScreen(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.scaledDensity;
    }

    public static ScreenSize getScreenSize(Context context) {

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();

        // For JellyBean 4.2 (API 17) and onward
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            display.getRealMetrics(displayMetrics);
            return ScreenSize.builder()
                    .width(displayMetrics.widthPixels)
                    .height(displayMetrics.heightPixels)
                    .build();
        }

        Method getRawH;
        Method getRawW;
        try {
            getRawH = Display.class.getMethod("getRawHeight");
            getRawW = Display.class.getMethod("getRawWidth");
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "NoSuchMethodException error: ", e);
            return ScreenSize.builder()
                    .width(0)
                    .height(0)
                    .build();
        }

        try {
            return ScreenSize.builder()
                    .width((int) getRawW.invoke(display))
                    .height((int) getRawH.invoke(display))
                    .build();
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Log.e(TAG, "error: ", e);
            return ScreenSize.builder()
                    .width(0)
                    .height(0)
                    .build();
        }
    }

    /**
     * Check navigation bar for device
     */
    public static boolean isNavigationBarAvailable() {
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        boolean hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);
        return (!(hasBackKey && hasHomeKey));
    }

    /**
     * This method used to get status bar height
     */
    public static int getStatusBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId == 0) {
            // cannot get resource id
            return 0;
        }
        return context.getResources().getDimensionPixelSize(resourceId);
    }

    /**
     * this method used to get navigation bar height
     */
    public static int getNavigationBarHeight(Context context) {
        if (isNavigationBarAvailable()) {
            Resources resources = context.getResources();
            int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId != 0) {
                return resources.getDimensionPixelSize(resourceId);
            }
        }
        return 0;
    }

    /**
     * Class that manage the size of screen
     */
    @Value
    @Builder
    public static class ScreenSize {
        private int width;
        private int height;
    }

}
