package gjum.minecraft.mapsync.common.net.packet;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IPacket {
	default void write(@NotNull ByteBuf buffer) {
		throw new NotImplementedException();
	}

	static byte[] readByteArray(final @NotNull ByteBuf buffer) {
		final var array = new byte[buffer.readInt()];
		buffer.readBytes(array);
		return array;
	}

	static void writeByteArray(final @NotNull ByteBuf buffer,
							   final byte @NotNull [] array) {
		buffer.writeInt(array.length);
		buffer.writeBytes(array);
	}

	static void writeString(final @NotNull ByteBuf buffer,
							final @Nullable String string) {
		if (StringUtils.isEmpty(string)) {
			buffer.writeInt(0);
			return;
		}
		final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		writeByteArray(buffer, bytes);
	}

	static @NotNull String readString(final @NotNull ByteBuf buffer) {
		return new String(readByteArray(buffer), StandardCharsets.UTF_8);
	}
}
