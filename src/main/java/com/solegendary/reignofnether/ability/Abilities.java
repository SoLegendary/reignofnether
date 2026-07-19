package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.List;

public class Abilities {
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
        List<Keybinding> keybindings = List.of(
                Keybindings.abilitySlot1,
                Keybindings.abilitySlot2,
                Keybindings.abilitySlot3,
                Keybindings.abilitySlot4,
                Keybindings.abilitySlot5,
                Keybindings.abilitySlot6
        );
        List<AbilityButton> buttons = new ArrayList<>();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            for (int i = 0; i < abilities.size(); i++) {
                Pair<Ability, Keybinding> ability = abilities.get(i);
                buttons.add(ability.getA().getButton(ability.getB() != null ? ability.getB() : keybindings.get(i) , placement));
            }
        }
        return buttons;
    }

    public List<Button> getButtons(Unit unit) {
        List<Keybinding> keybindings = List.of(
                Keybindings.abilitySlot1,
                Keybindings.abilitySlot2,
                Keybindings.abilitySlot3,
                Keybindings.abilitySlot4,
                Keybindings.abilitySlot5,
                Keybindings.abilitySlot6
        );
        List<Button> buttons = new ArrayList<>();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            for (int i = 0; i < abilities.size(); i++) {
                Pair<Ability, Keybinding> ability = abilities.get(i);
                buttons.add(ability.getA().getButton(ability.getB() != null ? ability.getB() : keybindings.get(i) , unit));
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
