# Buglist

---

```c
typedef superstruct ‹name› {
    /* fields */
    ‹method›
} ‹TypedefName›;
```

results in

```c
typedef struct ‹name› {
    /* fields */
}

; ‹method› {...}

‹TypedefName›; /* <- problem */
```

Maybe disallow `typedef`s?

---

Postfix expression doesn't work recursively

e.g. `list->items_readonly()[0]` -> `DynamicArray__items_readonly(list)` (no `[0]`)

---

`superstruct SSName **` (more than one `*`) gets parsed as a `superstruct SSName *` local variable

---

(only when not assigned)

```ssc
superstruct DynamicArray *files; 
```

```
[DEBUG] Arrow in: files -> length ( )
[DEBUG] 	Variable is not superstruct
[DEBUG] 		local vars: []
```

---

```ssc
[DEBUG] Arrow in: ( this )->size
[DEBUG] 	Variable is not superstruct
[DEBUG] 		local vars: [SuperstructVariable[ssName=DynamicArray, pointer=true, name=this]]
```
