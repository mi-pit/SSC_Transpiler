class Variable:
    def __init__(self, var_type: str, pointer: bool, name: str):
        self.var_type = var_type
        self.pointer = pointer
        self.name = name

    def __str__(self):
        ptr_str = "*" if self.pointer else ""
        return self.var_type + " " + ptr_str + self.name
