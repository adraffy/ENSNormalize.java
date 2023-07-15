import {from_signed, require_sorted, require_unsigned, collect_runs} from './utils.js';

export class Encoder {	
	constructor() {
		this.symbols = [];
	}
	reset() {
		this.symbols.length = 0;
	}
	str(s) {
		let v = Array.from(s, x => x.codePointAt(0));
		this.symbol(v.length);
		this.unsorted_deltas(v);
	}
	symbol(i) {
		require_unsigned(i);
		this.symbols.push(i);
	}
	unique(v) {
		require_sorted(v);
		let {v1, vX, vS} = collect_runs(v);
		this.symbol(v1.length);
		this.sorted_ascending(v1);
		this.symbol(vX.length);
		this.sorted_ascending(vX);
		this.unsorted_deltas(vS);
	}
	sorted_ascending(v) {
		this.array(v, (prev, next) => next - prev - 1);
	}
	unsorted_deltas(v) {
		this.array(v, (prev, next) => from_signed(next - prev));
	}
	array(v, fn) {
		let n = v.length;
		if (!n) return;
		let prev = -1;
		for (let i = 0; i < n; i++) {
			let next = v[i];
			this.symbol(fn(prev, next));
			prev = next;
		}
	}
	tree(m, depth = 0) {
		let buckets = new Map();
		let leaves = [];
		for (let v of m) {
			let key = v[depth];
			if (depth === v.length - 1) {
				leaves.push(key);
			} else {
				let bucket = buckets.get(key);
				if (!bucket) {
					bucket = [];
					buckets.set(key, bucket);
				}
				bucket.push(v);
			}
		}
		buckets = [...buckets].sort((a, b) => a[0] - b[0]);
		this.symbol(leaves.length);
		this.sorted_ascending(leaves);
		this.symbol(buckets.length);
		this.sorted_ascending(buckets.map(x => x[0]));
		for (let x of buckets) {
			this.tree(x[1], depth + 1);
		}
	}
}