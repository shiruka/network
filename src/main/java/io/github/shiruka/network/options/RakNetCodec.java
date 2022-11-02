package io.github.shiruka.network.options;

import com.google.common.base.Preconditions;
import io.github.shiruka.network.Ids;
import io.github.shiruka.network.Packet;
import io.github.shiruka.network.PacketBuffer;
import io.github.shiruka.network.packets.Ack;
import io.github.shiruka.network.packets.AlreadyConnected;
import io.github.shiruka.network.packets.ClientDisconnect;
import io.github.shiruka.network.packets.ClientHandshake;
import io.github.shiruka.network.packets.ConnectedPing;
import io.github.shiruka.network.packets.ConnectedPong;
import io.github.shiruka.network.packets.ConnectionBanned;
import io.github.shiruka.network.packets.ConnectionFailed;
import io.github.shiruka.network.packets.ConnectionReply1;
import io.github.shiruka.network.packets.ConnectionReply2;
import io.github.shiruka.network.packets.ConnectionRequest;
import io.github.shiruka.network.packets.ConnectionRequest1;
import io.github.shiruka.network.packets.ConnectionRequest2;
import io.github.shiruka.network.packets.Frame;
import io.github.shiruka.network.packets.FramedPacket;
import io.github.shiruka.network.packets.InvalidVersion;
import io.github.shiruka.network.packets.Nack;
import io.github.shiruka.network.packets.NoFreeConnections;
import io.github.shiruka.network.packets.ServerHandshake;
import io.github.shiruka.network.packets.UnconnectedPing;
import io.github.shiruka.network.packets.UnconnectedPong;
import io.netty.buffer.ByteBufAllocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

/**
 * an interface that represents packet codecs.
 */
public interface RakNetCodec {
  /**
   * obtains the simple codec.
   *
   * @return simple codec.
   */
  @NotNull
  static RakNetCodec simple() {
    return Impl.INSTANCE;
  }

  /**
   * decodes the packet.
   *
   * @param buffer the buffer to decode.
   *
   * @return packet.
   */
  @NotNull
  Packet decode(@NotNull PacketBuffer buffer);

  /**
   * decodes the data.
   *
   * @param data the data to decode.
   *
   * @return framed packet.
   */
  @NotNull
  FramedPacket decode(@NotNull Frame.Data data);

  /**
   * encodes the framed packet.
   *
   * @param packet the packet to encode.
   * @param allocator the allocator to encode.
   *
   * @return frame data.
   */
  @NotNull
  Frame.Data encode(
    @NotNull FramedPacket packet,
    @NotNull ByteBufAllocator allocator
  );

  /**
   * encodes the packet.
   *
   * @param packet the packet to encode.
   * @param buffer the buffer to encode.
   */
  void encode(@NotNull Packet packet, @NotNull PacketBuffer buffer);

  /**
   * gets the packet id from packet class type.
   *
   * @param type the type to get.
   *
   * @return packet id.
   */
  int packetIdFor(@NotNull Class<? extends Packet> type);

  /**
   * the produce packet.
   *
   * @param packet the packet to produce.
   * @param allocator the allocator to produce.
   *
   * @return produce encoded packet.
   */
  @NotNull
  PacketBuffer produceEncoded(
    @NotNull Packet packet,
    @NotNull ByteBufAllocator allocator
  );

  /**
   * a simple implementation of {@link RakNetCodec}.
   */
  final class Impl implements RakNetCodec {

    /**
     * the instance.
     */
    private static final RakNetCodec INSTANCE = new Impl();

    /**
     * the decoders.
     */
    private final Int2ObjectMap<Function<PacketBuffer, ? extends Packet>> decoders = new Int2ObjectOpenHashMap<>();

    /**
     * the encoders.
     */
    private final Int2ObjectMap<BiConsumer<? extends Packet, PacketBuffer>> encoders = new Int2ObjectOpenHashMap<>();

