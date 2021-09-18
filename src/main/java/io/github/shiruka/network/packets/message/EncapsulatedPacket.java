package io.github.shiruka.network.packets.message;

import io.github.shiruka.network.Packet;
import io.github.shiruka.network.peer.RakNetPeer;
import java.util.Arrays;

/**
 * a class that represents encapsulated packets.
 */
public final class EncapsulatedPacket implements Cloneable {

  /**
   * the minimum size of an encapsulated packet.
   */
  public static final int MINIMUM_SIZE = EncapsulatedPacket.size(null, false);

  private static final byte FLAG_RELIABILITY = (byte) 0b11100000;

  private static final int FLAG_RELIABILITY_INDEX = 5;

  private static final byte FLAG_SPLIT = (byte) 0b00010000;

  public Record ackRecord;

  public int messageIndex;

  public byte orderChannel;

  public int orderIndex;

  public Packet payload;

  public Reliability reliability;

  public boolean split;

  public int splitCount;

  public int splitId;

  public int splitIndex;

  private EncapsulatedPacket clone;

  private boolean isClone;

  public static int size(final Reliability reliability, final boolean split, final Packet payload) {
    int size = 3;
    if (reliability != null) {
      size += reliability.isReliable() ? 3 : 0;
      size += reliability.isOrdered() || reliability.isSequenced() ? 4 : 0;
    }
    size += split == true ? 10 : 0;
    size += payload != null ? payload.size() : 0;
    return size;
  }

  public static int size(final Reliability reliability, final boolean split) {
    return EncapsulatedPacket.size(reliability, split, null);
  }

  @Override
  public EncapsulatedPacket clone() throws CloneNotSupportedException {
    if (this.clone != null) {
      throw new CloneNotSupportedException("Encapsulated packets can only be cloned once");
    } else if (this.isClone == true) {
      throw new CloneNotSupportedException("Clones of encapsulated packets cannot be cloned");
    }
    this.clone = (EncapsulatedPacket) super.clone();
    this.clone.isClone = true;
    return this.clone;
  }

  @Override
  public String toString() {
    return "EncapsulatedPacket [isClone=" + this.isClone + ", ackRecord=" + this.ackRecord + ", reliability=" + this.reliability
      + ", split=" + this.split + ", messageIndex=" + this.messageIndex + ", orderIndex=" + this.orderIndex
      + ", orderChannel=" + this.orderChannel + ", splitCount=" + this.splitCount + ", splitId=" + this.splitId
      + ", splitIndex=" + this.splitIndex + ", calculateSize()=" + this.size() + "]";
  }

  public void decode(final Packet buffer) throws NullPointerException {
    if (buffer == null) {
      throw new NullPointerException("Buffer cannot be null");
    }
    final short flags = buffer.readUnsignedByte();
    this.reliability = Reliability.lookup((flags & EncapsulatedPacket.FLAG_RELIABILITY) >> EncapsulatedPacket.FLAG_RELIABILITY_INDEX);
    if (this.reliability == null) {
      throw new NullPointerException(
        "Failed to lookup reliability with ID " + ((flags & EncapsulatedPacket.FLAG_RELIABILITY) >> EncapsulatedPacket.FLAG_RELIABILITY_INDEX));
    }
    this.split = (flags & EncapsulatedPacket.FLAG_SPLIT) > 0;
    final int length = buffer.readUnsignedShort() / Byte.SIZE;
    if (this.reliability.isReliable()) {
      this.messageIndex = buffer.readTriadLE();
    }
    if (this.reliability.isOrdered() || this.reliability.isSequenced()) {
      this.orderIndex = buffer.readTriadLE();
      this.orderChannel = buffer.readByte();
    }
    if (this.split == true) {
      this.splitCount = buffer.readInt();
      this.splitId = buffer.readUnsignedShort();
      this.splitIndex = buffer.readInt();
    }
    this.payload = new Packet(buffer.read(length));
  }

