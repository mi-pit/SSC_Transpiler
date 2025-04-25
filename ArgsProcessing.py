import sys


class CommandLineArgs:
    def __init__(self, files=None, structs=None):
        if files is None:
            files = []
        if structs is None:
            structs = set()
        self.files: list[str] = files
        self.structs: set[str] = structs

    def __str__(self) -> str:
        return f"{{ {', '.join(self.files)} | {', '.join(self.structs) if self.structs else '‹›'} }}"


def process_argv() -> CommandLineArgs:
    opts = CommandLineArgs()
    # print(sys.argv[1:])
    for arg in sys.argv[1:]:
        if arg.startswith("-"):  # option
            if arg.startswith("--structs="):
                structs_strings: str = arg.split("=")[1]
                structs_ls: list[str] = structs_strings.split(",")
                opts.structs.update(structs_ls)
            else:
                print("Invalid option: " + arg, file=sys.stderr)
        else:
            opts.files.append(arg)

    if not opts.files:
        raise FileNotFoundError("No file given")

    return opts
