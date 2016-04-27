
package com.statistical.android.sdk;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;

/**
 * 设备信息
 *
 * 
 */
class DeviceInfo {
   
    /**
     * @return OS
     */
    static String getOS() {
        return "Android";
    }

    
    /**
     * @return OSVersion
     */
    static String getOSVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    /**
     * @return  device model.
     */
    static String getDevice() {
        return android.os.Build.MODEL;
    }

    /**
     * 获取屏幕尺寸
     * @param context context to use to retrieve the current WindowManager
     * @return a string in the format "WxH", or the empty string "" if resolution cannot be determined
     */
    static String getResolution(final Context context) {
        String resolution = "";
        try {
            final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            final Display display = wm.getDefaultDisplay();
            final DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            resolution = metrics.widthPixels + "x" + metrics.heightPixels;
        }
        catch (Throwable t) {
            if (Statistical.sharedInstance().isLoggingEnabled()) {
                Log.i(Statistical.TAG, "Device resolution cannot be determined");
            }
        }
        return resolution;
    }

    /**
     * 分辨率
     * @param context context to use to retrieve the current display metrics
     * @return a string constant representing the current display density, or the
     *         empty string if the density is unknown
     */
    static String getDensity(final Context context) {
        String densityStr = "";
        final int density = context.getResources().getDisplayMetrics().densityDpi;
        switch (density) {
            case DisplayMetrics.DENSITY_LOW:
                densityStr = "LDPI";
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                densityStr = "MDPI";
                break;
            case DisplayMetrics.DENSITY_TV:
                densityStr = "TVDPI";
                break;
            case DisplayMetrics.DENSITY_HIGH:
                densityStr = "HDPI";
                break;
            case DisplayMetrics.DENSITY_XHIGH:
                densityStr = "XHDPI";
                break;
            case DisplayMetrics.DENSITY_400:
                densityStr = "XMHDPI";
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                densityStr = "XXHDPI";
                break;
            case DisplayMetrics.DENSITY_XXXHIGH:
                densityStr = "XXXHDPI";
                break;
        }
        return densityStr;
    }

    /**
     * 运营商
     * @param context context to use to retrieve the TelephonyManager from
     * @return the display name of the current network operator, or the empty
     *         string if it cannot be accessed or determined
     */
    static String getCarrier(final Context context) {
        String carrier = "";
        final TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (manager != null) {
            carrier = manager.getNetworkOperatorName();
        }
        if (carrier == null || carrier.length() == 0) {
            carrier = "";
            if (Statistical.sharedInstance().isLoggingEnabled()) {
                Log.i(Statistical.TAG, "No carrier found");
            }
        }
        return carrier;
    }

    /**
     * 城市
     * Returns the current locale (ex. "en_US").
     */
    static String getLocale() {
        final Locale locale = Locale.getDefault();
        return locale.getLanguage() + "_" + locale.getCountry();
    }

    /**
     * 应用版本
     * Returns the application version string stored in the specified
     * context's package info versionName field, or "1.0" if versionName
     * is not present.
     */
    static String getAppVersion(final Context context) {
        String result = Statistical.DEFAULT_APP_VERSION;
        try {
            result = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        }
        catch (PackageManager.NameNotFoundException e) {
            if (Statistical.sharedInstance().isLoggingEnabled()) {
                Log.i(Statistical.TAG, "No app version found");
            }
        }
        return result;
    }

    /**
     * 应用包名
     * Returns the package name of the app that installed this app
     */
    static String getStore(final Context context) {
        String result = "";
        if(android.os.Build.VERSION.SDK_INT >= 3 ) {
            try {
                result = context.getPackageManager().getInstallerPackageName(context.getPackageName());
            } catch (Exception e) {
                if (Statistical.sharedInstance().isLoggingEnabled()) {
                    Log.i(Statistical.TAG, "Can't get Installer package");
                }
            }
            if (result == null || result.length() == 0) {
                result = "";
                if (Statistical.sharedInstance().isLoggingEnabled()) {
                    Log.i(Statistical.TAG, "No store found");
                }
            }
        }
        return result;
    }

    /**
     * 
     */
    static String getMetrics(final Context context) {
        final JSONObject json = new JSONObject();

        fillJSONIfValuesNotEmpty(json,
                "_device", getDevice(),
                "_os", getOS(),
                "_os_version", getOSVersion(),
                "_carrier", getCarrier(context),
                "_resolution", getResolution(context),
                "_density", getDensity(context),
                "_locale", getLocale(),
                "_app_version", getAppVersion(context),
                "_store", getStore(context));

        String result = json.toString();

        try {
            result = java.net.URLEncoder.encode(result, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }

        return result;
    }

    /**
     * 
     * @param json JSONObject to fill
     * @param objects varargs of this kind: key1, value1, key2, value2, ...
     */
    static void fillJSONIfValuesNotEmpty(final JSONObject json, final String ... objects) {
        try {
            if (objects.length > 0 && objects.length % 2 == 0) {
                for (int i = 0; i < objects.length; i += 2) {
                    final String key = objects[i];
                    final String value = objects[i + 1];
                    if (value != null && value.length() > 0) {
                        json.put(key, value);
                    }
                }
            }
        } catch (JSONException ignored) {
        }
    }
    public static String getIp(Context context) {
		String ip = "";
		if (getNetWorkType(context).equals("wifi")) {
			WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			// 判断wifi是否开启
			if (!wifiManager.isWifiEnabled()) {
				wifiManager.setWifiEnabled(true);
			}
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int ipAddress = wifiInfo.getIpAddress();
			ip = intToIp(ipAddress);
		} else if (getNetWorkType(context).equals("mobile")) {
			try {
				for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
					NetworkInterface intf = en.nextElement();
					for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
						InetAddress inetAddress = enumIpAddr.nextElement();
						if (!inetAddress.isLoopbackAddress()) {
							ip = inetAddress.getHostAddress().toString();
						}
					}
				}
			} catch (SocketException ex) {
				return null;
			}
		}

		return ip;
	}
    
    public static String getNetWorkType(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (cm != null) {
			NetworkInfo info = cm.getActiveNetworkInfo();
			if (info != null) {
				int networkInfoType = info.getType();
				if (networkInfoType == ConnectivityManager.TYPE_WIFI || networkInfoType == ConnectivityManager.TYPE_ETHERNET) {
					return "wifi";
				} else if (networkInfoType == ConnectivityManager.TYPE_MOBILE) {
					return "mobile";
				}

			}

		}
		return "wifi";
	}
    
    private static String intToIp(int i) {

		return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
	}
    
    
    
    
}
