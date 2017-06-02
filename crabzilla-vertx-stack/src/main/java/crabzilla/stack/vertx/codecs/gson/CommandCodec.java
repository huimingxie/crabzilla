package crabzilla.stack.vertx.codecs.gson;

import com.google.gson.Gson;
import crabzilla.model.Command;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

import javax.inject.Inject;

public class CommandCodec implements MessageCodec<Command, Command> {

  final Gson gson;

  @Inject
  public CommandCodec(Gson gson) {
    this.gson = gson;
  }

  @Override
  public void encodeToWire(Buffer buffer, Command command) {

    try {

      final String ajJson = gson.toJson(command, Command.class);

      // Length of JSON: is NOT characters count
      int length = ajJson.getBytes().length;

      // Write data into given buffer
      buffer.appendInt(length);
      buffer.appendString(ajJson);

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  @Override
  public Command decodeFromWire(int pos, Buffer buffer) {

    // My custom message starting from this *position* of buffer
    int _pos = pos;

    // Length of JSON
    int length = buffer.getInt(_pos);

    // Get JSON string by it`s length
    // Jump 4 because getInt() == 4 bytes
    final String jsonStr = buffer.getString(_pos += 4, _pos += length);

    return gson.fromJson(jsonStr, Command.class);

  }

  @Override
  public Command transform(Command command) {
    return command;
  }

  @Override
  public String name() {
    // Each codec must have a unique name.
    // This is used to identify a codec when sending a message and for unregistering codecs.
    return this.getClass().getSimpleName();
  }

  @Override
  public byte systemCodecID() {
    // Always -1
    return -1;
  }

}
