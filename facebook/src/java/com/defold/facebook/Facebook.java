package com.defold.facebook;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.facebook.login.DefaultAudience;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.FacebookSdk;

public class Facebook implements Handler.Callback {
    private static final String TAG = "defold.facebook";

    public static final String INTENT_EXTRA_MESSENGER    = "defold.fb.intent.extra.messenger";
    public static final String INTENT_EXTRA_APP_ID       = "defold.fb.intent.extra.app_id";
    public static final String INTENT_EXTRA_PERMISSIONS  = "defold.fb.intent.extra.permissions";
    public static final String INTENT_EXTRA_AUDIENCE     = "defold.fb.intent.extra.audience";
    public static final String INTENT_EXTRA_DIALOGTYPE   = "defold.fb.intent.extra.dialogtype";
    public static final String INTENT_EXTRA_DIALOGPARAMS = "defold.fb.intent.extra.dialogparams";

    public static final String MSG_KEY_ACTION           = "defold.fb.msg.action";
    public static final String MSG_KEY_SUCCESS          = "defold.fb.msg.success";
    public static final String MSG_KEY_ERROR            = "defold.fb.msg.error";
    public static final String MSG_KEY_STATE            = "defold.fb.msg.state";
    public static final String MSG_KEY_USER             = "defold.fb.msg.user";
    public static final String MSG_KEY_ACCESS_TOKEN     = "defold.fb.msg.access_token";
    public static final String MSG_KEY_DIALOG_RESULT    = "defold.fb.msg.dialog_result";

    public static final String ACTION_LOGIN             = "defold.fb.intent.action.LOGIN";
    public static final String ACTION_REQ_READ_PERMS    = "defold.fb.intent.action.REQ_READ_PERMS";
    public static final String ACTION_REQ_PUB_PERMS     = "defold.fb.intent.action.REQ_PUB_PERMS";
    public static final String ACTION_SHOW_DIALOG       = "defold.fb.intent.action.SHOW_DIALOG";
    public static final String ACTION_UPDATE_METABLE    = "defold.fb.intent.action.UPDATE_METABLE";

    public static final String ACTION_LOGIN_WITH_PERMISSIONS    = "defold.fb.intent.action.LOGIN_WITH_PERMISSIONS";

    private Handler handler;
    private Messenger messenger;
    private String appId;
    private Callback callback;
    private LoginCallback loginCallback;
    private StateCallback stateCallback;
    private DialogCallback dialogCallback;
    private Map<String, String> me;
    private String accessToken;
    private Activity activity;
    private AppEventsLogger eventLogger;

    public interface Callback {
        void onDone(String error);
    }

    public interface LoginCallback {
        void onDone(int state, String error);
    }

    public interface StateCallback {
        void onDone(int state, String error);
    }

    public interface DialogCallback {
        void onDone(Bundle result, String error);
    }

    public Facebook(Activity activity, String appId) {
        this.appId = appId;
        this.me = null;
        this.accessToken = null;
        this.activity = activity;
        this.eventLogger = AppEventsLogger.newLogger(this.activity);
    }

    private void startActivity(String action) {
        startActivity(action, null);
    }

    private void startActivity(String action, Bundle extras) {

        if (this.messenger == null) {
            this.handler = new Handler(this);
            this.messenger = new Messenger(this.handler);
        }


        Intent intent = new Intent(activity, FacebookActivity.class);
        intent.putExtra(INTENT_EXTRA_MESSENGER, messenger);
        intent.putExtra(INTENT_EXTRA_APP_ID, appId);
        if (extras != null) {
            intent.putExtras(extras);
        }
        intent.setAction(action);
        activity.startActivity(intent);
    }

    public void logout() {
        if (AccessToken.getCurrentAccessToken() != null) {
            LoginManager.getInstance().logOut();
        }

        this.me = null;
        this.accessToken = null;
    }

    public String getAccessToken() {
        if (AccessToken.getCurrentAccessToken() != null && !AccessToken.getCurrentAccessToken().isDataAccessExpired()) {
            return AccessToken.getCurrentAccessToken().getToken();
        }
        return null;
    }

