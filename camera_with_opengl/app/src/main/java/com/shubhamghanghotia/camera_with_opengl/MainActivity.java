package com.shubhamghanghotia.camera_with_opengl;

import android.os.Bundle;
import android.view.TextureView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        textView = new TextView(this);

        setContentView(textView);

        textView.setText("Hello user welcome to this video");
        textView.setLineHeight("matchpare");


    }
}
