// 2006-06-04  Stephen M. Webb <stephen.webb@bregmasoft.com>
//
// Copyright (C) 2006 Free Software Foundation, Inc.
//
// This file is part of the GNU ISO C++ Library.  This library is free
// software; you can redistribute it and/or modify it under the
// terms of the GNU General Public License as published by the
// Free Software Foundation; either version 2, or (at your option)
// any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this library; see the file COPYING.  If not, write to the Free
// Software Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
// USA.

// 5.1.4.3 class template subtract_with_carry [tr.rand.eng.sub]
// 5.1.1 Table 16 line 2 seed ctor

#include <tr1/random>
#include <testsuite_hooks.h>

void
test01() 
{ 
  using namespace std::tr1;

  unsigned long seed = 2;
  subtract_with_carry<unsigned long, (1 << 24), 10, 24> x(seed);
}

int main()
{
  test01();
  return 0;
}
