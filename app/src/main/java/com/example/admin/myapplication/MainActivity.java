package com.example.admin.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.pingplusplus.android.Pingpp;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    /**
     *开发者需要填一个服务端URL 该URL是用来请求支付需要的charge。务必确保，URL能返回json格式的charge对象。
     *服务端生成charge 的方式可以参考ping++官方文档，地址 https://pingxx.com/guidance/server/import
     *
     *【 http://218.244.151.190/demo/charge 】是 ping++ 为了方便开发者体验 sdk 而提供的一个临时 url 。
     * 该 url 仅能调用【模拟支付控件】，开发者需要改为自己服务端的 url 。
     */
    private static String YOUR_URL ="http://218.244.151.190/demo/charge";
    public static final String CHARGE_URL = YOUR_URL;

    /**
     * 银联支付渠道
     */
    private static final String CHANNEL_UPACP = "upacp";
    /**
     * 微信支付渠道
     */
    private static final String CHANNEL_WECHAT = "wx";
    /**
     * 微信支付渠道
     */
    private static final String CHANNEL_QPAY = "qpay";
    /**
     * 支付支付渠道
     */
    private static final String CHANNEL_ALIPAY = "alipay";
    /**
     * 百度支付渠道
     */
    private static final String CHANNEL_BFB = "bfb";
    /**
     * 京东支付渠道
     */
    private static final String CHANNEL_JDPAY_WAP = "jdpay_wap";

    private EditText amountEditText;
    private Button mZhifubao;
    private Button mWeixin;
    private Button mBaidu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        amountEditText = (EditText) findViewById(R.id.amountEditText);
        mZhifubao = (Button) findViewById(R.id.zhifubao);
        mWeixin = (Button)findViewById(R.id.weixin);
        mBaidu = (Button)findViewById(R.id.baidu);

        initEvent();
    }

    private void initEvent() {
        mZhifubao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new PaymentTask().execute(new PaymentRequest(CHANNEL_ALIPAY, getMoney()));
            }
        });
        mWeixin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new PaymentTask().execute(new PaymentRequest(CHANNEL_WECHAT, getMoney()));
            }
        });
        mBaidu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new PaymentTask().execute(new PaymentRequest(CHANNEL_BFB, getMoney()));
            }
        });
    }

    public int getMoney(){
        String amountText = amountEditText.getText().toString();
        if (amountText.equals("")){
            return 0;
        }

        String replaceable = String.format("[%s, \\s.]", NumberFormat.getCurrencyInstance(Locale.CHINA).getCurrency().getSymbol(Locale.CHINA));
        String cleanString = amountText.toString().replaceAll(replaceable, "");
        int amount = Integer.valueOf(new BigDecimal(cleanString).toString());
        return amount;
    }

    class PaymentTask extends AsyncTask<PaymentRequest, Void, String> {

        @Override
        protected void onPreExecute() {
            //按键点击之后的禁用，防止重复点击
            mZhifubao.setEnabled(false);
            mWeixin.setEnabled(false);
            mBaidu.setEnabled(false);
        }

        @Override
        protected String doInBackground(PaymentRequest... pr) {

            PaymentRequest paymentRequest = pr[0];
            String data = null;
            try {
                JSONObject object = new JSONObject();
                object.put("channel", paymentRequest.channel);
                object.put("amount", paymentRequest.amount);
                String json = object.toString();
                //向Your Ping++ Server SDK请求数据
                data = postJson(CHARGE_URL, json);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return data;
        }

        /**
         * 获得服务端的charge，调用ping++ sdk。
         */
        @Override
        protected void onPostExecute(String data) {
            if(null == data){
                showMsg("请求出错", "请检查URL", "URL无法获取charge");
                return;
            }
            Log.d("charge", data);

            //除QQ钱包外，其他渠道调起支付方式：
            //参数一：Activity  当前调起支付的Activity
            //参数二：data  获取到的charge或order的JSON字符串
            Pingpp.createPayment(MainActivity.this, data);

            //QQ钱包调用方式
            //参数一：Activity  当前调起支付的Activity
            //参数二：data  获取到的charge或order的JSON字符串
            //参数三：“qwalletXXXXXXX”需与AndroidManifest.xml中的scheme值一致
            //Pingpp.createPayment(ClientSDKActivity.this, data, "qwalletXXXXXXX");
        }
    }

    /**
     * onActivityResult 获得支付结果，如果支付成功，服务器会收到ping++ 服务器发送的异步通知。
     * 最终支付成功根据异步通知为准
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mZhifubao.setEnabled(true);
        mWeixin.setEnabled(true);
        mBaidu.setEnabled(true);
        //支付页面返回处理
        if (requestCode == Pingpp.REQUEST_CODE_PAYMENT) {
            if (resultCode == Activity.RESULT_OK) {
                String result = data.getExtras().getString("pay_result");
                /* 处理返回值
                 * "success" - payment succeed
                 * "fail"    - payment failed
                 * "cancel"  - user canceld
                 * "invalid" - payment plugin not installed
                 */
                String errorMsg = data.getExtras().getString("error_msg"); // 错误信息
                String extraMsg = data.getExtras().getString("extra_msg"); // 错误信息
                showMsg(result, errorMsg, extraMsg);
            }
        }
    }

    public void showMsg(String title, String msg1, String msg2) {
        String str = title;
        if (null !=msg1 && msg1.length() != 0) {
            str += "\n" + msg1;
        }
        if (null !=msg2 && msg2.length() != 0) {
            str += "\n" + msg2;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(str);
        builder.setTitle("提示");
        builder.setPositiveButton("OK", null);
        builder.create().show();
    }

    /**
     * 获取charge
     * @param urlStr charge_url
     * @param json 获取charge的传参
     * @return charge
     * @throws IOException
     */
    private static String postJson(String urlStr, String json) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type","application/json");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.getOutputStream().write(json.getBytes());

        if(conn.getResponseCode() == 200) {
            BufferedReader
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
        return null;
    }

    class PaymentRequest {
        String channel;
        int amount;

        public PaymentRequest(String channel, int amount) {
            this.channel = channel;
            this.amount = amount;
        }
    }
}
