import { bench, group } from "mitata";

group("buffer-concat", () => {
    {
        const [lhs, rhs] = createBuffer(
            (size) => Buffer.allocUnsafe(size),
            (buffer) => buffer,
        );
        bench("Buffer.concat()", () => {
            Buffer.concat([lhs, rhs]);
        });
    }

    {
        const [lhs, rhs] = createBuffer(
            (size) => Bun.allocUnsafe(size),
            (buffer) => buffer,
        );
        bench("Uint8Array.set()", () => {
            const result = new Uint8Array(lhs.byteLength + rhs.byteLength);
            result.set(lhs, 0);
            result.set(rhs, lhs.byteLength);
        });
    }

    {
        const [lhs, rhs] = createBuffer(
            (size) => new Uint8Array(size),
            (buffer) => buffer,
        );
        bench("Uint8Array.from()", () => {
            Uint8Array.from([...lhs, ...rhs]);
        });
    }

    {
        const [lhs, rhs] = createBuffer(
            (size) => new Uint8Array(size),
            (buffer) => buffer,
        );
        bench("new Blob().arrayBuffer()", async () => {
            await new Blob([lhs, rhs]).arrayBuffer();
        });
    }
});

/**
 * Creates two new buffers to test with.
 *
 * @param constructor Constructs a new buffer with a passed-in length.
 * @param asArrayBufferView Converts the constructed buffer into a ArrayBufferView just in case.
 */
function createBuffer<T>(
    constructor: (size: number) => T,
    asArrayBufferView: (buffer: T) => ArrayBufferView,
): [T, T] {
    const lhs = constructor(1024 * 1024 * 7);
    crypto.getRandomValues(asArrayBufferView(lhs));
    const rhs = constructor(1024 * 1024 * 6);
    crypto.getRandomValues(asArrayBufferView(rhs));
    return [lhs, rhs];
}
