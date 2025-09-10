package com.shoto290.reignofalpha;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReignOfAlpha implements ModInitializer {
	public static final String MOD_ID = "reign-of-alpha";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Reign of Alpha mod loaded successfully!");
	}
}