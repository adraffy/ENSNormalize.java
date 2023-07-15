import {fileURLToPath} from 'node:url';
import {join} from 'node:path';
import {readFileSync, writeFileSync} from 'node:fs';
import {Encoder} from './Encoder.js';
import {Magic} from './Magic.js';
import {
	compare_arrays, transpose, group_by, collect_while, same, partition, 
	read_unique, read_unsorted_deltas, read_sorted_ascending, read_tree, read_str
} from './utils.js';
import { createHash } from 'node:crypto';

const BASE_DIR = fileURLToPath(new URL('.', import.meta.url));
const DATA_DIR = join(BASE_DIR, 'data');
const SPEC_FILE = join(DATA_DIR, 'spec.json');
const RESOURCES_DIR = join(BASE_DIR, '../lib/src/main/resources/');

const NF = JSON.parse(readFileSync(join(DATA_DIR, 'nf.json')));
const SPEC = JSON.parse(readFileSync(SPEC_FILE));

let decomp = NF.decomp.map(x => x.flat()).sort(compare_arrays);
let decomp1 = decomp.filter(x => x.length === 2);
let decomp2 = decomp.filter(x => x.length === 3);
let mapped = SPEC.mapped.map(x => x.flat()).sort(compare_arrays);

function bit_flags_from_group(g) {
	return (g.restricted?1:0) + (g.cm?2:0);
}

const w1 = new Encoder();
w1.str(NF.unicode.split(' ')[0]);
w1.unique(NF.exclusions);
w1.unique(NF.qc);

w1.unique(decomp1.map(x => x[0]));
w1.unsorted_deltas(decomp1.map(x => x[1]));
w1.unique(decomp2.map(x => x[0]));
w1.unsorted_deltas(decomp2.map(x => x[1]));
w1.unsorted_deltas(decomp2.map(x => x[2]));

for (let v of NF.ranks) {
	w1.unique(v);
}
w1.unique([]);

const w2 = new Encoder();
//w2.str(createHash('SHA256').)
w2.unique(SPEC.escape);
w2.unique(SPEC.ignored);
w2.unique(SPEC.cm);
w2.symbol(SPEC.nsm_max);
w2.unique(SPEC.nsm);
w2.unique(SPEC.nfc_check);

w2.symbol(SPEC.fenced.length)
w2.sorted_ascending(SPEC.fenced.map(x => x[0]));
SPEC.fenced.forEach(x => w2.str(x[1]));

for (let v of group_by(SPEC.mapped, x => x[1].length)) {
	let m = transpose(v.map(x => x[1]));
	w2.symbol(m.length);
	w2.unique(v.map(x => x[0]));
	for (let u of m) {
		w2.unsorted_deltas(u);
	}
}
w2.symbol(0);

for (let g of SPEC.groups) {
	w2.str(g.name);
	w2.symbol(bit_flags_from_group(g));
	w2.unique(g.primary);
	w2.unique(g.secondary);	
}
w2.str('');

w2.tree(SPEC.emoji);
for (let x of SPEC.wholes) {
	w2.unique(x.confused);
	w2.unique(x.valid);
}
w2.unique([]);

let magic1 = new Magic([2,6,8,11,14,15,18]);
let magic2 = new Magic([1,3,7,13,16,17,18,19]);
let bytes1 = magic1.bytes_from_symbols(w1.symbols);
let bytes2 = magic2.bytes_from_symbols(w2.symbols);

if (0) { // enable to recompute (very slow)
	[magic1, bytes1] = Magic.optimize(w1.symbols, 20); 
	[magic2, bytes2] = Magic.optimize(w2.symbols, 20);
}

console.log(`  NF: ${bytes1.length} using ${magic1.widths}`);
console.log(`Spec: ${bytes2.length} using ${magic2.widths}`);

function align_buf(buf, align) {
	let x = buf.length % align;
	if (x) buf = Buffer.concat([buf, Buffer.alloc(align - x)]);


	return buf;
}

writeFileSync(join(RESOURCES_DIR, 'nf.bin'), align_buf(bytes1, 4));
writeFileSync(join(RESOURCES_DIR, 'ensip.bin'), align_buf(bytes2, 4));

const r1 = Magic.reader_from_bytes(bytes1);
console.log(read_str(r1));
console.log(same(NF.exclusions, read_unique(r1)));
console.log(same(NF.qc, read_unique(r1)));

let decomp1A = read_unique(r1);
let decomp1B = read_unsorted_deltas(decomp1A.length, r1);
let decomp2A = read_unique(r1);
let decomp2B = read_unsorted_deltas(decomp2A.length, r1);
let decomp2C = read_unsorted_deltas(decomp2A.length, r1);
console.log(same(decomp, [
	...decomp1A.map((x, i) => [x, decomp1B[i]]),
	...decomp2A.map((x, i) => [x, decomp2B[i], decomp2C[i]]),
].sort(compare_arrays)))

console.log(same(NF.ranks, collect_while(() => {
	let v = read_unique(r1);
	if (v.length) return v;
})));

const r2 = Magic.reader_from_bytes(bytes2);
console.log(same(SPEC.escape, read_unique(r2)));
console.log(same(SPEC.ignored, read_unique(r2)));
console.log(same(SPEC.cm, read_unique(r2)));
console.log(same(SPEC.nsm_max, r2()));
console.log(same(SPEC.nsm, read_unique(r2)));
console.log(same(SPEC.nfc_check, read_unique(r2)));
console.log(same(SPEC.fenced, read_sorted_ascending(r2(), r2).map(x => [x, read_str(r2)])));

console.log(same(mapped, collect_while(() => {
	let w = r2();
	if (w) {
		let m = read_unique(r2).map(x => [x]);
		for (let i = 0; i < w; i++) {
			read_unsorted_deltas(m.length, r2).forEach((x, i) => m[i].push(x));
		}
		return m;
	}
}).flat().sort(compare_arrays)));

console.log(same(SPEC.groups.map(x => [
	x.name,
	bit_flags_from_group(x),
	x.primary,
	x.secondary
]), collect_while(() => {
	let name = read_str(r2);
	if (name) {
		return [name, r2(), read_unique(r2), read_unique(r2)];
	}
})));

console.log(same(SPEC.emoji, read_tree(r2)));
console.log(same(SPEC.wholes.map(x => [x.confused, x.valid]), collect_while(() => {
	let confused = read_unique(r2);
	if (confused.length) {
		let valid = read_unique(r2);
		return [confused, valid];
	}
})));
