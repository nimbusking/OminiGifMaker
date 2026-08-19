# [gif.ski](https://gif.ski)

You'll find [gif.ski](https://gif.ski) executables in:

* `node_modules/gifski/bin/windows/gifski.exe`
* `node_modules/gifski/bin/macos/gifski`
* `node_modules/gifski/bin/debian/gifski` (for Debian [deb package](https://gif.ski/gifski-1.7.1.zip) is better)

This is just a binary package. You need to launch the binary using `child_process.spawn()` or similar.

```
gifski 1.7.1
https://gif.ski by Kornel Lesiński

USAGE:
    gifski [OPTIONS] --output <a.gif> <FILE>...

ARGS:
    <FILE>...    PNG image files

OPTIONS:
    -o, --output <a.gif>      Destination file to write to; "-" means stdout
    -r, --fps <num>           Frame rate of animation. This means the speed, as all frames are kept. [default: 20]
        --fast                50% faster encoding, but 10% worse quality and larger file size
        --extra               50% slower encoding, but 1% better quality
    -Q, --quality <1-100>     Lower quality may give smaller file [default: 90]
    -W, --width <px>          Maximum width.
                              By default anims are limited to about 800x600
    -H, --height <px>         Maximum height (stretches if the width is also set)
        --no-sort             Use files exactly in the order given, rather than sorted
    -q, --quiet               Do not display anything on standard output/console
        --repeat <num>        Number of times the animation is repeated (-1 none, 0 forever or
                              <value> repetitions
```

<img src="https://gif.ski/icon.png" width="128">
