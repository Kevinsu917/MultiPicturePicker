package com.example.multipicturepicker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;

/**
 * @ClassName PicturePickActivity.java
 * @author KevinSu kevinsu917@126.com
 * @version 创建时间：2014-10-17 下午8:01:22
 * @Description:多图选择的Activity
 */

public class PicturePickActivity extends ActionBarActivity implements
		LoaderCallbacks<Cursor>, View.OnClickListener {

	private ImageLoader imageLoader = ImageLoader.getInstance();
	private DisplayImageOptions options;

	private Context mContext;

	private TextView tvLeft;// 返回按钮
	private TextView tvTitle;// 标题
	private TextView tvRight;// 确定按钮
	private GridView mGridView;

	private TextView tvAlbum;// 相册按钮
	private RelativeLayout rlAlbum;// 相册背景布局
	private ListView lvAlbum;// 相册listView

	private ArrayList<String> pathList = new ArrayList<String>();// 照片路径的list
	private SparseArray<String> buckSpareseArray = new SparseArray<String>();// 相册名字SparseArray
	private Set<String> selectedPathSet = new HashSet<String>();//选中的路径Set

	private BuckNameAdapter albumAdapter;//相册的Adapter
	private AlbumGridAdapter picAdapter;//图片的Adapter

	private boolean isFirst = true;//是否第一次加载
	private final String ALBUM_ALL = "全部";
	private int IMAGEVIEW_WIDTH = 0;
	private static int MAX_SIZE = 6;
	private static int NUM_PER_COLUMN = 3;//每行展示多少个View

	private String TITLE_TEXT = "选择照片%s";

	public static String PATH_LIST = "pathList";

	final String[] columns = { MediaStore.Images.Media._ID,
			MediaStore.Images.Media.DATA,
			MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
			MediaStore.Images.Media.BUCKET_ID };

	String seletion = null;// buckName的选择

	private final int VIEW_TAG = R.layout.multi_select_item;
	private final int PATH_TAG = R.layout.pick_picture_activity;

	
	/**
	 * 跳转到多图选择的Activity
	 * @param context
	 * @param requestCode 
	 * @param MaxSeleted 最大照片选择数
	 */
	public static void skipToPictureMultiSelectAlbum(Context context, int requestCode, int MaxSeleted)
	{
		Intent intent = new Intent();
		intent.setClass(context, PicturePickActivity.class);
		MAX_SIZE =  MaxSeleted;
		((Activity)context).startActivityForResult(intent, requestCode);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏 
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//去掉信息栏
		
		setContentView(R.layout.pick_picture_activity);

		mContext = this;
		initImageloader();
		initView();

		getLoaderManager().initLoader(0, null, this);

	}

	private void initImageloader() {
		options = new DisplayImageOptions.Builder()
				.showStubImage(R.drawable.ic_launcher)
				.showImageForEmptyUri(R.drawable.ic_launcher).cacheInMemory()
				.cacheOnDisc().build();

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				getApplicationContext())
				.threadPoolSize(3)
				.threadPriority(Thread.NORM_PRIORITY - 2)
				.memoryCacheSize(1500000)
				// 1.5 Mb
				.denyCacheImageMultipleSizesInMemory()
				.discCacheFileNameGenerator(new Md5FileNameGenerator())
				.enableLogging() // Not necessary in common
				.build();
		imageLoader.init(config);
	}

	//计算每张图片显示的宽
	private int getWith() {
		if (IMAGEVIEW_WIDTH == 0) {
			DisplayMetrics dm = new DisplayMetrics();
			dm = this.getResources().getDisplayMetrics();
			IMAGEVIEW_WIDTH = (dm.widthPixels - 2 * (NUM_PER_COLUMN-1)) / NUM_PER_COLUMN;
		}
		return IMAGEVIEW_WIDTH;
	}

	private void initView() {
		tvLeft = (TextView) findViewById(R.id.title_left_text);
		tvLeft.setBackgroundResource(R.drawable.title_back);
		tvTitle = (TextView) findViewById(R.id.title_name);
		updateTitle();
		tvRight = (TextView) findViewById(R.id.title_right_text);
		tvRight.setText("确定");

		tvRight.setOnClickListener(this);
		tvLeft.setOnClickListener(this);
		picAdapter = new AlbumGridAdapter(pathList);
		mGridView = (GridView) findViewById(R.id.gvPic);
		mGridView.setAdapter(picAdapter);

		albumAdapter = new BuckNameAdapter(buckSpareseArray);
		tvAlbum = (TextView) findViewById(R.id.tvAlbum);
		tvAlbum.setOnClickListener(this);
		rlAlbum = (RelativeLayout) findViewById(R.id.rlAlbum);
		lvAlbum = (ListView) findViewById(R.id.lvAlbum);
		lvAlbum.setAdapter(albumAdapter);
	}

	// 更新title
	private void updateTitle() {
		String args = selectedPathSet.size() + "/" + MAX_SIZE;
		String title = String.format(TITLE_TEXT, args);
		tvTitle.setText(title);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		// TODO Auto-generated method stub
		return new CursorLoader(mContext,
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns,
				seletion, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		// TODO Auto-generated method stub
		pathList.clear();
		if (arg1.moveToFirst()) {
			if (isFirst) {
				buckSpareseArray.put(0, ALBUM_ALL);
			}
			
			//把获取的数据压到对应的list中
			while (!arg1.isAfterLast()) {
				int dataColumnIndex = arg1
						.getColumnIndex(MediaStore.Images.Media.DATA);
				String path = arg1.getString(dataColumnIndex);

				File file = new File(path);
				if (file.exists()) {
					pathList.add(path);

					if (isFirst) {
						int buckColumnIndex = arg1
								.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
						String buckName = arg1.getString(buckColumnIndex);

						int buckIdColumnIndex = arg1
								.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
						int buckId = arg1.getInt(buckIdColumnIndex);

						buckSpareseArray.put(buckId, buckName);
					}
				}

				arg1.moveToNext();
			}
		}
		isFirst = false;
		picAdapter.notifyDataSetChanged();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub
	}

	// 相册Grid的适配器
	private class AlbumGridAdapter extends BaseAdapter {

		private ArrayList<String> list;

		public AlbumGridAdapter(ArrayList<String> list) {
			// TODO Auto-generated constructor stub
			this.list = list;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return list == null ? 0 : list.size();
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return list == null ? null : list.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return arg0;
		}

		@Override
		public View getView(int arg0, View arg1, ViewGroup arg2) {
			// TODO Auto-generated method stub

			if (arg1 == null) {
				arg1 = View.inflate(mContext, R.layout.multi_select_item, null);
			}

			final ViewHolder holder = new ViewHolder();
			holder.rlContent = (RelativeLayout) arg1
					.findViewById(R.id.rlContent);
			holder.rlContent.setLayoutParams(new RelativeLayout.LayoutParams(
					getWith(), getWith()));
			holder.mCheckBox = (CheckBox) arg1.findViewById(R.id.cbSelect);
			holder.mImageView = (ImageView) arg1.findViewById(R.id.ivImage);
			holder.mImageView.setLayoutParams(new RelativeLayout.LayoutParams(
					getWith(), getWith()));

			String path = pathList.get(arg0);
			imageLoader.displayImage("file://" + path, holder.mImageView,
					options, new SimpleImageLoadingListener() {
						@Override
						public void onLoadingComplete(Bitmap loadedImage) {
							Animation anim = AnimationUtils.loadAnimation(
									PicturePickActivity.this, R.anim.fade_in);
							holder.mImageView.setAnimation(anim);
							anim.start();
						}
					});

			int colorId = selectedPathSet.contains(path) ? R.color.c_33000000
					: R.color.transparent;
			holder.rlContent.setBackgroundColor(getResources()
					.getColor(colorId));

			holder.mCheckBox.setTag(VIEW_TAG, holder);
			holder.mCheckBox.setTag(PATH_TAG, path);

			holder.mCheckBox.setChecked(selectedPathSet.contains(path));
			holder.mCheckBox.setOnCheckedChangeListener(mCheckedChangeListener);
			holder.mCheckBox.setOnTouchListener(mTouchListener);

			return arg1;
		}

		private class ViewHolder {
			public RelativeLayout rlContent;
			public ImageView mImageView;
			public CheckBox mCheckBox;
		}

		OnCheckedChangeListener mCheckedChangeListener = new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				ViewHolder holder = (ViewHolder) buttonView.getTag(VIEW_TAG);
				String path = (String) buttonView.getTag(PATH_TAG);
				if (isChecked) {
					selectedPathSet.add(path);
				} else {
					selectedPathSet.remove(path);
				}
				updateTitle();
				int colorId = isChecked ? R.color.c_33000000
						: R.color.transparent;
				holder.rlContent.setBackgroundColor(getResources().getColor(
						colorId));
			}
		};

		OnTouchListener mTouchListener = new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub
				String path = (String) arg0.getTag(PATH_TAG);
				if (selectedPathSet.size() >= MAX_SIZE
						&& !selectedPathSet.contains(path)) {
					Toast.makeText(mContext, "超出可选范围", Toast.LENGTH_SHORT)
							.show();
					return true;
				}
				return false;

			}
		};
	}

	// 相册Adapter
	private class BuckNameAdapter extends BaseAdapter {

		private SparseArray<String> array;

		public BuckNameAdapter(SparseArray<String> array) {
			// TODO Auto-generated constructor stub
			this.array = array;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return array == null ? 0 : array.size();
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return array == null ? null : array.valueAt(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return arg0;
		}

		@Override
		public View getView(int arg0, View arg1, ViewGroup arg2) {
			// TODO Auto-generated method stub
			if (arg1 == null) {
				arg1 = View.inflate(mContext, R.layout.mult_select_album_item,
						null);
			}
			TextView tvName = (TextView) arg1.findViewById(R.id.tvBuckname);
			tvName.setText(array.valueAt(arg0));
			tvName.setTag(array.valueAt(arg0));
			tvName.setOnClickListener(selectClickListener);
			return arg1;
		}
	}

	// 相册选择点击事件
	private View.OnClickListener selectClickListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			String buckName = (String) arg0.getTag();
			if (buckName == ALBUM_ALL) {
				seletion = null;
			} else {
				seletion = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + "="
						+ "'" + buckName + "'";
			}
			hideAlbum();
			getLoaderManager().restartLoader(0, null, PicturePickActivity.this);
		}
	};

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		if (arg0.equals(tvAlbum)) {
			albumBtnClick();
		}else if(arg0.equals(tvRight))
		{
			if(selectedPathSet.size() > 0)
			{
				Intent intent = new Intent();
				ArrayList<String> pathArray = new ArrayList<String>(selectedPathSet.size());
				Iterator<String> iterator = selectedPathSet.iterator();
				for(;iterator.hasNext();)
				{
					pathArray.add(iterator.next());
				}
				intent.putStringArrayListExtra(PATH_LIST, pathArray);
				setResult(RESULT_OK, intent);
				finish();
			}else
			{
				Toast.makeText(mContext, "请选择照片", Toast.LENGTH_SHORT).show();
			}
		}else if(arg0.equals(tvLeft))
		{
			finish();
		}
	}

	// 相册按钮点击
	private void albumBtnClick() {
		if (rlAlbum != null && rlAlbum.getVisibility() == View.GONE) {
			showAlbum();
		} else {
			hideAlbum();
		}
	}

	// 显示相册选择器
	private void showAlbum() {
		if (rlAlbum != null) {
			rlAlbum.setVisibility(View.VISIBLE);
		}
	}

	// 显示相册选择器
	private void hideAlbum() {
		if (rlAlbum != null) {
			rlAlbum.setVisibility(View.GONE);
		}
	}
}
