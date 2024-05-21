import { run } from "mitata";

await import("./buffer-concat.bench.ts");

await run({
    silent: false,
    avg: true,
    json: false,
    colors: true,
    min_max: true,
    percentiles: false,
});
