package com.example.qride.helper;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.qride.sqlite.UserDAO;

public class APIHelper {
    public static final String BASE_URL = "http://192.168.100.13:3000/api/";
    //public static final String BASE_URL = "http://192.168.100.13:3000/api/";
    //public static final String BASE_URL = "http://10.242.93.214:3000/api/";
    // public static final String BASE_URL = "http://192.168.1.9:3000/api/"; // IP tu GitHub
    //public static final String BASE_URL = "http://10.30.10.247:3000/api/";
    //public static final String BASE_URL = "http://192.168.1.195:3000/api/"; // IP tu GitHub
    //public static final String BASE_URL = "http://10.30.10.247:3000/api/";
    public static final String LOGIN = BASE_URL + "login";
    public static final String USER = BASE_URL + "user";
    public static final String VEHICLE = BASE_URL + "vehicle/";
    public static final String REGISTER = BASE_URL + "register";
    public static final String CHANGE_PHONE = BASE_URL + "change-phone";
    public static final String RESET_PASSWORD = BASE_URL + "reset-password";
    public static final String CHECK_PHONE = BASE_URL + "check-phone";
    public static final String UPDATE = BASE_URL + "user/update";
    public static final String CHANGE_PASSWORD = BASE_URL + "user/change-password";
    public static final String USER_STATS = BASE_URL + "user/stats";
    public static final String WALLET_TOPUP = BASE_URL + "wallet/topup";
    public static final String WALLET_WITHDRAW = BASE_URL + "wallet/withdraw";
    public static final String LOAD_WALLET = BASE_URL + "wallet";

    public static final String WALLET_HISTORY = BASE_URL + "wallet/history";
    public static final String TRANSACTION_DETAIL = BASE_URL + "transaction/";

    public static final String RIDES = BASE_URL + "rides";
    public static final String VOUCHERS = BASE_URL + "vouchers";
    public static final String UPDATE_VOUCHER_PROGRESS = BASE_URL + "vouchers/update-progress";
    public static final String ACTIVATE_VOUCHER = BASE_URL + "vouchers/activate";
    public static final String BUY_VOUCHER = BASE_URL + "vouchers/buy";
    public static final String PAYMENT_VIP_MOMO = BASE_URL + "payment/vip/momo";
    public static final String NOTIFICATIONS_LIST = BASE_URL + "notifications";
    public static final String CLIENT_LOG = BASE_URL + "client-log";
    public static final String CREATE_REVIEW = BASE_URL + "reviews";

    public static final String COMMUNITY_FEED = BASE_URL+"community/feed";

    public static final String CREATE_POST = BASE_URL+"community/posts";

    public static final String LIKE_POST = BASE_URL+"community/like";

    public static String getToken(Context context) {
        UserDAO dao = new UserDAO(context);
        return dao.getToken();
    }

}
