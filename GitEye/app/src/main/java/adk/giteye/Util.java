package adk.giteye;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;


class Util {

    static int dpToPixels(Context context, int dp) {
        return (int) ((dp * context.getTheme().getResources().getDisplayMetrics().density) / 0.5);
    }

    public static RectF rectToRectF(Rect r) {
        return new RectF(r.left, r.top, r.right, r.bottom);
    }
}
