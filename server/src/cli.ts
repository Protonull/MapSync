import node_readline from "node:readline";
import node_stream from "node:stream";
import * as z from "zod";
import * as metadata from "./metadata";

type TermType = node_readline.Interface & {
    // idk where these come from lol
    output: node_stream.Writable;
    _refreshLine(): void;
};

const term = node_readline.createInterface({
    input: process.stdin,
    output: process.stdout
}) as TermType;

if (process.env["MAPSYNC_DUMB_TERM"] !== "true") {
    { // Adapted from https://stackoverflow.com/questions/10606814/readline-with-console-log-in-the-background/10608048#10608048
        const oldStdout = process.stdout;
        const newStdout = Object.create(oldStdout);
        const oldStderr = process.stderr;
        const newStderr = Object.create(oldStdout);
        function write_func(writable: node_stream.Writable) {
            return function write(this: node_stream.Writable) {
                term.output.write("\x1b[2K\r");
                const result = writable.write.apply(this, arguments as any);
                term._refreshLine();
                return result;
            };
        }
        newStdout.write = write_func(oldStdout);
        newStderr.write = write_func(oldStderr);
        Object.defineProperty(process, "stdout", {
            get() {
                return newStdout;
            }
        });
        Object.defineProperty(process, "stderr", {
            get() {
                return newStderr;
            }
        });
    }

    const old_log = console.log;
    console.log = function () {
        term.output.write("\x1b[2K\r");
        old_log.apply(this, arguments as any);
        term._refreshLine();
    };
    const old_error = console.error;
    console.error = function () {
        term.output.write("\x1b[2K\r");
        old_error.apply(this, arguments as any);
        term._refreshLine();
    };
}

async function handle_input(input: string): Promise<void> {
    const command_end_i = input.indexOf(" ");
    const command = command_end_i > -1 ? input.substring(0, command_end_i) : input;
    const extras = command_end_i > -1 ? input.substring(command_end_i + 1) : "";

    switch (command.toLowerCase()) {
        case "":
            break;
        case "ping":
            console.log("pong");
            break;
        case "help":
            console.log('ping - Prints "pong" for my sanity. -SirAlador');
            console.log("help - Prints info about commands, including the help command.");
            console.log("whitelist_load - Loads the whitelist from disk");
            console.log("whitelist_save - Saves the whitelist to disk");
            console.log("whitelist_add <uuid> - Adds the given account UUID to the\n    whitelist, and saves the whitelist to disk");
            console.log("whitelist_add_ign <ign> - Adds the UUID cached with the\n    given IGN to the whitelist, and saves the whitelist to disk");
            console.log("whitelist_remove <uuid> - Removes the given account UUID\n    from the whitelist, and saves the whitelist to disk");
            console.log("whitelist_remove_ign <ign> - Removes the UUID cached with\n    the given IGN from the whitelist, and saves the whitelist to disk");
            break;
        case "whitelist_load":
            await metadata.whitelist_load();
            break;
        case "whitelist_save":
            await metadata.whitelist_save();
            break;
        case "whitelist_add": {
            const parsed = z.string().uuid().safeParse(extras);
            if (!parsed.success) {
                throw new Error("Did not provide [a valid] UUID to whitelist");
            }
            metadata.whitelist.add(parsed.data);
            await metadata.whitelist_save();
            break;
        }
        case "whitelist_add_ign": {
            if (extras.length < 1) {
                throw new Error("Did not provide an IGN to whitelist");
            }
            const uuid = metadata.uuid_cache.get(extras) ?? null;
            if (uuid === null) {
                throw new Error("No cached UUID for IGN " + extras);
            }
            metadata.whitelist.add(uuid);
            await metadata.whitelist_save();
            break;
        }
        case "whitelist_remove": {
            const parsed = z.string().uuid().safeParse(extras);
            if (!parsed.success) {
                throw new Error("Did not provide [a valid] UUID to de-whitelist");
            }
            metadata.whitelist.delete(parsed.data);
            await metadata.whitelist_save();
            break;
        }
        case "whitelist_remove_ign": {
            if (extras.length < 1) {
                throw new Error("Did not provide an IGN to de-whitelist");
            }
            const uuid = metadata.uuid_cache.get(extras) ?? null;
            if (uuid === null) {
                throw new Error("No cached UUID for IGN " + extras);
            }
            metadata.whitelist.delete(uuid);
            await metadata.whitelist_save();
            break;
        }
        default:
            throw new Error(`Unknown command "${command}"`);
    }
}

function input_loop() {
    console.log("===========================================================");
    term.question(">", (input: string) =>
        handle_input(input.trim())
            .catch((err) => console.error("Command failed:", err))
            .finally(input_loop)
    );
}
input_loop();
