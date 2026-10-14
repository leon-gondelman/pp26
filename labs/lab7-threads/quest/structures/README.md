The four structures of the warm-up go here: every file of `Counter`, `Workers`,
`BoundedBuffer` and `Barrier`, finished or not (the skeletons compile). The quest compiles
them with `javac -d out *.java tracer/*.java structures/*.java`. A structure not finished
yet is taken from the sealed copies in `given/` with `--given counter`, `--given workers`,
`--given buffer` or `--given barrier` on any run (`--given counter,workers` for two).
