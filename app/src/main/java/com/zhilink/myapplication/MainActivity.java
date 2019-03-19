package com.zhilink.myapplication;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * IP地址
     */
    private EditText mEtIp;
    /**
     * 端口
     */
    private EditText mEtPort;
    /**
     * SocketDemo
     */
    private EditText mEtMessage;
    /**
     * Hello World!
     */
    private TextView mTvMessage;
    /**
     * 连接
     */
    private Button mBtnConnect;
    /**
     * 发送
     */
    private Button mBtnSend;
    /**
     * 接收
     */
    private Button mBtnReceipt;
    /**
     * 断开
     */
    private Button mBtnUnConnect;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        initView();
        mEtIp.setText("192.168.12.216");
        mEtPort.setText("4197");
    }

    private void initView() {
        mEtIp = findViewById(R.id.et_ip);
        mEtPort = findViewById(R.id.et_port);
        mEtMessage = findViewById(R.id.et_message);
        mTvMessage = findViewById(R.id.tv_message);
        mEtIp = findViewById(R.id.et_ip);
        mEtIp.setOnClickListener(this);
        mEtPort = findViewById(R.id.et_port);
        mEtPort.setOnClickListener(this);
        mEtMessage = findViewById(R.id.et_message);
        mEtMessage.setOnClickListener(this);
        mBtnConnect = findViewById(R.id.btn_connect);
        mBtnConnect.setOnClickListener(this);
        mBtnSend = findViewById(R.id.btn_send);
        mBtnSend.setOnClickListener(this);
        mBtnReceipt = findViewById(R.id.btn_receipt);
        mBtnReceipt.setOnClickListener(this);
        mBtnUnConnect = findViewById(R.id.btn_un_connect);
        mBtnUnConnect.setOnClickListener(this);
        mTvMessage = findViewById(R.id.tv_message);
        mTvMessage.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        final SocketUtils instance = SocketUtils.getInstance();
        switch (v.getId()) {
            default:
                break;
            case R.id.btn_connect:
                instance.setHost(mEtIp.getText().toString().trim());
                instance.setPort(Integer.valueOf(mEtPort.getText().toString().trim()));
                instance.connection(new SocketUtils.SocketListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(mContext, R.string.connect_success, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed() {
                        Toast.makeText(mContext, R.string.connect_failed, Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case R.id.btn_send:
                instance.sendData(mEtMessage.getText().toString());
                break;
            case R.id.btn_receipt:
                instance.receiveMsg(new SocketUtils.ReceiveMsgListener() {
                    @Override
                    public void onSuccess(String msg) {
                        String s = mTvMessage.getText().toString();
                        mTvMessage.setText(s +  msg);
                    }

                    @Override
                    public void onFailed(int errorCode) {

                    }
                });
                break;
            case R.id.btn_un_connect:
                instance.close();
                break;
        }
    }
}
