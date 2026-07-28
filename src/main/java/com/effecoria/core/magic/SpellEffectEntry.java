package com.effecoria.core.magic;

import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;

public record SpellEffectEntry(ResourceLocation type, JsonObject params) {}
