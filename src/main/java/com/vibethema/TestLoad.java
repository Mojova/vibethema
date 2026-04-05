package com.vibethema;

import com.vibethema.model.CharacterData;
import com.vibethema.model.CharacterSaveState;
import com.vibethema.model.Attribute;
import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestLoad {
    public static void main(String[] args) throws Exception {
        String json = Files.readString(Paths.get("data_source/Tulentanssija kopio.vbtm"));
        CharacterSaveState state = new Gson().fromJson(json, CharacterSaveState.class);
        CharacterData data = new CharacterData();
        data.importState(state);
        System.out.println("CHARISMA: " + data.getAttribute(Attribute.CHARISMA).get());
        System.out.println("MANIPULATION: " + data.getAttribute(Attribute.MANIPULATION).get());
        System.out.println("APPEARANCE: " + data.getAttribute(Attribute.APPEARANCE).get());
        
        System.out.println("STRENGTH: " + data.getAttribute(Attribute.STRENGTH).get());
        System.out.println("DEXTERITY: " + data.getAttribute(Attribute.DEXTERITY).get());
        System.out.println("STAMINA: " + data.getAttribute(Attribute.STAMINA).get());
    }
}
