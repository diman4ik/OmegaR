package test.omegar.chernov.omegarrss;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.Glide;

import java.util.Date;

import test.omegar.chernov.omegarrss.data.Entry;
import test.omegar.chernov.omegarrss.provider.FeedData;
import test.omegar.chernov.omegarrss.utils.NetworkUtils;


public class EntriesRecyclerAdapter extends RecyclerView.Adapter<EntriesRecyclerAdapter.ViewHolder> {

    private Cursor mCursor;
    private Context mContext;

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTitleView;
        TextView mContentView;
        ImageView mImageView;

        ViewHolder(View view) {
            super(view);
            mTitleView = (TextView)view.findViewById(R.id.title_text);
            mContentView =  (TextView)view.findViewById(R.id.content_text);
            mImageView = (ImageView)view.findViewById(R.id.image);
        }
    }

    public EntriesRecyclerAdapter(Context context) {
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.entrylist_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        String title = getTitle();
        holder.mTitleView.setText(title);
        holder.mContentView.setText(getAbstract());

        // Get the cached image url
        String mainImgUrl = getImageUrl();
        mainImgUrl = TextUtils.isEmpty(mainImgUrl) ? null : NetworkUtils.getDownloadedOrDistantImageUrl(getId(), mainImgUrl);

        // If no image url for current entry than create and show letters drawable
        //
        String lettersForName = title != null ? (title.length() < 2 ? title.toUpperCase() : title.substring(0, 2).toUpperCase()) : "";
        TextDrawable letterDrawable = TextDrawable.builder().buildRect(lettersForName, Color.BLUE);
        if (mainImgUrl != null) {
            Glide.with(mContext).load(mainImgUrl).centerCrop().placeholder(letterDrawable).error(letterDrawable).into(holder.mImageView);
        } else {
            Glide.clear(holder.mImageView);
            holder.mImageView.setImageDrawable(letterDrawable);
        }

    }

    @Override
    public int getItemCount() {
        return (mCursor == null) ? 0 : mCursor.getCount();
    }

    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    public Cursor swapCursor(Cursor cursor) {
        if (mCursor == cursor) {
            return null;
        }
        Cursor oldCursor = mCursor;
        this.mCursor = cursor;
        if (cursor != null) {
            this.notifyDataSetChanged();
        }
        return oldCursor;
    }

    private long getId() {
        return mCursor.getLong(mCursor.getColumnIndex(FeedData.EntryColumns._ID));
    }

    private String getTitle() {
        return mCursor.getString(mCursor.getColumnIndex(FeedData.EntryColumns.TITLE));
    }

    private String getAbstract() {
        return mCursor.getString(mCursor.getColumnIndex(FeedData.EntryColumns.ABSTRACT));
    }

    private String getImageUrl() {
        return mCursor.getString(mCursor.getColumnIndex(FeedData.EntryColumns.IMAGE_URL));
    }

    private Date getDate() {
        return new Date(mCursor.getLong(mCursor.getColumnIndex(FeedData.EntryColumns.DATE)));
    }

    private Date getFetchDate() {
        return new Date(mCursor.getLong(mCursor.getColumnIndex(FeedData.EntryColumns.FETCH_DATE)));
    }

    private String getAuthor() {
        return mCursor.getString(mCursor.getColumnIndex(FeedData.EntryColumns.AUTHOR));
    }

    private String getLink() {
        return mCursor.getString(mCursor.getColumnIndex(FeedData.EntryColumns.LINK));
    }

    public Entry getItem(int position) {
        mCursor.moveToPosition(position);
        Entry ret = new Entry();
        ret.setTitle(getTitle());
        ret.setAbstractText(getAbstract());
        ret.setDate(getDate());
        ret.setFetchDate(getFetchDate());
        ret.setAuthor(getAuthor());
        ret.setLink(getLink());
        ret.setImageUrl(getImageUrl());
        return ret;
    }

    interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private GestureDetector gestureDetector;
        private ClickListener clickListener;

        public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final ClickListener clickListener) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {

            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }
    }
}

