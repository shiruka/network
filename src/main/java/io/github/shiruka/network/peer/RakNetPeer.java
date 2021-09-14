package io.github.shiruka.network.peer;

import io.github.shiruka.network.ConnectionType;
import io.github.shiruka.network.Constants;
import io.github.shiruka.network.maps.ConcurrentIntMap;
import io.github.shiruka.network.packets.message.EncapsulatedPacket;
import io.github.shiruka.network.packets.message.acknowledge.Record;
import io.netty.channel.Channel;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

/**
 * an abstract class that represents rak net peers.
 */
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class RakNetPeer {

  /**
   * the interval at which keep-alive detection packets are sent.
   */
  private static final long DETECTION_SEND_INTERVAL = 2500L;

  /**
   * the maximum amount of split packets can be in the split handle queue.
   */
  private static final int MAX_SPLITS_PER_QUEUE = 4;

  /**
   * the maximum amount of chunks a single encapsulated packet can be split into.
   */
  private static final int MAX_SPLIT_COUNT = 128;

  /**
   * the default amount of time in milliseconds it will take for the peer to timeout.
   */
  private static final long PEER_TIMEOUT = RakNetPeer.DETECTION_SEND_INTERVAL * 10;

  /**
   * the interval at which pings are sent.
   */
  private static final long PING_SEND_INTERVAL = 2500L;

  /**
   * the interval at which not acknowledged packets are automatically resent.
   */
  private static final long RECOVERY_SEND_INTERVAL = 500L;

  /**
   * the ack receipt packets.
   */
  private final ConcurrentHashMap<EncapsulatedPacket, Integer> ackReceiptPackets = new ConcurrentHashMap<>();

  /**
   * the address.
   */
  @NotNull
  private final InetSocketAddress address;

  /**
   * the channel.
   */
  @NotNull
  private final Channel channel;

  /**
   * the connection type.
   */
  @NotNull
  private final ConnectionType connectionType;

  /**
   * the guid.
   */
  private final long guid;

  /**
   * the handle queue.
   */
  private final ConcurrentIntMap<ConcurrentIntMap<EncapsulatedPacket>> handleQueue = new ConcurrentIntMap<>();

  /**
   * the highest latency
   */
  private final long highestLatency = -1;

  /**
   * the last latency.
   */
  private final long lastLatency = -1;

  /**
   * the latency.
   */
  private final long latency = -1;

  /**
   * the latency enabled.
   */
  private final boolean latencyEnabled = true;

  /**
   * the latency timestamps
   */
  private final List<Long> latencyTimestamps = new ArrayList<>();

  /**
   * the logger.
   */
  @NotNull
  private final Logger logger = LogManager.getLogger(
    "%s-%s".formatted(RakNetPeer.class.getSimpleName(), Long.toHexString(this.guid).toUpperCase(Locale.ROOT)));

  /**
   * the lowest latency.
   */
  private final long lowestLatency = -1;

  /**
   * the maximum transfer unit.
   */
  private final int maximumTransferUnit;

  /**
   * the order receive index.
   */
  private final int[] orderReceiveIndex = new int[Constants.CHANNEL_COUNT];

  /**
   * the order send index.
   */
  private final int[] orderSendIndex = new int[Constants.CHANNEL_COUNT];

  /**
   * the receive sequence number.
   */
  private final int receiveSequenceNumber = -1;

  /**
   * the recovery queue.
   */
  private final ConcurrentIntMap<EncapsulatedPacket[]> recoveryQueue = new ConcurrentIntMap<>();

  /**
   * the reliable packets.
   */
  private final ConcurrentMessageIndexList reliablePackets = new ConcurrentMessageIndexList();

  /**
   * the send queue.
   */
  private final ConcurrentLinkedQueue<EncapsulatedPacket> sendQueue = new ConcurrentLinkedQueue<>();

  /**
   * the sequence receive index
   */
  private final int[] sequenceReceiveIndex = new int[Constants.CHANNEL_COUNT];

  /**
   * the sequence send index.
   */
  private final int[] sequenceSendIndex = new int[Constants.CHANNEL_COUNT];

  /**
   * the split queue.
   */
  private final ConcurrentIntMap<EncapsulatedPacket.Split> splitQueue = new ConcurrentIntMap<>();

  /**
   * the last detection send time.
   */
  private long lastDetectionSendTime;

  /**
   * the last packet receive time
   */
  @Getter
  @Setter(AccessLevel.PRIVATE)
  private long lastPacketReceiveTime = System.currentTimeMillis();

  /**
   * the last packet send time.
   */
  private long lastPacketSendTime;

  /**
   * the last packet received this second reset time.
   */
  private long lastPacketsReceivedThisSecondResetTime;

  /**
   * the last packets sent this second reset time.
   */
  private long lastPacketsSentThisSecondResetTime;

  /**
   * the last ping send time.
   */
  private long lastPingSendTime;

  /**
   * the last recovery send time.
   */
  private long lastRecoverySendTime;

  /**
   * the message index.
   */
  private int messageIndex;

  /**
   * the packets received this second.
   */
  private int packetsReceivedThisSecond;

  /**
   * the packets sent this second.
   */
  private int packetsSentThisSecond;

  /**
   * the pongs received.
   */
  private int pongsReceived;

  /**
   * the send sequence number.
   */
  private int sendSequenceNumber;

  /**
   * the split id.
   */
  private int splitId;

  /**
   * the state.
   */
  @Getter
  @Setter
  private RakNetState state = RakNetState.CONNECTED;

  /**
   * the timeout
   */
  @Getter
  private long timeout = RakNetPeer.PEER_TIMEOUT;

  /**
   * the total latency.
   */
  private long totalLatency;

  {
    for (var index = 0; index < Constants.CHANNEL_COUNT; index++) {
      this.sequenceReceiveIndex[index] = -1;
      this.handleQueue.put(index, new ConcurrentIntMap<>());
    }
  }

  /**
   * returns the message index and bumps it.
   *
   * @return the message index.
   */
  public final int bumpMessageIndex() {
    this.logger.trace("Bumped message index from {} to {}", this.messageIndex, this.messageIndex + 1);
    return this.messageIndex++;
  }

  /**
   * returns whether or not the peer has timed out.
   *
   * @return {@code true} if the peer has timed out, {@code false} otherwise.
   */
  public final boolean hasTimedOut() {
    return System.currentTimeMillis() - this.lastPacketReceiveTime >= this.timeout;
  }

  /**
   * obtains the inet address.
   *
   * @return inet address.
   */
  @NotNull
  public final InetAddress inetAddress() {
    return this.address.getAddress();
  }

  /**
   * returns whether or not the peer is connected.
   *
   * @return {@code true} if the peer is connected, {@code false} otherwise.
   */
  public final boolean isConnected() {
    return this.state.isDerivative(RakNetState.CONNECTED);
  }

  /**
   * returns whether or not the peer is disconnected.
   *
   * @return {@code true} if the peer is disconnected, {@code false} otherwise.
   */
  public final boolean isDisconnected() {
    return this.state == RakNetState.DISCONNECTED;
  }

  /**
   * returns whether or not the peer is handshaking.
   *
   * @return {@code true} if the peer is handshaking, {@code false} otherwise.
   */
  public final boolean isHandshaking() {
    return this.state.isDerivative(RakNetState.HANDSHAKING);
  }

  /**
   * returns whether or not the peer is logged in.
   *
   * @return {@code true} if the peer is logged in, {@code false} otherwise.
   */
  public final boolean isLoggedIn() {
    return this.state.isDerivative(RakNetState.LOGGED_IN);
  }

  /**
   * obtains the port.
   *
   * @return port.
   */
  public final int port() {
    return this.address.getPort();
  }

  /**
   * sets the amount of time in milliseconds it will take for the peer to not respond in order for it to timeout.
   *
   * @param timeout the timeout to set.
   */
  public final void timeout(@Range(from = 0, to = Long.MAX_VALUE) final long timeout) {
    this.timeout = timeout;
  }

  /**
   * returns the peer's timestamp.
   *
   * @return the peer's timestamp.
   */
  public abstract long timestamp();

  /**
   * used to store the message index for received reliable packets in a condensed fashion.
   */
  private static final class ConcurrentMessageIndexList {

    /**
     * the indexes.
     */
    @NotNull
    private final ArrayList<Record> indexes = new ArrayList<>();

    /**
     * adds the specified message index to the list.
     *
     * @param index the index to add.
     */
    public synchronized void add(final int index) {
      this.indexes.add(new Record(index));
      final var condensed = Record.condense(this.indexes);
      this.indexes.clear();
      this.indexes.addAll(Arrays.asList(condensed));
    }

    /**
     * returns whether or not the list contains the specified message index.
     *
     * @param index the index to check.
     *
     * @return {@code true} if the list contains the {@code index}.
     */
    public synchronized boolean contains(final int index) {
      return this.indexes.stream()
        .anyMatch(record ->
          record.isRanged() &&
            record.index() >= index &&
            record.index() <= index || record.index() == index);
    }
  }
}
