package com.mikkel.tais.imeme.Fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.mikkel.tais.imeme.R;

// REF: Autogenerated with Android studio
public class BlankFragment extends Fragment {

    // #  # # Utility functions # # #
    private void initiateTestButton() {
        Button testButton = getActivity().findViewById(R.id.testBtn);
        if (testButton != null) {
            testButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getActivity(), "Button clicked!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // # # # onFunctions # # #
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_blank, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initiateTestButton();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