  public void encode(final Packet buffer) throws NullPointerException, IllegalArgumentException {
    if (buffer == null) {
      throw new NullPointerException("Buffer cannot be null");
    } else if (this.reliability == null) {
      throw new NullPointerException("Reliability cannot be null");
    } else if (this.payload == null) {
      throw new NullPointerException("Payload cannot be null");
    }
    byte flags = 0x00;
    flags |= this.reliability.getId() << EncapsulatedPacket.FLAG_RELIABILITY_INDEX;
    flags |= this.split == true ? EncapsulatedPacket.FLAG_SPLIT : 0;
    buffer.writeByte(flags);
    buffer.writeUnsignedShort(this.payload.size() * Byte.SIZE);
    if (this.ackRecord == null && this.reliability.requiresAck()) {
      throw new NullPointerException("No ACK record set for encapsulated packet with reliability " + this.reliability);
    } else if (this.ackRecord != null) {
      if (this.ackRecord.isRanged()) {
        throw new IllegalArgumentException("ACK record cannot be ranged");
      }
    }
    if (this.reliability.isReliable()) {
      buffer.writeTriadLE(this.messageIndex);
    }
    if (this.reliability.isOrdered() || this.reliability.isSequenced()) {
      buffer.writeTriadLE(this.orderIndex);
      buffer.writeUnsignedByte(this.orderChannel);
    }
    if (this.split == true) {
      buffer.writeInt(this.splitCount);
      buffer.writeUnsignedShort(this.splitId);
      buffer.writeInt(this.splitIndex);
    }
    buffer.write(this.payload.array());
  }

