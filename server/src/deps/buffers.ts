import { Packets } from "../protocol/mod";

/**
 * Copies from the given buffer at a given offset and length.
 *
 * @param sourceBuffer The buffer to copy from.
 * @param offset The offset to copy from the sourceBuffer.
 * @param length The amount to copy from the sourceBuffer.
 * @return Returns a new buffer of length "length" containing the data copied from sourceBuffer.
 */
export function duplicateSlice(
    sourceBuffer: Buffer,
    offset: number,
    length: number
): Buffer {
    const resultBuffer = Buffer.allocUnsafe(length);
    sourceBuffer.copy(resultBuffer, 0, offset, offset + length);
    return resultBuffer;
}

/** Each read advances the internal offset into the buffer. */
export class BufReader {
    private readonly offsetStack: number[] = [];

    public constructor(
        public readonly buffer: Buffer,
        private offset: number = 0
    ) { }

    public saveOffset() {
        this.offsetStack.push(this.offset);
    }

    public restoreOffset() {
        const offset = this.offsetStack.pop();
        if (offset === undefined) {
            throw new Error("Offset stack is empty");
        }
        this.offset = offset;
    }

    public readUInt8(): number {
        const value = this.buffer.readUInt8(this.offset);
        this.offset += 1;
        return value;
    }

    public readInt8(): number {
        const value = this.buffer.readInt8(this.offset);
        this.offset += 1;
        return value;
    }

    public readUInt16(): number {
        const value = this.buffer.readUInt16BE(this.offset);
        this.offset += 2;
        return value;
    }

    public readInt16(): number {
        const value = this.buffer.readInt16BE(this.offset);
        this.offset += 2;
        return value;
    }

    public readUInt32(): number {
        const value = this.buffer.readUInt32BE(this.offset);
        this.offset += 4;
        return value;
    }

    public readInt32(): number {
        const value = this.buffer.readInt32BE(this.offset);
        this.offset += 4;
        return value;
    }

    public readUInt64(): number {
        const value = this.buffer.readBigUInt64BE(this.offset);
        if (value > Number.MAX_SAFE_INTEGER) {
            throw new Error(`64-bit number too big: ${value}`);
        }
        if (value < 0) {
            throw new Error(`64-bit number too small: ${value}`);
        }
        this.offset += 8;
        return Number(value);
    }

    public readInt64(): number {
        const value = this.buffer.readBigInt64BE(this.offset);
        if (value > Number.MAX_SAFE_INTEGER) {
            throw new Error(`64-bit number too big: ${value}`);
        }
        if (value < Number.MIN_SAFE_INTEGER) {
            throw new Error(`64-bit number too small: ${value}`);
        }
        this.offset += 8;
        return Number(value);
    }

    /** length-prefixed (32 bits), UTF-8 encoded */
    public readString(): string {
        const length = this.readUInt32();
        const value = this.buffer.toString("utf8", this.offset, this.offset + length);
        this.offset += length;
        return value;
    }

    public readBoolean(): boolean {
        return this.readUInt8() == 0b1;
    }

    public readBufWithLen(): Buffer {
        return this.readBufLen(
            this.readUInt32()
        );
    }

    public readBufLen(length: number): Buffer {
        const buffer = duplicateSlice(this.buffer, this.offset, length);
        this.offset += length;
        return buffer;
    }

    /** any reads after this will fail */
    public readRemainder(): Buffer {
        return this.readBufLen(this.buffer.length - this.offset);
    }
}

/** Each write advances the internal offset into the buffer.
 * Grows the buffer to twice the current size if a write would exceed the buffer. */
export class BufWriter {
    private offset = 0;
    private readonly operations = new Array<(buffer: Buffer) => void>();

    /** Returns a slice reference to the written bytes so far. */
    public getBuffer(): Buffer {
        const buffer = Buffer.allocUnsafe(this.offset);
        for (const operation of this.operations) {
            operation(buffer);
        }
        return buffer;
    }

    public writeUInt8(
        value: number
    ): this {
        const offset = this.offset;
        this.operations.push((buffer) => buffer.writeUInt8(value, offset));
        this.offset += 1;
        return this;
    }

    public writeInt8(
        value: number
    ): this {
        const offset = this.offset;
        this.operations.push((buffer) => buffer.writeInt8(value, offset));
        this.offset += 1;
        return this;
    }

    public writeUInt16(
        value: number
    ): this {
        const offset = this.offset;
        this.operations.push((buffer) => buffer.writeUInt16BE(value, offset));
        this.offset += 2;
        return this;
    }

    public writeInt16(
        value: number
    ): this {
        const offset = this.offset;
        this.operations.push((buffer) => buffer.writeInt16BE(value, offset));
        this.offset += 2;
        return this;
    }

    public writeUInt32(
        value: number
    ): this {
        const offset = this.offset;
        this.operations.push((buffer) => buffer.writeUInt32BE(value, offset));
        this.offset += 4;
        return this;
    }

    public writeInt32(
        value: number
    ): this {
        const offset = this.offset;
        this.operations.push((buffer) => buffer.writeInt32BE(value, offset));
        this.offset += 4;
        return this;
    }

    public writeUInt64(
        value: number
    ): this {
        const offset = this.offset;
        this.operations.push((buffer) => buffer.writeBigUInt64BE(BigInt(value), offset));
        this.offset += 8;
        return this;
    }

    public writeInt64(
        value: number
    ): this {
        const offset = this.offset;
        this.operations.push((buffer) => buffer.writeBigInt64BE(BigInt(value), offset));
        this.offset += 8;
        return this;
    }

    public writeBufRaw(
        value: Buffer
    ): this {
        const offset = this.offset;
        this.operations.push((buffer) => buffer.set(value, offset));
        this.offset += value.length;
        return this;
    }

    public writeBufWithLen(
        buffer: Buffer
    ): this {
        return this
            .writeUInt32(buffer.length)
            .writeBufRaw(buffer);
    }

    /**
     * Writes a UTF-8 encoded string prefixed with its length as a 32 bit int.
     */
    public writeString(
        value: string
    ): this {
        return this.writeBufWithLen(Buffer.from(value, "utf8"));
    }

    public writeBoolean(
        value: boolean
    ): this {
        return this.writeUInt8(value ? 0b1 : 0b0);
    }

    /**
     * Purely a convenience method to allow for functional chaining.
     */
    public writeMany(
        many: (builder: this) => void
    ): this {
        many(this);
        return this;
    }
}

export class PacketBuilder extends BufWriter {
    private constructor(
        public readonly packet: Packets
    ) {
        super();
    }

    /**
     * Returns the packet's length-prefixed buffer.
     */
    public override getBuffer(): Buffer {
        const data = super.getBuffer();
        const buffer = Buffer.allocUnsafe(data.length + 4);
        buffer.writeUInt32BE(data.length, 0);
        buffer.set(data, 4);
        return buffer;
    }

    /**
     * Starts building a new packet with the given id.
     */
    public static packet(
        packet: Packets
    ): PacketBuilder {
        return new PacketBuilder(packet)
            .writeUInt8(packet as number);
    }

    /**
     * Pretends that the given buffer is a PacketBuilder. You will not be able
     * to call write functions. This is merely a convenience function to send
     * buffers as-is without having to build a new packet.
     */
    public static cast(
        buffer: Buffer
    ): PacketBuilder {
        buffer = buffer.subarray();
        buffer["packet"] = buffer.readUint8(0);
        buffer["getBuffer"] = function getBuffer() {
            return this;
        };
        return buffer as unknown as PacketBuilder;
    }
}