    /**
     * the framed packet ids.
     */
    private final IntSet framedPacketIds = new IntOpenHashSet();

    /**
     * the id from class.
     */
    private final Object2IntMap<Class<?>> idFromClass = new Object2IntOpenHashMap<>();

    /**
     * ctor.
     */
    private Impl() {
      this.register(
          Ids.CONNECTED_PING,
          ConnectedPing.class,
          ConnectedPing::new
        );
      this.register(
          Ids.UNCONNECTED_PING,
          UnconnectedPing.class,
          UnconnectedPing::new
        );
      this.register(
          Ids.CONNECTED_PONG,
          ConnectedPong.class,
          ConnectedPong::new
        );
      this.register(
          Ids.CONNECTION_REQUEST_1,
          ConnectionRequest1.class,
          ConnectionRequest1::new
        );
      this.register(
          Ids.CONNECTION_REPLY_1,
          ConnectionReply1.class,
          ConnectionReply1::new
        );
      this.register(
          Ids.CONNECTION_REQUEST_2,
          ConnectionRequest2.class,
          ConnectionRequest2::new
        );
      this.register(
          Ids.CONNECTION_REPLY_2,
          ConnectionReply2.class,
          ConnectionReply2::new
        );
      this.register(
          Ids.CONNECTION_REQUEST,
          ConnectionRequest.class,
          ConnectionRequest::new
        );
      this.register(
          Ids.SERVER_HANDSHAKE,
          ServerHandshake.class,
          ServerHandshake::new
        );
      this.register(
          Ids.CONNECTION_FAILED,
          ConnectionFailed.class,
          ConnectionFailed::new
        );
      this.register(
          Ids.ALREADY_CONNECTED,
          AlreadyConnected.class,
          AlreadyConnected::new
        );
      this.register(
          Ids.CLIENT_HANDSHAKE,
          ClientHandshake.class,
          ClientHandshake::new
        );
      this.register(
          Ids.NO_FREE_CONNECTIONS,
          NoFreeConnections.class,
          NoFreeConnections::new
        );
      this.register(
          Ids.CLIENT_DISCONNECT,
          ClientDisconnect.class,
          ClientDisconnect::new
        );
      this.register(
          Ids.CONNECTION_BANNED,
          ConnectionBanned.class,
          ConnectionBanned::new
        );
      this.register(
          Ids.INVALID_VERSION,
          InvalidVersion.class,
          InvalidVersion::new
        );
      this.register(
          Ids.UNCONNECTED_PONG,
          UnconnectedPong.class,
          UnconnectedPong::new
        );
      for (
        var index = Ids.FRAME_DATA_START;
        index <= Ids.FRAME_DATA_END;
        index++
      ) {
        this.register(
            index,
            Frame.Set.class,
            Frame.Set::read,
            Frame.Set::write
          );
      }
      this.register(Ids.NACK, Nack.class, Nack::new);
      this.register(Ids.ACK, Ack.class, Ack::new);
      this.idFromClass.defaultReturnValue(-1);
    }

    /**
     * decodes simply.
     *
     * @param supplier the supplier to decode.
     * @param <T> type of the packet class.
     *
     * @return decode function.
     */
    @NotNull
    private static <T extends Packet> Function<PacketBuffer, T> decodeSimple(
      @NotNull final Supplier<T> supplier
    ) {
      return buf -> {
        final var packet = supplier.get();
        buf.skip(1);
        packet.decode(buf);
        return packet;
      };
    }

    /**
     * encodes simply.
     *
     * @param id the id to encode.
     * @param <T> type of the packet class.
     *
     * @return encode function.
     */
    @NotNull
    private static <T extends Packet> BiConsumer<T, PacketBuffer> encodeSimple(
      final int id
    ) {
      return (packet, buffer) -> {
        buffer.writeByte(id);
        packet.encode(buffer);
      };
    }

