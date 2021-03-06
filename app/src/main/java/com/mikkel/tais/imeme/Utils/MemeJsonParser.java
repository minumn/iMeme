package com.mikkel.tais.imeme.Utils;

import com.mikkel.tais.imeme.Models.Meme;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MemeJsonParser {
    public static List<Meme> parseMemeJson(String jsonString) {
        List<Meme> memes = new ArrayList<>();

        try {
            JSONArray jsonArray = new JSONObject(jsonString).getJSONObject("data").getJSONArray("memes");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                String id = obj.getString("id");
                String name = obj.getString("name");

                memes.add(new Meme(id, name));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return memes;
    }
}