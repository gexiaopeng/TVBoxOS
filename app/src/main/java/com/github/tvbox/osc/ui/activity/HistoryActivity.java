package com.github.tvbox.osc.ui.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.TextView;

import android.widget.Toast;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.CacheManager;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.adapter.HistoryAdapter;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.MD5;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_history;
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
                VodInfo vodInfo=null;
                while(historyAdapter.getItemCount()>0){
                    vodInfo = historyAdapter.getData().get(0);
                    historyAdapter.remove(0);
                    RoomDataManger.deleteVodRecord(vodInfo.sourceKey, vodInfo);
                }
            }
        });
        mGridView.setOnInBorderKeyEventListener(new TvRecyclerView.OnInBorderKeyEventListener() {
            @Override
            public boolean onInBorderKeyEvent(int direction, View focused) {
                if (direction == View.FOCUS_UP) {
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
                itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
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

//                HistoryDialog historyDialog = new HistoryDialog().build(mContext, vodInfo).setOnHistoryListener(new HistoryDialog.OnHistoryListener() {
//                    @Override
//                    public void onLook(VodInfo vodInfo) {
//                        if (vodInfo != null) {
//                            Bundle bundle = new Bundle();
//                            bundle.putInt("id", vodInfo.id);
//                            bundle.putString("sourceKey", vodInfo.sourceKey);
//                            jumpActivity(DetailActivity.class, bundle);
//                        }
//                    }
//
//                    @Override
//                    public void onDelete(VodInfo vodInfo) {
//                        if (vodInfo != null) {
//                               for (int i = 0; i < historyAdapter.getData().size(); i++) {
//                                    if (vodInfo.id == historyAdapter.getData().get(i).id) {
//                                        historyAdapter.remove(i);
//                                        break;
//                                    }
//                                }
//                                RoomDataManger.deleteVodRecord(vodInfo.sourceKey, vodInfo);
//                        }
//                    }
//                });
//                historyDialog.show();

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
        List<VodInfo> allVodRecord = RoomDataManger.getAllVodRecord(100);
        List<VodInfo> vodInfoList = new ArrayList<>();
        for (VodInfo vodInfo : allVodRecord) {
            if (vodInfo.playNote != null && !vodInfo.playNote.isEmpty()) {
                String info="";
                long rate=0;
                String progressKey=vodInfo.sourceKey + vodInfo.id + vodInfo.playIndex + vodInfo.name;
                try {
                    Object cTime= CacheManager.getCache(MD5.string2MD5(progressKey));
                    Object dTime= CacheManager.getCache(MD5.string2MD5(progressKey+"_Duration"));
                    long c=cTime==null? 0: (long) cTime;
                    long d=dTime==null? 0:(long) dTime;
                    if(d==0){
                        rate=0;
                    }else{
                        rate=Math.round((c*100.0f)/d);
                    }
                    if(rate<1){
                        info="观看不足1%";
                    }else if(rate<98){
                        info="观看至"+rate+"%";
                    }else{
                        info="观看完";
                    }
                } catch (Exception e) {
                    info=e.getMessage();
                }
                vodInfo.note = info;
            }
            vodInfoList.add(vodInfo);
        }
        historyAdapter.setNewData(vodInfoList);
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
}