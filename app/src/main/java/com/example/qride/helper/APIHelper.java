package com.example.qride.helper;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.qride.sqlite.UserDAO;

public class APIHelper {
    //public static final String BASE_URL = "http://192.168.100.134:3000/api/";
    //public static final String BASE_URL = "http://10.242.93.214:3000/api/";
    public static final String BASE_URL = "http://192.168.1.9:3000/api/";
    public static final String LOGIN = BASE_URL + "login";
    public static final String USER = BASE_URL + "user";
    public static final String VEHICLE = BASE_URL + "vehicle/";
    public static final String REGISTER = BASE_URL + "register";
    public static final String CHANGE_PHONE = BASE_URL + "change-phone";
    public static final String RESET_PASSWORD = BASE_URL + "reset-password";
    public static final String CHECK_PHONE = BASE_URL + "check-phone";
    public static final String UPDATE = BASE_URL + "user/update";
    public static final String WALLET_TOPUP = BASE_URL + "wallet/topup";
    public static final String WALLET_WITHDRAW = BASE_URL + "wallet/withdraw";
    public static final String LOAD_WALLET = BASE_URL + "wallet";

    public static final String WALLET_HISTORY = BASE_URL + "wallet/history";
    public static final String TRANSACTION_DETAIL = BASE_URL + "transaction/";

    public static String getToken(Context context) {
        UserDAO dao = new UserDAO(context);
        return dao.getToken();
    }

}