    public List<String> getPermissions() {
        if (AccessToken.getCurrentAccessToken() != null) {
            return new ArrayList<String>(AccessToken.getCurrentAccessToken().getPermissions());
        }

        return Collections.<String> emptyList();
    }

    public void loginWithPermissions(String[] permissions, int audience, LoginCallback callback) {
        this.loginCallback = callback;

        Bundle extras = new Bundle();
        extras.putInt(INTENT_EXTRA_AUDIENCE, audience);
        extras.putStringArray(INTENT_EXTRA_PERMISSIONS, permissions);

        startActivity(ACTION_LOGIN_WITH_PERMISSIONS, extras);
    }

    public void showDialog(String dialogType, Bundle params, DialogCallback cb) {

        this.dialogCallback = cb;
        Bundle extras = new Bundle();
        extras.putString(INTENT_EXTRA_DIALOGTYPE, dialogType);
        extras.putBundle(INTENT_EXTRA_DIALOGPARAMS, params);
        startActivity(ACTION_SHOW_DIALOG, extras);
    }

    public void postEvent(String event, double valueToSum, String[] keys, String[] values) {
        try {
            if (this.eventLogger != null) {
                if (keys.length == 0) {
                    this.eventLogger.logEvent(event, valueToSum);
                } else {
                    Bundle bundle = new Bundle(keys.length);
                    for (int i = 0; i < keys.length; ++i) {
                        bundle.putString(keys[i], values[i]);
                    }

                    this.eventLogger.logEvent(event, valueToSum, bundle);
                }
            }
        } catch (RuntimeException exception) {
            Log.e(TAG, "Unable to post event to Facebook Analytics");
        } catch (Exception exception) {
            Log.e(TAG, "An error occurred while attempting to post event to Facebook Analytics");
        } catch (Throwable throwable) {
            Log.e(TAG, "A critical error occurred while attempting to post event to Facebook Analytics");
        }
    }

    public String getSdkVersion() {
        return FacebookSdk.getSdkVersion();
    }

    public void disableEventUsage() {
        FacebookSdk.setLimitEventAndDataUsage(this.activity, true);
    }

    public void enableEventUsage() {
        FacebookSdk.setLimitEventAndDataUsage(this.activity, false);
    }

    @Override
    public boolean handleMessage(Message msg) {
        boolean handled = false;
        Bundle data = msg.getData();
        String error = null;
        boolean success = data.containsKey(MSG_KEY_SUCCESS);
        if (data.containsKey(MSG_KEY_ERROR)) {
            error = data.getString(MSG_KEY_ERROR);
        }
        String action = data.getString(MSG_KEY_ACTION);
        if (action.equals(ACTION_LOGIN) || action.equals(ACTION_UPDATE_METABLE)) {
            if (success && data.getString(MSG_KEY_USER) != null) {
                JSONObject me;
                try {
                    me = new JSONObject(data.getString(MSG_KEY_USER));
                    Iterator<?> it = me.keys();
                    Map<String, String> meMap = new HashMap<String, String>();
                    while (it.hasNext()) {
                        String key = it.next().toString();
                        String val = me.getString(key);
                        meMap.put(key, val);
                    }
                    this.me = meMap;
                    this.accessToken = data.getString(MSG_KEY_ACCESS_TOKEN);
                } catch (JSONException e) {
                    Log.wtf(TAG, e);
                }
            }

            if (action.equals(ACTION_LOGIN)) {
                this.stateCallback.onDone(data.getInt(MSG_KEY_STATE), error);
            }
        } else if (action.equals(ACTION_LOGIN_WITH_PERMISSIONS)) {
            this.loginCallback.onDone(data.getInt(MSG_KEY_STATE), error);
        } else if (action.equals(ACTION_SHOW_DIALOG)) {
            this.dialogCallback.onDone(data.getBundle(MSG_KEY_DIALOG_RESULT), error);
        }
        return handled;
    }
}
