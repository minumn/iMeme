package com.mikkel.tais.imeme.Fragments;

import android.arch.lifecycle.ViewModel;
import android.content.BroadcastReceiver;
import android.graphics.Bitmap;

public class RandomMemeViewModel extends ViewModel {
    // TODO: Implement the ViewModel
    // https://developer.android.com/topic/libraries/architecture/viewmodel
    private Bitmap randomMeme;

    public Bitmap getRandomMeme() {
        if (randomMeme == null){
            randomMeme = loadRandomMemes();
        }

        return randomMeme;
    }

    private Bitmap loadRandomMemes(){
        // Get randomMeme from Service
        // TODO: Fix function. Dont return randomMeme.

        return randomMeme; //R.mipmap.ic_launcher;
    }

    private void setupBroadcastReciever(){

    }
}

