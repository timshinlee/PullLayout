package com.timshinlee.pulllayout;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final PullLayout pullLayout = findViewById(R.id.pull_layout);
        pullLayout.setPullLayoutListener(new PullLayout.PullLayoutListener(){
            @Override
            public void onRefresh(final PullLayout layout) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        layout.finishRefresh();
                    }
                },3000);
            }
        });
    }
}
