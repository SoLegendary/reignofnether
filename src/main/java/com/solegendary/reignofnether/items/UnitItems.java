package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.items.unititems.MerchantChestplate;
import com.solegendary.reignofnether.items.unititems.MerchantGoldenApple;
import com.solegendary.reignofnether.items.unititems.MerchantSword;
import com.solegendary.reignofnether.items.unititems.MerchantTrident;

import java.util.List;

public class UnitItems {

    public static final MerchantTrident MERCHANT_TRIDENT = new MerchantTrident();
    public static final MerchantSword MERCHANT_SWORD = new MerchantSword();
    public static final MerchantGoldenApple MERCHANT_GOLDEN_APPLE = new MerchantGoldenApple();
    public static final MerchantChestplate MERCHANT_CHESTPLATE = new MerchantChestplate();

    public static final List<UnitItem> ITEMS = List.of(
        MERCHANT_TRIDENT,
        MERCHANT_SWORD,
        MERCHANT_GOLDEN_APPLE,
        MERCHANT_CHESTPLATE
    );
}
