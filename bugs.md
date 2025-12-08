# Buglist

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

# TODO: check

1. `superstruct ‹no name› ...`
