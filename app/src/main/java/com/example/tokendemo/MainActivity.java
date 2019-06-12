package com.example.tokendemo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.HttpClient;
import com.braintreepayments.api.models.PaymentMethodNonce;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1234;
    String API_GET_TOKEN = "http://10.0.2.2/braintree/main.php";
    String API_CHECK_OUT = "http://10.0.2.2/braintree/checkout.php";

    String token, amount;
    HashMap<String, String> paramshash;
    Button buttonPay;
    EditText amountEditText;
    LinearLayout group_waiting, group_payment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        buttonPay = findViewById(R.id.pay_button);
        amountEditText = findViewById(R.id.amount_edit_text);

        group_payment = findViewById(R.id.group_payment);
        group_waiting = findViewById(R.id.group_waiting);

        new getToken().execute();

        buttonPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitPayment();

            }
        });
    }

    private void submitPayment() {

        DropInRequest dropInRequest = new DropInRequest().clientToken(token);
        startActivityForResult(dropInRequest.getIntent(this), REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == REQUEST_CODE){

            if (resultCode == RESULT_OK){

                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                PaymentMethodNonce nonce = result.getPaymentMethodNonce();
                String strNonce = nonce.getNonce();

                if (!amountEditText.getText().toString().isEmpty()){

                    amount = amountEditText.getText().toString().trim();
                    paramshash = new HashMap<>();
                    paramshash.put("amount", amount);
                    paramshash.put("nonce", strNonce);

                    sendPayments();
                }else{
                    Toast.makeText(this, "Please Enter A Valid Amount", Toast.LENGTH_SHORT).show();
                }

            }else if (resultCode == RESULT_CANCELED)

                Toast.makeText(this, "User Cancel", Toast.LENGTH_SHORT).show();

            else{
                Exception error = (Exception)data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
                Log.d("EDMT_ERROR", error.toString());
            }
        }
    }

    private void sendPayments() {
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, API_CHECK_OUT,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        if (response.toString().contains("successful"))
                            Toast.makeText(MainActivity.this, "Transaction Successful", Toast.LENGTH_SHORT).show();
                        else {
                            Toast.makeText(MainActivity.this, "Transaction Failed", Toast.LENGTH_SHORT).show();

                        }
                        Log.d("EDMT_LOG", response);


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("EDMT_ERROR", error.toString());

            }
        })

        {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                if (paramshash == null)
                    return null;

                Map<String, String> params = new HashMap<>();
                for (String key:params.keySet()){
                    params.put(key, paramshash.get(key));
                }

                return params;


            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {

                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }
        } ;

        requestQueue.add(stringRequest);

    }

    private class getToken extends AsyncTask{

        ProgressBar progressBar;
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(MainActivity.this, android.R.style.Theme_DeviceDefault_Dialog);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Please Wait....");
            progressDialog.show();

        }

        @Override
        protected Object doInBackground(Object[] objects) {
            HttpClient client =  new HttpClient();
            client.get(API_GET_TOKEN, new HttpResponseCallback() {
                @Override
                public void success(final String responseBody) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            group_waiting.setVisibility(View.GONE);
                            group_payment.setVisibility(View.VISIBLE);

                            token = responseBody;



                        }
                    });

                }

                @Override
                public void failure(Exception exception) {

                    Log.d("EDMT_ERROR", exception.toString());

                }
            });

            return null;
        }
    }
}
