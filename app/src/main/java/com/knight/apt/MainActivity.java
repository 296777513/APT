package com.knight.apt;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.knight.aptlib.AptCreate;

/**
 * description
 *
 * @author liyachao
 * @date 2018/1/29
 */
@AptCreate
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
