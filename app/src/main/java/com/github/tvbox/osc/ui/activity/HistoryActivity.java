package com.github.tvbox.osc.ui.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.PopupMenu;
import android.widget.TextView;

import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.CacheManager;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.adapter.HistoryAdapter;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.util.*;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pj567
 * @date :2021/1/7
 * @description:
 */
public class HistoryActivity extends BaseActivity {
    private TextView tvDel;
    private TextView tvDelAll;
    private TextView tvDelTip;
    private TvRecyclerView mGridView;
    private HistoryAdapter historyAdapter;
    private boolean delMode = false;
    private int curPosition=-1;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_history;
    }

    @Override
    protected void init() {
        initView();
        //initData();
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
        historyAdapter = new HistoryAdapter();
        mGridView.setAdapter(historyAdapter);
        tvDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDelMode();
            }
        });
        tvDelAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delAll();
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
        historyAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                VodInfo vodInfo = historyAdapter.getData().get(position);
                if (vodInfo != null) {
                    if (delMode) {
                        historyAdapter.remove(position);
                        RoomDataManger.deleteVodRecord(vodInfo.sourceKey, vodInfo);
                    } else {
                        Bundle bundle = new Bundle();
                        bundle.putString("id", vodInfo.id);
                        bundle.putString("sourceKey", vodInfo.sourceKey);
                        jumpActivity(DetailActivity.class, bundle);
                    }
                }
            }
        });
        historyAdapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                VodInfo vodInfo = historyAdapter.getData().get(position);
                historyAdapter.remove(position);
                RoomDataManger.deleteVodRecord(vodInfo.sourceKey, vodInfo);
                return true;
            }
        });
    }

    private void initData() {
        List<VodInfo> allVodRecord =null;
        try {
            allVodRecord=RoomDataManger.getAllVodRecord(HistoryHelper.getHisNum(Hawk.get(HawkConfig.HISTORY_NUM, 0)));
            List<VodInfo> vodInfoList = new ArrayList<>();
            if(allVodRecord!=null) {
                for (VodInfo vodInfo : allVodRecord) {
                    if (vodInfo.playNote != null && !vodInfo.playNote.isEmpty()) {
                        String info = "";
                        long rate = 0;
                        String progressKey = vodInfo.sourceKey + vodInfo.id + "_" + vodInfo.getSeq() + vodInfo.name;
                        Object cTime = null;
                        Object dTime = null;
                        try {
                            cTime = CacheManager.getCache(MD5.string2MD5(progressKey));
                            dTime = CacheManager.getCache(MD5.string2MD5(progressKey + "_Duration"));
                            long c = cTime == null ? 0 : (long) cTime;
                            long d = dTime == null ? 0 : (long) dTime;
                            if (d < 1) {
                                rate = 0;
                            } else {
                                rate = Math.round((c * 100.0f) / d);
                            }
                            if (rate < 1) {
                                info = "观看不足1%";
                            } else if (rate < 98) {
                                info = "观看至" + rate + "%";
                            } else {
                                info = "观看完";
                            }
                        } catch (Exception e) {
                            info = e.getMessage();
                        }
                        vodInfo.note = info;
                    }
                    vodInfoList.add(vodInfo);
                }
                historyAdapter.setNewData(vodInfoList);
            }
        } catch (Throwable e) {
            Toast.makeText(this,e.getMessage()+"|"+allVodRecord,Toast.LENGTH_LONG).show();
        }

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_HISTORY_REFRESH) {
            //initData();
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
    protected void onResume(){
        super.onResume();
        initData();
        //Toast.makeText(this,"resum",Toast.LENGTH_LONG).show();
        //initData();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean ret=super.onKeyDown(keyCode,event);
        //showMsg("-keyCode:"+keyCode+",ret:"+ret+",p:"+curPosition);
        if(ret){
            return true;
        }
        if(keyCode==KeyEvent.KEYCODE_MENU && curPosition>=0){
            showDelDialog();
            return true;
        }
        return false;
    }
    private void showDelDialog(){
        SelectDialog<Integer> dialog = new SelectDialog<>(HistoryActivity.this);
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
                        VodInfo vodInfo = historyAdapter.getData().get(p);
                        historyAdapter.remove(p);
                        RoomDataManger.deleteVodRecord(vodInfo.sourceKey, vodInfo);
                    }else{
                        delAll();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public String getDisplay(Integer val) {
                return val==1?"删除选中记录":"清空所有记录";
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
        VodInfo vodInfo=null;
        while(historyAdapter.getItemCount()>0){
            vodInfo = historyAdapter.getData().get(0);
            historyAdapter.remove(0);
            RoomDataManger.deleteVodRecord(vodInfo.sourceKey, vodInfo);
        }
    }
    private void popupMenu(View view){
        PopupMenu popup = new PopupMenu(HistoryActivity.this,view);
        popup.getMenuInflater().inflate(R.menu.menu_pop, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String info = "";
                switch (item.getItemId()){
                    case R.id.saosao:
                        info = "你点了扫一扫";
                        break;
                    case R.id.add:
                        info = "你点了添加";
                        break;
                }
                showMsg(info);
                return true;
            }
        });
        popup.show();

    }
}