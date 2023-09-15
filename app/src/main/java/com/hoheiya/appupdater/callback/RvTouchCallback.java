package com.hoheiya.appupdater.callback;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.hoheiya.appupdater.R;
import com.hoheiya.appupdater.adapter.AppsMoreAdapter;
import com.hoheiya.appupdater.log.MLog;

import java.util.Objects;

public class RvTouchCallback extends ItemTouchHelper.Callback {

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager || recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                    ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            final int swipeFlags = 0;
            return makeMovementFlags(dragFlags, swipeFlags);
        } else {
            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            final int swipeFlags = 0;
            return makeMovementFlags(dragFlags, swipeFlags);
        }
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
//        拖拽中的viewHolder的Position
        int fromPosition = viewHolder.getAdapterPosition();
        //当前拖拽到的item的viewHolder
        int toPosition = target.getAdapterPosition();
        ((AppsMoreAdapter) Objects.requireNonNull(recyclerView.getAdapter())).onItemMove(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

    }

    @Override
    public boolean isLongPressDragEnabled() {
//        return super.isLongPressDragEnabled();//
        return  false;
    }

    @Override
    public void onMoved(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, int fromPos, @NonNull RecyclerView.ViewHolder target, int toPos, int x, int y) {
        super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
        //
        MLog.d("========onMoved=======fromPos:" + fromPos + ",toPos:" + toPos);
        target.itemView.findViewById(R.id.tv_item_name).setVisibility(View.VISIBLE);
        viewHolder.itemView.findViewById(R.id.tv_item_name).setVisibility(toPos == 0 ? View.GONE : View.VISIBLE);
    }
}
