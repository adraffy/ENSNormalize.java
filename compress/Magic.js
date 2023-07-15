import {BitReader} from './BitReader.js';
import {BitWriter} from './BitWriter.js';
import {require_sorted, require_unsigned, collect_while} from './utils.js';

export class Magic {
	constructor(v) {
		require_sorted(v);
		let sum = 0;
		this.m = v.map(w => {
			let n = 1 << w;
			let a = sum;
			sum += n;
			return {w, n, a};
		});
	}
	get widths() {
		return this.m.map(x => x.w);
	}
	write(enc, x) {
		require_unsigned(x);
		for (let i = 0, {m} = this, last = m.length - 1; i <= last; i++) {
			let {w, n} = m[i];
			if (x < n) {
				enc.repeat(i, 1);
				if (i < last) enc.bit(0);
				enc.binary(x, w);
				return;
			}
			x -= n;
		}
		throw new TypeError('too big');
	}
	read(dec) {
		let i = 0;
		let {m} = this;
		while (i < m.length-1 && dec.bit()) i++;
		let {a, w} = m[i];
		return a + dec.binary(w);
	}
	bytes_from_symbols(symbols) {
		let w = new BitWriter();
		let prev = 0;
		for (let x of this.widths) {
			w.unary(x - prev); 
			prev = x;
		}
		w.unary(0);
		for (let x of symbols) {
			this.write(w, x);
		}
		return w.bytes;
	}
	static reader_from_bytes(bytes) {
		let r = new BitReader(bytes);
		let w = 0;
		let magic = new Magic(collect_while(() => {
			let dw = r.unary();
			if (dw) return w += dw;
		}));
		return () => magic.read(r);
	}
	// note: this is really inefficient
	static optimize(symbols, max, len = Infinity) {
		let min = Infinity;
		let ret;
		loop(len, 1, []);
		return ret;
		function loop(depth, start, path) {	
			if (depth == 0) return;
			for (let i = start; i <= max; i++) {		
				check([...path, i]);
				loop(depth - 1, i + 1, [...path, i]);
			}
		}
		function check(v) {
			try {
				let magic = new Magic(v);
				let buf = magic.bytes_from_symbols(symbols);
				if (buf.length < min) {
					min = buf.length;
					console.log(v.join(','), min);
					ret = [magic, buf];
				}
			} catch (err) {
			}
		}
	}
}