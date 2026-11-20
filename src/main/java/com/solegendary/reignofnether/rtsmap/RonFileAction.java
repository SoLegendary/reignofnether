package com.solegendary.reignofnether.rtsmap;

// what a RonFile packet carries; paths are relative to ron/ without ".mcfunction"
public enum RonFileAction {
    LIST,     // to ops: every ron/ file, "path\tcommands" per line; from the client: send it again
    READ,     // from the client: send this file's text back as CONTENT (empty if there's no such file)
    CONTENT,  // to ops: a file's text, as on disk now
    WRITE,    // from the client: save this text, then check it like VALIDATE
    DELETE,
    RUN,      // from the client: run the file now, as that player (/rtsapi run)
    VALIDATE, // from the client: check this text without saving it
    PROBLEMS, // to the client: a file's problems, "line\tmessage[\targ]" per line (message can be a lang key)
    LOG       // to ops: an error running a file, for the Editor's Console (it also goes to their chat)
}
