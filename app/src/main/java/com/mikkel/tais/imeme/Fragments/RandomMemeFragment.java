package com.mikkel.tais.imeme.Fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.mikkel.tais.imeme.R;

public class RandomMemeFragment extends Fragment {

    private RandomMemeViewModel mViewModel;
    private ImageView randomMemeImage;

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

        randomMemeImage = getActivity().findViewById(R.id.randomMemeImageView);

        // TODO: Apply randomMeme instead of launcherIcon
        //randomMemeImage.setImageBitmap(mViewModel.getRandomMeme());
        randomMemeImage.setImageResource(R.mipmap.ic_launcher);
    }



}
