package adk.giteye;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

public class PersonInfoView extends View {

    private static final String TAG = "Checks";
    int LOD;
    int radiusPixels;
    int gap;
    int textLineHeight;
    int textLinePadding;
    int textPadding;
    int baseStartHeight;
    int baseHeight;
    private Context context;
    private Activity activity;
    private Person person;
    private Rect bounds;
    private Paint outlinePaint;
    private Paint basePaint;
    private Paint textPaint;
    private boolean dialogsAllowed;
    private PersonInfoView ref;

    public PersonInfoView(Context context, Rect bounds, Person person, int LOD, Activity activity) {
        super(context);
        this.context = context;
        this.bounds = bounds;
        this.person = person;
        this.LOD = LOD;
        this.activity = activity;
        this.ref = this;
        setupInfoView();
        setOnTouchListener(getOnTouchListener());
        allowDialogs(true);
    }

    private void setupInfoView() {

        outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(Util.dpToPixels(context, 1));
        outlinePaint.setColor(getRandomColor());

        basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        basePaint.setStyle(Paint.Style.FILL);
        basePaint.setColor(Color.WHITE);
        basePaint.setShadowLayer(Util.dpToPixels(context, 1), 0, 0, Color.DKGRAY);
        setLayerType(LAYER_TYPE_SOFTWARE, basePaint);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(Util.dpToPixels(context, 6));
        textPaint.setColor(Color.BLACK);
        textPaint.setStyle(Paint.Style.FILL);

        gap = Util.dpToPixels(context, 2);
        radiusPixels = Util.dpToPixels(context, 2);
        textPadding = Util.dpToPixels(context, 2);
        textLinePadding = Util.dpToPixels(context, 1);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width, height;

        width = getSuggestedMinimumWidth();
        height = getSuggestedMinimumHeight();

        textPaint.setTextSize(bounds.width() / 8);
        textLineHeight = (int) textPaint.measureText("H");

        baseHeight = 2 * textLinePadding + (LOD + 1) * (textLineHeight + 2 * textLinePadding);
        width += radiusPixels + bounds.width() + radiusPixels;
        height += bounds.height() + baseHeight + 2 * radiusPixels;
        baseStartHeight = bounds.height() + gap;

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Outline
        canvas.drawRoundRect(radiusPixels, radiusPixels,
                getWidth() - 2 * radiusPixels, bounds.height(),
                radiusPixels, radiusPixels, outlinePaint);

        // Base for the info
        canvas.drawRoundRect(radiusPixels, baseStartHeight,
                getWidth() - 2 * radiusPixels,
                baseStartHeight + baseHeight,
                radiusPixels, radiusPixels, basePaint);

        // Person's info
        if (LOD >= 0)
            canvas.drawText(person.getInfo(0),
                    radiusPixels + textPadding,
                    baseStartHeight + textLineHeight + 2 * textLinePadding, textPaint);
        if (LOD >= 1)
            canvas.drawText(person.getInfo(1),
                    radiusPixels + textPadding,
                    baseStartHeight + (2) * (textLineHeight + 2 * textLinePadding),
                    textPaint);
        if (LOD >= 2)
            canvas.drawText(person.getInfo(2),
                    radiusPixels + textPadding,
                    baseStartHeight + (3) * (textLineHeight + 2 * textLinePadding),
                    textPaint);

    }

    public void updateBounds(Rect bounds) {
        this.bounds = bounds;
        invalidate();
    }

    public void updateLOD(int LOD) {
        this.LOD = LOD;
        invalidate();
    }

    public int getRandomColor() {
        Random random = new Random();

        switch (random.nextInt(10)) {
            case 0:
                return Color.WHITE;
            case 1:
                return Color.BLACK;
            case 2:
                return Color.BLUE;
            case 3:
                return Color.CYAN;
            case 4:
                return Color.YELLOW;
            case 5:
                return Color.GREEN;
            case 6:
                return Color.RED;
            case 7:
                return Color.GRAY;
            case 8:
                return Color.LTGRAY;
            case 9:
                return Color.DKGRAY;
            default:
                return Color.MAGENTA;
        }
    }

    public void allowDialogs(boolean allow){
        this.dialogsAllowed = allow;
    }

    private OnTouchListener getOnTouchListener() {

        return new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (dialogsAllowed) {

                    Log.d(TAG, "Touched on a person");
                    PersonDialog personDialog = new PersonDialog();
                    personDialog.setData(context, person, ref);
                    personDialog.show(activity.getFragmentManager(), "person");

                }
                return true;
            }
        };
    }

}
