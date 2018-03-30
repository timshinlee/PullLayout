package com.timshinlee.pulllayout;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.TextView;

public class PullLayout extends ViewGroup {
    private static final String TAG = "PullLayout";
    private View mHeader;
    private View mFooter;
    private int mContentHeight;
    private int mLastTouchY;
    private Scroller mScroller;
    private static final float SCROLL_FRICTION = .6f;
    private static int SCROLL_MAX_DISTANCE;
    private static int SCROLL_DISTANCE_TO_REFRESH;
    private TextView mHeaderContent;
    private ProgressBar mHeaderProgress;
    private Drawable mArrowDown;
    private Drawable mArrowUp;
    private boolean mToRefresh = false;
    private boolean mRefreshing = false;

    public PullLayout(Context context) {
        super(context);
        init(context);
    }


    public PullLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PullLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mHeader = LayoutInflater.from(context).inflate(R.layout.header_pull_layout, this, false);
        mFooter = LayoutInflater.from(context).inflate(R.layout.footer_pull_layout, this, false);

        mHeaderContent = mHeader.findViewById(R.id.content);
        mHeaderProgress = mHeader.findViewById(R.id.progress);

        mScroller = new Scroller(context);

        final ViewConfiguration viewConfig = ViewConfiguration.get(context);
        SCROLL_DISTANCE_TO_REFRESH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f,
                Resources.getSystem().getDisplayMetrics());
        SCROLL_MAX_DISTANCE = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100f,
                Resources.getSystem().getDisplayMetrics());

        mArrowDown = getDrawable(R.drawable.ic_arrow_down);
        mArrowUp = getDrawable(R.drawable.ic_arrow_up);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        addView(mHeader);
        addView(mFooter);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        for (int i = 0; i < getChildCount(); i++) {
            measureChild(getChildAt(i), widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mContentHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child == mHeader) {
                child.layout(0, -mHeader.getMeasuredHeight(), child.getMeasuredWidth(), 0);
            } else if (child == mFooter) {
                child.layout(0, mContentHeight,
                        child.getMeasuredWidth(), mContentHeight + child.getMeasuredHeight());
            } else {
                int newContentHeight = mContentHeight + child.getMeasuredHeight();
                child.layout(0, mContentHeight, child.getMeasuredWidth(), newContentHeight);
                mContentHeight = newContentHeight;
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final View child = getChildAt(0);
        boolean intercept = false;
        final int y = (int) ev.getY();
        int deltaY = 0;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                deltaY = mLastTouchY - y;
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                break;
        }
        mLastTouchY = y;

        Log.e(TAG, "onInterceptTouchEvent: deltaY " + deltaY);
        if (child instanceof ScrollView) {
            ScrollView scrollView = (ScrollView) child;
            if (deltaY < 0) {
                Log.e(TAG, "onInterceptTouchEvent: scrollView.getScrollY " + scrollView.getScrollY());
                if (scrollView.getScrollY() == 0) {
                    intercept = true;
                    Log.e(TAG, "onInterceptTouchEvent: " + intercept);
                }
            }

        }
        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchY = y;
                if (mRefreshing) {
                    toggleRefreshing(false);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                final int deltaY = mLastTouchY - y;
                mLastTouchY = y;
                scrollBy(0, (int) (SCROLL_FRICTION * deltaY));
                if (Math.abs(getScrollY()) > SCROLL_MAX_DISTANCE) {
                    scrollTo(0, getScrollY() > 0 ? SCROLL_MAX_DISTANCE : -SCROLL_MAX_DISTANCE);
                }
                updateHeader(Math.abs(getScrollY()) > SCROLL_DISTANCE_TO_REFRESH);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mToRefresh) {
                    mScroller.startScroll(0, getScrollY(), 0, -SCROLL_DISTANCE_TO_REFRESH - getScrollY());
                    toggleRefreshing(true);
                } else {
                    mScroller.startScroll(0, getScrollY(), 0, -getScrollY());
                }
                invalidate();
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private void toggleRefreshing(boolean refreshing) {
        mHeaderContent.setVisibility(refreshing ? View.GONE : View.VISIBLE);
        mHeaderProgress.setVisibility(refreshing ? View.VISIBLE : View.GONE);
        if (refreshing && mListener != null) {
            mListener.onRefresh(this);
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(0, mScroller.getCurrY());
            invalidate();
        }
    }

    private void updateHeader(boolean showRefreshing) {
        if (showRefreshing != mToRefresh) {
            mHeaderContent.setText(showRefreshing ? "释放刷新" : "继续下拉刷新");
            mHeaderContent.setCompoundDrawables(showRefreshing ? mArrowUp : mArrowDown,
                    null, null, null);
            mToRefresh = showRefreshing;
        }
    }

    private Drawable getDrawable(int resId) {
        Drawable drawable = ContextCompat.getDrawable(getContext(), resId);
        if (drawable != null) {
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            return drawable;
        }
        throw new Resources.NotFoundException("找不到指定的资源图片");
    }

    public static abstract class PullLayoutListener {
        public void onRefresh(PullLayout pullLayout) {
        }
    }

    private PullLayoutListener mListener;

    public void setPullLayoutListener(PullLayoutListener listener) {
        mListener = listener;
    }

    public void finishRefresh() {
        mScroller.startScroll(0, getScrollY(), 0, -getScrollY());
        invalidate();
        toggleRefreshing(false);
    }
}
