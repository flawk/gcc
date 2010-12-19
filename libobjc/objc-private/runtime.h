/* GNU Objective C Runtime internal declarations
   Copyright (C) 1993, 1995, 1996, 1997, 2002, 2004, 2009 Free Software Foundation, Inc.
   Contributed by Kresten Krab Thorup

This file is part of GCC.

GCC is free software; you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation; either version 3, or (at your option) any later version.

GCC is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details.

Under Section 7 of GPL version 3, you are granted additional
permissions described in the GCC Runtime Library Exception, version
3.1, as published by the Free Software Foundation.

You should have received a copy of the GNU General Public License and
a copy of the GCC Runtime Library Exception along with this program;
see the files COPYING3 and COPYING.RUNTIME respectively.  If not, see
<http://www.gnu.org/licenses/>.  */

/* You need to include this file after including a number of standard ObjC files.

The original list was:

#include "objc/objc.h"
#include "objc/objc-api.h"
#include "objc/thr.h"
#include "objc/hash.h"
#include "objc/objc-list.h"

but can almost certainly be shrinked down.

Note that you can use this file both with objc/objc-api.h and with
objc/runtime.h.  */

#ifndef __objc_private_runtime_INCLUDE_GNU
#define __objc_private_runtime_INCLUDE_GNU

#include <stdarg.h>		/* for varargs and va_list's */

#include <stdio.h>
#include <ctype.h>

#include <stddef.h>		/* so noone else will get system versions */
#include <assert.h>

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

extern void __objc_add_class_to_hash(Class);   /* (objc-class.c) */
extern void __objc_init_selector_tables(void); /* (objc-sel.c) */
extern void __objc_init_class_tables(void);    /* (objc-class.c) */
extern void __objc_init_dispatch_tables(void); /* (objc-dispatch.c) */
extern void __objc_install_premature_dtable(Class); /* (objc-dispatch.c) */
extern void __objc_resolve_class_links(void);  /* (objc-class.c) */
extern void __objc_register_selectors_from_class(Class); /* (objc-sel.c) */
extern void __objc_register_selectors_from_list (struct objc_method_list *); /* (selector.c) */
extern void __objc_register_selectors_from_description_list
(struct objc_method_description_list *method_list); /* (selector.c) */
extern void __objc_update_dispatch_table_for_class (Class);/* (objc-msg.c) */

extern int  __objc_init_thread_system(void);    /* thread.c */
extern int  __objc_fini_thread_system(void);    /* thread.c */
extern void __objc_init_class (Class class);  /* init.c */
extern void class_add_method_list(Class, struct objc_method_list *);

/* Registering instance methods as class methods for root classes */
extern void __objc_register_instance_methods_to_class(Class);
extern struct objc_method * search_for_method_in_list(struct objc_method_list * list, SEL op);

extern void
__objc_update_classes_with_methods (struct objc_method *method_a, struct objc_method *method_b); /* class.c */

/* Number of selectors stored in each of the selector  tables */
extern unsigned int __objc_selector_max_index;

/* Mutex locking __objc_selector_max_index and its arrays. */
extern objc_mutex_t __objc_runtime_mutex;

/* Number of threads which are alive. */
extern int __objc_runtime_threads_alive;

#ifdef DEBUG
#define DEBUG_PRINTF(format, args...) printf (format, ## args)
#else
#define DEBUG_PRINTF(format, args...)
#endif 

BOOL __objc_responds_to (id object, SEL sel); /* for internal use only! */
extern void __objc_generate_gc_type_description (Class);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* not __objc_private_runtime_INCLUDE_GNU */
