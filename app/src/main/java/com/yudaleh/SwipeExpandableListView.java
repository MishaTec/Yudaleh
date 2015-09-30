/*
package com.yudaleh;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;

import com.fortysevendeg.swipelistview.SwipeListViewListener;
import com.fortysevendeg.swipelistview.SwipeListViewTouchListener;

import java.util.List;

*/
/**
 * Created by Michael on 30/09/2015.
 *//*

public class SwipeExpandableListView extends ExpandableListView{
    public static final String TAG = "SwipeListView";
    public static final boolean DEBUG = false;
    public static final int SWIPE_MODE_DEFAULT = -1;
    public static final int SWIPE_MODE_NONE = 0;
    public static final int SWIPE_MODE_BOTH = 1;
    public static final int SWIPE_MODE_RIGHT = 2;
    public static final int SWIPE_MODE_LEFT = 3;
    public static final int SWIPE_ACTION_REVEAL = 0;
    public static final int SWIPE_ACTION_DISMISS = 1;
    public static final int SWIPE_ACTION_CHOICE = 2;
    public static final int SWIPE_ACTION_NONE = 3;
    public static final String SWIPE_DEFAULT_FRONT_VIEW = "swipelist_frontview";
    public static final String SWIPE_DEFAULT_BACK_VIEW = "swipelist_backview";
    private static final int TOUCH_STATE_REST = 0;
    private static final int TOUCH_STATE_SCROLLING_X = 1;
    private static final int TOUCH_STATE_SCROLLING_Y = 2;
    private int touchState = 0;
    private float lastMotionX;
    private float lastMotionY;
    private int touchSlop;
    int swipeFrontView = 0;
    int swipeBackView = 0;
    private SwipeListViewListener swipeListViewListener;
    private SwipeListViewTouchListener touchListener;

    public SwipeExpandableListView(Context context, int swipeBackView, int swipeFrontView) {
        super(context);
        this.swipeFrontView = swipeFrontView;
        this.swipeBackView = swipeBackView;
        this.init((AttributeSet)null);
    }

    public SwipeExpandableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init(attrs);
    }

    public SwipeExpandableListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.init(attrs);
    }

    private void init(AttributeSet attrs) {
        int swipeMode = 1;
        boolean swipeOpenOnLongPress = true;
        boolean swipeCloseAllItemsWhenMoveList = true;
        long swipeAnimationTime = 0L;
        float swipeOffsetLeft = 0.0F;
        float swipeOffsetRight = 0.0F;
        int swipeDrawableChecked = 0;
        int swipeDrawableUnchecked = 0;
        int swipeActionLeft = 0;
        int swipeActionRight = 0;
        if(attrs != null) {
            TypedArray configuration = this.getContext().obtainStyledAttributes(attrs, com.fortysevendeg.swipelistview.R.styleable.SwipeListView);
            swipeMode = configuration.getInt(7, 1);
            swipeActionLeft = configuration.getInt(8, 0);
            swipeActionRight = configuration.getInt(9, 0);
            swipeOffsetLeft = configuration.getDimension(2, 0.0F);
            swipeOffsetRight = configuration.getDimension(3, 0.0F);
            swipeOpenOnLongPress = configuration.getBoolean(0, true);
            swipeAnimationTime = (long)configuration.getInteger(1, 0);
            swipeCloseAllItemsWhenMoveList = configuration.getBoolean(4, true);
            swipeDrawableChecked = configuration.getResourceId(10, 0);
            swipeDrawableUnchecked = configuration.getResourceId(11, 0);
            this.swipeFrontView = configuration.getResourceId(5, 0);
            this.swipeBackView = configuration.getResourceId(6, 0);
            configuration.recycle();
        }

        if(this.swipeFrontView == 0 || this.swipeBackView == 0) {
            this.swipeFrontView = this.getContext().getResources().getIdentifier("swipelist_frontview", "id", this.getContext().getPackageName());
            this.swipeBackView = this.getContext().getResources().getIdentifier("swipelist_backview", "id", this.getContext().getPackageName());
            if(this.swipeFrontView == 0 || this.swipeBackView == 0) {
                throw new RuntimeException(String.format("You forgot the attributes swipeFrontView or swipeBackView. You can add this attributes or use \'%s\' and \'%s\' identifiers", new Object[]{"swipelist_frontview", "swipelist_backview"}));
            }
        }

        ViewConfiguration configuration1 = ViewConfiguration.get(this.getContext());
        this.touchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration1);
        this.touchListener = new SwipeListViewTouchListener(this, this.swipeFrontView, this.swipeBackView);
        if(swipeAnimationTime > 0L) {
            this.touchListener.setAnimationTime(swipeAnimationTime);
        }

        this.touchListener.setRightOffset(swipeOffsetRight);
        this.touchListener.setLeftOffset(swipeOffsetLeft);
        this.touchListener.setSwipeActionLeft(swipeActionLeft);
        this.touchListener.setSwipeActionRight(swipeActionRight);
        this.touchListener.setSwipeMode(swipeMode);
        this.touchListener.setSwipeClosesAllItemsWhenListMoves(swipeCloseAllItemsWhenMoveList);
        this.touchListener.setSwipeOpenOnLongPress(swipeOpenOnLongPress);
        this.touchListener.setSwipeDrawableChecked(swipeDrawableChecked);
        this.touchListener.setSwipeDrawableUnchecked(swipeDrawableUnchecked);
        this.setOnTouchListener(this.touchListener);
        this.setOnScrollListener(this.touchListener.makeScrollListener());
    }

    public void recycle(View convertView, int position) {
        this.touchListener.reloadChoiceStateInView(convertView.findViewById(this.swipeFrontView), position);
        this.touchListener.reloadSwipeStateInView(convertView.findViewById(this.swipeFrontView), position);

        for(int j = 0; j < ((ViewGroup)convertView).getChildCount(); ++j) {
            View nextChild = ((ViewGroup)convertView).getChildAt(j);
            nextChild.setPressed(false);
        }

    }

    public boolean isChecked(int position) {
        return this.touchListener.isChecked(position);
    }

    public List<Integer> getPositionsSelected() {
        return this.touchListener.getPositionsSelected();
    }

    public int getCountSelected() {
        return this.touchListener.getCountSelected();
    }

    public void unselectedChoiceStates() {
        this.touchListener.unselectedChoiceStates();
    }

    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        this.touchListener.resetItems();
        if(null != adapter) {
            adapter.registerDataSetObserver(new DataSetObserver() {
                public void onChanged() {
                    super.onChanged();
                    SwipeListView.this.onListChanged();
                    SwipeListView.this.touchListener.resetItems();
                }
            });
        }

    }

    public void dismiss(int position) {
        int height = this.touchListener.dismiss(position);
        if(height > 0) {
            this.touchListener.handlerPendingDismisses(height);
        } else {
            int[] dismissPositions = new int[]{position};
            this.onDismiss(dismissPositions);
            this.touchListener.resetPendingDismisses();
        }

    }

    public void dismissSelected() {
        List list = this.touchListener.getPositionsSelected();
        int[] dismissPositions = new int[list.size()];
        int height = 0;

        for(int i = 0; i < list.size(); ++i) {
            int position = ((Integer)list.get(i)).intValue();
            dismissPositions[i] = position;
            int auxHeight = this.touchListener.dismiss(position);
            if(auxHeight > 0) {
                height = auxHeight;
            }
        }

        if(height > 0) {
            this.touchListener.handlerPendingDismisses(height);
        } else {
            this.onDismiss(dismissPositions);
            this.touchListener.resetPendingDismisses();
        }

        this.touchListener.returnOldActions();
    }

    public void openAnimate(int position) {
        this.touchListener.openAnimate(position);
    }

    public void closeAnimate(int position) {
        this.touchListener.closeAnimate(position);
    }

    protected void onDismiss(int[] reverseSortedPositions) {
        if(this.swipeListViewListener != null) {
            this.swipeListViewListener.onDismiss(reverseSortedPositions);
        }

    }

    protected void onStartOpen(int position, int action, boolean right) {
        if(this.swipeListViewListener != null && position != -1) {
            this.swipeListViewListener.onStartOpen(position, action, right);
        }

    }

    protected void onStartClose(int position, boolean right) {
        if(this.swipeListViewListener != null && position != -1) {
            this.swipeListViewListener.onStartClose(position, right);
        }

    }

    protected void onClickFrontView(int position) {
        if(this.swipeListViewListener != null && position != -1) {
            this.swipeListViewListener.onClickFrontView(position);
        }

    }

    protected void onClickBackView(int position) {
        if(this.swipeListViewListener != null && position != -1) {
            this.swipeListViewListener.onClickBackView(position);
        }

    }

    protected void onOpened(int position, boolean toRight) {
        if(this.swipeListViewListener != null && position != -1) {
            this.swipeListViewListener.onOpened(position, toRight);
        }

    }

    protected void onClosed(int position, boolean fromRight) {
        if(this.swipeListViewListener != null && position != -1) {
            this.swipeListViewListener.onClosed(position, fromRight);
        }

    }

    protected void onChoiceChanged(int position, boolean selected) {
        if(this.swipeListViewListener != null && position != -1) {
            this.swipeListViewListener.onChoiceChanged(position, selected);
        }

    }

    protected void onChoiceStarted() {
        if(this.swipeListViewListener != null) {
            this.swipeListViewListener.onChoiceStarted();
        }

    }

    protected void onChoiceEnded() {
        if(this.swipeListViewListener != null) {
            this.swipeListViewListener.onChoiceEnded();
        }

    }

    protected void onFirstListItem() {
        if(this.swipeListViewListener != null) {
            this.swipeListViewListener.onFirstListItem();
        }

    }

    protected void onLastListItem() {
        if(this.swipeListViewListener != null) {
            this.swipeListViewListener.onLastListItem();
        }

    }

    protected void onListChanged() {
        if(this.swipeListViewListener != null) {
            this.swipeListViewListener.onListChanged();
        }

    }

    protected void onMove(int position, float x) {
        if(this.swipeListViewListener != null && position != -1) {
            this.swipeListViewListener.onMove(position, x);
        }

    }

    protected int changeSwipeMode(int position) {
        return this.swipeListViewListener != null && position != -1?this.swipeListViewListener.onChangeSwipeMode(position):-1;
    }

    public void setSwipeListViewListener(SwipeListViewListener swipeListViewListener) {
        this.swipeListViewListener = swipeListViewListener;
    }

    public void resetScrolling() {
        this.touchState = 0;
    }

    public void setOffsetRight(float offsetRight) {
        this.touchListener.setRightOffset(offsetRight);
    }

    public void setOffsetLeft(float offsetLeft) {
        this.touchListener.setLeftOffset(offsetLeft);
    }

    public void setSwipeCloseAllItemsWhenMoveList(boolean swipeCloseAllItemsWhenMoveList) {
        this.touchListener.setSwipeClosesAllItemsWhenListMoves(swipeCloseAllItemsWhenMoveList);
    }

    public void setSwipeOpenOnLongPress(boolean swipeOpenOnLongPress) {
        this.touchListener.setSwipeOpenOnLongPress(swipeOpenOnLongPress);
    }

    public void setSwipeMode(int swipeMode) {
        this.touchListener.setSwipeMode(swipeMode);
    }

    public int getSwipeActionLeft() {
        return this.touchListener.getSwipeActionLeft();
    }

    public void setSwipeActionLeft(int swipeActionLeft) {
        this.touchListener.setSwipeActionLeft(swipeActionLeft);
    }

    public int getSwipeActionRight() {
        return this.touchListener.getSwipeActionRight();
    }

    public void setSwipeActionRight(int swipeActionRight) {
        this.touchListener.setSwipeActionRight(swipeActionRight);
    }

    public void setAnimationTime(long animationTime) {
        this.touchListener.setAnimationTime(animationTime);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = MotionEventCompat.getActionMasked(ev);
        float x = ev.getX();
        float y = ev.getY();
        if(this.isEnabled() && this.touchListener.isSwipeEnabled()) {
            if(this.touchState == 1) {
                return this.touchListener.onTouch(this, ev);
            }

            switch(action) {
                case 0:
                    super.onInterceptTouchEvent(ev);
                    this.touchListener.onTouch(this, ev);
                    this.touchState = 0;
                    this.lastMotionX = x;
                    this.lastMotionY = y;
                    return false;
                case 1:
                    this.touchListener.onTouch(this, ev);
                    return this.touchState == 2;
                case 2:
                    this.checkInMoving(x, y);
                    return this.touchState == 2;
                case 3:
                    this.touchState = 0;
            }
        }

        return super.onInterceptTouchEvent(ev);
    }

    private void checkInMoving(float x, float y) {
        int xDiff = (int)Math.abs(x - this.lastMotionX);
        int yDiff = (int)Math.abs(y - this.lastMotionY);
        int touchSlop = this.touchSlop;
        boolean xMoved = xDiff > touchSlop;
        boolean yMoved = yDiff > touchSlop;
        if(xMoved) {
            this.touchState = 1;
            this.lastMotionX = x;
            this.lastMotionY = y;
        }

        if(yMoved) {
            this.touchState = 2;
            this.lastMotionX = x;
            this.lastMotionY = y;
        }

    }

    public void closeOpenedItems() {
        this.touchListener.closeOpenedItems();
    }
}
*/
