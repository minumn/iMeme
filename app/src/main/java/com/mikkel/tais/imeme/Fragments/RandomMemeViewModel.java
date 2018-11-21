package com.mikkel.tais.imeme.Fragments;

import android.arch.lifecycle.ViewModel;
import android.graphics.Bitmap;
import android.media.Image;

import com.mikkel.tais.imeme.R;

public class RandomMemeViewModel extends ViewModel {
    // TODO: Implement the ViewModel

    private Bitmap randomMeme;

    public Bitmap getRandomMeme() {
        if (randomMeme == null){
            randomMeme = loadRandomMemes();
        }

        return randomMeme;
    }

    private Bitmap loadRandomMemes(){
        // Get randomMeme from Service
        // TODO: Fix function. Dont return randomMeme
        return randomMeme; //R.mipmap.ic_launcher;
    }
}
