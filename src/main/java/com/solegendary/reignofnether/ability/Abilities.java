package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Abilities {
    public static final List<Keybinding> ABILITY_KEYBINDS = List.of(
            Keybindings.keyQ,
            Keybindings.keyW,
            Keybindings.keyE,
            Keybindings.keyR,
            Keybindings.keyT,
            Keybindings.keyY
    );
    List<Pair<Ability, Keybinding>> abilities = new ArrayList<>();

    public Abilities() { }

    public Abilities(List<Pair<Ability, Keybinding>> abilities) {this.abilities = abilities;}

    public void add(Ability ability) {
        abilities.add(new Pair<>(ability, null));
    }

    public void add(Ability ability, Keybinding keybind) {
        abilities.add(new Pair<>(ability, keybind));
    }

    public List<AbilityButton> getButtons(BuildingPlacement placement) {
        List<AbilityButton> buttons = new ArrayList<>();
        //TODO Remove need for Minecraft
        if (FMLEnvironment.dist == Dist.CLIENT) {
            for (int i = 0; i < abilities.size(); i++) {
                Pair<Ability, Keybinding> ability = abilities.get(i);
                buttons.add(ability.getA().getButton(ability.getB() != null ? ability.getB() : ABILITY_KEYBINDS.get(i) , placement));
            }
        }
        return buttons;
    }

    public List<Button> getButtons(Unit unit) {
        List<Button> buttons = new ArrayList<>();
        //TODO Remove need for I18n
        if (FMLEnvironment.dist == Dist.CLIENT) {
            for (int i = 0; i < abilities.size(); i++) {
                Pair<Ability, Keybinding> ability = abilities.get(i);
                buttons.add(ability.getA().getButton(ability.getB() != null ? ability.getB() : ABILITY_KEYBINDS.get(i) , unit));
            }
        }
        return buttons;
    }

    public List<Ability> get() {
        var list = new ArrayList<Ability>();
        for (Pair<Ability, Keybinding> ability : abilities) {
            list.add(ability.getA());
        }
        return list;
    }

    public Ability getDefaultAutocast() {
        for (Pair<Ability, Keybinding> ability:abilities) {
            if (ability.getA().isDefaultAutocast())
                return ability.getA();
        }
        return null;
    }

    public Abilities clone() {
        return new Abilities(new ArrayList<>(abilities));
    }

    public boolean isEmpty() {
        return abilities.isEmpty();
    }
}
