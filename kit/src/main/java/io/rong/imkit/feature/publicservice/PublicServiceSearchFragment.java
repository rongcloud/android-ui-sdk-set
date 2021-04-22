package io.rong.imkit.feature.publicservice;

public class PublicServiceSearchFragment extends DispatchResultFragment {
//    private final static String TAG = "PublicServiceSearchFragment";
//    private EditText mEditText;
//    private Button mSearchBtn;
//    private ListView mListView;
//    private PublicServiceListAdapter mAdapter;
//
//    private LoadingDialogFragment mLoadingDialogFragment;
//
//    @Override
//    protected void initFragment(Uri uri) {
//
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.rc_fr_public_service_search, container, false);
//        mEditText = view.findViewById(R.id.rc_search_ed);
//        mSearchBtn = view.findViewById(R.id.rc_search_btn);
//        mListView = view.findViewById(R.id.rc_search_list);
//        RongContext.getInstance().getEventBus().register(this);
//        return view;
//    }
//
//    @Override
//    public boolean handleMessage(Message msg) {
//        return false;
//    }
//
//    @Override
//    public void onViewCreated(View view, Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        mLoadingDialogFragment = LoadingDialogFragment.newInstance("", getResources().getString(R.string.rc_notice_data_is_loading));
//        mSearchBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mLoadingDialogFragment.show(getFragmentManager());
//                RongIM.getInstance().searchPublicService(RongIMClient.SearchType.FUZZY, mEditText.getText().toString(), new RongIMClient.ResultCallback<PublicServiceProfileList>() {
//
//                    @Override
//                    public void onError(RongIMClient.ErrorCode e) {
//                        mLoadingDialogFragment.dismiss();
//                    }
//
//                    @Override
//                    public void onSuccess(PublicServiceProfileList list) {
//                        mAdapter.clear();
//                        mAdapter.addCollection(list.getPublicServiceData());
//                        mAdapter.notifyDataSetChanged();
//                        mLoadingDialogFragment.dismiss();
//                    }
//                });
//            }
//        });
//
//        mAdapter = new PublicServiceListAdapter(PublicServiceSearchFragment.this.getActivity());
//        mListView.setAdapter(mAdapter);
//        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                PublicServiceProfile info = mAdapter.getItem(position);
//                if (info.isFollow()) {
//                    RongIM.getInstance().startConversation(getActivity(), info.getConversationType(), info.getTargetId(), info.getName());
//                } else {
//                    Uri uri = Uri.parse("rong://" + view.getContext().getApplicationInfo().packageName).buildUpon()
//                            .appendPath("publicServiceProfile").appendPath(info.getConversationType().getName().toLowerCase()).appendQueryParameter("targetId", info.getTargetId()).build();
//                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//                    intent.putExtra(PublicServiceProfileFragment.AGS_PUBLIC_ACCOUNT_INFO, info);
//                    startActivity(intent);
//                }
//            }
//        });
//    }
//
//    @Override
//    public void onDestroy() {
//        RongContext.getInstance().getEventBus().unregister(this);
//        super.onDestroy();
//    }
//
//    private class PublicServiceListAdapter extends io.rong.imkit.widget.adapter.BaseAdapter<PublicServiceProfile> {
//        LayoutInflater mInflater;
//        public PublicServiceListAdapter(Context context) {
//            mInflater = LayoutInflater.from(context);
//        }
//
//        @Override
//        protected View newView(Context context, int position, ViewGroup group) {
//            View view = mInflater.inflate(R.layout.rc_item_public_service_search, null);
//            ViewHolder viewHolder = new ViewHolder();
//            viewHolder.portrait = view.findViewById(R.id.rc_portrait);
//            viewHolder.title = view.findViewById(R.id.rc_title);
//            viewHolder.description = view.findViewById(R.id.rc_description);
//
//            view.setTag(viewHolder);
//            return view;
//        }
//
//        @Override
//        protected void bindView(View v, int position, PublicServiceProfile data) {
//            ViewHolder viewHolder = (ViewHolder) v.getTag();
//            if (data != null) {
//                viewHolder.portrait.setResource(data.getPortraitUri());
//                viewHolder.title.setText(data.getName());
//                viewHolder.description.setText(data.getIntroduction());
//            }
//        }
//
//        @Override
//        public int getCount() {
//            return super.getCount();
//        }
//
//        @Override
//        public PublicServiceProfile getItem(int position) {
//            return super.getItem(position);
//        }
//
//        @Override
//        public long getItemId(int position) {
//            return 0;
//        }
//
//        class ViewHolder {
//            AsyncImageView portrait;
//            TextView title;
//            TextView description;
//        }
//    }
//
//    public void onEventMainThread(Event.PublicServiceFollowableEvent event) {
//        if (event != null && getActivity() != null) {
//            RLog.d(TAG, "onEventMainThread PublicAccountIsFollowEvent, follow=" + event.isFollow());
//            getActivity().finish();
//        }
//    }
}