    @NotNull
    @Override
    public Packet decode(@NotNull final PacketBuffer buffer) {
      final var packetId = buffer.unsignedByte(buffer.readerIndex());
      final var decoder = this.decoders.get(packetId);
      Preconditions.checkArgument(
        decoder != null,
        "Unknown decoder for packet id %",
        packetId
      );
      return decoder.apply(buffer);
    }

    @NotNull
    @Override
    public FramedPacket decode(@NotNull final Frame.Data data) {
      final var packetId = data.packetId();
      final var decoder = this.decoders.get(packetId);
      if (decoder == null || !this.framedPacketIds.contains(packetId)) {
        return data.retain();
      }
      final var buffer = data.createData();
      try {
        return ((FramedPacket) decoder.apply(buffer)).reliability(
            data.reliability()
          )
          .orderChannel(data.orderChannel());
      } finally {
        buffer.release();
      }
    }

    @Override
    @NotNull
    public Frame.Data encode(
      @NotNull final FramedPacket packet,
      @NotNull final ByteBufAllocator allocator
    ) {
      if (packet instanceof Frame.Data data) {
        return data.retain();
      }
      final var out = new PacketBuffer(
        allocator.ioBuffer(packet.initialSizeHint())
      );
      try {
        this.encode(packet, out);
        return Frame.Data
          .read(out, out.remaining(), false)
          .reliability(packet.reliability())
          .orderChannel(packet.orderChannel());
      } finally {
        out.release();
      }
    }

    @Override
    public void encode(
      @NotNull final Packet packet,
      @NotNull final PacketBuffer buffer
    ) {
      Preconditions.checkArgument(
        this.idFromClass.containsKey(packet.getClass()),
        "Unknown encoder for %s!",
        packet.getClass()
      );
      final var packetId = this.packetIdFor(packet.getClass());
      //noinspection unchecked
      final var encoder = (BiConsumer<Packet, PacketBuffer>) this.encoders.get(
          packetId
        );
      encoder.accept(packet, buffer);
    }

    @Override
    public int packetIdFor(@NotNull final Class<? extends Packet> type) {
      return this.idFromClass.getInt(type);
    }

    @NotNull
    @Override
    public PacketBuffer produceEncoded(
      @NotNull final Packet packet,
      @NotNull final ByteBufAllocator allocator
    ) {
      if (packet instanceof Frame.Set set && set.roughSize() >= 128) {
        return set.produce(allocator);
      }
      final var buffer = new PacketBuffer(
        allocator.ioBuffer(packet.initialSizeHint())
      );
      try {
        this.encode(packet, buffer);
        return buffer.retain();
      } finally {
        buffer.release();
      }
    }

    /**
     * registers the packet.
     *
     * @param id the id to register.
     * @param cls the cls to register.
     * @param supplier the supplier to register.
     * @param <T> type of the packet class.
     */
    private <T extends Packet> void register(
      final int id,
      @NotNull final Class<T> cls,
      @NotNull final Supplier<T> supplier
    ) {
      this.register(
          id,
          cls,
          Impl.decodeSimple(supplier),
          Impl.encodeSimple(id)
        );
    }

    /**
     * registers the packet.
     *
     * @param id the id to register.
     * @param cls the cls to register.
     * @param decoder the decoder to register.
     * @param encoder the encoder to register.
     * @param <T> type of the packet class.
     */
    private <T extends Packet> void register(
      final int id,
      @NotNull final Class<? extends Packet> cls,
      @NotNull final Function<PacketBuffer, T> decoder,
      @NotNull final BiConsumer<T, PacketBuffer> encoder
    ) {
      this.idFromClass.put(cls, id);
      this.decoders.put(id, decoder);
      this.encoders.put(id, encoder);
      if (FramedPacket.class.isAssignableFrom(cls)) {
        this.framedPacketIds.add(id);
      }
    }
  }
}
