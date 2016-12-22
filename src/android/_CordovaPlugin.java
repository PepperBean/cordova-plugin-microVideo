package com.pepperbean.plugin;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;

import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.widget.Toast;

import java.io.File;

public class _CordovaPlugin extends CordovaPlugin {
    private CallbackContext context;
    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        this.context = callbackContext;
        //根据action判断调用方法
        if (action.equals("startRecorder")) {
            //通过Intent绑定将要调用的Activity
            Intent intent = new Intent(this.cordova.getActivity(), RecActivity.class);
            //加入将要传输到activity中的参数
            //intent.putExtra("province", args.getString(0));
            //启动activity
            this.cordova.startActivityForResult(this, intent, 0);
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String filePath = data.getStringExtra("back");
            context.success(filePath);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            String filePath = data.getStringExtra("back");
            context.success("canceled");
        }
    }

}