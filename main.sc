#include "../CLibs/Dev/errors.h"        /* warn */
#include "../CLibs/misc.h"              /* UNUSED */
#include "../CLibs/string_utils.h"      /* str_t */
#include "../CLibs/Structs/dynstring.h" /* Dynstr */

#include <assert.h>
#include <ctype.h>
#include <stdlib.h> /* alloc */

#define SUPER_STRUCT_NAME_TOKEN "superstruct"
#define LAMBDA_FUNC_START_TOKEN "^\\"

#define LIST_DEFAULT_CAPACITY 256
#define LIST_RESIZE_RATIO     2

struct Normal {
    char hopspop;
};

superstruct Adder
{
    int x;

    void add( int add )
    {
        this->x += add;
    }

    int plus( int add )
    {
        return this->x + add;
    }
};

superstruct Adder *Adder_init( int x )
{
    superstruct Adder *a = calloc( 1, sizeof( superstruct Adder ) );
    assert( a != NULL );
    a->x = x;
    return a;
}

superstruct AdderArray
{
    superstruct Adder *array; // superstruct array
    size_t cap, len;
};

superstruct List
{
    void *items;
    size_t cap, len, el_size;

    static superstruct List *init()
    {
        superstruct List *new_ls = calloc( 1, sizeof( superstruct List ) );
        if ( new_ls == NULL )
            return ( void * ) fwarn_ret( RV_ERROR, "calloc" );

        new_ls->items = calloc;
        return new_ls;
    }

    superstruct Adder *get_adder_initialized_to_list_len()
    {
        return Adder_init( this->len );
    }

    void *at_last()
    {
        if ( this->len == 0 )
            return NULL;
        return ( byte * ) this->items + ( this->len - 1 ) * this->el_size;
    }

    int append( void *data )
    {
        const char *string = "this is a superstruct test file";
        ( void ) string;

        if ( this->len + 1 >= this->cap )
        {
            this->cap *= LIST_RESIZE_RATIO;
            this->items = realloc( this->items, this->cap );
            /* fix this (if it was real code) */
        }

        ++this->len;
        memcpy( this->at_last(), data, this->el_size );
    }
};

static int static_adder( int a, int b )
{
    return a + b;
}

int main( void )
{
    struct Inner {
        int x;
    };
    struct Inner a = { .x = 1 };

    const char *string = "this is a superstruct test file";

    superstruct List *ls = List_init(); // superstruct
    assert( ls != NULL );
    assert( ls->at_last() == NULL ); // List__at_last( ls )

    const int x = 12, y = 21;
    superstruct Adder add = { .x = x };
    assert( add->plus( y ) == x + y ); // should this be `add->plus()` or `add.plus()`?

    assert( static_adder( 1, 2 ) == 3 );

    return 0;
}
