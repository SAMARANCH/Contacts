/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.contacts.R;
import com.android.contacts.common.list.ContactListAdapter;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.list.ContactListFilterController;
import com.android.contacts.common.list.ContactListItemView;
import com.android.contacts.common.list.DefaultContactListAdapter;
import com.android.contacts.common.list.ProfileAndContactsLoader;
import com.android.contacts.common.util.AccountFilterUtil;
import com.android.contacts.editor.ContactEditorFragment;

import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.util.ContactsListUtils;
import com.mediatek.contacts.util.LogUtils;
import com.mediatek.contacts.vcs.VcsController;
import com.mediatek.contacts.widget.WaitCursorView;
//bird
import com.nbbsw.contacts.BirdFeature;
import android.widget.EditText;
import android.text.Editable;
import android.widget.ImageView;
import android.text.style.ImageSpan;
import android.text.TextWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.graphics.drawable.Drawable;
import com.nbbsw.contactscommon.BirdCommonFeature;
import android.app.Activity;
import com.nbbsw.contactscommon.util.BirdContactsCommonUtils;
import android.database.Cursor;
import com.android.contacts.common.model.account.AccountType;

/**
 * Fragment containing a contact list used for browsing (as compared to
 * picking a contact with one of the PICK intents).
 */
public class DefaultContactBrowseListFragment extends ContactBrowseListFragment 
 implements OnClickListener{//modify by sunjunwei for fto 20150303
    private static final String TAG = DefaultContactBrowseListFragment.class.getSimpleName();

    private static final int REQUEST_CODE_ACCOUNT_FILTER = 1;

    private View mSearchHeaderView;
    private View mAccountFilterHeader;
    private FrameLayout mProfileHeaderContainer;
    private View mProfileHeader;
    private Button mProfileMessage;
    private TextView mProfileTitle;
    private View mSearchProgress;
    private TextView mSearchProgressText;

    private class FilterHeaderClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            AccountFilterUtil.startAccountFilterActivityForResult(
                        DefaultContactBrowseListFragment.this,
                        REQUEST_CODE_ACCOUNT_FILTER,
                        getFilter());
        }
    }
    private OnClickListener mFilterHeaderClickListener = new FilterHeaderClickListener();

    public DefaultContactBrowseListFragment() {
        setPhotoLoaderEnabled(true);
        // Don't use a QuickContactBadge. Just use a regular ImageView. Using a QuickContactBadge
        // inside the ListView prevents us from using MODE_FULLY_EXPANDED and messes up ripples.
        setQuickContactEnabled(false);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
    }

    @Override
    public CursorLoader createCursorLoader(Context context) {
        /** M: Bug Fix for ALPS00115673 Descriptions: add wait cursor. @{ */
        Log.d(TAG, "createCursorLoader");
        if (mLoadingContainer != null) {
            mLoadingContainer.setVisibility(View.GONE);
        }
        // qinglei comment out this for L migration bug: people list will stop on wait dialog when enter it.
        //mWaitCursorView.startWaitCursor();
        /** @} */

        return new ProfileAndContactsLoader(context);
    }

    @Override
    protected void onItemClick(int position, long id) {
        LogUtils.d(TAG, "[onItemClick][launch]start");
        final Uri uri = getAdapter().getContactUri(position);
        if (uri == null) {
            return;
        }
        if (ExtensionManager.getInstance().getRcsExtension()
                .addRcsProfileEntryListener(uri, false)) {
            return;
        }
        viewContact(uri);
        LogUtils.d(TAG, "[onItemClick][launch]end");
    }

    @Override
    protected ContactListAdapter createListAdapter() {
        DefaultContactListAdapter adapter = new DefaultContactListAdapter(getContext());
        //BUG #4249,chengting,@20150512,begin
        adapter.setContactsCountEnable(byFTO);
        //BUG #4249,chengting,@20150512,end
        adapter.setSectionHeaderDisplayEnabled(isSectionHeaderDisplayEnabled());
        adapter.setDisplayPhotos(true);
        adapter.setPhotoPosition(
                ContactListItemView.getDefaultPhotoPosition(/* opposite = */ false));
        return adapter;
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_list_content, null);
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);

        mAccountFilterHeader = getView().findViewById(R.id.account_filter_header_container);
        mAccountFilterHeader.setOnClickListener(mFilterHeaderClickListener);

        // Create an entry for public account and show it from now
        ExtensionManager.getInstance().getRcsExtension()
            .createPublicAccountEntryView(getListView());

        // Create an empty user profile header and hide it for now (it will be visible if the
        // contacts list will have no user profile).
        addEmptyUserProfileHeader(inflater);
        showEmptyUserProfile(false);
        /** M: Bug Fix for ALPS00115673 Descriptions: add wait cursor */
       mWaitCursorView = ContactsListUtils.initLoadingView(this.getContext(),
                getView(), mLoadingContainer, mLoadingContact, mProgress);

        // Putting the header view inside a container will allow us to make
        // it invisible later. See checkHeaderViewVisibility()
        FrameLayout headerContainer = new FrameLayout(inflater.getContext());
        mSearchHeaderView = inflater.inflate(R.layout.search_header, null, false);
        headerContainer.addView(mSearchHeaderView);
        getListView().addHeaderView(headerContainer, null, false);
        checkHeaderViewVisibility();

        mSearchProgress = getView().findViewById(R.id.search_progress);
        mSearchProgressText = (TextView) mSearchHeaderView.findViewById(R.id.totalContactsText);
        //add by sunjunwei for fto 20150123 begin
        if(byFTO){
        	((View)getView().findViewById(R.id.bird_fto_contacts_fragment_header)).setVisibility(View.VISIBLE);
        	prepareSearchView();
        }
        //add by sunjunwei for fto 20150123 end
    }

    @Override
    protected void setSearchMode(boolean flag) {
        super.setSearchMode(flag);
        checkHeaderViewVisibility();
        if (!flag) showSearchProgress(false);
    }

    /** Show or hide the directory-search progress spinner. */
    private void showSearchProgress(boolean show) {
        if (mSearchProgress != null) {
            mSearchProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void checkHeaderViewVisibility() {
        updateFilterHeaderView();

        // Hide the search header by default.
        if (mSearchHeaderView != null) {
            mSearchHeaderView.setVisibility(View.GONE);
        }
    }

    @Override
    public void setFilter(ContactListFilter filter) {
        super.setFilter(filter);
        updateFilterHeaderView();
    }

    private void updateFilterHeaderView() {
        if (mAccountFilterHeader == null) {
            return; // Before onCreateView -- just ignore it.
        }
        final ContactListFilter filter = getFilter();
        if (filter != null && !isSearchMode()) {
            final boolean shouldShowHeader = AccountFilterUtil.updateAccountFilterTitleForPeople(
                    mAccountFilterHeader, filter, false);
            mAccountFilterHeader.setVisibility(shouldShowHeader ? View.VISIBLE : View.GONE);
        } else {
            mAccountFilterHeader.setVisibility(View.GONE);
        }
    }

    @Override
    public void setProfileHeader() {
        mUserProfileExists = getAdapter().hasProfile();
        showEmptyUserProfile(!mUserProfileExists && !isSearchMode());

        if (isSearchMode()) {
            ContactListAdapter adapter = getAdapter();
            if (adapter == null) {
                return;
            }

            // In search mode we only display the header if there is nothing found
            if (TextUtils.isEmpty(getQueryString()) || !adapter.areAllPartitionsEmpty()) {
                mSearchHeaderView.setVisibility(View.GONE);
                showSearchProgress(false);
            } else {
                mSearchHeaderView.setVisibility(View.VISIBLE);
                if (adapter.isLoading()) {
                    mSearchProgressText.setText(R.string.search_results_searching);
                    showSearchProgress(true);
                } else {
                    mSearchProgressText.setText(R.string.listFoundAllContactsZero);
                    mSearchProgressText.sendAccessibilityEvent(
                            AccessibilityEvent.TYPE_VIEW_SELECTED);
                    showSearchProgress(false);
                }
            }
            showEmptyUserProfile(false);
        }

        /// M: [VCS] @{
        int count = getContactCount();
        if (mContactsLoadListener != null) {
            mContactsLoadListener.onContactsLoad(count);
        }
        /// @}
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ACCOUNT_FILTER) {
            if (getActivity() != null) {
                AccountFilterUtil.handleAccountFilterResult(
                        ContactListFilterController.getInstance(getActivity()), resultCode, data);
            } else {
                Log.e(TAG, "getActivity() returns null during Fragment#onActivityResult()");
            }
        }
    }

    private void showEmptyUserProfile(boolean show) {
        // Changing visibility of just the mProfileHeader doesn't do anything unless
        // you change visibility of its children, hence the call to mCounterHeaderView
        // and mProfileTitle
        Log.d(TAG, "showEmptyUserProfile show : " + show);
        mProfileHeaderContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        mProfileHeader.setVisibility(show ? View.VISIBLE : View.GONE);
        mProfileTitle.setVisibility(show ? View.VISIBLE : View.GONE);
        mProfileMessage.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * This method creates a pseudo user profile contact. When the returned query doesn't have
     * a profile, this methods creates 2 views that are inserted as headers to the listview:
     * 1. A header view with the "ME" title and the contacts count.
     * 2. A button that prompts the user to create a local profile
     */
    private void addEmptyUserProfileHeader(LayoutInflater inflater) {
        ListView list = getListView();
        // Add a header with the "ME" name. The view is embedded in a frame view since you cannot
        // change the visibility of a view in a ListView without having a parent view.
        //BIRD_DIALER_FTO,chengting,@20150327,begin
        if(BirdFeature.BIRD_DIALER_FTO){
        	mProfileHeader = inflater.inflate(R.layout.bird_fto_user_profile_header, null, false);
			//BUG #4249,chengting,@20150512,begin
        	mCounterHeaderView = (TextView) mProfileHeader.findViewById(R.id.contacts_count);
			//BUG #4249,chengting,@20150512,end
        }else{
        	mProfileHeader = inflater.inflate(R.layout.user_profile_header, null, false);
        }
        //BIRD_DIALER_FTO,chengting,@20150327,end
        mProfileTitle = (TextView) mProfileHeader.findViewById(R.id.profile_title);
        mProfileHeaderContainer = new FrameLayout(inflater.getContext());
        mProfileHeaderContainer.addView(mProfileHeader);
        list.addHeaderView(mProfileHeaderContainer, null, false);

        // Add a button with a message inviting the user to create a local profile
        mProfileMessage = (Button) mProfileHeader.findViewById(R.id.user_profile_button);
        mProfileMessage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ExtensionManager.getInstance().getRcsExtension()
                       .addRcsProfileEntryListener(null, true)) { 
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                intent.putExtra(ContactEditorFragment.INTENT_EXTRA_NEW_LOCAL_PROFILE, true);
                startActivity(intent);
            }
        });
    }

    /** M: Bug Fix For ALPS00115673. @{*/
    private ProgressBar mProgress;
    private View mLoadingContainer;
    private WaitCursorView mWaitCursorView;
    private TextView mLoadingContact;
    /** @} */
    /// M: for vcs
    private VcsController.ContactsLoadListener mContactsLoadListener = null;

    /**
     * M: Bug Fix CR ID: ALPS00279111.
     */
    public void closeWaitCursor() {
        // TODO Auto-generated method stub
        Log.d(TAG, "closeWaitCursor   DefaultContactBrowseListFragment");
        mWaitCursorView.stopWaitCursor();
    }

    /**
     * M: [vcs] for vcs.
     */
    public void setContactsLoadListener(VcsController.ContactsLoadListener listener) {
        mContactsLoadListener = listener;
    }

    /**
     * M: for ALPS01766595.
     */
    private int getContactCount() {
        int count = isSearchMode() ? 0 : getAdapter().getCount();
        if (mUserProfileExists) {
            count -= PROFILE_NUM;
        }
        return count;
    }
    
  	//add by sunjunwei for fto 20150303 begin
  	private View mSearchViewContainer;
  	private EditText mSearchView;
  	private ImageView mSearchViewCloseButton;
  	private Button mAddContactButton;
  	
  	private String mSearchQuery;
  	
  	private boolean byFTO = false;
  	
  	
  	private void prepareSearchView() {
          mSearchViewContainer = getView().findViewById(R.id.bird_search_view_container);
          mSearchViewCloseButton = (ImageView)getView().findViewById(R.id.bird_search_close_button);
          mAddContactButton = (Button)getView().findViewById(R.id.bird_fto_contacts_fragment_add_contact_button);

          mSearchViewCloseButton.setOnClickListener(this);
          mAddContactButton.setOnClickListener(this);
          
          mSearchView = (EditText) getView().findViewById(R.id.bird_search_view);
          mSearchView.addTextChangedListener(mPhoneSearchQueryTextListener);

          final String hintText = getString(R.string.dialer_hint_find_contact);

          // The following code is used to insert an icon into a CharSequence (copied from
          // SearchView)
          final SpannableStringBuilder ssb = new SpannableStringBuilder("   "); // for the icon
          ssb.append(hintText);
          final Drawable searchIcon = getResources().getDrawable(R.drawable.bird_fto_ic_search);
          final int textSize = (int) (mSearchView.getTextSize() * 1.20);
          searchIcon.setBounds(0, 0, textSize, textSize);
          ssb.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
          mSearchView.setHint(ssb);
      }
  	@Override
  	public void onClick(View v) {
  		// TODO Auto-generated method stub
  		switch (v.getId()) {
  		case R.id.bird_search_close_button:
  			mSearchViewCloseButton.setVisibility(View.GONE);
          	mSearchView.setText("");
  			break;
  		case R.id.bird_fto_contacts_fragment_add_contact_button:
  			getActivity().startActivity(new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI));
  			break;
  		default:
  			break;
  		}
  	}
  	
  	/**
       * Listener used to send search queries to the phone search fragment.
       */
      private final TextWatcher mPhoneSearchQueryTextListener = new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {
          }

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
              final String newText = s.toString();
              if (newText.equals(mSearchQuery)) {
                  // If the query hasn't changed (perhaps due to activity being
                  // destroyed
                  // and restored, or user launching the same DIAL intent twice),
                  // then there is
                  // no need to do anything here.
                  return;
              }
              mSearchQuery = newText;
              if(!TextUtils.isEmpty(mSearchQuery)){
              	 setQueryString(mSearchQuery, false);
              	 mSearchViewCloseButton.setVisibility(View.VISIBLE);
              }else{
              	mSearchViewCloseButton.setVisibility(View.GONE);
              	setQueryString("", false);
              }
             
          }

          @Override
          public void afterTextChanged(Editable s) {
          }
      };
  	
      public boolean onBackPressed(){
      	if(mSearchViewCloseButton == null)
      		return true;
      	if(mSearchViewCloseButton.getVisibility()==View.VISIBLE){
      		mSearchViewCloseButton.setVisibility(View.GONE);
          	mSearchView.setText("");
          	return false;
      	}
      	
      	return true;
      }
      @Override
      public boolean showBladeView(){
      	return byFTO;
      }
      @Override
      public boolean showChineseFamilyName(){
      	//return BirdFeature.BIRD_DIALER_FTO;
      	return byFTO;
      }
      @Override
      public boolean isWhiteTheme(){
      	return byFTO; 
      }
    
	//BUG #4268,frequently can not show fto style(with title on top),chengting,@20150423,begin
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);		
		if(BirdFeature.BIRD_DIALER_FTO){
			byFTO =  BirdContactsCommonUtils.isBirdDialerActivity(activity);		
		}
	}
	//BUG #4268,frequently can not show fto style(with title on top),chengting,@20150423,end
	
	//BUG #4249,chengting,@20150512,begin
    private TextView mCounterHeaderView;
    
	@Override
    protected void showCount(int partitionIndex, Cursor data) { 
		if(!byFTO || mCounterHeaderView==null){
			Log.d("cting","show count, return!!");
			return;
		}
		Log.d("cting","show count");
        /*
         * Bug Fix by Mediatek Begin.
         *   Original Android's code:
         *     xxx
         *   CR ID: ALPS00279111
         *   Descriptions: 
         */
        Log.i(TAG, "showCount is called");
        mWaitCursorView.stopWaitCursor();
        /*
         * Bug Fix by Mediatek End.
         */
//        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
        if (!isSearchMode() && data != null) {
            int count = data.getCount();
            Log.i(TAG, "showCount count is : " + count);
            if (count != 0) {
                count -= (mUserProfileExists ? 1 : 0);
                String format = getResources().getQuantityText(
                        R.plurals.listTotalAllContacts, count).toString();
                // Do not count the user profile in the contacts count
                if (mUserProfileExists) {
                    getAdapter().setContactsCount(String.format(format, count));
                } else {
                    mCounterHeaderView.setText(String.format(format, count));
                }
            } else {
                ContactListFilter filter = getFilter();
                int filterType = filter != null ? filter.filterType
                        : ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS;
                switch (filterType) {
                    case ContactListFilter.FILTER_TYPE_ACCOUNT:
                        String accountName;
                        if (AccountType.ACCOUNT_NAME_LOCAL_PHONE.equals(filter.accountName)) {
                            accountName = getString(R.string.account_phone_only);
                            /** M: ALPS913966 cache displayname in account filter and  push to intent @{ */
                            if (accountName == null && filter.displayName != null) {
                                accountName = filter.displayName;
                            }
                            /**@}*/
                        } else {
                        	accountName = filter.displayName;
//                            accountName = AccountType.getDisplayLabel(filter.accountName);
                            /** M: ALPS913966 cache displayname in account filter and  push to intent @{ */
//                            if (accountName == null && filter.displayName != null) {
//                                accountName = filter.displayName;
//                            }
                            /**@}*/
                        }

                        //M: TODO In case hot swap happened, the account name may null ALPS01472789
                        if (accountName == null) {
                            Log.d(TAG,"Need to check since account name should not be null");
                            mCounterHeaderView.setText(getString(R.string.listFoundAllContactsZero));    
                        } else {
                            mCounterHeaderView.setText(getString(
                                R.string.listTotalAllContactsZeroGroup, accountName));    
                        }
                        
                        /*
                         * Bug Fix by Mediatek Begin.
                         *   Original Android's code:
                         *     xxx
                         *   CR ID: ALPS00382262
                         *   Descriptions: 
                         */
                        updateFilterHeaderView();
                        /*
                         * Bug Fix by Mediatek End.
                         */
                        break;
                    case ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY:
                        mCounterHeaderView.setText(R.string.listTotalPhoneContactsZero);
                        break;
                    case ContactListFilter.FILTER_TYPE_STARRED:
                        mCounterHeaderView.setText(R.string.listTotalAllContactsZeroStarred);
                        break;
                    case ContactListFilter.FILTER_TYPE_CUSTOM:
                        mCounterHeaderView.setText(R.string.listTotalAllContactsZeroCustom);
                        break;
                    default:
                        mCounterHeaderView.setText(R.string.listTotalAllContactsZero);
                        break;
                }
//                setSectionHeaderDisplayEnabled(false);
                setVisibleScrollbarEnabled(false);
            }
        } else {
            ContactListAdapter adapter = getAdapter();
            if (adapter == null) {
                return;
            }

            // In search mode we only display the header if there is nothing found
            if (TextUtils.isEmpty(getQueryString()) || !adapter.areAllPartitionsEmpty()) {
                mSearchHeaderView.setVisibility(View.GONE);
                showSearchProgress(false);
            } else {
                mSearchHeaderView.setVisibility(View.VISIBLE);
                if (adapter.isLoading()) {
                    mSearchProgressText.setText(R.string.search_results_searching);
                    showSearchProgress(true);
                } else {
                    mSearchProgressText.setText(R.string.listFoundAllContactsZero);
                    mSearchProgressText.sendAccessibilityEvent(
                            AccessibilityEvent.TYPE_VIEW_SELECTED);
                    showSearchProgress(false);
                }
            }
            showEmptyUserProfile(false);
        }
    }
	//BUG #4249,chengting,@20150512,end
  	//add by sunjunwei for fto 20150303 end
}
