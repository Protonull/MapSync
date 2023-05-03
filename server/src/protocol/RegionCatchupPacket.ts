import { BufReader } from "../deps/buffers";
import { Pos2D } from "./structs";

export interface RegionCatchupPacket {
    type: "RegionCatchup";
    dimension: string;
    regions: Pos2D[];
}

export namespace RegionCatchupPacket {
    export function decode(reader: BufReader): RegionCatchupPacket {
        let dimension = reader.readString();
        const len = reader.readInt16();
        const regions: Pos2D[] = [];
        for (let i = 0; i < len; i++) {
            regions.push({
                x: reader.readInt16(),
                z: reader.readInt16()
            });
        }
        return { type: "RegionCatchup", dimension, regions };
    }
}
