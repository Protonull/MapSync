import "./cli";
import * as database from "./database";
import * as metadata from "./metadata";
import { TcpServer } from "./server";

let config: metadata.Config = null!;
Promise.resolve().then(async () => {
    await database.setup();

    config = await metadata.getConfig();

    // These two are only used if whitelist is enabled... but best to load them
    // anyway lest there be a modification to them that is then saved.
    await metadata.whitelist_load();
    await metadata.uuid_cache_load();

    new TcpServer(config);
});
