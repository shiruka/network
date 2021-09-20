package io.github.shiruka.network.packets;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.options.RakNetMagic;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * an abstract class that represents connection replies.
 */
@Accessors(fluent = true)
public abstract class ConnectionReply implements Packet {

  /**
   * the needs security.
   */
  protected static final boolean NEEDS_SECURITY = false;

  /**
   * the magic.
   */
  @Nullable
  @Setter
  private RakNetMagic magic;

  /**
   * the mtu.
   */
  @Getter
  @Setter
  private int mtu;

  /**
   * the server id.
   */
  @Getter
  @Setter
  private long serverId;

  /**
   * ctor.
   */
  protected ConnectionReply() {
  }

  /**
   * ctor.
   *
   * @param magic the magic.
   * @param mtu the mtu.
   * @param serverId the server id.
   */
  protected ConnectionReply(@NotNull final RakNetMagic magic, final int mtu, final long serverId) {
    this.magic = magic;
    this.mtu = mtu;
    this.serverId = serverId;
  }

  /**
   * obtains the magic.
   *
   * @return magic.
   */
  @NotNull
  public final RakNetMagic magic() {
    return Objects.requireNonNull(this.magic, "magic");
  }
}
