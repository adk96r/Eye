package adk.giteye;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

public class PersonInfoView extends View {

    Context context;
    Person person;
    Rect bounds;
    Paint outlinePaint;
    Paint basePaint;
    Paint textPaint;
    int LOD;
    int radiusPixels;
    int gap;
    int textLineHeight;
    int textLinePadding;
    int textPadding;
    int baseStartHeight;
    int baseHeight;

    public PersonInfoView(Context context, Rect bounds, Person person, int LOD) {
        super(context);
        this.context = context;
        this.bounds = bounds;
        this.person = person;
        this.LOD = LOD;
        setupInfoView();
    }

    private void setupInfoView() {

        outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setColor(Color.WHITE);

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


}
