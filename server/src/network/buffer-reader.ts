export class BufferReader {
    readonly #buffer: Buffer;
    #offset: number = 0;

    public constructor(internal: Buffer) {
        this.#buffer = internal;
    }

    get byteLength(): number {
        return this.#buffer.byteLength;
    }

    get position(): number {
        return this.#offset;
    }

    get remainder(): number {
        return Math.max(0, this.byteLength - this.position);
    }

    skip(bytes: number) {
        this.#offset += Math.max(0, bytes);
    }

    peekInt8(at?: number): number {
        return this.#buffer.readInt8(at ?? this.#offset);
    }

    peekInt16(at?: number): number {
        return this.#buffer.readInt16BE(at ?? this.#offset);
    }

    peekInt32(at?: number): number {
        return this.#buffer.readInt32BE(at ?? this.#offset);
    }

    peekInt64(at?: number): bigint {
        return this.#buffer.readBigInt64BE(at ?? this.#offset);
    }

    peekSlice(length: number, at?: number): Uint8Array {
        at = at ?? this.#offset;
        return this.#buffer.subarray(at, at + length);
    }

    pollInt8(): number {
        const value = this.#buffer.readInt8(this.#offset);
        this.#offset += 1;
        return value;
    }

    pollInt16(): number {
        const value = this.#buffer.readInt16BE(this.#offset);
        this.#offset += 2;
        return value;
    }

    pollInt32(): number {
        const value = this.#buffer.readInt32BE(this.#offset);
        this.#offset += 4;
        return value;
    }

    pollInt64(): bigint {
        const value = this.#buffer.readBigInt64BE(this.#offset);
        this.#offset += 8;
        return value;
    }

    pollSlice(length: number): Buffer {
        const slice = this.#buffer.subarray(
            this.#offset,
            this.#offset + length,
        );
        this.#offset += length;
        return slice;
    }

    readonly #stringDecoder = new TextDecoder("utf-8");
    pollString(length: number): string {
        return this.#stringDecoder.decode(
            this.pollSlice(length)
        );
    }
}
