class Variable:
    def __init__(self, specs, var_type: str, pointer: bool, name: str):
        self.specs: str = specs
        self.var_type: str = var_type
        self.pointer: bool = pointer
        self.name: str = name

    def __str__(self):
        ptr_str = "*" if self.pointer else ""
        return self.specs + self.var_type + " " + ptr_str + self.name
