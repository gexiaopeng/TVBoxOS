package com.github.tvbox.osc.player.controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.DiffUtil;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.IJKCode;
import com.github.tvbox.osc.bean.ParseBean;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.subtitle.widget.SimpleSubtitleView;
import com.github.tvbox.osc.ui.activity.PlayActivity;
import com.github.tvbox.osc.ui.adapter.ParseAdapter;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.PlayerHelper;
import com.github.tvbox.osc.util.ScreenUtils;
import com.github.tvbox.osc.util.SubtitleHelper;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.util.Date;

import xyz.doikki.videoplayer.player.VideoView;
import xyz.doikki.videoplayer.util.PlayerUtils;

import static xyz.doikki.videoplayer.util.PlayerUtils.stringForTime;

public class VodController extends BaseController {
    @SuppressLint("NewApi")
    public VodController(@NonNull @NotNull Context context) {
        super(context);
        this.context=context;
        setTopContainerWidth();
        thumbView = LayoutInflater.from(this.context).inflate(R.layout.item_seekbar_time, null, false);
        mTimeBar.setThumb(getThumb(0));
        list.add(2);
        list.add(0);
        list.add(1);
        mHandlerCallback = new HandlerCallback() {
            @Override
            public void callback(Message msg) {
                switch (msg.what) {
                    case 1000: { // seek 刷新
                        isUpdateSeekUI=true;
                        //mProgressRoot.setVisibility(VISIBLE);
                        hidePause();
                        showSeekBar(true);
                        break;
                    }
                    case 1001: { // seek 关闭
                        count=0;
                        isUpdateSeekUI=false;
                        mProgressRoot.setVisibility(GONE);
                        hideSeekBar();
                        break;
                    }
                    case 1002: { // 显示底部菜单
                        mBottomRoot.setVisibility(VISIBLE);
                        showTooBar();
                        setTopContainerWidth();
                        mTopRoot1.setVisibility(VISIBLE);
                        mTopRoot2.setVisibility(VISIBLE);
                        mPlayTitle.setVisibility(GONE);
                       // mBottomRoot.requestFocus();
                         //Toast.makeText(context,"1002:"+(sToolBar.getVisibility()==VISIBLE),Toast.LENGTH_LONG).show();
                        break;
                    }
                    case 1003: { // 隐藏底部菜单
                        if(!fromLongPress) {
                            mBottomRoot.setVisibility(GONE);
                        }
                        hideToolBar();
                        mTopRoot1.setVisibility(GONE);
                        mTopRoot2.setVisibility(GONE);
                        break;
                    }
                    case 1004: { // 设置速度
                        if (isInPlaybackState()) {
                            try {
                                float speed = (float) mPlayerConfig.getDouble("sp");
                                mControlWrapper.setSpeed(speed);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else
                            mHandler.sendEmptyMessageDelayed(1004, 100);
                        break;
                    }
                }
            }
        };
    }
    @RequiresApi(api = Build.VERSION_CODES.R)
    private  void  setTopContainerWidth(){
               Point point=new Point();
               try {
                   context.getDisplay().getRealSize(point);
                   width=point.x;
                   int rate=(int)(width*0.85);
                   if(width>3800){
                       rate=(int)(width*0.95);
                   }else if(width>2300){
                       rate=(int)(width*0.89);
                   }else if(width>1800){
                       rate=(int)(width*0.90);
                   }
                   if(isPortrait){
                       rate=(int)(rate*0.95);
                   }
                   LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(rate, LinearLayout.LayoutParams.MATCH_PARENT);
                   mTopRoot1.setLayoutParams(lp);
              } catch (Throwable e) {
                   width=-1;
              }
    }
   public Drawable getThumb(int position) {
        ((TextView) thumbView.findViewById(R.id.tvProgress)).setText(stringForTime(position));
        thumbView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        Bitmap bitmap = Bitmap.createBitmap(thumbView.getMeasuredWidth(), thumbView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        thumbView.layout(0, 0, thumbView.getMeasuredWidth(), thumbView.getMeasuredHeight());
        thumbView.draw(canvas);
       return new BitmapDrawable(getResources(), bitmap);
    }
    SeekBar mSeekBar;
    SeekBar mTimeBar;
    View thumbView;
    TextView mCurrentTime;
    TextView mTotalTime;
    boolean mIsDragging;
    LinearLayout mProgressRoot;
    TextView mProgressText;
    TextView seekTime;
    ImageView mProgressIcon;
    LinearLayout mBottomRoot;
    LinearLayout bottomCenterContainer;
    LinearLayout mTopRoot1;
    LinearLayout mTopRoot2;
    LinearLayout mParseRoot;
    LinearLayout sToolBar;
    LinearLayout mMyseekBar;
    TvRecyclerView mGridView;
    TextView mPlayTitle;
    TextView mPlayTitle1;
    TextView mPlayLoadNetSpeedRightTop;
    TextView mNextBtn;
    TextView mPreBtn;
    TextView mPlayerScaleBtn;
    public TextView mPlayerSpeedBtn;
    TextView mPlayerBtn;
    TextView mPlayerIJKBtn;
    TextView mPlayerRetry;
    TextView mPlayrefresh;
    public TextView mPlayerTimeStartEndText;
    public TextView mPlayerTimeStartBtn;
    public TextView mPlayerTimeSkipBtn;
    public TextView mPlayerTimeResetBtn;
    TextView mPlayPauseTime;
    TextView mPlayLoadNetSpeed;
    TextView mVideoSize;
    public SimpleSubtitleView mSubtitleView;
    TextView mZimuBtn;
    TextView mAudioTrackBtn;
    private ViewGroup mPauseRoot;
    public TextView mLandscapePortraitBtn;

    Handler myHandle;
    Runnable myRunnable;
    Runnable myNewPlayRunnable;
    int myHandleSeconds = 4000;//闲置多少毫秒秒关闭底栏  默认4秒
    int videoPlayState = 0;
    int waitTime=1000;//快进等待确认时间
    boolean isPaused=false;
    private Context context=null;
    boolean isUpdateSeekUI=false;
    int count=0;
    boolean isKeyOn=false;
    List<Integer> list=new ArrayList();
    boolean isPreviewBack=false;
    private int width;
    private int currentTime;
    private boolean isPortrait=false;//是否竖屏
    private Runnable myRunnable2 = new Runnable() {
        @Override
        public void run() {
            Date date = new Date();
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            mPlayPauseTime.setText(timeFormat.format(date));
            String speed = PlayerHelper.getDisplaySpeed(mControlWrapper.getTcpSpeed());
            mPlayLoadNetSpeedRightTop.setText(speed);
            mPlayLoadNetSpeed.setText(speed);
            String width = Integer.toString(mControlWrapper.getVideoSize()[0]);
            String height = Integer.toString(mControlWrapper.getVideoSize()[1]);
            mVideoSize.setText("[ " + width + " X " + height +" ]");
            int getCurrentPosition = (int) (mControlWrapper.getCurrentPosition() / 1000.0);
            int getDuration = (int) (mControlWrapper.getDuration() / 1000.0);
            seekTime.setText(String.format("%02d", getCurrentPosition / 60) + ":" + String.format("%02d", getCurrentPosition % 60) + " | " + String.format("%02d", getDuration / 60) + ":" + String.format("%02d", getDuration % 60));

            mHandler.postDelayed(this, 1000);
        }
    };





    @Override
    protected void initView() {
        super.initView();
        mCurrentTime = findViewById(R.id.curr_time);
        mTotalTime = findViewById(R.id.total_time);
        mPlayTitle = findViewById(R.id.tv_info_name);
        mPlayTitle1 = findViewById(R.id.tv_info_name1);
        mPlayLoadNetSpeedRightTop = findViewById(R.id.tv_play_load_net_speed_right_top);
        mSeekBar = findViewById(R.id.seekBar);
        mTimeBar = findViewById(R.id.timeBar);
        mTimeBar.setEnabled(false);
        mProgressRoot = findViewById(R.id.tv_progress_container);
        mProgressIcon = findViewById(R.id.tv_progress_icon);
        mProgressText = findViewById(R.id.tv_progress_text);
        mBottomRoot = findViewById(R.id.bottom_container);
        mTopRoot1 = findViewById(R.id.tv_top_l_container);
        mTopRoot2 = findViewById(R.id.tv_top_r_container);
        mParseRoot = findViewById(R.id.parse_root);
        mGridView = findViewById(R.id.mGridView);
        mPlayerRetry = findViewById(R.id.play_retry);
        mPlayrefresh = findViewById(R.id.play_refresh);
        mNextBtn = findViewById(R.id.play_next);
        mPreBtn = findViewById(R.id.play_pre);
        mPlayerScaleBtn = findViewById(R.id.play_scale);
        mPlayerSpeedBtn = findViewById(R.id.play_speed);
        mPlayerBtn = findViewById(R.id.play_player);
        mPlayerIJKBtn = findViewById(R.id.play_ijk);
        mPlayerTimeStartEndText = findViewById(R.id.play_time_start_end_text);
        mPlayerTimeStartBtn = findViewById(R.id.play_time_start);
        mPlayerTimeSkipBtn = findViewById(R.id.play_time_end);
        mPlayerTimeResetBtn = findViewById(R.id.play_time_reset);
        mPlayPauseTime = findViewById(R.id.tv_sys_time);
        mPlayLoadNetSpeed = findViewById(R.id.tv_play_load_net_speed);
        mVideoSize = findViewById(R.id.tv_videosize);
        mSubtitleView = findViewById(R.id.subtitle_view);
        mZimuBtn = findViewById(R.id.zimu_select);
        mAudioTrackBtn = findViewById(R.id.audio_track_select);
        sToolBar=findViewById(R.id.tool_bar);
        mMyseekBar=findViewById(R.id.myseekBar);
        mPauseRoot = findViewWithTag("vod_control_pause");
        int subtitleTextSize = SubtitleHelper.getTextSize(mActivity);
        mSubtitleView.setTextSize(subtitleTextSize);
        mLandscapePortraitBtn = findViewById(R.id.landscape_portrait);
        bottomCenterContainer = findViewById(R.id.tv_bottom_center_container);
        seekTime = findViewById(R.id.tv_seek_time);

        initSubtitleInfo();
        myHandle = new Handler();
        myRunnable = new Runnable() {
            @Override
            public void run() {
                if(!isPaused){
                    hideBottom();
                }else if(isToolBarVisible()){
                    hideToolBar();
                }
            }
        };
       myNewPlayRunnable= new Runnable() {
            @Override
            public void run() {
                if (isInPlaybackState()) {
                    tvSlideStop();
                }
           }
        };

        mPlayPauseTime.post(new Runnable() {
            @Override
            public void run() {
                mHandler.post(myRunnable2);
            }
        });

        mGridView.setLayoutManager(new V7LinearLayoutManager(getContext(), 0, false));
        ParseAdapter parseAdapter = new ParseAdapter();
        parseAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                ParseBean parseBean = parseAdapter.getItem(position);
                // 当前默认解析需要刷新
                int currentDefault = parseAdapter.getData().indexOf(ApiConfig.get().getDefaultParse());
                parseAdapter.notifyItemChanged(currentDefault);
                ApiConfig.get().setDefaultParse(parseBean);
                parseAdapter.notifyItemChanged(position);
                listener.changeParse(parseBean);
                hideBottom();
            }
        });
        mGridView.setAdapter(parseAdapter);
        parseAdapter.setNewData(ApiConfig.get().getParseBeanList());

        //mParseRoot.setVisibility(VISIBLE);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser && !isUpdateSeekUI) {
                    return;
                }
                long duration = mControlWrapper.getDuration();
                long newPosition = (duration * progress) / seekBar.getMax();
                if (mCurrentTime != null) {
                    mCurrentTime.setText(stringForTime((int) newPosition));
                }
                if (mTotalTime!= null) {
                    mTotalTime.setText("-"+stringForTime((int)(duration-newPosition))+"/"+stringForTime((int) duration));
                }
                mSeekBar.setProgress(progress);
                mTimeBar.setProgress(progress);
                mTimeBar.setThumb(getThumb((int)(newPosition)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsDragging = true;
                mControlWrapper.stopProgress();
                mControlWrapper.stopFadeOut();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                myHandle.removeCallbacks(myRunnable);
                myHandle.postDelayed(myRunnable, myHandleSeconds);
                long duration = mControlWrapper.getDuration();
                long newPosition = (duration * seekBar.getProgress()) / seekBar.getMax();
                mControlWrapper.seekTo((int) newPosition);
                mIsDragging = false;
                mControlWrapper.startProgress();
                mControlWrapper.startFadeOut();
            }
        });
        mPlayerRetry.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hidePause();
                hideBottom();
                listener.replay(true);

            }
        });
        mPlayrefresh.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hidePause();
                hideBottom();
                listener.replay(false);

            }
        });
        mNextBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (isPaused) {
//                    togglePlay();
//                } else {
                hideBottom();
                //listener.playNext(false);
                listener.playNext(new VodPauseManager() {
                    @Override
                    public void hidePauseShow() {
                        hidePause();
                    }
                });
                //}

            }
        });
        mPreBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                hideBottom();
                //listener.playPre();
                listener.playPre(new VodPauseManager() {
                    @Override
                    public void hidePauseShow() {
                        hidePause();
                    }
                });

            }
        });
        mPlayerScaleBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                myHandle.removeCallbacks(myRunnable);
                myHandle.postDelayed(myRunnable, myHandleSeconds);
                try {
                    int scaleType = mPlayerConfig.getInt("sc");
                    scaleType++;
                    if (scaleType > 5)
                        scaleType = 0;
                    mPlayerConfig.put("sc", scaleType);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                    mControlWrapper.setScreenScaleType(scaleType);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mPlayerSpeedBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                myHandle.removeCallbacks(myRunnable);
                myHandle.postDelayed(myRunnable, myHandleSeconds);
                try {
                    float speed = (float) mPlayerConfig.getDouble("sp");
                    if(speed==0.5f){
                        speed=1.0f;
                    }else {
                        speed += 1.0f;
                    }
                    if (speed > 4)
                        speed = 1.0f;
                    mPlayerConfig.put("sp", speed);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                    speed_old = speed;
                    mControlWrapper.setSpeed(speed);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        mPlayerSpeedBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                try {
                    float speed = (float) mPlayerConfig.getDouble("sp");
                    if(speed==1.0f){
                        speed=0.5f;
                    }else{
                        speed=1.0f;
                    }
                    mPlayerConfig.put("sp", speed);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                    speed_old=speed;
                    mControlWrapper.setSpeed(speed);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        mPlayerBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
//                myHandle.removeCallbacks(myRunnable);
//                myHandle.postDelayed(myRunnable, myHandleSeconds);
                try {
                    int playerType = mPlayerConfig.getInt("pl");
                    ArrayList<Integer> exsitPlayerTypes = PlayerHelper.getExistPlayerTypes();
                    int playerTypeIdx = 0;
                    int playerTypeSize = exsitPlayerTypes.size();
                    for(int i = 0; i<playerTypeSize; i++) {
                        if (playerType == exsitPlayerTypes.get(i)) {
                            if (i == playerTypeSize - 1) {
                                playerTypeIdx = 0;
                            } else {
                                playerTypeIdx = i + 1;
                            }
                        }
                    }
                    playerType = exsitPlayerTypes.get(playerTypeIdx);
                    selectPlayType(playerType);
//                    hideBottom();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //mPlayerBtn.requestFocus();
            }
        });

        mPlayerBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
//                myHandle.removeCallbacks(myRunnable);
//                myHandle.postDelayed(myRunnable, myHandleSeconds);
                FastClickCheckUtil.check(view);
                try {
                    int playerType = mPlayerConfig.getInt("pl");
                    int defaultPos = 0;
                    ArrayList<Integer> players = PlayerHelper.getExistPlayerTypes();
                    ArrayList<Integer> renders = new ArrayList<>();
                    for(int p = 0; p<players.size(); p++) {
                        renders.add(p);
                        if (players.get(p) == playerType) {
                            defaultPos = p;
                        }
                    }
                    SelectDialog<Integer> dialog = new SelectDialog<>(mActivity);
                    dialog.setTip("请选择播放器");
                    dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Integer>() {
                        @Override
                        public void click(Integer value, int pos) {
                            try {
                                dialog.cancel();
                                int thisPlayType = players.get(pos);
                                if (thisPlayType != playerType) {
                                    selectPlayType(thisPlayType);
                                    //mPlayerBtn.requestFocus();
                              }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public String getDisplay(Integer val) {
                            Integer playerType = players.get(val);
                            return PlayerHelper.getPlayerName(playerType);
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
                    }, renders, defaultPos);
                    dialog.show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        mPlayerIJKBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
//                myHandle.removeCallbacks(myRunnable);
//                myHandle.postDelayed(myRunnable, myHandleSeconds);
                try {
                    String ijk = mPlayerConfig.getString("ijk");
                    List<IJKCode> codecs = ApiConfig.get().getIjkCodes();
                    for (int i = 0; i < codecs.size(); i++) {
                        if (ijk.equals(codecs.get(i).getName())) {
                            if (i >= codecs.size() - 1)
                                ijk = codecs.get(0).getName();
                            else {
                                ijk = codecs.get(i + 1).getName();
                            }
                            break;
                        }
                    }
                    mPlayerConfig.put("ijk", ijk);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                    listener.replay(false);
//                    hideBottom();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mPlayerIJKBtn.requestFocus();
            }
        });
//        增加播放页面片头片尾时间重置
        mPlayerTimeResetBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                myHandle.removeCallbacks(myRunnable);
                myHandle.postDelayed(myRunnable, myHandleSeconds);
                try {
                    mPlayerConfig.put("et", 0);
                    mPlayerConfig.put("st", 0);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mPlayerTimeStartBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                myHandle.removeCallbacks(myRunnable);
                myHandle.postDelayed(myRunnable, myHandleSeconds);
                try {
//                    int step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5);
//                    int st = mPlayerConfig.getInt("st");
//                    st += step;
//                    //片头最大跳过时间10分钟
//                    if (st > 60 * 10)
//                        st = 0;
//                    mPlayerConfig.put("st", st);
                    int current = (int) mControlWrapper.getCurrentPosition();
                    int duration = (int) mControlWrapper.getDuration();
                    if (current > duration / 2) return;
                    mPlayerConfig.put("st",current/1000);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        // takagen99: Add long press to reset counter
        mPlayerTimeStartBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                try {
                    mPlayerConfig.put("st", 0);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        mPlayerTimeSkipBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                myHandle.removeCallbacks(myRunnable);
                myHandle.postDelayed(myRunnable, myHandleSeconds);
                try {
//                    int step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5);
//                    int et = mPlayerConfig.getInt("et");
//                    et += step;
//                    //片尾最大跳过时间10分钟
//                    if (et > 60 * 10)
//                        et = 0;
//                    mPlayerConfig.put("et", et);
                    int current = (int) mControlWrapper.getCurrentPosition();
                    int duration = (int) mControlWrapper.getDuration();
                    if (current < duration / 2) return;
                    mPlayerConfig.put("et", (duration - current)/1000);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mPlayerTimeSkipBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                try {
                    mPlayerConfig.put("et", 0);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
//        mPlayerTimeStepBtn.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                myHandle.removeCallbacks(myRunnable);
//                myHandle.postDelayed(myRunnable, myHandleSeconds);
//                int step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5);
//                step += 5;
//                if (step > 30) {
//                    step = 5;
//                }
//                Hawk.put(HawkConfig.PLAY_TIME_STEP, step);
//                updatePlayerCfgView();
//            }
//        });
        mZimuBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FastClickCheckUtil.check(view);
                listener.selectSubtitle();
                hideBottom();
            }
        });
        mZimuBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mSubtitleView.setVisibility(View.GONE);
                mSubtitleView.destroy();
                mSubtitleView.clearSubtitleCache();
                mSubtitleView.isInternal = false;
                hideBottom();
                Toast.makeText(getContext(), "字幕已关闭", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        mAudioTrackBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FastClickCheckUtil.check(view);
                listener.selectAudioTrack();
                hideBottom();
            }
        });
        mLandscapePortraitBtn.setOnClickListener(new OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onClick(View view) {
                FastClickCheckUtil.check(view);
                setLandscapePortrait();
                hideBottom();
            }
        });
        initLandscapePortraitBtnInfo();
    }

    public void initLandscapePortraitBtnInfo() {
        double screenSqrt = ScreenUtils.getSqrt(mActivity);
        if (screenSqrt < 20.0) {
            mLandscapePortraitBtn.setVisibility(View.VISIBLE);
            mLandscapePortraitBtn.setText("竖屏");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    void setLandscapePortrait() {
        int requestedOrientation = mActivity.getRequestedOrientation();
        if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            isPortrait=true;
            mLandscapePortraitBtn.setText("横屏");
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        } else if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
            isPortrait=false;
            mLandscapePortraitBtn.setText("竖屏");
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    void initSubtitleInfo() {
        int subtitleTextSize = SubtitleHelper.getTextSize(mActivity);
        mSubtitleView.setTextSize(subtitleTextSize);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.player_vod_control_view;
    }

    public void showParse(boolean userJxList) {
        //mParseRoot.setVisibility(userJxList ? VISIBLE : GONE);
    }

    private JSONObject mPlayerConfig = null;

    public void setPlayerConfig(JSONObject playerCfg) {
        this.mPlayerConfig = playerCfg;
        updatePlayerCfgView();
    }

    void updatePlayerCfgView() {
        try {
            int playerType = mPlayerConfig.getInt("pl");
            mPlayerBtn.setText(PlayerHelper.getPlayerName(playerType));
            mPlayerScaleBtn.setText(PlayerHelper.getScaleName(mPlayerConfig.getInt("sc")));
            mPlayerIJKBtn.setText(mPlayerConfig.getString("ijk"));
            mPlayerIJKBtn.setVisibility(playerType == 1 ? VISIBLE : GONE);
            mPlayerScaleBtn.setText(PlayerHelper.getScaleName(mPlayerConfig.getInt("sc")));
            mPlayerSpeedBtn.setText("x" + mPlayerConfig.getDouble("sp"));
            mPlayerTimeStartBtn.setText(PlayerUtils.stringForTime(mPlayerConfig.getInt("st") * 1000));
            mPlayerTimeSkipBtn.setText(PlayerUtils.stringForTime(mPlayerConfig.getInt("et") * 1000));
            //mAudioTrackBtn.setVisibility((playerType == 1) ? VISIBLE : GONE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setTitle(String playTitleInfo) {
        mPlayTitle.setText(playTitleInfo);
        mPlayTitle1.setText(playTitleInfo);
    }

    public void setUrlTitle(String playTitleInfo) {
        mPlayTitle.setText(playTitleInfo);
    }

    public void resetSpeed() {
        skipEnd = true;
        mHandler.removeMessages(1004);
        mHandler.sendEmptyMessageDelayed(1004, 100);
    }
    public interface VodPauseManager{
        void hidePauseShow();
    }
    public interface VodControlListener {
        void playNext(VodPauseManager  vPause);
        void playPre(VodPauseManager  vPause);
        void playNext(boolean rmProgress);

        void playPre();

        void prepared();

        void changeParse(ParseBean pb);

        void updatePlayerCfg();

        void replay(boolean replay);

        void errReplay();

        void selectSubtitle();

        void selectAudioTrack();
    }

    public void setListener(VodControlListener listener) {
        this.listener = listener;
    }

    private VodControlListener listener;

    private boolean skipEnd = true;

    @Override
    protected void setProgress(int duration, int position) {

        if (mIsDragging || isUpdateSeekUI) {
            return;
        }
        super.setProgress(duration, position);
        if (skipEnd && position != 0 && duration != 0) {
            int et = 0;
            try {
                et = mPlayerConfig.getInt("et");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (et > 0 && position + (et * 1000) >= duration) {
                skipEnd = false;
                listener.playNext(true);
            }
        }
        mCurrentTime.setText(PlayerUtils.stringForTime(position));
        int totalTime=duration>0 ? duration:currentTime;
        mTotalTime.setText("-"+PlayerUtils.stringForTime(duration-position)+"/"+PlayerUtils.stringForTime(totalTime));
        if (duration > 0) {
            mSeekBar.setEnabled(true);
            int pos = (int) (position * 1.0 / duration * mSeekBar.getMax());
            mSeekBar.setProgress(pos);
            mTimeBar.setProgress(pos);
            currentTime=position;
            mTimeBar.setThumb(getThumb(position));
         } else {
            mSeekBar.setEnabled(false);
        }
        int percent = mControlWrapper.getBufferedPercentage();
        if (percent >= 95) {
            mSeekBar.setSecondaryProgress(mSeekBar.getMax());
        } else {
            mSeekBar.setSecondaryProgress(percent * 10);
        }
    }

    private boolean simSlideStart = false;
    private int simSeekPosition = 0;
    private long simSlideOffset = 0;

    public void tvSlideStop() {
        if (!simSlideStart)
            return;
        mControlWrapper.seekTo(simSeekPosition);
        if (!mControlWrapper.isPlaying())
            mControlWrapper.start();
        simSlideStart = false;
        simSeekPosition = 0;
        simSlideOffset = 0;
    }

    public void tvSlideStart(int dir) {
        int duration = (int) mControlWrapper.getDuration();
        if (duration <= 0)
            return;
        if (!simSlideStart) {
            simSlideStart = true;
        }
        // 每次10秒
        simSlideOffset += (10000.0f * dir);
        int currentPosition = (int) mControlWrapper.getCurrentPosition();
        int position = (int) (simSlideOffset + currentPosition);
        if (position > duration) position = duration;
        if (position < 0) position = 0;
        updateSeekUI(currentPosition, position, duration);
        simSeekPosition = position;
    }

    @Override
    protected void updateSeekUI(int curr, int seekTo, int duration) {
        super.updateSeekUI(curr, seekTo, duration);
        int max=mSeekBar.getMax();
        int progress=(int)((seekTo * 1.0 *max)/duration);
        String ps="";
        int steep=0;
        if (seekTo > curr) {
            mProgressIcon.setImageResource(R.drawable.icon_pre);
            ps="+";
            steep=seekTo-curr;
        } else {
            ps="-";
            steep=curr-seekTo;
            mProgressIcon.setImageResource(R.drawable.icon_back);
        }
        mProgressText.setText(PlayerUtils.stringForTime(seekTo) + " / " + PlayerUtils.stringForTime(duration)+"["+ps+PlayerUtils.stringForTime(steep)+"]");
        mHandler.sendEmptyMessage(1000);
        mHandler.removeMessages(1001);
        mHandler.sendEmptyMessageDelayed(1001, 1000);
        mSeekBar.setProgress(progress);
        mTimeBar.setProgress(progress);
        mTimeBar.setThumb(getThumb(seekTo));

    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onPlayStateChanged(int playState) {
       // Toast.makeText(getContext(), "playState:"+playState+",isPreviewBack:"+isPreviewBack+",isKeyOn:"+isKeyOn+",isPaused:"+isPaused, Toast.LENGTH_LONG).show();
        if((playState==VideoView.STATE_ERROR && isPaused && !isKeyOn) ||  (playState==VideoView.STATE_PAUSED && isPreviewBack)){
             //Toast.makeText(getContext(), "isPlaying:"+mControlWrapper.isPlaying()+",pause:"+isPaused+",isKeyOn:"+isKeyOn, Toast.LENGTH_SHORT).show();
            isPreviewBack=false;
            return;
        }
        super.onPlayStateChanged(playState);
        videoPlayState = playState;
        switch (playState) {
            case VideoView.STATE_IDLE:
                break;
            case VideoView.STATE_PLAYING:
                isPaused=false;
                startProgress();
                hideSeekBar();//09-26
               // hideBottom() ;
                break;
            case VideoView.STATE_PAUSED:
                isPaused=true;
                mPauseRoot.setVisibility(VISIBLE);
                mTopRoot1.setVisibility(GONE);
                mTopRoot2.setVisibility(GONE);
                mPlayTitle.setVisibility(VISIBLE);
                hideToolBar();
                showSeekBar(true);//09-26
                break;
            case VideoView.STATE_ERROR:
                int playerType =0;
                try {
                    playerType=mPlayerConfig.getInt("pl");
                } catch (JSONException e) {
                }
                int rePlayTypeType=getRePlayType(playerType);
                if(rePlayTypeType!=-1) {
                    selectPlayType(rePlayTypeType);
                }else {
                    listener.errReplay();
                }
                break;
            case VideoView.STATE_PREPARED:
                mPlayLoadNetSpeed.setVisibility(GONE);
                listener.prepared();
                break;
            case VideoView.STATE_BUFFERED:
                mPlayLoadNetSpeed.setVisibility(GONE);
                break;
            case VideoView.STATE_PREPARING:
            case VideoView.STATE_BUFFERING:
                //if(mProgressRoot.getVisibility()==GONE)mPlayLoadNetSpeed.setVisibility(VISIBLE);
                break;
            case VideoView.STATE_PLAYBACK_COMPLETED:
                listener.playNext(true);
                break;
        }
        isPreviewBack=false;
        isKeyOn=false;
    }
    boolean isToolBarVisible() {
       return sToolBar.getVisibility()==VISIBLE;
    }
    boolean isBottomVisible() {
        return mBottomRoot.getVisibility() == VISIBLE;
    }
    @RequiresApi(api = Build.VERSION_CODES.R)
    void showSeekBar(boolean isShowTop){
         mMyseekBar.setVisibility(VISIBLE);
         mBottomRoot.setVisibility(VISIBLE);
         hideToolBar();
         setTopContainerWidth();
         if(isShowTop) {
             mTopRoot1.setVisibility(VISIBLE);
             mTopRoot2.setVisibility(VISIBLE);
         }else{
             mTopRoot1.setVisibility(GONE);
             mTopRoot2.setVisibility(GONE);
         }
         mPlayTitle.setVisibility(GONE);
         mBottomRoot.requestFocus();
    }
    void hideSeekBar(){
        count=0;
        mBottomRoot.setVisibility(GONE);
        mTopRoot1.setVisibility(GONE);
        mTopRoot2.setVisibility(GONE);
    }
    void showBottom() {
        mHandler.removeMessages(1003);
        mHandler.sendEmptyMessage(1002);
    }

    void hideBottom() {
        count=0;
        mHandler.removeMessages(1002);
        mHandler.sendEmptyMessage(1003);
    }
    void hidePause() {
      mPauseRoot.setVisibility(GONE);
    }
    void showTooBar(){
        VodInfo mVodInfo = App.getInstance().getVodInfo();
        int size=mVodInfo.seriesMap.get(mVodInfo.playFlag).size();
        sToolBar.setVisibility(VISIBLE);
        if(size>1) {
            if(mVodInfo.playIndex+1<size){
                mNextBtn.setVisibility(VISIBLE);
                mNextBtn.requestFocus();
            }else{
                mNextBtn.setVisibility(GONE);
            }
            if(mVodInfo.playIndex!=0){
                mPreBtn.setVisibility(VISIBLE);
                if(mVodInfo.playIndex+1==size){
                    mPreBtn.requestFocus();
                }
            }else{
                mPreBtn.setVisibility(GONE);
            }


        }else {
            mNextBtn.setVisibility(GONE);
            mPreBtn.setVisibility(GONE);
            mPlayerBtn.requestFocus();
            //mPlayerRetry.requestFocus();
        }
    }
    void hideToolBar(){
        sToolBar.setVisibility(GONE);
    }
    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public boolean onKeyEvent(KeyEvent event) {
        isKeyOn=true;
        count++;
        myHandle.removeCallbacks(myRunnable);
        if (super.onKeyEvent(event)) {
            return true;
        }
        int keyCode = event.getKeyCode();
        isPreviewBack=(keyCode==KeyEvent.KEYCODE_BACK);
        int action = event.getAction();
        if (!isPaused && keyCode != KeyEvent.KEYCODE_DPAD_RIGHT && keyCode != KeyEvent.KEYCODE_DPAD_LEFT && keyCode != KeyEvent.KEYCODE_DPAD_UP) {
            count=0;
        }
        if (isToolBarVisible() && isBottomVisible() && keyCode != KeyEvent.KEYCODE_DPAD_DOWN && keyCode != KeyEvent.KEYCODE_DPAD_UP && keyCode!= KeyEvent.KEYCODE_MENU) {
            if(!isPaused) {
                count = 0;
            }
            myHandle.postDelayed(myRunnable, myHandleSeconds);
            isKeyOn=false;
            if(action == KeyEvent.ACTION_UP && keyCode==KeyEvent.KEYCODE_BACK){
                return this.onBackPressed();
            }
            return super.dispatchKeyEvent(event);
        }
        boolean isInPlayback = isInPlaybackState();
        if (action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if(count==1){
                    if (!isBottomVisible()) {
                        showSeekBar(true);
                    }
                    return true;
                }
                if (isInPlayback) {
                    mHandler.removeCallbacks(myNewPlayRunnable);
                    tvSlideStart(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
               if (isInPlayback) {
                    togglePlay();
                    return true;
                }else if(isPaused){
                   isPaused=false;
                   hidePause();
                   hideSeekBar();
                   listener.replay(false);
                    return true;
                }
//            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {  return true;// 闲置开启计时关闭透明底栏
            } else if ( keyCode == KeyEvent.KEYCODE_DPAD_UP ) {
               if(!isBottomVisible()) {
                   showSeekBar(true);
                   myHandle.postDelayed(myRunnable, myHandleSeconds);
               }else{
                   if(isPaused){
                       hideToolBar();
                   }else {
                       hideSeekBar();
                   }
               }
               isKeyOn=false;
               return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN  || keyCode== KeyEvent.KEYCODE_MENU) {
                // Toast.makeText(getContext(), "Action:"+event.getAction()+",Code:"+event.getKeyCode()+",r:"+isToolBarVisible(), Toast.LENGTH_LONG).show();
                if(!isToolBarVisible() || !isBottomVisible()) {
                    showBottom();
                    myHandle.postDelayed(myRunnable, myHandleSeconds);
                }else{
                    if(isPaused){
                        hideToolBar();
                    }else {
                        hideBottom();
                    }
                }
                isKeyOn=false;
                return true;
            }
        } else if (action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                isKeyOn=false;
                if(count==2){
                    myHandle.postDelayed(myRunnable, myHandleSeconds);
                    return true;
                }
                if (isInPlayback) {
                    mHandler.postDelayed(myNewPlayRunnable,waitTime);
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode== KeyEvent.KEYCODE_MENU) {
               if(isBottomVisible()) {
                   myHandle.postDelayed(myRunnable, myHandleSeconds);
                   isKeyOn = false;
                   return true;
               }else if(keyCode == KeyEvent.KEYCODE_DPAD_UP){
                   count=0;
               }
            }else if(keyCode==KeyEvent.KEYCODE_BACK){
                return this.onBackPressed();
            }
            isKeyOn=false;
        }
        return super.dispatchKeyEvent(event);
    }


    private boolean fromLongPress;
    private float speed_old = 1.0f;
    @Override
    public void onLongPress(MotionEvent e) {
        if (videoPlayState!=VideoView.STATE_PAUSED) {
            fromLongPress = true;
            try {
                speed_old = (float) mPlayerConfig.getDouble("sp");
                float speed = 3.0f;
                mPlayerConfig.put("sp", speed);
                updatePlayerCfgView();
                mBottomRoot.setVisibility(VISIBLE);
                listener.updatePlayerCfg();
                mControlWrapper.setSpeed(speed);
            } catch (JSONException f) {
                f.printStackTrace();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            if (fromLongPress) {
                fromLongPress =false;
                try {
                    float speed = speed_old;
                    mPlayerConfig.put("sp", speed);
                    updatePlayerCfgView();
                    mBottomRoot.setVisibility(GONE);
                    listener.updatePlayerCfg();
                    mControlWrapper.setSpeed(speed);
                } catch (JSONException f) {
                    f.printStackTrace();
                }
            }
        }
        return super.onTouchEvent(e);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        myHandle.removeCallbacks(myRunnable);
        if (!isBottomVisible()) {
            showBottom();
            // 闲置计时关闭
            myHandle.postDelayed(myRunnable, myHandleSeconds);
        } else {
            hideBottom();
        }
        return true;
    }
    @SuppressLint("NewApi")
    @Override
    public boolean onBackPressed() {
        isKeyOn=false;
        this.isPreviewBack=false;
         if (super.onBackPressed()) {
            return true;
        }
        if(isPaused){
            if(isToolBarVisible()){
                hideToolBar();
                return true;
            }
            isPaused=false;
            hidePause();
            hideSeekBar();
           if (isInPlaybackState()) {
               togglePlay();
               return true;
           }
           listener.replay(false);
           return true;
        }else if (isBottomVisible() && !isPaused) {
            hideBottom();
            return true;
        }
        if(isPortrait){
            setLandscapePortrait();
            return true;
        }
        this.isPreviewBack=true;
        return false;
    }
    public void previewBackPress(){
        this.isPreviewBack=true;
    }
    private void selectPlayType(int type){
        try {
            hideBottom();
            hidePause();
            mPlayerConfig.put("pl", type);
            updatePlayerCfgView();
            listener.updatePlayerCfg();
            listener.replay(false);
        } catch (JSONException e) {

        }
    }
    private  int getRePlayType(int playType){
        int newType=-1;
        Iterator<Integer> iterator = list.iterator();
        while (iterator.hasNext()){
            int type = iterator.next();
            if(playType==type){
                iterator.remove();
            }else if(newType==-1 && playType!=type){
                newType=type;
            }

        }
        return newType;
    }
}
