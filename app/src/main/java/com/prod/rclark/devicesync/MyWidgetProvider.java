package com.prod.rclark.devicesync;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.prod.rclark.devicesync.PhoneUI.MainPhoneActivity;

/**
 * Created by rclar on 7/19/2016.
 */
public class MyWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_CLICK = "ACTION_CLICK";
    private static final String TAG = "DS_Widget";

    @Override
    public void onUpdate(Context ctx, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {

        // Get all ids
        ComponentName thisWidget = new ComponentName(ctx, MyWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        for (int widgetId : allWidgetIds) {
            //Okay - calculate the number of unique apps in CP
            int uniqueApps = DBUtils.getMissingCount(ctx);

            //get the removeview
            RemoteViews remoteViews = new RemoteViews(ctx.getPackageName(), R.layout.phone_widget);

            Log.d(TAG, "Setting widget text to " + uniqueApps);

            // Set the text
            remoteViews.setTextViewText(R.id.widget_count, String.valueOf(uniqueApps));

            // Register an onClickListener and set it to launch phone activity (no widgets on ATV)
            Intent intent = new Intent(ctx, MainPhoneActivity.class);

            //highlight if non-zero and set up launch intent to launch to missing page
            if (uniqueApps != 0) {
                remoteViews.setTextColor(R.id.widget_count, ctx.getResources().getColor(R.color.accent));
                intent.setAction("android.intent.action.MAIN");
                intent.putExtra(MainPhoneActivity.INTENT_EXTRA_LAUNCH, MainPhoneActivity.INTENT_EXTRA_LAUNCH_MISSING);
                Log.d(TAG, "Widget setup to launch missing");
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent, 0);

            remoteViews.setOnClickPendingIntent(R.id.widget_count, pendingIntent);

            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }
}
