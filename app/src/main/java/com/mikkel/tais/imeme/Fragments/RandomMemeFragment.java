package com.mikkel.tais.imeme.Fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.mikkel.tais.imeme.R;

public class RandomMemeFragment extends Fragment {

    private RandomMemeViewModel mViewModel;
    private ImageView randomMemeImage;
    private Button backBtn, refreshMemeBtn;

    private RequestQueue volleyQueue;

    public static RandomMemeFragment newInstance() {
        return new RandomMemeFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.random_meme_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(RandomMemeViewModel.class);
        // TODO: Use the ViewModel

        initiateVariables();
        setButtonFunctionality();

        getRandomMeme();
    }

    private void setButtonFunctionality() {
        backBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getActivity(), "Back button clicked!", Toast.LENGTH_SHORT).show();

                    }
                }
        );

        refreshMemeBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getActivity(), "Refresh meme clicked!", Toast.LENGTH_SHORT).show();
                        // TODO: Should prob use ViewModel?
                        getRandomMeme();
                    }
                }
        );
    }

    private void initiateVariables() {
        randomMemeImage = getActivity().findViewById(R.id.randomMemeImageView);
        volleyQueue = Volley.newRequestQueue(getContext());
        backBtn = getActivity().findViewById(R.id.backBtn);
        refreshMemeBtn = getActivity().findViewById(R.id.newMemeBtn);

        // Set default picture
        randomMemeImage.setImageResource(R.mipmap.ic_launcher);
    }

    public void getRandomMeme(){
        final String imageUrl = "https://belikebill.ga/billgen-API.php?default=1";
        // TODO: Should be in the service
        // REF: Inspiration found on https://android--examples.blogspot.com/2017/02/android-volley-image-request-example.html
        // TODO: Could look at above link how they save files.

        ImageRequest imageRequest = new ImageRequest(
                imageUrl,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        // TODO: Do something with response! Maybe save it locally and send a broadcast with link.
                        randomMemeImage.setImageBitmap(response);
                    }
                },
                0, // Image width
                0, // Image height
                ImageView.ScaleType.CENTER_CROP, // Image scale type
                Bitmap.Config.RGB_565, //Image decode configuration
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }
        );
        volleyQueue.add(imageRequest);
    }



}
