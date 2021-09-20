package io.github.shiruka.network.options;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;

/**
 * an interface to determine rak net magic numbers.
 */
public interface RakNetMagic {

  /**
   * creates a simple rak net magic.
   *
   * @param buffer the buffer to create.
   *
   * @return magic.
   */
  @NotNull
  static RakNetMagic from(@NotNull final ByteBuf buffer) {
    final var magicData = new byte[16];
    buffer.readBytes(magicData);
    return RakNetMagic.from(magicData);
  }

  /**
   * creates a simple rak net magic.
   *
   * @param magic the magic to create.
   *
   * @return magic.
   */
  @NotNull
  static RakNetMagic from(final byte @NotNull [] magic) {
    return new Impl(magic);
  }

  /**
   * obtains the default rak net magic.
   *
   * @return default rak net magic.
   */
  @NotNull
  static RakNetMagic simple() {
    return Impl.INSTANCE;
  }

  /**
   * reads and checks the buffer.
   *
   * @param buffer the buffer.
   */
  void read(@NotNull ByteBuf buffer);

  /**
   * verifies the given magic.
   *
   * @param magic the magic to verify.
   */
  void verify(@NotNull RakNetMagic magic);

  /**
   * writes the magic into the buffer.
   *
   * @param buffer the buffer to write.
   */
  void write(@NotNull ByteBuf buffer);

  /**
   * a simple implementation of {@link RakNetMagic}.
   *
   * @param magic the magic.
   */
  record Impl(
    byte @NotNull [] magic
  ) implements RakNetMagic {

    /**
     * the magic.
     */
    private static final byte[] DEFAULT = new byte[]{
      (byte) 0x00, (byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0xfe, (byte) 0xfe,
      (byte) 0xfe, (byte) 0xfe, (byte) 0xfd, (byte) 0xfd, (byte) 0xfd, (byte) 0xfd,
      (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78};

    /**
     * the instance.
     */
    private static final RakNetMagic INSTANCE = RakNetMagic.from(Impl.DEFAULT);

    @Override
    public void read(@NotNull final ByteBuf buffer) {
      for (final var data : this.magic) {
        Preconditions.checkArgument(buffer.readByte() == data, "Incorrect RakNet magic value");
      }
    }

    @Override
    public void verify(@NotNull final RakNetMagic magic) {
      final var buffer = Unpooled.buffer(16);
      this.write(buffer);
      magic.read(buffer);
    }

    @Override
    public void write(@NotNull final ByteBuf buffer) {
      buffer.writeBytes(this.magic);
    }
  }
}
