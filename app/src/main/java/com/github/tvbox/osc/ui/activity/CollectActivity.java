package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.cache.VodCollect;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.adapter.CollectAdapter;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CollectActivity extends BaseActivity {
    private TextView tvDel;
    private TextView tvDelAll;
    private TextView tvDelTip;
    private TvRecyclerView mGridView;
    private CollectAdapter collectAdapter;
    private boolean delMode = false;
    private int curPosition=-1;
    @Override
    protected int getLayoutResID() {
        return R.layout.activity_collect;
    }

    @Override
    protected void init() {
        initView();
        initData();
    }

    private void toggleDelMode() {
        delMode = !delMode;
        tvDelTip.setVisibility(delMode ? View.VISIBLE : View.GONE);
        tvDel.setTextColor(delMode ? getResources().getColor(R.color.color_FF0057) : Color.WHITE);
    }

    private void initView() {
        EventBus.getDefault().register(this);
        tvDel = findViewById(R.id.tvDel);
        tvDelAll = findViewById(R.id.tvDelAll);
        tvDelTip = findViewById(R.id.tvDelTip);
        mGridView = findViewById(R.id.mGridView);
        mGridView.setHasFixedSize(true);
        mGridView.setLayoutManager(new V7GridLayoutManager(this.mContext, isBaseOnWidth() ? 5 : 6));
        collectAdapter = new CollectAdapter();
        mGridView.setAdapter(collectAdapter);
        tvDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDelMode();
            }
        });
        tvDelAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VodCollect vodInfo=null;
                while(collectAdapter.getItemCount()>0){
                    vodInfo = collectAdapter.getData().get(0);
                    collectAdapter.remove(0);
                    RoomDataManger.deleteVodCollect(vodInfo.getId());
               }
            }
        });
        mGridView.setOnInBorderKeyEventListener(new TvRecyclerView.OnInBorderKeyEventListener() {
            @Override
            public boolean onInBorderKeyEvent(int direction, View focused) {
                if (direction == View.FOCUS_UP) {
                    curPosition=-1;
                    tvDel.setFocusable(true);
                    tvDelAll.setFocusable(true);
                    tvDel.requestFocus();
                }
                return false;
            }
        });
        mGridView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
                curPosition=position;
                itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                curPosition=position;
                itemView.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {

            }
        });
        collectAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                VodCollect vodInfo = collectAdapter.getData().get(position);
                if (vodInfo != null) {
                    if (delMode) {
                        collectAdapter.remove(position);
                        RoomDataManger.deleteVodCollect(vodInfo.getId());
                    } else {
                        if (ApiConfig.get().getSource(vodInfo.sourceKey) != null) {
                            Bundle bundle = new Bundle();
                            bundle.putString("id", vodInfo.vodId);
                            bundle.putString("sourceKey", vodInfo.sourceKey);
                            jumpActivity(DetailActivity.class, bundle);
                        } else {
                            Intent newIntent = new Intent(mContext, SearchActivity.class);
                            newIntent.putExtra("title", vodInfo.name);
                            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(newIntent);
                        }
                    }
                }
            }
        });
        collectAdapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                VodCollect vodInfo = collectAdapter.getData().get(position);
                collectAdapter.remove(position);
                RoomDataManger.deleteVodCollect(vodInfo.getId());
                return true;
            }
        });
    }

    private void initData() {
        List<VodCollect> allVodRecord = RoomDataManger.getAllVodCollect();
        List<VodCollect> vodInfoList = new ArrayList<>();
        for (VodCollect vodInfo : allVodRecord) {
            vodInfoList.add(vodInfo);
        }
        collectAdapter.setNewData(vodInfoList);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_HISTORY_REFRESH) {
            initData();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onBackPressed() {
        if (delMode) {
            toggleDelMode();
            return;
        }
        super.onBackPressed();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(super.onKeyDown(keyCode,event)){
            return true;
        }
        if(keyCode==KeyEvent.KEYCODE_MENU && curPosition>=0){
            showDelDialog();
            return true;
        }
        return false;
    }
    private void showDelDialog(){
        SelectDialog<Integer> dialog = new SelectDialog<>(CollectActivity.this);
        List<Integer> list=new ArrayList<>();
        list.add(1);
        list.add(2);
        dialog.setTip("");
        dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Integer>() {
            @Override
            public void click(Integer value, int pos) {
                try {
                    int p=curPosition;
                    curPosition=-1;
                    dialog.cancel();
                    if(value==1){
                        if(p>=0) {
                            VodCollect vodInfo = collectAdapter.getData().get(p);
                            if(vodInfo!=null) {
                                collectAdapter.remove(p);
                                RoomDataManger.deleteVodCollect(vodInfo.getId());
                            }
                        }
                    }else{
                        delAll();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public String getDisplay(Integer val) {
                return val==1?"删除选中收藏":"清空所有收藏";
            }
        }, new DiffUtil.ItemCallback<Integer>() {
            @Override
            public boolean areItemsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                return oldItem.intValue() == newItem.intValue();
            }
            @Override
            public boolean areContentsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                return oldItem.intValue() == newItem.intValue();
            }
        }, list, -1);
        dialog.show();
    }
    private void delAll(){
        VodCollect vodInfo=null;
        while(collectAdapter.getItemCount()>0){
            vodInfo = collectAdapter.getData().get(0);
            if(vodInfo!=null) {
                collectAdapter.remove(0);
                RoomDataManger.deleteVodCollect(vodInfo.getId());
            }
        }
    }
}