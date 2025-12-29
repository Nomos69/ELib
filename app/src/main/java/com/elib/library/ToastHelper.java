package com.elib.library;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ToastHelper {

    public static void showSuccess(Context context, String message) {
        showCustomToast(context, message, R.drawable.ic_toast_success);
    }

    public static void showError(Context context, String message) {
        showCustomToast(context, message, R.drawable.ic_toast_error);
    }

    public static void showInfo(Context context, String message) {
        // Reusing success or creating a generic info icon. 
        // For now, let's use success icon but maybe we can tint it blue or just use it as is.
        // Or default generic icon.
        showCustomToast(context, message, R.drawable.ic_books); // Using app icon or generic
    }

    private static void showCustomToast(Context context, String message, int iconResId) {
        if (context == null) return;

        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.layout_custom_toast, null);

        ImageView icon = layout.findViewById(R.id.toast_icon);
        TextView text = layout.findViewById(R.id.toast_text);

        icon.setImageResource(iconResId);
        text.setText(message);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}
