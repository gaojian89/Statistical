
package com.statistical.android.sdk;

import android.content.Context;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 * ConnectionQueue队列会话和事件数据，并定时发送数据到后台线程Count.ly服务器。
 */
public class ConnectionQueue {
	private StatisticalStore store_;
	private ExecutorService executor_;
	private String appKey_;
	private Context context_;
	private String serverURL_;
	private Future<?> connectionProcessorFuture_;
	private DeviceId deviceId_;
	private SSLContext sslContext_;
	private Statistical.CountlyMode mode_;

	Statistical.CountlyMode getMode() {
		return mode_;
	}

	void setMode(Statistical.CountlyMode mode) {
		mode_ = mode;
	}

	String getAppKey() {
		return appKey_;
	}

	void setAppKey(final String appKey) {
		appKey_ = appKey;
	}

	Context getContext() {
		return context_;
	}

	void setContext(final Context context) {
		context_ = context;
	}

	String getServerURL() {
		return serverURL_;
	}

	void setServerURL(final String serverURL) {
		serverURL_ = serverURL;

		if (Statistical.publicKeyPinCertificates == null) {
			sslContext_ = null;
		} else {
			try {
				TrustManager tm[] = { new CertificateTrustManager(Statistical.publicKeyPinCertificates) };
				sslContext_ = SSLContext.getInstance("TLS");
				sslContext_.init(null, tm, null);
			} catch (Throwable e) {
				throw new IllegalStateException(e);
			}
		}
	}

	StatisticalStore getCountlyStore() {
		return store_;
	}

	void setCountlyStore(final StatisticalStore countlyStore) {
		store_ = countlyStore;
	}

	DeviceId getDeviceId() {
		return deviceId_;
	}

	public void setDeviceId(DeviceId deviceId) {
		this.deviceId_ = deviceId;
	}

	/**
	 * 检查appkey store url
	 */
	void checkInternalState() {
		if (context_ == null) {
			throw new IllegalStateException("context has not been set");
		}
		if (appKey_ == null || appKey_.length() == 0) {
			throw new IllegalStateException("app key has not been set");
		}
		if (store_ == null) {
			throw new IllegalStateException("countly store has not been set");
		}
		if (serverURL_ == null || !Statistical.isValidURL(serverURL_)) {
			throw new IllegalStateException("server URL is not valid");
		}
		if (Statistical.publicKeyPinCertificates != null && !serverURL_.startsWith("https")) {
			throw new IllegalStateException("server must start with https once you specified public keys");
		}
	}

	/**
	 * 记录 启动事件并将他发送到
	 * @throws IllegalStateException
	 *             if context, app key, store, or server URL have not been set
	 */
	void beginSession() {
		checkInternalState();
		final String data = "app_key=" + appKey_ + "&begin_session=1" + "&timestamp=" + Statistical.currentTimestamp() + "&sdk_version=" + Statistical.SDK_VERSION_STRING + "&ip_address=" + DeviceInfo.getIp(context_) + "&test_mode=" + (mode_ == Statistical.CountlyMode.TEST ? 2 : 0)
				+ "&metrics=" + DeviceInfo.getMetrics(context_);
		store_.addConnection(data);

		tick();
	}

	/**
	 * 记录一个持续事件并将其发送到server 
	 * 
	 * @param duration
	 *            duration in seconds to extend the current app session, should
	 *            be more than zero
	 * @throws IllegalStateException
	 *             if context, app key, store, or server URL have not been set
	 */
	void updateSession(final int duration) {
		checkInternalState();
		if (duration > 0) {
			final String data = "app_key=" + appKey_ + "&timestamp=" + Statistical.currentTimestamp() + "&test_mode=" + (mode_ == Statistical.CountlyMode.TEST ? 2 : 0) + "&session_duration=" + duration;

			store_.addConnection(data);

			tick();
		}
	}

	public void tokenSession(String token, Statistical.CountlyMessagingMode mode) {
		checkInternalState();

		final String data = "app_key=" + appKey_ + "&" + "timestamp=" + Statistical.currentTimestamp() + "&hour=" + Statistical.currentHour() + "&dow=" + Statistical.currentDayOfWeek() + "&" + "token_session=1" + "&" + "android_token=" + token + "&" + "test_mode="
				+ (mode == Statistical.CountlyMessagingMode.TEST ? 2 : 0) + "&" + "locale=" + DeviceInfo.getLocale();

		final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
		worker.schedule(new Runnable() {
			@Override
			public void run() {
				store_.addConnection(data);
				tick();
			}
		}, 10, TimeUnit.SECONDS);
	}

