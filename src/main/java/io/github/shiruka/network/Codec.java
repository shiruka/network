package io.github.shiruka.network;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * an interface that represents packet codecs.
 */
public interface Codec {

  /**
   * the decoders.
   */
  Int2ObjectOpenHashMap<Function<ByteBuf, ? extends Packet>> DECODERS = new Int2ObjectOpenHashMap<>();

  /**
   * the encoders.
   */
  Int2ObjectOpenHashMap<BiConsumer<? extends Packet, ByteBuf>> ENCODERS = new Int2ObjectOpenHashMap<>();

  /**
   * the framed packet ids.
   */
  IntOpenHashSet FRAMED_PACKET_IDS = new IntOpenHashSet();

  /**
   * the id from class.
   */
  Object2IntOpenHashMap<Class<?>> ID_FROM_CLASS = new Object2IntOpenHashMap<>();
}
