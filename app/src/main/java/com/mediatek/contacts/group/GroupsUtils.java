package com.mediatek.contacts.group;

import java.util.ArrayList;
import java.util.HashMap;

import android.database.Cursor;

import com.android.contacts.GroupListLoader;
import com.mediatek.contacts.util.LogUtils;
import com.google.common.base.Objects;

public class GroupsUtils {


    private static final String TAG = "GroupsUtils";
    /// M: We need get group numbers for Move to other group feature.@{
    /**
     * Get group numbers for every account.
     */
    public static void initGroupsByAllAccount(Cursor mCursor, ArrayList<String> mAccountNameList,
            HashMap<String, Integer> mAccountGroupMembers) {
        if (mCursor == null || mCursor.getCount() == 0) {
            return;
        }
        LogUtils.d(TAG, "initAccountGroupMemberCount start");

        mCursor.moveToPosition(-1);
        int accountNum = getAllAccoutNums(mCursor, mAccountNameList);
        LogUtils.d(TAG, "group count:" + accountNum);
        int groups = 0;
        for (int index = 0; index < accountNum; index++) {
            groups = getGroupNumsByAccountName(mCursor, mAccountNameList.get(index));
            mAccountGroupMembers.put(mAccountNameList.get(index), groups);
        }
    }
    /**
     * Get groups by specified account name.
     * @param name, account name.
     * @return
     */
    public static int getGroupNumsByAccountName(Cursor mCursor, String name) {
        int count = 0;
        int index = 0;
        while (mCursor.moveToPosition(index)) {
            String accountName = mCursor.getString(GroupListLoader.ACCOUNT_NAME);
            if (accountName.equals(name)) {
                count++;
            }
            index++;
        }
        return count;
    }
    /**
     * Get all account numbers.
     * @return
     */
    public static int getAllAccoutNums(Cursor mCursor, ArrayList<String> mAccountNameList) {
        int pos = 0;
        int count = 0;
        mAccountNameList.clear();
        while (mCursor.moveToPosition(pos)) {
            String accountName = mCursor.getString(GroupListLoader.ACCOUNT_NAME);
            String accountType = mCursor.getString(GroupListLoader.ACCOUNT_TYPE);
            String dataSet = mCursor.getString(GroupListLoader.DATA_SET);
            int previousIndex = pos - 1;
            if (previousIndex >= 0 && mCursor.moveToPosition(previousIndex)) {
                String previousGroupAccountName = mCursor.getString(GroupListLoader.ACCOUNT_NAME);
                String previousGroupAccountType = mCursor.getString(GroupListLoader.ACCOUNT_TYPE);
                String previousGroupDataSet = mCursor.getString(GroupListLoader.DATA_SET);

                if (!(accountName.equals(previousGroupAccountName) && accountType.equals(previousGroupAccountType) && Objects
                        .equal(dataSet, previousGroupDataSet))) {
                    count++;
                    mAccountNameList.add(accountName);
                }
            }
            else {
                mAccountNameList.add(accountName);
                count++;
            }
            pos++;
        }

        return count;
    }
  /// @}
}
