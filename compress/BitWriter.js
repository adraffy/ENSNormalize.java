import {require_unsigned} from './utils.js';

export class BitWriter {
	constructor() {
		this._bytes = [];
		this._value = 0;
		this._width = 0;
	}
	unary(i) {
		require_unsigned(i);
		this.repeat(i, 1);
		this.bit(0);
	}
	binary(i, w) {
		for (let b = 1 << w-1; b > 0; b >>= 1) {
			this.bit(i & b);
		}
	}
	finish() {
		while (this._width) this.bit(0);
	}
	repeat(n, x) {
		require_unsigned(n);
		while (n--) this.bit(x);
	}
	bit(x) {
		let {_width, _value} = this;
		if (x) {
			_value |= 1 << _width;
		}
		if (++_width == 8) {
			this._bytes.push(_value);
			_width = _value = 0;
		}
		this._width = _width;
		this._value = _value;
	}
	get bytes() {
		this.finish();
		return Buffer.from(this._bytes);
	}
	get bits() {
		return (this._bytes.length << 3) + this._width;
	}
}