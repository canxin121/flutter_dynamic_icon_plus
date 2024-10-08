package com.solusibejo.flutter_dynamic_icon_plus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result


/** FlutterDynamicIconPlusPlugin */
class FlutterDynamicIconPlusPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null

    companion object {
        const val PLUGIN_NAME = "flutter_dynamic_icon_plus"
        const val APP_ICON = "app_icon"
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, PLUGIN_NAME)
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            MethodNames.SET_ALTERNATE_ICON_NAME -> {
                if (activity != null) {
                    val sp = activity?.getSharedPreferences(PLUGIN_NAME, Context.MODE_PRIVATE)
                    val iconName = call.argument<String?>(Arguments.ICON_NAME)
                    val forceExit = call.argument<Boolean?>(Arguments.FORCE_EXIT) ?: false

                    val saved = sp?.edit()?.putString(APP_ICON, iconName)?.commit()

                    Log.d("setAlternateIconName", "Saved app icon status: $saved")

                    if (saved == true) {
                        if (forceExit
                        ) {
                            if (activity != null) {
                                if (iconName != null) {
                                    ComponentUtil.changeAppIcon(
                                        activity!!,
                                        activity!!.packageManager,
                                        activity!!.packageName
                                    )
                                }

                                ComponentUtil.removeCurrentAppIcon(activity!!)
                            }
                        } else {
                            val flutterDynamicIconPlusService =
                                Intent(activity, FlutterDynamicIconPlusService::class.java)
                            activity?.startService(flutterDynamicIconPlusService)
                        }

                        result.success(true)
                    } else {
                        result.error(
                            "500",
                            "Failed store $iconName to local storage",
                            "When failed store to local storage we will provide wrong value on method getAlternateIconName"
                        )
                    }
                } else {
                    result.error("500", "Activity not found", "Activity didn't attached")
                }
            }

            MethodNames.SUPPORTS_ALTERNATE_ICONS -> {
                if (activity != null) {
                    val packageInfo = ComponentUtil.packageInfo(activity!!)
                    //By default, we have one activity (MainActivity).
                    //If there is more than one activity, it indicates that we have an alternative activity
                    val isAlternateAvailable = packageInfo.activities.size > 1
                    result.success(isAlternateAvailable)
                } else {
                    result.success(false)
                }
            }

            MethodNames.GET_ALTERNATE_ICON_NAME -> {
                if (activity != null) {
                    val enabledComponent = ComponentUtil.getCurrentEnabledAlias(activity!!)
                    result.success(enabledComponent?.name)
                } else {
                    result.error("500", "Activity not found", "Activity didn't attached")
                }
            }

            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
}
