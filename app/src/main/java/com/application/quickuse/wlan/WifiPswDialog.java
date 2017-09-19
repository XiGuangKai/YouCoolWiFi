package com.application.quickuse.wlan;

/**
 * Created by guangkai on 16-10-28.
 */

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class WifiPswDialog extends Dialog {
    private Button cancelButton;
    private Button okButton;
    private EditText pswEdit;
    private OnCustomDialogListener customDialogListener;
    private TextView wifiDialogSSID;

    private String WiFi_SSID;

    public WifiPswDialog(Context context, OnCustomDialogListener customListener,String SSID) {
        super(context);
        customDialogListener = customListener;

        WiFi_SSID = SSID;
    }

    /**
     * 定义dialog的回调事件
     */
    public interface OnCustomDialogListener {
        void back(String str);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_dialog_psw);

        pswEdit = (EditText) findViewById(R.id.wifiDialogPsw);
        wifiDialogSSID = (TextView) findViewById(R.id.wifiDialogSSID);

        cancelButton = (Button) findViewById(R.id.wifiDialogCancel);
        okButton = (Button) findViewById(R.id.wifiDialogCertain);
        cancelButton.setOnClickListener(buttonDialogListener);
        okButton.setOnClickListener(buttonDialogListener);
        wifiDialogSSID.setText(WiFi_SSID);

    }

    private View.OnClickListener buttonDialogListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            // TODO Auto-generated method stub
            if(view.getId() == R.id.wifiDialogCancel){
                pswEdit = null;
                customDialogListener.back(null);
                cancel();//自动调用dismiss();
            }
            else{
                customDialogListener.back(pswEdit.getText().toString());
                dismiss();
            }
        }
    };

}
