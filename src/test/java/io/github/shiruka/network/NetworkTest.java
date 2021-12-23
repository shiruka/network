package io.github.shiruka.network;

import io.github.shiruka.network.options.RakNetChannelOptions;
import io.github.shiruka.network.pipelines.UserDataCodec;
import io.github.shiruka.network.server.RakNetServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetSocketAddress;
import java.util.StringJoiner;
import org.jetbrains.annotations.NotNull;

final class NetworkTest {

  public static void main(final String[] args) throws Exception {
    final var serverInfo = new StringJoiner(";", "", ";")
      .add("MCPE")
      .add("Motd")
      .add("448")
      .add("1.17.11")
      .add("0")
      .add("10")
      .add("1100224433")
      .toString();
    new Thread(() -> {
      final var ioGroup = new NioEventLoopGroup();
      final var localhost = new InetSocketAddress("127.0.0.1", 19132);
      final var bootstrap = new ServerBootstrap()
        .group(ioGroup)
        .channel(RakNetServer.CHANNEL)
        .option(RakNetChannelOptions.SERVER_ID, 1100224433L)
        .option(RakNetChannelOptions.SERVER_IDENTIFIER, Identifier.simple(serverInfo))
        .childHandler(new ChannelInitializer<>() {
          @Override
          protected void initChannel(@NotNull final Channel ch) {
            ch.pipeline().addLast(UserDataCodec.NAME, new UserDataCodec(0xFE));
            ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
              @Override
              protected void channelRead0(final ChannelHandlerContext ctx, final ByteBuf msg) {
                System.out.println("User packet received!");
              }
            });
          }
        });
      bootstrap.bind(localhost).syncUninterruptibly();
    }).start();
    while (true) {
      Thread.sleep(5L);
    }
  }
}
