// Copyright (c) 2008, Felix Hupfeld, Jan Stender, Bjoern Kolbeck, Mikael Hoegqvist, Zuse Institute Berlin.
// Licensed under the BSD License, see LICENSE file for details.

#ifndef LOGINDEX_H
#define LOGINDEX_H

#include <yield/platform/yunit.h>

#include "babudb/key.h"
#include "babudb/buffer.h"
#include "babudb/log/sequential_file.h"

#include <map>
using std::map;
#include <vector>
using std::vector;
#include <utility>

namespace babudb {

class LogSection;
class LogIndex;

class LogIndex {
public:
	LogIndex(const KeyOrder& order, lsn_t first) : order(order), latest_value(MapCompare(order)), first_lsn(first) {}

	Buffer lookup(const Buffer& key);
//	vector<std::pair<Buffer,Buffer> > search(Buffer value); // needs value comp. operator

	bool Add(const Buffer&, const Buffer&);

	lsn_t getFirstLSN()			{ return first_lsn; }

	typedef map<Buffer,Buffer,MapCompare> Tree;
	typedef Tree::const_iterator iterator;

	iterator begin() const { return latest_value.begin(); }
	iterator end() const	 { return latest_value.end(); }

	iterator find(const Buffer& key)
	{
		return latest_value.lower_bound(key);
	}

private:
	lsn_t first_lsn;

	Tree latest_value;
	const KeyOrder& order;
};

};

#endif