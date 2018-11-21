package com.mikkel.tais.imeme.Fragments;

import android.arch.lifecycle.ViewModel;
import android.media.Image;

public class RandomMemeViewModel extends ViewModel {
    // TODO: Implement the ViewModel

    private Image randomMeme;

    public Image getRandomMeme() {
        if (randomMeme == null){
            randomMeme = loadRandomMemes();
        }

        return randomMeme;
    }

    private Image loadRandomMemes(){
        // Get randomMeme from Service
        // TODO: Fix function. Dont return randomMeme
        return randomMeme;
    }
}
