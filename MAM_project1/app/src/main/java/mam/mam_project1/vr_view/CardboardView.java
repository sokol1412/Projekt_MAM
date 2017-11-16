package mam.mam_project1.vr_view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class CardboardView extends LinearLayout {
    private final CardboardOverlayEyeView leftView;
    private final CardboardOverlayEyeView rightView;
    private AlphaAnimation textFadeAnimation;

    public CardboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);

        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f);
        params.setMargins(0, 0, 0, 0);

        leftView = new CardboardOverlayEyeView(context, attrs);
        leftView.setLayoutParams(params);
        addView(leftView);

        rightView = new CardboardOverlayEyeView(context, attrs);
        rightView.setLayoutParams(params);
        addView(rightView);
        
        setDepthOffset(0.016f);
        setColor(Color.rgb(150, 255, 180));
        setVisibility(View.VISIBLE);

        textFadeAnimation = new AlphaAnimation(1.0f, 0.0f);
        textFadeAnimation.setDuration(5000);
    }


    private void setDepthOffset(float offset) {
        leftView.setOffset(offset);
        rightView.setOffset(-offset);
    }

    private void setColor(int color) {
        leftView.setColor(color);
        rightView.setColor(color);
    }

    private class CardboardOverlayEyeView extends ViewGroup {
        private final ImageView imageView;
        private final TextView textView;
        private float offset;

        public CardboardOverlayEyeView(Context context, AttributeSet attrs) {
            super(context, attrs);
            imageView = new ImageView(context, attrs);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setAdjustViewBounds(true);
            addView(imageView);

            textView = new TextView(context, attrs);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.0f);
            textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
            textView.setGravity(Gravity.CENTER);
            textView.setShadowLayer(3.0f, 0.0f, 0.0f, Color.DKGRAY);
            addView(textView);
        }

        public void setColor(int color) {
            imageView.setColorFilter(color);
            textView.setTextColor(color);
        }

        public void setText(String text) {
            textView.setText(text);
        }

        public void setOffset(float offset) {
            this.offset = offset;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            final int width = right - left;
            final int height = bottom - top;

            final float imageSize = 0.12f;

            final float verticalImageOffset = -0.07f;

            final float verticalTextPos = 0.52f;

            float imageMargin = (1.0f - imageSize) / 2.0f;
            float leftMargin = (int) (width * (imageMargin + offset));
            float topMargin = (int) (height * (imageMargin + verticalImageOffset));
            imageView.layout(
                    (int) leftMargin, (int) topMargin,
                    (int) (leftMargin + width * imageSize), (int) (topMargin + height * imageSize));

            leftMargin = offset * width;
            topMargin = height * verticalTextPos;
            textView.layout(
                    (int) leftMargin, (int) topMargin,
                    (int) (leftMargin + width), (int) (topMargin + height * (1.0f - verticalTextPos)));
        }
    }
}
