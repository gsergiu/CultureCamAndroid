package com.culturecam.culturecam.app.gui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.culturecam.culturecam.R;
import com.culturecam.culturecam.entities.ImageDetails;
import com.culturecam.culturecam.entities.ResultImage;
import com.culturecam.culturecam.entities.SearchResult;
import com.culturecam.culturecam.rest.CultureCamAPI;
import com.google.gson.stream.MalformedJsonException;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.culturecam.culturecam.app.gui.LoadViewActivity.IMAGE_ID;

public class ResultViewActivity extends AppCompatActivity {
    private static final String TAG = "ResultViewActivity";
    private SearchResult searchResult;
    private View previousItem;
    private boolean reloadDetails = true;
    private static CultureCamAPI cultureCamApi;

    @BindView(R.id.listView)
    public ListView listView;

    private TextView elementTitle;
    private TextView elementProvider;
    private TextView elementRights;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        searchResult = (SearchResult) intent.getSerializableExtra(LoadViewActivity.RESULT);
        String imageId = intent.getStringExtra(IMAGE_ID);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.addInterceptor(logging);
        OkHttpClient httpClient = httpClientBuilder.build();
        cultureCamApi = new Retrofit.Builder().baseUrl("http://www.culturecam.eu")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient).build().create(CultureCamAPI.class);

        ResultListAdapter adapter = new
                ResultListAdapter(this, searchResult.getItems(), imageId);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                if (view == null) {
                    return;
                }
                Log.d(TAG,"Element " + position + " clicked");
                View layout = view.findViewById(R.id.l_detailview);
                if(previousItem != null && previousItem != layout) {
                    previousItem.setVisibility(View.GONE);
                    reloadDetails = true;
                }
                if (reloadDetails) {
                    elementTitle = (TextView) view.findViewById(R.id.tv_title);
                    elementProvider = (TextView) view.findViewById(R.id.tv_provider);
                    elementRights = (TextView) view.findViewById(R.id.tv_rights);
                    elementTitle.setText("Fetching details from Server...");
                    elementProvider.setText("");
                    elementRights.setText("");

                    final ResultImage item = (ResultImage) parent.getItemAtPosition(position);

                    cultureCamApi.getDetails(item.getResourceId()).enqueue(new Callback<ImageDetails>() {
                        @Override
                        public void onResponse(Call<ImageDetails> call, Response<ImageDetails> response) {
                            if(!response.isSuccessful()) {
                                Log.e(TAG, "Fetch details not successful");
                                return;
                            }
                            ImageDetails details = response.body();
                            if(details == null) {
                                Log.e(TAG, "Server responded with null");
                                return;
                            }
                            if(details.getSuccess() == null || !details.getSuccess()) {
                                Log.e(TAG, "Server responded with no success");
                                return;
                            }
                            Log.i(TAG, "Fetched details for image " + details.getObject().getAbout());
                            elementTitle.setText(details.getObject().getTitle().get(0));
                            elementProvider.setText(details.getProvider());
                            elementRights.setText(details.getRights());
                        }

                        @Override
                        public void onFailure(Call<ImageDetails> call, Throwable t) {
                            if(t instanceof MalformedJsonException) {
                                Log.w(TAG, "Got a malformed JSON from Server");
                                elementTitle.setText("No details available");
                                return;
                            }
                            elementTitle.setText("Failed to fetch details from server");
                            Log.e(TAG, "Failed to fetch details", t);
                        }
                    });
                    reloadDetails = false;
                }
                if(layout.getVisibility() == View.GONE) {
                    layout.setVisibility(View.VISIBLE);
                } else {
                    layout.setVisibility(View.GONE);
                }
                previousItem = layout;
                listView.smoothScrollToPositionFromTop(position,0,500);
            }
        });
    }
}