  public EncapsulatedPacket getClone() throws RuntimeException {
    if (this.isClone == true) {
      return this;
    } else if (this.clone == null) {
      try {
        this.clone();
      } catch (final CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
    }
    return this.clone;
  }

  public boolean needsSplit(final RakNetPeer peer) throws NullPointerException {
    return Split.needsSplit(peer, this);
  }

  public int size() {
    return EncapsulatedPacket.size(this.reliability, this.split, this.payload);
  }

  public EncapsulatedPacket[] split(final RakNetPeer peer) throws IllegalStateException, NullPointerException {
    if (this.split == true) {
      throw new IllegalStateException("Already split");
    } else if (!this.needsSplit(peer)) {
      throw new IllegalStateException("Too small to be split");
    }
    return Split.split(peer, this);
  }

  public static final class Split {

    private final IntMap<Packet> payloads;

    private final Reliability reliability;

    private final int splitCount;

    private final int splitId;

    public Split(final int splitId, final int splitCount, final Reliability reliability) {
      if (splitId < 0) {
        throw new IllegalArgumentException("Split ID cannot be negative");
      } else if (splitCount > RakNetPeer.MAX_SPLIT_COUNT) {
        throw new IllegalArgumentException("Split count can be no greater than " + RakNetPeer.MAX_SPLIT_COUNT);
      } else if (reliability == null) {
        throw new NullPointerException("Reliability cannot be null");
      }
      this.splitId = splitId;
      this.splitCount = splitCount;
      this.reliability = reliability;
      this.payloads = new IntMap<Packet>();
    }

    public static boolean needsSplit(final RakNetPeer peer, final EncapsulatedPacket encapsulated)
      throws NullPointerException, IllegalArgumentException {
      if (peer == null) {
        throw new NullPointerException("Peer cannot be null");
      } else if (encapsulated == null) {
        throw new NullPointerException("Encapsulated packet cannot be null");
      } else if (encapsulated.split == true) {
        throw new IllegalArgumentException("Encapsulated packet is already split");
      }
      return CustomPacket.MINIMUM_SIZE + encapsulated.size() > peer.getMaximumTransferUnit();
    }

    public static EncapsulatedPacket[] split(final RakNetPeer peer, final EncapsulatedPacket encapsulated)
      throws NullPointerException, IllegalArgumentException {
      if (peer == null) {
        throw new NullPointerException("Peer cannot be null");
      } else if (encapsulated == null) {
        throw new NullPointerException("Encapsulated packet cannot be null");
      } else if (encapsulated.split == true) {
        throw new NullPointerException("Encapsulated packet is already split");
      } else if (!Split.needsSplit(peer, encapsulated)) {
        throw new IllegalArgumentException("Encapsulated packet is too small to be split");
      }
      final int size = peer.getMaximumTransferUnit() - CustomPacket.MINIMUM_SIZE
        - EncapsulatedPacket.size(encapsulated.reliability, true);
      final byte[] src = encapsulated.payload.array();
      int payloadIndex = 0;
      int splitIndex = 0;
      final byte[][] split = new byte[(int) Math.ceil((float) src.length / (float) size)][size];
      while (payloadIndex < src.length) {
        if (payloadIndex + size <= src.length) {
          split[splitIndex++] = Arrays.copyOfRange(src, payloadIndex, payloadIndex + size);
          payloadIndex += size;
        } else {
          split[splitIndex++] = Arrays.copyOfRange(src, payloadIndex, src.length);
          payloadIndex = src.length;
        }
      }
      final EncapsulatedPacket[] splitPackets = new EncapsulatedPacket[split.length];
      for (int i = 0; i < split.length; i++) {
        final EncapsulatedPacket encapsulatedSplit = new EncapsulatedPacket();
        encapsulatedSplit.reliability = encapsulated.reliability;
        encapsulatedSplit.payload = new Packet(split[i]);
        encapsulatedSplit.messageIndex = encapsulated.reliability.isReliable() ? peer.bumpMessageIndex() : 0;
        if (encapsulated.reliability.isOrdered() || encapsulated.reliability.isSequenced()) {
          encapsulatedSplit.orderChannel = encapsulated.orderChannel;
          encapsulatedSplit.orderIndex = encapsulated.orderIndex;
        }
        encapsulatedSplit.split = true;
        encapsulatedSplit.splitCount = split.length;
        encapsulatedSplit.splitId = encapsulated.splitId;
        encapsulatedSplit.splitIndex = i;
        splitPackets[i] = encapsulatedSplit;
      }
      return splitPackets;
    }

    public Reliability getReliability() {
      return this.reliability;
    }

    @Override
    public String toString() {
      return "Split [splitId=" + this.splitId + ", splitCount=" + this.splitCount + ", reliability=" + this.reliability + "]";
    }

    public EncapsulatedPacket update(final EncapsulatedPacket encapsulated)
      throws NullPointerException, IllegalArgumentException {
      if (encapsulated == null) {
        throw new NullPointerException("Encapsulated packet cannot be null");
      } else if (encapsulated.split != true || encapsulated.splitId != this.splitId
        || encapsulated.splitCount != this.splitCount || encapsulated.reliability != this.reliability) {
        throw new IllegalArgumentException("This split packet does not belong to this one");
      } else if (encapsulated.splitIndex < 0 || encapsulated.splitIndex >= encapsulated.splitCount) {
        throw new IllegalArgumentException("Encapsulated packet split index out of range");
      } else if (this.payloads.containsKey(encapsulated.splitIndex)) {
        throw new IllegalArgumentException("Encapsulated packet with split index has already been registered");
      }
      this.payloads.put(encapsulated.splitIndex, encapsulated.payload);
      if (this.payloads.size() >= this.splitCount) {
        final Packet payload = new Packet();
        for (int i = 0; i < this.payloads.size(); i++) {
          payload.write(this.payloads.get(i).array());
        }
        this.payloads.clear();
        final EncapsulatedPacket stitched = new EncapsulatedPacket();
        stitched.ackRecord = null;
        stitched.reliability = encapsulated.reliability;
        stitched.split = false; // No longer split
        stitched.messageIndex = encapsulated.messageIndex;
        stitched.orderIndex = encapsulated.orderIndex;
        stitched.orderChannel = encapsulated.orderChannel;
        stitched.splitCount = encapsulated.splitCount;
        stitched.splitId = encapsulated.splitId;
        stitched.splitIndex = -1;
        stitched.payload = payload;
        return stitched;
      }
      return null;
    }
  }
}
