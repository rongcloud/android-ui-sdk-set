package io.rong.imkit.feature.location;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.utils.language.LangUtils;
import io.rong.imkit.utils.language.RongConfigurationManager;

/**
 * 搜索位置页面
 */
public class SearchLocationActivity extends Activity {

    private EditText mSearchEditText;
    private ListView searchListView;
    private TextView searchNoResult;
    private SearchListAdapter searchListAdapter;
    private String filterString;
    private final int PAGE_COUNT = 20;
    private int currentPage = 1;
    private String cityCode = "";

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = RongConfigurationManager.getInstance().getConfigurationContext(newBase);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_location_search_activity);
        Intent intent = getIntent();
        cityCode = intent.getStringExtra(LocationConst.CITY_CODE);
        initView();
    }

    private void initView() {
        mSearchEditText = findViewById(R.id.rc_et_search);
        ImageView mPressBackImageView = findViewById(R.id.rc_iv_press_back);
        searchListView = findViewById(R.id.rc_filtered_location_list);
        searchNoResult = findViewById(R.id.rc_tv_search_no_results);
        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                filterString = s.toString();
                searchLocationByKeyword(filterString);
            }
        });

        mSearchEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (mSearchEditText.getRight() - 2 * mSearchEditText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        mSearchEditText.setText("");
                        mSearchEditText.clearFocus();
                        return true;
                    }
                }
                return false;
            }
        });

        searchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SearchLocationInfo searchLocationInfo = (SearchLocationInfo) searchListAdapter.getItem(position);
                Intent intent = new Intent();
                intent.putExtra(LocationConst.LONGITUDE, searchLocationInfo.getLongitude());
                intent.putExtra(LocationConst.LATITUDE, searchLocationInfo.getLatitude());
                intent.putExtra(LocationConst.POI, searchLocationInfo.getPoi());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        mPressBackImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        searchListView.setOnScrollListener(onScrollListener);
    }


    private AbsListView.OnScrollListener onScrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (firstVisibleItem != 0 && (firstVisibleItem + visibleItemCount) == totalItemCount) {
                View lastVisibleItemView = searchListView.getChildAt(searchListView.getChildCount() - 1);
                if (lastVisibleItemView != null && lastVisibleItemView.getBottom() == searchListView.getHeight()) {
                    searchNextPageByKeyword(filterString);
                }
            }
        }
    };

    private void searchLocationByKeyword(final String keyword) {
        String searchCityCode = cityCode;
        if (TextUtils.isEmpty(cityCode)) {
            searchCityCode = LocationConst.DEFAULT_CITY_CODE;//北京市
        }
        PoiSearch.Query query = new PoiSearch.Query(keyword, "", searchCityCode);
        query.setPageSize(PAGE_COUNT);// 设置每页最多返回多少条poiitem
        currentPage = 1;
        query.setPageNum(currentPage);// 设置查第一页
        PoiSearch poiSearch = new PoiSearch(this, query);
        poiSearch.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
            @Override
            public void onPoiSearched(PoiResult poiResult, int i) {
                if (poiResult != null && poiResult.getPois().size() > 0) {
                    List<PoiItem> poiItemList = poiResult.getPois();
                    SearchLocationInfo searchLocationInfo;
                    List<SearchLocationInfo> searchLocationInfoList = new ArrayList<>();
                    for (int j = 0; j < poiItemList.size(); j++) {
                        PoiItem poiItem = poiItemList.get(j);
                        searchLocationInfo = new SearchLocationInfo(poiItem.getTitle(), poiItem.getSnippet());
                        searchLocationInfo.setLongitude(poiItem.getLatLonPoint().getLongitude());
                        searchLocationInfo.setLatitude(poiItem.getLatLonPoint().getLatitude());
                        searchLocationInfo.setPoi(poiItem.getTitle());
                        searchLocationInfoList.add(searchLocationInfo);
                    }
                    searchNoResult.setVisibility(View.GONE);
                    searchListView.setVisibility(View.VISIBLE);
                    searchListAdapter = new SearchListAdapter(SearchLocationActivity.this, searchLocationInfoList, keyword);
                    searchListView.setAdapter(searchListAdapter);
                } else {
                    if (filterString.equals("")) {
                        searchNoResult.setVisibility(View.GONE);
                    } else {
                        searchNoResult.setVisibility(View.VISIBLE);
                        searchNoResult.setText(getResources().getString(R.string.rc_search_no_result));
                    }
                    searchListView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPoiItemSearched(PoiItem poiItem, int i) {

            }
        });// 设置数据返回的监听器
        poiSearch.searchPOIAsyn();// 开始搜索
    }

    private void searchNextPageByKeyword(final String keyword) {
        String searchCityCode = cityCode;
        if (TextUtils.isEmpty(cityCode)) {
            searchCityCode = LocationConst.DEFAULT_CITY_CODE;//北京市
        }
        PoiSearch.Query query = new PoiSearch.Query(keyword, "", searchCityCode);
        query.setPageSize(PAGE_COUNT);
        query.setPageNum(++currentPage);
        PoiSearch poiSearch = new PoiSearch(this, query);
        poiSearch.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
            @Override
            public void onPoiSearched(PoiResult poiResult, int i) {
                if (poiResult != null && poiResult.getPois().size() > 0) {
                    List<PoiItem> poiItemList = poiResult.getPois();
                    SearchLocationInfo searchLocationInfo;
                    List<SearchLocationInfo> searchLocationInfoList = new ArrayList<>();
                    for (int j = 0; j < poiItemList.size(); j++) {
                        PoiItem poiItem = poiItemList.get(j);
                        searchLocationInfo = new SearchLocationInfo(poiItem.getTitle(), poiItem.getSnippet());
                        searchLocationInfo.setLongitude(poiItem.getLatLonPoint().getLongitude());
                        searchLocationInfo.setLatitude(poiItem.getLatLonPoint().getLatitude());
                        searchLocationInfo.setPoi(poiItem.getTitle());
                        searchLocationInfoList.add(searchLocationInfo);
                    }
                    searchListAdapter.addItems(searchLocationInfoList);
                    searchListAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onPoiItemSearched(PoiItem poiItem, int i) {

            }
        });// 设置数据返回的监听器
        poiSearch.searchPOIAsyn();// 开始搜索
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (null != this.getCurrentFocus() && event.getAction() == MotionEvent.ACTION_UP) {
            /*
             * 点击空白位置 隐藏软键盘
             */
            InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (mInputMethodManager != null) {
                return mInputMethodManager.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onResume() {
        mSearchEditText.requestFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(mSearchEditText, 0);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
        }
        super.onPause();
    }

    private class SearchListAdapter extends BaseAdapter {
        private List<SearchLocationInfo> searchList = new ArrayList<>();
        private Context context;
        private String keyword;

        public SearchListAdapter(Context context, List<SearchLocationInfo> searchList, String keyword) {
            this.context = context;
            this.searchList = searchList;
            this.keyword = keyword;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SearchViewHolder searchViewHolder;
            final SearchLocationInfo searchLocationInfo = searchList.get(position);
            if (convertView == null) {
                searchViewHolder = new SearchViewHolder();
                convertView = View.inflate(context, R.layout.rc_location_search_item, null);
                searchViewHolder.tvName = convertView.findViewById(R.id.rc_search_name);
                searchViewHolder.tvAddress = convertView.findViewById(R.id.rc_search_address);
                convertView.setTag(searchViewHolder);
            } else {
                searchViewHolder = (SearchViewHolder) convertView.getTag();
            }
            setLocationTitle(searchViewHolder.tvName, searchLocationInfo.getName(), keyword);
            setLocationAddress(searchViewHolder.tvAddress, searchLocationInfo.getAddress(), keyword);
            return convertView;
        }

        @Override
        public Object getItem(int position) {
            if (searchList != null && searchList.size() > 0) {
                return searchList.get(position);
            } else {
                return null;
            }
        }

        public void addItems(List<SearchLocationInfo> searchList) {
            if (this.searchList != null) {
                this.searchList.addAll(searchList);
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getCount() {
            if (TextUtils.isEmpty(filterString)) {
                return 0;
            }
            return searchList.size();
        }

        class SearchViewHolder {
            TextView tvName;
            TextView tvAddress;
        }
    }

    private void setLocationTitle(TextView titleView, String locationTitle, String keyword) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        SpannableStringBuilder colorFilterStr = new SpannableStringBuilder(locationTitle);
        int subPosition = (locationTitle.toLowerCase()).indexOf(keyword.toLowerCase());
        if (subPosition >= 0) {
            colorFilterStr.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.rc_map_search_highlight_color)), subPosition,
                    subPosition + keyword.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannableStringBuilder.append(colorFilterStr);
            titleView.setText(spannableStringBuilder);
        } else {
            titleView.setText(locationTitle);
        }
    }

    private void setLocationAddress(TextView addressView, String locationAddress, String keyword) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        SpannableStringBuilder colorFilterStr = new SpannableStringBuilder(locationAddress);
        int subPosition = (locationAddress.toLowerCase()).indexOf(keyword.toLowerCase());
        if (subPosition >= 0) {
            colorFilterStr.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.rc_map_search_highlight_color)), subPosition,
                    subPosition + keyword.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannableStringBuilder.append(colorFilterStr);
            addressView.setText(spannableStringBuilder);
            addressView.setText(spannableStringBuilder);
        } else {
            addressView.setText(locationAddress);
        }
    }

    private class SearchLocationInfo {
        String name;
        String address;
        double latitude;
        double longitude;
        String poi;

        public SearchLocationInfo(String name, String address) {
            this.name = name;
            this.address = address;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getAddress() {
            return address;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public String getPoi() {
            return poi;
        }

        public void setPoi(String poi) {
            this.poi = poi;
        }
    }
}
