export class BitReader {
	constructor(buf) {
		this._buf = buf;
		this._pos = 0;
		this._value = 0;
		this._width = 0;
	}
	bit() {
		let {_width, _value} = this;
		if (_width == 0) {
			_value = this._buf[this._pos++];
			_width = 8;
		}
		this._value = _value >> 1;
		this._width = _width - 1;
		return (_value & 1) > 0;
	}
	unary() {
		let x = 0;
		while (this.bit()) x++;
		return x;
	}
	binary(w) {
		let x = 0;
		while (w--) {
			x |= this.bit() << w;
		}
		return x;
	}
}