	/**
	 * 记录一个结束事件并将其发送到server 
	 * 
	 * @param duration
	 *            duration in seconds to extend the current app session
	 * @throws IllegalStateException
	 *             if context, app key, store, or server URL have not been set
	 */
	void endSession(final int duration) {
		checkInternalState();
		String data = "app_key=" + appKey_ + "&timestamp=" + Statistical.currentTimestamp() + "&test_mode=" + (mode_ == Statistical.CountlyMode.TEST ? 2 : 0) + "&end_session=1";
		if (duration > 0) {
			data += "&session_duration=" + duration;
		}

		store_.addConnection(data);

		tick();
	}

	/**
	 * 将用户信息发送到server 
	 * 
	 * @throws java.lang.IllegalStateException
	 *             if context, app key, store, or server URL have not been set
	 */
	void sendUserData() {
		checkInternalState();
		String userdata = UserData.getDataForRequest();

		if (!userdata.equals("")) {
			String data = "app_key=" + appKey_ + "&timestamp=" + Statistical.currentTimestamp() + "&hour=" + Statistical.currentHour() + "&dow=" + Statistical.currentDayOfWeek() + userdata;
			store_.addConnection(data);

			tick();
		}
	}

	/**
	 * 发送安装信息到server
	 * 
	 * @param referrer
	 *            query parameters
	 * @throws java.lang.IllegalStateException
	 *             if context, app key, store, or server URL have not been set
	 */
	void sendReferrerData(String referrer) {
		checkInternalState();

		if (referrer != null) {
			String data = "app_key=" + appKey_ + "&timestamp=" + Statistical.currentTimestamp() + "&test_mode=" + (mode_ == Statistical.CountlyMode.TEST ? 2 : 0) + referrer;
			store_.addConnection(data);

			tick();
		}
	}

	/**
	 * 发送错误报告到server 
	 * 
	 * @throws IllegalStateException
	 *             if context, app key, store, or server URL have not been set
	 */
	void sendCrashReport(String error, boolean nonfatal) {
		checkInternalState();
		final String data = "app_key=" + appKey_ + "&timestamp=" + Statistical.currentTimestamp() + "&test_mode=" + (mode_ == Statistical.CountlyMode.TEST ? 2 : 0) + "&hour=" + Statistical.currentHour() + "&dow=" + Statistical.currentDayOfWeek() + "&sdk_version="
				+ Statistical.SDK_VERSION_STRING + "&crash=" + CrashDetails.getCrashData(context_, error, nonfatal);

		store_.addConnection(data);

		tick();
	}

	/**
	 * 记录指定事件并发送到server 
	 * server.
	 * 
	 * @param events
	 *            URL-encoded JSON string of event data
	 * @throws IllegalStateException
	 *             if context, app key, store, or server URL have not been set
	 */
	void recordEvents(final String events) {
		checkInternalState();
		final String data = "app_key=" + appKey_ + "&timestamp=" + Statistical.currentTimestamp() + "&test_mode=" + (mode_ == Statistical.CountlyMode.TEST ? 2 : 0)
		// + "&hour=" + Countly.currentHour()
		// + "&dow=" + Countly.currentDayOfWeek()
				+ "&events=" + events;

		store_.addConnection(data);

		tick();
	}

	/**
	 * Records the specified events and sends them to the server.
	 * 
	 * @param events
	 *            URL-encoded JSON string of event data
	 * @throws IllegalStateException
	 *             if context, app key, store, or server URL have not been set
	 */
	void recordLocation(final String events) {
		checkInternalState();
		final String data = "app_key=" + appKey_ + "&timestamp=" + Statistical.currentTimestamp() + "&hour=" + Statistical.currentHour() + "&dow=" + Statistical.currentDayOfWeek() + "&events=" + events;

		store_.addConnection(data);

		tick();
	}

	/**
	 * Ensures that an executor has been created for ConnectionProcessor
	 * instances to be submitted to.
	 */
	void ensureExecutor() {
		if (executor_ == null) {
			executor_ = Executors.newSingleThreadExecutor();
		}
	}

	/**
	 * 发送数据到server 
	 */
	void tick() {
		if (!store_.isEmptyConnections() && (connectionProcessorFuture_ == null || connectionProcessorFuture_.isDone())) {
			ensureExecutor();
			connectionProcessorFuture_ = executor_.submit(new ConnectionProcessor(serverURL_, store_, deviceId_, sslContext_));
		}
	}

	// for unit testing
	ExecutorService getExecutor() {
		return executor_;
	}

	void setExecutor(final ExecutorService executor) {
		executor_ = executor;
	}

	Future<?> getConnectionProcessorFuture() {
		return connectionProcessorFuture_;
	}

	void setConnectionProcessorFuture(final Future<?> connectionProcessorFuture) {
		connectionProcessorFuture_ = connectionProcessorFuture;
	}

}
