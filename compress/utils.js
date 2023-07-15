export function collect_runs(v) {
	let m = split_between(v, (a, b) => b - a > 1);
	let v1 = m.filter(x => x.length === 1).flat();
	m = m.filter(x => x.length > 1);
	let vX = m.map(x => x[0]);
	let vS = m.map(x => x.length);
	return {v1, vX, vS}
}

export function split_between(v, fn) {
	let ret = [];
	let start = 0;
	for (let i = 1; i < v.length; i++) {
		if (fn(v[i - 1], v[i])) {
			ret.push(v.slice(start, i));
			start = i;
		}
	}
	if (start < v.length) {
		ret.push(v.slice(start));
	}
	return ret;
}

export function same(a, b) {
	return JSON.stringify(a) === JSON.stringify(b);
}

export function partition(v, n) {
	let m = [];
	for (let i = 0; i < v.length; i += n) {
		m.push(v.slice(i, i + n));
	}
	return m; 
}

export function group_by(v, fn) {
	let map = new Map();
	for (let x of v) {
		let key = fn(x);
		let bucket = map.get(key);
		if (!bucket) {
			bucket = [];
			map.set(key, bucket);
		}
		bucket.push(x);
	}
	return [...map.values()];
}

export function transpose(m) {
	return m[0].map((_, i) => m.map(v => v[i]));
}

export function compare_arrays(a, b) {
	let n = a.length;
	let c = n - b.length;
	for (let i = 0; c == 0 && i < n; i++) c = a[i] - b[i];
	return c;
}

export function bit_width(x) {
	return 32 - Math.clz32(x);
}

export function from_signed(i) {
	return i < 0 ? ~(i << 1) : (i << 1)
}

export function to_signed(i) {
	return (i & 1) ? (~i >> 1) : (i >> 1);
}

export function require_sorted(v) {
	for (let i = 1; i < v.length; i++) {
		if (v[i] < v[i-1]) {
			throw new TypeError('not sorted');
		}
	}
}

export function require_unsigned(i) {
	if (!Number.isInteger(i) || i < 0) throw new TypeError(`expected unsigned: ${i}`);
}

export function read_str(next) {
	return String.fromCodePoint(...read_unsorted_deltas(next(), next));
}

export function read_unsorted_deltas(n, next) {
	return read_array(n, next, (prev, next) => prev + to_signed(next));
}

export function read_sorted_ascending(n, next) {
	return read_array(n, next, (next, prev) => prev + 1 + next);
}

export function read_array(n, next, fn) {
	let v = [];
	if (n) {
		let prev = -1;
		for (let i = 0; i < n; i++) {
			v.push(prev = fn(prev, next()));
		}
	}
	return v;
}

export function read_tree(next) {
	let ret = [];
	f([]);
	return ret.sort(compare_arrays);
	function f(v) {
		for (let x of read_sorted_ascending(next(), next)) {
			ret.push([...v, x]);
		}
		for (let x of read_sorted_ascending(next(), next)) {
			f([...v, x]);
		}	
	}
}

export function read_unique(next) {
	let v1 = read_sorted_ascending(next(), next);
	let n = next();
	let vX = read_sorted_ascending(n, next);
	let vS = read_unsorted_deltas(n, next);
	for (let i = 0; i < n; i++) {
		let x = vX[i];
		let s = vS[i];
		for (let j = 0; j < s; j++) {
			v1.push(x + j);
		}
	}
	return v1.sort((a, b) => a - b);
}

export function collect_while(fn) {
	let ret = [];
	while (true) {
		let temp = fn();
		if (!temp) break;
		ret.push(temp);
	}
	return ret;
}