package gjum.minecraft.mapsync.common.net;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Packet {
	public abstract void write(ByteBuf buf);

	public static byte[] readByteArray(final @NotNull ByteBuf buffer) {
		final var array = new byte[buffer.readInt()];
		buffer.readBytes(array);
		return array;
	}

	public static void writeByteArray(final @NotNull ByteBuf buffer,
									  final byte @NotNull [] array) {
		buffer.writeInt(array.length);
		buffer.writeBytes(array);
	}

	public static void writeString(final @NotNull ByteBuf buffer,
								   final @Nullable String string) {
		if (StringUtils.isEmpty(string)) {
			buffer.writeInt(0);
			return;
		}
		final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		writeByteArray(buffer, bytes);
	}

	public static @NotNull String readString(final @NotNull ByteBuf buffer) {
		return new String(readByteArray(buffer), StandardCharsets.UTF_8);
	}

}
