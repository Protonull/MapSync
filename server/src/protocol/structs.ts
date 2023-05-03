export interface CatchupChunk {
    dimension: string;
    chunk_x: number;
    chunk_z: number;
    ts: number;
}

export interface Pos2D {
    readonly x: number;
    readonly z: number;
}

export interface Timestamped {
    readonly timestamp: bigint;
}
