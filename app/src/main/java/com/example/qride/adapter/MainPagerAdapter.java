package com.example.qride.adapter;

import android.util.SparseArray;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.example.qride.fragment.QuetQRFragment;
import com.example.qride.fragment.TaiKhoanFragment;
import com.example.qride.fragment.ThanhToanFragment;
import com.example.qride.fragment.TramXeFragment;
import com.example.qride.fragment.UuDaiFragment;

public class MainPagerAdapter extends FragmentStatePagerAdapter {

    // Cache fragment để MainActivity có thể gọi lại
    private final SparseArray<Fragment> registeredFragments = new SparseArray<>();

    public MainPagerAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0: return new TramXeFragment();
            case 1: return new UuDaiFragment();
            case 2: return new QuetQRFragment();
            case 3: return new ThanhToanFragment();
            case 4: return new TaiKhoanFragment();
            default: return new TramXeFragment();
        }
    }

    @Override
    public int getCount() { return 5; }

    // Lưu fragment khi ViewPager tạo
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        registeredFragments.put(position, fragment);
        return fragment;
    }

    // Xóa khi ViewPager hủy
    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public Fragment getRegisteredFragment(int position) {
        return registeredFragments.get(position);
    }
}