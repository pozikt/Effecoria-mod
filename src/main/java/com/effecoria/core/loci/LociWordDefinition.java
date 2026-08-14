package com.effecoria.core.loci;

import com.effecoria.core.seal.SealWordKind;

import net.minecraft.resources.ResourceLocation;

/** Datapack Lex Loci lexicon entry ({@code data/<ns>/loci_words/*.json}). */
public record LociWordDefinition(ResourceLocation id, SealWordKind kind, String effect) {}
